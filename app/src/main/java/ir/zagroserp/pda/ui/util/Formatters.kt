package ir.zagroserp.pda.ui.util

import ir.zagroserp.pda.core.JalaliDate
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val moneyFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
private val qtyFormat = DecimalFormat("#.###", DecimalFormatSymbols(Locale.US))

/** `1,250,000` – grouped, then rendered with Persian digits. */
fun formatMoney(value: Double, persianDigits: Boolean = true): String {
    val rounded = value.toLong()
    val text = moneyFormat.format(rounded)
    return if (persianDigits) JalaliDate.toPersianDigits(text) else text
}

fun formatMoneyShort(value: Double): String {
    val abs = kotlin.math.abs(value)
    val text = when {
        abs >= 1_000_000_000 -> qtyFormat.format(value / 1_000_000_000) + " میلیارد"
        abs >= 1_000_000 -> qtyFormat.format(value / 1_000_000) + " میلیون"
        abs >= 1_000 -> qtyFormat.format(value / 1_000) + " هزار"
        else -> qtyFormat.format(value)
    }
    return JalaliDate.toPersianDigits(text)
}

fun formatQty(value: Double, persianDigits: Boolean = true): String {
    val text = if (value % 1.0 == 0.0) value.toLong().toString() else qtyFormat.format(value)
    return if (persianDigits) JalaliDate.toPersianDigits(text) else text
}

fun formatPercent(value: Double): String =
    JalaliDate.toPersianDigits("${qtyFormat.format(value)}٪")

fun formatCount(value: Int): String = JalaliDate.toPersianDigits(value.toString())

fun formatDate(iso: String?): String = JalaliDate.formatIso(iso)

fun formatDateLong(iso: String?): String = JalaliDate.formatIsoLong(iso)

fun formatDateTime(iso: String?): String = JalaliDate.formatDateTime(iso)

fun formatTime(iso: String?): String = JalaliDate.formatTime(iso)

/** Relative "x minutes ago" label used by the message list. */
fun formatRelative(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val instant = runCatching { java.time.Instant.parse(iso) }.getOrNull() ?: return formatDate(iso)
    val minutes = java.time.Duration.between(instant, java.time.Instant.now()).toMinutes()
    val text = when {
        minutes < 1 -> "همین حالا"
        minutes < 60 -> "$minutes دقیقه پیش"
        minutes < 60 * 24 -> "${minutes / 60} ساعت پیش"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)} روز پیش"
        else -> return formatDate(iso)
    }
    return JalaliDate.toPersianDigits(text)
}

/** Builds the absolute product image URL from the relative `image_url` returned by the API. */
fun absoluteImageUrl(origin: String, imageUrl: String?): String? {
    if (imageUrl.isNullOrBlank()) return null
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) return imageUrl
    return origin.trimEnd('/') + "/" + imageUrl.trimStart('/')
}
