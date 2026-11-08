package ir.zagroserp.pda.data.remote.interceptor

import ir.zagroserp.pda.core.ServerAddress
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites every outgoing request onto the server address configured by the visitor.
 *
 * Retrofit is built once against a placeholder base URL; this interceptor swaps the scheme, host,
 * port and `/api/v1` prefix at call time. That keeps a single Hilt-scoped Retrofit instance while
 * still allowing each company to point the app at its own host without a rebuild.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val serverConfig: ServerConfigStore,
) : Interceptor {

    companion object {
        /**
         * Set on requests that must keep their own absolute URL — e.g. the "test connection"
         * probe, which targets an address the visitor has not saved yet.
         */
        const val HEADER_SKIP = "X-Skip-Base-Url"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.header(HEADER_SKIP) != null) {
            return chain.proceed(request.newBuilder().removeHeader(HEADER_SKIP).build())
        }

        val rewritten = ServerAddress.rewrite(request.url, serverConfig.baseUrlBlocking())
            ?: return chain.proceed(request)

        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
