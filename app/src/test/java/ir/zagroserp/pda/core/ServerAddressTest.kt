package ir.zagroserp.pda.core

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressTest {

    @Test
    fun `a bare domain gets https-less scheme and the api suffix`() {
        assertEquals("http://dalbara.ir/api/v1/", ServerAddress.normalize("dalbara.ir"))
    }

    @Test
    fun `https domain keeps its scheme`() {
        assertEquals("https://dalbara.ir/api/v1/", ServerAddress.normalize("https://dalbara.ir"))
    }

    @Test
    fun `an existing api suffix is not duplicated`() {
        assertEquals(
            "https://dalbara.ir/api/v1/",
            ServerAddress.normalize("https://dalbara.ir/api/v1"),
        )
        assertEquals(
            "https://dalbara.ir/api/v1/",
            ServerAddress.normalize("https://dalbara.ir/api/v1/"),
        )
    }

    @Test
    fun `on-premise ip with port is preserved`() {
        assertEquals(
            "http://192.168.1.50:8080/api/v1/",
            ServerAddress.normalize("http://192.168.1.50:8080"),
        )
        assertEquals(
            "http://192.168.1.50:8080/api/v1/",
            ServerAddress.normalize("192.168.1.50:8080/api/v1"),
        )
    }

    @Test
    fun `default ports are dropped from the canonical form`() {
        assertEquals("https://dalbara.ir/api/v1/", ServerAddress.normalize("https://dalbara.ir:443"))
        assertEquals("http://dalbara.ir/api/v1/", ServerAddress.normalize("http://dalbara.ir:80"))
    }

    @Test
    fun `a nested deployment path is preserved`() {
        assertEquals(
            "https://company.ir/zagros/api/v1/",
            ServerAddress.normalize("https://company.ir/zagros"),
        )
    }

    @Test
    fun `trailing whitespace and slashes are tolerated`() {
        assertEquals(
            "https://dalbara.ir/api/v1/",
            ServerAddress.normalize("  https://dalbara.ir/  "),
        )
    }

    @Test
    fun `invalid input is rejected`() {
        assertNull(ServerAddress.normalize(""))
        assertNull(ServerAddress.normalize("   "))
        assertNull(ServerAddress.normalize("http://"))
        assertNotNull(ServerAddress.validate(""))
        assertNotNull(ServerAddress.validate("http://"))
        assertNull(ServerAddress.validate("dalbara.ir"))
    }

    @Test
    fun `origin never contains the api path`() {
        val origin = ServerAddress.originOf("http://192.168.1.50:8080/api/v1/")
        assertEquals("http://192.168.1.50:8080/", origin)
        assertFalse(origin.contains("/api"))
    }

    @Test
    fun `https detection drives the security badge`() {
        assertTrue(ServerAddress.isSecure("https://dalbara.ir/api/v1/"))
        assertFalse(ServerAddress.isSecure("http://192.168.1.50:8080/api/v1/"))
    }

    // ---- request rewriting (BaseUrlInterceptor) ----------------------------

    private val placeholder = "https://dalbara.ir/api/v1/".toHttpUrl()

    @Test
    fun `a request is re-pointed at the configured host`() {
        val rewritten = ServerAddress.rewrite(
            requestUrl = (placeholder.toString() + "mobile/visitor/products?page=2").toHttpUrl(),
            baseUrl = "http://192.168.1.50:8080/api/v1/",
        )
        assertEquals(
            "http://192.168.1.50:8080/api/v1/mobile/visitor/products?page=2",
            rewritten.toString(),
        )
    }

    @Test
    fun `query parameters survive the rewrite`() {
        val rewritten = ServerAddress.rewrite(
            requestUrl = (placeholder.toString() + "mobile/visitor/sync?since=2026-09-19T10%3A00%3A00Z")
                .toHttpUrl(),
            baseUrl = "https://company.ir/zagros/api/v1/",
        )
        assertEquals(
            "https://company.ir/zagros/api/v1/mobile/visitor/sync?since=2026-09-19T10%3A00%3A00Z",
            rewritten.toString(),
        )
    }

    @Test
    fun `path segments with ids are preserved`() {
        val rewritten = ServerAddress.rewrite(
            requestUrl = (placeholder.toString() + "mobile/visitor/products/p1/uoms").toHttpUrl(),
            baseUrl = "https://dalbara.ir/api/v1/",
        )
        assertEquals(
            "https://dalbara.ir/api/v1/mobile/visitor/products/p1/uoms",
            rewritten.toString(),
        )
    }

    @Test
    fun `switching scheme from https to http is honoured`() {
        val rewritten = ServerAddress.rewrite(
            requestUrl = (placeholder.toString() + "mobile/visitor/health").toHttpUrl(),
            baseUrl = "http://10.0.2.2:8080/api/v1/",
        )
        assertEquals("http", rewritten?.scheme)
        assertEquals(8080, rewritten?.port)
    }

    @Test
    fun `an unparsable base url leaves the request untouched`() {
        assertNull(ServerAddress.rewrite(placeholder, "http://"))
    }
}
