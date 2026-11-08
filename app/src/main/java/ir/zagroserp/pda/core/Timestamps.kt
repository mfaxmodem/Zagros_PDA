package ir.zagroserp.pda.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * RFC3339 timestamps for the fields the API types as timestamps rather than dates.
 *
 * `POST /mobile/visitor/zone-permissions` expects `target_date` and `until` as RFC3339
 * (`2026-09-19T00:00:00Z`). Sending a plain `2026-09-19` is rejected with a 400
 * "بدنه درخواست نامعتبر است".
 */
object Timestamps {

    /** Start of a UTC day, `n` days from today. `2026-09-19T00:00:00Z` */
    fun startOfDayUtc(daysFromNow: Long = 0): String =
        LocalDate.now(ZoneOffset.UTC)
            .plusDays(daysFromNow)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toString()

    /** The moment of the call, truncated to the second, e.g. `2026-09-19T10:15:30Z`. */
    fun nowUtc(): String = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
}
