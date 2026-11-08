package ir.zagroserp.pda.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val fullName: String,
    val avatarUrl: String,
    val visitorId: String,
    val username: String,
) {
    val isExpired: Boolean
        get() = expiresAtEpochSeconds > 0 && System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds

    /** True once 80% of the token lifetime has elapsed – the trigger for a proactive refresh. */
    fun needsProactiveRefresh(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        if (expiresAtEpochSeconds <= 0) return false
        return nowEpochSeconds >= expiresAtEpochSeconds - 60
    }
}

/**
 * Single source of truth for the JWT session, the delta-sync cursor and user preferences.
 * A volatile in-memory mirror of the access token exists so OkHttp interceptors can read it
 * without suspending.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val FULL_NAME = stringPreferencesKey("full_name")
        val AVATAR = stringPreferencesKey("avatar_url")
        val VISITOR_ID = stringPreferencesKey("visitor_id")
        val USERNAME = stringPreferencesKey("username")
        val SERVER_TIME = stringPreferencesKey("server_time")
        val THEME = stringPreferencesKey("theme_mode")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SELECTED_TOUR = stringPreferencesKey("selected_tour")
        val NEXT_ORDER_SEQ = intPreferencesKey("next_order_seq")
        val PUSH_TOKEN = stringPreferencesKey("push_token")
        val PUSH_REGISTERED = booleanPreferencesKey("push_token_registered")
        val PUSH_ERROR = stringPreferencesKey("push_token_error")
    }

    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[Keys.ACCESS] ?: return@map null
        Session(
            accessToken = token,
            refreshToken = prefs[Keys.REFRESH].orEmpty(),
            expiresAtEpochSeconds = prefs[Keys.EXPIRES_AT] ?: 0L,
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            avatarUrl = prefs[Keys.AVATAR].orEmpty(),
            visitorId = prefs[Keys.VISITOR_ID].orEmpty(),
            username = prefs[Keys.USERNAME].orEmpty(),
        )
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { !it[Keys.ACCESS].isNullOrBlank() }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    /** FCM registration token, once Google has issued one. */
    val pushToken: Flow<String?> = dataStore.data.map { it[Keys.PUSH_TOKEN] }

    /** Whether the current token has been accepted by `POST /mobile/visitor/devices`. */
    val pushTokenRegistered: Flow<Boolean> = dataStore.data.map { it[Keys.PUSH_REGISTERED] ?: false }

    /** Why the last token request failed, surfaced in the settings diagnostics. */
    val pushError: Flow<String?> = dataStore.data.map { it[Keys.PUSH_ERROR] }

    val serverTime: Flow<String?> = dataStore.data.map { it[Keys.SERVER_TIME] }
    val lastSyncAt: Flow<Long> = dataStore.data.map { it[Keys.LAST_SYNC_AT] ?: 0L }
    val onboarded: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun cachedSession(): Session? {
        val prefs = dataStore.data.first()
        val token = prefs[Keys.ACCESS] ?: return null
        return Session(
            accessToken = token,
            refreshToken = prefs[Keys.REFRESH].orEmpty(),
            expiresAtEpochSeconds = prefs[Keys.EXPIRES_AT] ?: 0L,
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            avatarUrl = prefs[Keys.AVATAR].orEmpty(),
            visitorId = prefs[Keys.VISITOR_ID].orEmpty(),
            username = prefs[Keys.USERNAME].orEmpty(),
        )
    }

    fun accessTokenBlocking(): String? = cachedToken

    fun refreshTokenBlocking(): String? = cachedRefreshToken

    suspend fun warmUp() {
        cachedSession()?.let {
            cachedToken = it.accessToken
            cachedRefreshToken = it.refreshToken
        }
    }

    suspend fun saveLogin(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        fullName: String,
        avatarUrl: String,
    ) {
        val expiresAt = System.currentTimeMillis() / 1000 + expiresInSeconds
        cachedToken = accessToken
        cachedRefreshToken = refreshToken
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            if (refreshToken.isNotBlank()) prefs[Keys.REFRESH] = refreshToken
            prefs[Keys.EXPIRES_AT] = expiresAt
            prefs[Keys.FULL_NAME] = fullName
            prefs[Keys.AVATAR] = avatarUrl
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() / 1000 + expiresInSeconds
        cachedToken = accessToken
        if (refreshToken.isNotBlank()) cachedRefreshToken = refreshToken
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            if (refreshToken.isNotBlank()) prefs[Keys.REFRESH] = refreshToken
            prefs[Keys.EXPIRES_AT] = expiresAt
        }
    }

    suspend fun saveVisitor(visitorId: String, username: String, fullName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.VISITOR_ID] = visitorId
            prefs[Keys.USERNAME] = username
            if (fullName.isNotBlank()) prefs[Keys.FULL_NAME] = fullName
        }
    }

    suspend fun currentPushToken(): String? = dataStore.data.first()[Keys.PUSH_TOKEN]

    suspend fun savePushToken(token: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PUSH_TOKEN] = token
            prefs[Keys.PUSH_REGISTERED] = false
            prefs.remove(Keys.PUSH_ERROR)
        }
    }

    suspend fun markPushTokenRegistered(registered: Boolean, error: String? = null) {
        dataStore.edit { prefs ->
            prefs[Keys.PUSH_REGISTERED] = registered
            if (error.isNullOrBlank()) prefs.remove(Keys.PUSH_ERROR) else prefs[Keys.PUSH_ERROR] = error
        }
    }

    suspend fun savePushError(message: String?) {
        dataStore.edit { prefs ->
            if (message.isNullOrBlank()) prefs.remove(Keys.PUSH_ERROR) else prefs[Keys.PUSH_ERROR] = message
        }
    }

    suspend fun clearPushToken() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PUSH_TOKEN)
            prefs.remove(Keys.PUSH_REGISTERED)
            prefs.remove(Keys.PUSH_ERROR)
        }
    }

    suspend fun saveServerTime(serverTime: String) {
        if (serverTime.isBlank()) return
        dataStore.edit { prefs ->
            prefs[Keys.SERVER_TIME] = serverTime
            prefs[Keys.LAST_SYNC_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOnboarded(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDED] = value }
    }

    suspend fun setSelectedTour(tourId: String?) {
        dataStore.edit { prefs ->
            if (tourId == null) prefs.remove(Keys.SELECTED_TOUR) else prefs[Keys.SELECTED_TOUR] = tourId
        }
    }

    suspend fun nextLocalSequence(): Int {
        var next = 1
        dataStore.edit { prefs ->
            next = (prefs[Keys.NEXT_ORDER_SEQ] ?: 1)
            prefs[Keys.NEXT_ORDER_SEQ] = next + 1
        }
        return next
    }

    suspend fun clear() {
        cachedToken = null
        cachedRefreshToken = null
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS)
            prefs.remove(Keys.REFRESH)
            prefs.remove(Keys.EXPIRES_AT)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.AVATAR)
            prefs.remove(Keys.VISITOR_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.SERVER_TIME)
            prefs.remove(Keys.LAST_SYNC_AT)
        }
    }

    suspend fun clearAll() {
        cachedToken = null
        cachedRefreshToken = null
        dataStore.edit { it.clear() }
    }
}
