package ir.zagroserp.pda.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PushState(
    val token: String? = null,
    val registered: Boolean = false,
    val error: String? = null,
) {
    val hasToken: Boolean get() = !token.isNullOrBlank()

    /** Short form for the diagnostics row – the full token is useless to a human. */
    val tokenPreview: String
        get() = token?.let { if (it.length <= 14) it else "${it.take(8)}…${it.takeLast(4)}" }.orEmpty()

    val statusLabel: String
        get() = when {
            hasToken && registered -> "فعال"
            hasToken -> "دریافت شد، ثبت نشد"
            else -> "غیرفعال"
        }
}

/**
 * Owns the FCM registration token.
 *
 * Token acquisition is the point where an unsupported environment shows up: without Google Play
 * Services, or without a route to Google's servers, `FirebaseMessaging.getToken()` throws. That
 * failure is captured rather than swallowed so the settings screen can say plainly whether push
 * is working — which is exactly the question this build is meant to answer.
 */
@Singleton
class PushTokenManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) {

    val state: Flow<PushState> = combine(
        sessionStore.pushToken,
        sessionStore.pushTokenRegistered,
        sessionStore.pushError,
    ) { token, registered, error ->
        PushState(token = token, registered = registered, error = error)
    }

    /**
     * Asks Google for the token and hands it to the server.
     * Safe to call repeatedly; it is a no-op once the token is registered.
     */
    suspend fun syncToken(force: Boolean = false): PushState {
        val existing = sessionStore.currentPushToken()
        if (!force && !existing.isNullOrBlank()) {
            register(existing)
            return PushState(existing, sessionStore.pushTokenRegistered.first())
        }

        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (t: Throwable) {
            val reason = t.message ?: t::class.java.simpleName
            sessionStore.savePushError(
                "دریافت توکن اعلان ناموفق بود: $reason\n" +
                    "این معمولاً یعنی Google Play Services روی دستگاه نصب نیست " +
                    "یا دسترسی به سرورهای گوگل برقرار نیست.",
            )
            return PushState(null, false, sessionStore.pushError.first())
        }

        if (token.isBlank()) {
            sessionStore.savePushError("گوگل توکن خالی برگرداند")
            return PushState(null, false, sessionStore.pushError.first())
        }

        sessionStore.savePushToken(token)
        register(token)
        return PushState(token, sessionStore.pushTokenRegistered.first(), sessionStore.pushError.first())
    }

    /** Called by [ZagrosMessagingService] when Google rotates the token. */
    suspend fun onNewToken(token: String) {
        sessionStore.savePushToken(token)
        register(token)
    }

    /** Best effort removal on logout, then forget the token locally. */
    suspend fun unregister() {
        val token = sessionStore.currentPushToken()
        if (!token.isNullOrBlank()) {
            runCatching { authRepository.unregisterPushToken(token) }
        }
        sessionStore.clearPushToken()
    }

    private suspend fun register(token: String) {
        when (val result = authRepository.registerPushToken(token)) {
            is ir.zagroserp.pda.core.AppResult.Success ->
                sessionStore.markPushTokenRegistered(true)

            is ir.zagroserp.pda.core.AppResult.Failure ->
                sessionStore.markPushTokenRegistered(false, result.error.message)
        }
    }
}
