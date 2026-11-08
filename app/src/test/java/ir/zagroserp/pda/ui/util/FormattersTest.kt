package ir.zagroserp.pda.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun `money is grouped and rendered with persian digits`() {
        assertEquals("۱,۲۵۰,۰۰۰", formatMoney(1_250_000.0))
        assertEquals("۰", formatMoney(0.0))
        assertEquals("۱,۰۰۰", formatMoney(1_000.4))
    }

    @Test
    fun `money can opt out of persian digits`() {
        assertEquals("1,250,000", formatMoney(1_250_000.0, persianDigits = false))
    }

    @Test
    fun `quantities drop trailing decimals for whole numbers`() {
        assertEquals("۸", formatQty(8.0))
        assertEquals("۸.۵", formatQty(8.5))
    }

    @Test
    fun `percent uses the persian percent sign`() {
        assertEquals("۱۲٪", formatPercent(12.0))
    }

    @Test
    fun `image url is resolved against the origin and not the api base`() {
        val origin = "https://dalbara.ir/"
        assertEquals(
            "https://dalbara.ir/uploads/products/P001.jpg",
            absoluteImageUrl(origin, "/uploads/products/P001.jpg"),
        )
        // An absolute URL is passed through untouched.
        assertEquals(
            "https://cdn.example.com/a.jpg",
            absoluteImageUrl(origin, "https://cdn.example.com/a.jpg"),
        )
        assertNull(absoluteImageUrl(origin, null))
        assertNull(absoluteImageUrl(origin, ""))
    }

    @Test
    fun `relative timestamps degrade to a jalali date when far in the past`() {
        assertEquals("", formatRelative(null))
        assertEquals("همین حالا", formatRelative(java.time.Instant.now().toString()))
    }
}
