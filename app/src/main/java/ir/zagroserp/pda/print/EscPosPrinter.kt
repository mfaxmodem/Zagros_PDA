package ir.zagroserp.pda.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.R
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.domain.model.Invoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal ESC/POS driver for 57mm and 80mm thermal printers.
 *
 * Layout: centred logo, invoice header, buyer details, one block per item (dynamic — as many rows
 * as the invoice has), totals, footer. Persian text is encoded as windows-1256, which is the
 * Arabic code page these printers expose.
 */
@Singleton
class EscPosPrinter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CHARSET = "windows-1256"

        private const val ESC: Byte = 0x1B
        private const val GS: Byte = 0x1D
        private const val LF: Byte = 0x0A
    }

    private val logoBitmap: Bitmap? by lazy {
        runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)
        }.getOrNull()
    }

    fun pairedPrinters(): List<BluetoothDevice> {
        if (!hasPermission()) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return runCatching { adapter.bondedDevices?.toList().orEmpty() }.getOrDefault(emptyList())
    }

    fun hasPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun print(
        device: BluetoothDevice,
        invoice: Invoice,
        paper: PaperWidth,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val output = socket.outputStream
            output.write(buildInvoice(invoice, paper))
            output.flush()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            runCatching { socket?.close() }
        }
    }

    /** Renders the exact byte stream the printer will receive; used by the preview screen. */
    fun buildInvoice(invoice: Invoice, paper: PaperWidth): ByteArray {
        val out = ByteArrayOutputStream()
        val width = paper.charsPerLine

        out.write(byteArrayOf(ESC, '@'.code.toByte())) // initialise
        out.write(byteArrayOf(ESC, 'a'.code.toByte(), 1)) // centre

        // ---- logo --------------------------------------------------------
        logoBitmap?.let { bitmap ->
            val raster = EscPosRaster.encode(bitmap, paper.dots - 96, maxHeightDots = 120)
            if (raster.isNotEmpty()) out.write(raster)
        }

        out.write(byteArrayOf(ESC, '!'.code.toByte(), 0x18)) // double size
        writeText(out, "فاکتور فروش")
        out.write(byteArrayOf(LF))
        out.write(byteArrayOf(ESC, '!'.code.toByte(), 0x00))
        writeText(out, "شماره: ${JalaliDate.toPersianDigits(invoice.number)}")
        out.write(byteArrayOf(LF))
        writeText(out, "تاریخ: ${JalaliDate.formatIso(invoice.date)}")
        out.write(byteArrayOf(LF))

        out.write(byteArrayOf(ESC, 'a'.code.toByte(), 0)) // left

        // ---- buyer -------------------------------------------------------
        separator(out, width)
        writeWrapped(out, "خریدار: ${invoice.customerName}", width)
        if (invoice.customerPhone.isNotBlank()) {
            writeWrapped(out, "تلفن: ${JalaliDate.toPersianDigits(invoice.customerPhone)}", width)
        }
        if (invoice.customerAddress.isNotBlank()) {
            writeWrapped(out, "آدرس: ${invoice.customerAddress}", width)
        }
        separator(out, width)

        // ---- items (one block per line, dynamic) -------------------------
        writeText(out, "شرح کالا")
        out.write(byteArrayOf(LF))
        invoice.items.forEachIndexed { index, item ->
            writeWrapped(out, "${JalaliDate.toPersianDigits((index + 1).toString())}. ${item.productName}", width)
            writeText(
                out,
                "   تعداد ${JalaliDate.toPersianDigits(trim(item.quantity))} ${item.unit} × " +
                    "${JalaliDate.toPersianDigits(group(item.unitPrice))}",
            )
            out.write(byteArrayOf(LF))
            writeText(
                out,
                "   جمع: ${JalaliDate.toPersianDigits(group(item.totalLine))}",
            )
            out.write(byteArrayOf(LF))
            if (item.discount > 0) {
                writeText(
                    out,
                    "   تخفیف: ${JalaliDate.toPersianDigits(group(item.discount))}",
                )
                out.write(byteArrayOf(LF))
            }
        }

        // ---- totals ------------------------------------------------------
        separator(out, width)
        writeText(out, "جمع اقلام: ${JalaliDate.toPersianDigits(group(invoice.subTotal))}")
        out.write(byteArrayOf(LF))
        if (invoice.discount > 0) {
            writeText(out, "تخفیف کل: ${JalaliDate.toPersianDigits(group(invoice.discount))}")
            out.write(byteArrayOf(LF))
        }
        if (invoice.taxAmount > 0) {
            writeText(out, "مالیات: ${JalaliDate.toPersianDigits(group(invoice.taxAmount))}")
            out.write(byteArrayOf(LF))
        }
        out.write(byteArrayOf(ESC, '!'.code.toByte(), 0x08))
        writeText(out, "مبلغ نهایی: ${JalaliDate.toPersianDigits(group(invoice.finalAmount))}")
        out.write(byteArrayOf(LF))
        out.write(byteArrayOf(ESC, '!'.code.toByte(), 0x00))

        if (invoice.note.isNotBlank()) {
            separator(out, width)
            writeWrapped(out, "یادداشت: ${invoice.note}", width)
        }

        separator(out, width)
        out.write(byteArrayOf(ESC, 'a'.code.toByte(), 1))
        writeText(out, "با تشکر از خرید شما")
        out.write(byteArrayOf(LF))
        out.write(byteArrayOf(LF))
        out.write(byteArrayOf(LF))
        out.write(byteArrayOf(GS, 'V'.code.toByte(), 0)) // full cut
        return out.toByteArray()
    }

    private fun writeText(out: OutputStream, text: String) {
        out.write(text.toByteArray(charset(CHARSET)))
    }

    /** Wraps a logical line to the roll width so nothing is silently clipped. */
    private fun writeWrapped(out: OutputStream, text: String, width: Int) {
        text.chunked(width.coerceAtLeast(8)).forEach { chunk ->
            writeText(out, chunk)
            out.write(byteArrayOf(LF))
        }
    }

    private fun separator(out: OutputStream, width: Int) {
        writeText(out, "-".repeat(width.coerceAtLeast(8)))
        out.write(byteArrayOf(LF))
    }

    private fun group(value: Double): String =
        java.text.DecimalFormat("#,###").format(value.toLong())

    private fun trim(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
