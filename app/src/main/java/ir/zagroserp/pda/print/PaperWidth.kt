package ir.zagroserp.pda.print

/**
 * Thermal paper profiles.
 *
 * `charsPerLine` is the number of font-A characters the printer fits across the roll, which is
 * what separators and line wrapping must be based on. `dots` is the printable dot width, used to
 * scale the logo raster.
 */
enum class PaperWidth(
    val label: String,
    val charsPerLine: Int,
    val dots: Int,
    val millimeters: Int,
) {
    MM_57("۵۷ میلی‌متر", charsPerLine = 32, dots = 384, millimeters = 57),
    MM_80("۸۰ میلی‌متر", charsPerLine = 48, dots = 576, millimeters = 80),
    ;

    /** Preview width on screen – proportionally wider than reality so it stays readable. */
    val previewWidthDp: Int get() = when (this) {
        MM_57 -> 210
        MM_80 -> 290
    }
}
