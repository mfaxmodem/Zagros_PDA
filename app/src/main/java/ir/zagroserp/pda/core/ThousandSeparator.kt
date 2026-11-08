package ir.zagroserp.pda.core

/**
 * Groups digits in threes for money inputs (`1250000` → `1,250,000`) while typing.
 *
 * The pure formatting and offset mapping live here so they can be unit tested; the Compose
 * `VisualTransformation` is a thin wrapper in the UI layer.
 */
object ThousandSeparator {

    const val SEPARATOR = ','

    /**
     * Keeps only digits and normalises them to ASCII.
     *
     * `Char.isDigit()` is true for Persian (U+06F0–U+06F9) and Arabic-Indic (U+0660–U+0669)
     * digits too. A plain filter would therefore keep them as-is, and `toLongOrNull()` would
     * return null — so a value the user typed on a Persian keyboard would silently become zero.
     */
    fun digitsOnly(input: String): String = buildString(input.length) {
        for (char in input) {
            val ascii = when (char) {
                in '0'..'9' -> char
                in '\u06F0'..'\u06F9' -> '0' + (char - '\u06F0')
                in '\u0660'..'\u0669' -> '0' + (char - '\u0660')
                else -> null
            }
            if (ascii != null) append(ascii)
        }
    }

    /** `1250000` → `1,250,000`. Empty stays empty. */
    fun format(digits: String): String {
        if (digits.isEmpty()) return ""
        val builder = StringBuilder(digits.length + digits.length / 3)
        for ((index, char) in digits.withIndex()) {
            val remaining = digits.length - index
            if (index > 0 && remaining % 3 == 0) builder.append(SEPARATOR)
            builder.append(char)
        }
        return builder.toString()
    }

    /** Inverse of [format] – tolerates already-formatted input. */
    fun parse(formatted: String): Long = digitsOnly(formatted).toLongOrNull() ?: 0L

    fun parseDouble(formatted: String): Double = digitsOnly(formatted).toDoubleOrNull() ?: 0.0

    /**
     * Index translation for the text field cursor.
     *
     * @return `[originalToTransformed, transformedToOriginal]`, both sized so that the index
     *   `length` is valid.
     */
    fun offsetMaps(digits: String): Pair<IntArray, IntArray> {
        val formatted = format(digits)
        val originalToTransformed = IntArray(digits.length + 1)
        val transformedToOriginal = IntArray(formatted.length + 1)

        var transformed = 0
        for (index in digits.indices) {
            if (index > 0 && (digits.length - index) % 3 == 0) {
                // The separator sits before this digit.
                transformedToOriginal[transformed] = index
                transformed++
            }
            originalToTransformed[index] = transformed
            transformedToOriginal[transformed] = index
            transformed++
        }
        originalToTransformed[digits.length] = transformed
        transformedToOriginal[transformed] = digits.length
        return originalToTransformed to transformedToOriginal
    }
}
