package ir.zagroserp.pda.print

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * Minimal 1-bit raster encoder for ESC/POS (`GS v 0`).
 *
 * Used to print the company logo at the top of the invoice. The source bitmap is scaled to the
 * requested dot width, alpha-composited onto white and thresholded on luminance — the launcher
 * foreground is a transparent PNG, so treating transparent pixels as white is essential.
 */
object EscPosRaster {

    private const val GS = 0x1D
    private const val LF = 0x0A

    fun encode(
        source: Bitmap,
        targetWidthDots: Int,
        maxHeightDots: Int = 160,
        threshold: Int = 170,
    ): ByteArray {
        val scaled = scaleToWidth(source, targetWidthDots, maxHeightDots) ?: return ByteArray(0)
        val width = scaled.width
        val height = scaled.height
        if (width <= 0 || height <= 0) return ByteArray(0)

        val bytesPerRow = (width + 7) / 8
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val out = ByteArrayOutputStream()
        // GS v 0  m  xL xH  yL yH  <data>
        out.write(GS)
        out.write('v'.code)
        out.write(0)
        out.write(0)
        out.write(bytesPerRow and 0xFF)
        out.write((bytesPerRow shr 8) and 0xFF)
        out.write(height and 0xFF)
        out.write((height shr 8) and 0xFF)

        for (y in 0 until height) {
            var bitIndex = 0
            var current = 0
            for (x in 0 until width) {
                val argb = pixels[y * width + x]
                val alpha = (argb ushr 24) and 0xFF
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF

                // Composite onto white using the alpha channel.
                val rr = (r * alpha + 255 * (255 - alpha)) / 255
                val gg = (g * alpha + 255 * (255 - alpha)) / 255
                val bb = (b * alpha + 255 * (255 - alpha)) / 255
                val luminance = (rr * 299 + gg * 587 + bb * 114) / 1000

                if (luminance < threshold) current = current or (0x80 shr bitIndex)

                bitIndex++
                if (bitIndex == 8) {
                    out.write(current)
                    current = 0
                    bitIndex = 0
                }
            }
            if (bitIndex != 0) out.write(current)
        }

        out.write(LF)
        return out.toByteArray()
    }

    private fun scaleToWidth(source: Bitmap, targetWidth: Int, maxHeight: Int): Bitmap? {
        if (source.width <= 0 || source.height <= 0) return null
        val ratio = targetWidth.toFloat() / source.width.toFloat()
        var width = targetWidth
        var height = (source.height * ratio).toInt().coerceAtLeast(1)
        if (height > maxHeight) {
            height = maxHeight
            width = (source.width.toFloat() * (height.toFloat() / source.height.toFloat())).toInt()
                .coerceAtLeast(1)
        }
        return runCatching {
            Bitmap.createScaledBitmap(source, width, height, true)
        }.getOrNull()
    }
}
