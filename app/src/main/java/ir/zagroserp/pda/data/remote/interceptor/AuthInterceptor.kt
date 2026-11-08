package ir.zagroserp.pda.data.remote.interceptor

import ir.zagroserp.pda.data.prefs.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects `Authorization: Bearer <JWT>` into every request that is not explicitly anonymous.
 * A request is anonymous when it already carries an Authorization header or sets
 * [AuthInterceptor.HEADER_ANONYMOUS].
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {

    companion object {
        const val HEADER_ANONYMOUS = "X-Anonymous"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER_ANONYMOUS) != null) {
            return chain.proceed(request.newBuilder().removeHeader(HEADER_ANONYMOUS).build())
        }
        if (request.header("Authorization") != null) return chain.proceed(request)

        val token = sessionStore.accessTokenBlocking()
        return if (token.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build(),
            )
        }
    }
}
