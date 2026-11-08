package ir.zagroserp.pda.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Self contained Gregorian <-> Jalali (Solar Hijri) conversion.
 * The API always speaks Gregorian `YYYY-MM-DD` / RFC3339; the UI always speaks Jalali.
 */
object JalaliDate {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    private val MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val WEEK_DAYS = arrayOf(
        "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه", "یک‌شنبه",
    )

    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    data class Jalali(val year: Int, val month: Int, val day: Int) {
        val monthName: String get() = MONTH_NAMES[month - 1]

        fun format(): String = "${year}/${pad(month)}/${pad(day)}"

        fun formatLong(): String = "$day $monthName $year"

        private fun pad(v: Int) = if (v < 10) "0$v" else "$v"

        override fun toString(): String = format()
    }

    private val G_DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    private fun isLeapGregorian(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Long {
        val a = (14 - gm) / 12
        val y = gy + 4800 - a
        val m = gm + 12 * a - 3
        return gd + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun jdnToGregorian(jdn: Long): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461
        val e = c - 1461 * d / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        return Triple(year, month, day)
    }

    /**
     * The classical Persian conversion works with `floor(JD)` while [gregorianToJdn] returns the
     * Julian Day Number; they differ by exactly one, hence the `- 1` / `+ 1` in the two directions.
     */
    fun fromGregorian(gy: Int, gm: Int, gd: Int): Jalali =
        fromJdn(gregorianToJdn(gy, gm, gd) - 1)

    fun fromGregorian(date: LocalDate): Jalali = fromGregorian(date.year, date.monthValue, date.dayOfMonth)

    fun fromGregorian(isoDate: String?): Jalali? {
        if (isoDate.isNullOrBlank()) return null
        return runCatching { fromGregorian(LocalDate.parse(isoDate.take(10), ISO_DATE)) }.getOrNull()
    }

    fun fromInstant(instant: Instant?): Jalali? =
        instant?.let { fromGregorian(it.atZone(ZoneId.systemDefault()).toLocalDate()) }

    /** Converts a Jalali date to the Gregorian ISO string expected by the API. */
    fun toGregorianIso(jy: Int, jm: Int, jd: Int): String {
        val (gy, gm, gd) = toGregorian(jy, jm, jd)
        return LocalDate.of(gy, gm, gd).format(ISO_DATE)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> =
        jdnToGregorian(jalaliToJdn(jy, jm, jd) + 1)

    private fun fromJdn(jdn: Long): Jalali {
        val (jy, jm, jd) = jdnToJalali(jdn)
        return Jalali(jy, jm, jd)
    }

    private fun jdnToJalali(jdn: Long): Triple<Int, Int, Int> {
        val depoch = jdn - jalaliToJdn(475, 1, 1)
        val cycle = depoch / 1029983
        val cyear = depoch % 1029983
        val ycycle: Long
        val aux1: Long
        val aux2: Long
        if (cyear == 1029982L) {
            ycycle = 2820
            aux1 = 0
            aux2 = 0
        } else {
            aux1 = cyear / 366
            aux2 = cyear % 366
            ycycle = ((2134 * aux1 + 2816 * aux2 + 2815) / 1028522) + aux1 + 1
        }
        var jy = (ycycle + 2820 * cycle + 474).toInt()
        if (jy <= 0) jy -= 1
        val yday = (jdn - jalaliToJdn(jy, 1, 1) + 1).toInt()
        val jm = if (yday <= 186) ((yday - 1) / 31) + 1 else ((yday - 187) / 30) + 7
        val jd = (jdn - jalaliToJdn(jy, jm, 1) + 1).toInt()
        return Triple(jy, jm, jd)
    }

    private fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Long {
        val epBase = jy - if (jy >= 0) 474 else 473
        val epYear = 474 + (epBase % 2820)
        return jd + (if (jm <= 7) (jm - 1) * 31 else (jm - 1) * 30 + 6) +
            ((epYear * 682 - 110) / 2816) +
            (epYear - 1) * 365 +
            (epBase / 2820) * 1029983L +
            (1948320L - 1)
    }

    /** Renders any ISO date / RFC3339 timestamp as a Jalali `YYYY/MM/DD` string. */
    fun formatIso(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val jalali = fromGregorian(iso.take(10)) ?: return "—"
        return jalali.format()
    }

    fun formatIsoLong(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val jalali = fromGregorian(iso.take(10)) ?: return "—"
        return jalali.formatLong()
    }

    /** `HH:mm` of an RFC3339 timestamp in local time. */
    fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            Instant.parse(iso).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrDefault("")
    }

    fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val date = formatIso(iso)
        val time = formatTime(iso)
        return if (time.isEmpty()) date else "$date $time"
    }

    fun todayJalali(): Jalali = fromGregorian(LocalDate.now())

    fun todayIso(): String = LocalDate.now().format(ISO_DATE)

    /** Converts latin digits inside [text] into Persian digits. */
    fun toPersianDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch)
        }
        return sb.toString()
    }

    fun weekdayName(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            val date = LocalDate.parse(iso.take(10), ISO_DATE)
            WEEK_DAYS[date.dayOfWeek.value - 1]
        }.getOrDefault("")
    }

    fun daysInGregorianMonth(year: Int, month: Int): Int =
        if (month == 2 && isLeapGregorian(year)) 29 else G_DAYS_IN_MONTH[month - 1]
}
