package ir.zagroserp.pda.core

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normalises whatever a company types into a canonical API base URL.
 *
 * Accepted inputs (all resolve to `…/api/v1/`):
 *  * `dalbara.ir`
 *  * `https://dalbara.ir`
 *  * `https://dalbara.ir/api/v1`
 *  * `http://192.168.1.50:8080`
 *  * `192.168.1.50:8080/api/v1`
 *
 * The `/api/v1` suffix is added when missing, because the on-premise deployments are reached as
 * `http(s)://<host>:<port>/api/v1` while the domain deployment is `https://<domain>/api/v1`.
 */
object ServerAddress {

    const val API_PATH = "/api/v1"

    /** Canonical base URL ending with `/`, or null when the input cannot be parsed. */
    fun normalize(raw: String): String? {
        // Scheme detection must happen before trailing slashes are stripped, otherwise
        // "http://" degrades into "http:/" and gets treated as a host name.
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }

        // `toHttpUrlOrNull` already guarantees a well-formed, hierarchical http(s) URL.
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.host.isBlank()) return null

        val path = parsed.encodedPath.trimEnd('/')
        val apiPath = if (path.endsWith(API_PATH)) path else path + API_PATH

        val defaultPort = HttpUrl.defaultPort(parsed.scheme)
        val portPart = if (parsed.port == defaultPort) "" else ":${parsed.port}"

        return "${parsed.scheme}://${parsed.host}$portPart$apiPath/"
    }

    /** Scheme + host + port + `/` — used to build absolute product image URLs (API 4.6.1). */
    fun originOf(baseUrl: String): String {
        val parsed = baseUrl.toHttpUrlOrNull() ?: return baseUrl
        val defaultPort = HttpUrl.defaultPort(parsed.scheme)
        val portPart = if (parsed.port == defaultPort) "" else ":${parsed.port}"
        return "${parsed.scheme}://${parsed.host}$portPart/"
    }

    /**
     * Re-points [requestUrl] (built by Retrofit against the placeholder base) onto [baseUrl],
     * preserving the relative path and the query string.
     *
     * Extracted from the interceptor so it can be unit tested without a network stack.
     */
    fun rewrite(requestUrl: HttpUrl, baseUrl: String): HttpUrl? {
        val target = baseUrl.toHttpUrlOrNull() ?: return null

        val encodedPath = requestUrl.encodedPath
        val relative = if (encodedPath.startsWith(API_PATH)) {
            encodedPath.removePrefix(API_PATH).trimStart('/')
        } else {
            encodedPath.trimStart('/')
        }

        // Never force a trailing slash: `…/products` and `…/products/` are different resources.
        val targetPrefix = target.encodedPath.trimEnd('/')
        val newPath = if (relative.isEmpty()) "$targetPrefix/" else "$targetPrefix/$relative"

        return requestUrl.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(newPath)
            .build()
    }

    /** Human friendly rendering for the settings screen. */
    fun display(baseUrl: String): String =
        baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')

    fun isSecure(baseUrl: String): Boolean =
        baseUrl.startsWith("https://", ignoreCase = true)

    /** Validates without throwing; returns a Persian message when invalid. */
    fun validate(raw: String): String? = when {
        raw.isBlank() -> "آدرس سرور را وارد کنید"
        normalize(raw) == null -> "آدرس نامعتبر است. نمونه: https://dalbara.ir یا http://192.168.1.50:8080"
        else -> null
    }
}
