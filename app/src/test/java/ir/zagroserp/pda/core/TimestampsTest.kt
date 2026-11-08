package ir.zagroserp.pda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimestampsTest {

    private val isoPattern = DateTimeFormatter.ISO_INSTANT

    @Test
    fun `start of today is midnight UTC and parses as an instant`() {
        val value = Timestamps.startOfDayUtc()
        assertTrue("was $value", value.endsWith("Z"))
        assertTrue("was $value", value.contains("T00:00:00"))

        val parsed = Instant.parse(value)
        assertEquals(0, parsed.atZone(ZoneOffset.UTC).hour)
        assertEquals(0, parsed.atZone(ZoneOffset.UTC).minute)
    }

    @Test
    fun `day offsets produce distinct instants`() {
        val today = Instant.parse(Timestamps.startOfDayUtc(0))
        val nextWeek = Instant.parse(Timestamps.startOfDayUtc(7))
        assertEquals(7L * 24 * 60 * 60, nextWeek.epochSecond - today.epochSecond)
    }

    @Test
    fun `now is a valid instant that can be re-parsed`() {
        val value = Timestamps.nowUtc()
        val parsed = Instant.parse(value)
        assertTrue(isoPattern.format(parsed).isNotEmpty())
    }

    @Test
    fun `values are accepted by Instant parse which is what the server does`() {
        listOf(Timestamps.startOfDayUtc(0), Timestamps.startOfDayUtc(30), Timestamps.nowUtc())
            .forEach { assertTrue("failed to parse $it", runCatching { Instant.parse(it) }.isSuccess) }
    }
}
