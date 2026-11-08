package ir.zagroserp.pda.data.remote.interceptor

import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.remote.api.TokenRefreshApi
import ir.zagroserp.pda.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Handles `401 Unauthorized` by calling `POST /auth/refresh` exactly once and replaying the
 * original request with the new access token. Concurrent 401s share a single refresh via a mutex.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    private val refreshApi: Provider<TokenRefreshApi>,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        return runBlocking {
            mutex.withLock {
                val current = sessionStore.accessTokenBlocking()

                // Another thread already refreshed while we were waiting for the lock.
                if (!current.isNullOrBlank() && current != failedToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }

                val refreshToken = sessionStore.refreshTokenBlocking()
                if (refreshToken.isNullOrBlank()) {
                    sessionStore.clear()
                    return@withLock null
                }

                val result = runCatching {
                    refreshApi.get().refresh(RefreshRequest(refreshToken))
                }.getOrNull()

                val body = result?.body()
                if (result?.isSuccessful == true && body != null && body.token.isNotBlank()) {
                    sessionStore.updateTokens(
                        accessToken = body.token,
                        refreshToken = body.refreshToken,
                        expiresInSeconds = body.expiresIn,
                    )
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.token}")
                        .build()
                } else {
                    if (result?.code() == 401 || result?.code() == 403) {
                        sessionStore.clear()
                    }
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
