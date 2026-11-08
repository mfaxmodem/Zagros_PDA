package ir.zagroserp.pda.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.core.ServerAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the per-company server address. Every company installs the same APK and points it at
 * its own host (`http(s)://<host>:<port>/api/v1`), so the base URL is runtime configuration and
 * never a build-time constant.
 *
 * A volatile mirror keeps the OkHttp interceptor non-suspending.
 */
@Singleton
class ServerConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedOrigin: String? = null

    private object Keys {
        val BASE_URL = stringPreferencesKey("server_base_url")
        val ORIGIN = stringPreferencesKey("server_origin")
    }

    /** `true` once the visitor has confirmed a server address. */
    val isConfigured: Flow<Boolean> = dataStore.data.map { !it[Keys.BASE_URL].isNullOrBlank() }

    val baseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.BASE_URL] ?: BuildConfig.BASE_URL
    }

    val origin: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.ORIGIN] ?: BuildConfig.ORIGIN_URL
    }

    fun baseUrlBlocking(): String = cachedBaseUrl ?: BuildConfig.BASE_URL

    fun originBlocking(): String = cachedOrigin ?: BuildConfig.ORIGIN_URL

    suspend fun currentBaseUrl(): String = dataStore.data.first()[Keys.BASE_URL] ?: BuildConfig.BASE_URL

    /** Loads the cached mirrors. Must run before the first network call of a process. */
    suspend fun warmUp() {
        val prefs = dataStore.data.first()
        prefs[Keys.BASE_URL]?.let {
            cachedBaseUrl = it
            cachedOrigin = prefs[Keys.ORIGIN] ?: ServerAddress.originOf(it)
        }
    }

    /** Returns true when the address actually changed (callers must then drop the session). */
    suspend fun save(rawAddress: String): Boolean {
        val normalized = ServerAddress.normalize(rawAddress) ?: return false
        val previous = dataStore.data.first()[Keys.BASE_URL]
        val origin = ServerAddress.originOf(normalized)

        cachedBaseUrl = normalized
        cachedOrigin = origin

        dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = normalized
            prefs[Keys.ORIGIN] = origin
        }
        return previous != null && previous != normalized
    }

    suspend fun clear() {
        cachedBaseUrl = null
        cachedOrigin = null
        dataStore.edit { prefs ->
            prefs.remove(Keys.BASE_URL)
            prefs.remove(Keys.ORIGIN)
        }
    }
}
