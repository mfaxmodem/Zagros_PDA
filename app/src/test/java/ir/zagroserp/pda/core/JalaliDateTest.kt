package ir.zagroserp.pda.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JalaliDateTest {

    @Test
    fun `nowruz anchors map to the first day of Farvardin`() {
        assertEquals(JalaliDate.Jalali(1403, 1, 1), JalaliDate.fromGregorian(2024, 3, 20))
        assertEquals(JalaliDate.Jalali(1404, 1, 1), JalaliDate.fromGregorian(2025, 3, 20))
        assertEquals(JalaliDate.Jalali(1405, 1, 1), JalaliDate.fromGregorian(2026, 3, 21))
    }

    @Test
    fun `a mid-year gregorian date converts to the expected jalali day`() {
        // 2026-09-19 is 28 Shahrivar 1405.
        assertEquals(JalaliDate.Jalali(1405, 6, 28), JalaliDate.fromGregorian(2026, 9, 19))
    }

    @Test
    fun `gregorian to jalali round trips across a decade`() {
        var date = LocalDate.of(2018, 1, 1)
        val end = LocalDate.of(2030, 12, 31)
        while (!date.isAfter(end)) {
            val jalali = JalaliDate.fromGregorian(date)
            val iso = JalaliDate.toGregorianIso(jalali.year, jalali.month, jalali.day)
            assertEquals("mismatch for $date", date.toString(), iso)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `formatIso renders jalali with latin digits`() {
        assertEquals("1405/06/28", JalaliDate.formatIso("2026-09-19"))
        assertEquals("1405/06/28", JalaliDate.formatIso("2026-09-19T10:00:00Z"))
    }

    @Test
    fun `formatIso tolerates blank and malformed input`() {
        assertEquals("—", JalaliDate.formatIso(null))
        assertEquals("—", JalaliDate.formatIso(""))
        assertEquals("—", JalaliDate.formatIso("not-a-date"))
    }

    @Test
    fun `persian digits replace latin digits only`() {
        assertEquals("۱۲۳۴۵۶۷۸۹۰", JalaliDate.toPersianDigits("1234567890"))
        assertEquals("۱۲٪ تخفیف", JalaliDate.toPersianDigits("12٪ تخفیف"))
    }

    @Test
    fun `month names are correct for the first and last month`() {
        assertEquals("فروردین", JalaliDate.fromGregorian(2026, 3, 21).monthName)
        assertEquals("اسفند", JalaliDate.fromGregorian(2026, 2, 20).monthName)
    }

    @Test
    fun `time is formatted from rfc3339 in local zone`() {
        // Only assert shape: local timezone makes the exact hour environment dependent.
        val formatted = JalaliDate.formatTime("2026-09-19T10:00:00Z")
        assertEquals(5, formatted.length)
        assertEquals(':', formatted[2])
    }
}
