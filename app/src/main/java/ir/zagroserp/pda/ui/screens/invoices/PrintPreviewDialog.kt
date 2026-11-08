package ir.zagroserp.pda.ui.screens.invoices

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.zagroserp.pda.R
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.print.PaperWidth
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.theme.IranSans
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatQty

/**
 * What-you-see-is-what-you-print preview.
 *
 * The paper mock mirrors the ESC/POS layout: centred logo, invoice header, buyer block, one row
 * per invoice item (dynamic), then totals. Switching between 57mm and 80mm changes both the
 * rendered width and the character budget used for separators and wrapping.
 */
@Composable
fun PrintPreviewDialog(
    invoice: Invoice,
    printers: List<BluetoothDevice>,
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onLoadPrinters: () -> Unit,
    onPrint: (device: BluetoothDevice, paper: PaperWidth) -> Unit,
) {
    var paper by remember { mutableStateOf(PaperWidth.MM_80) }
    var selectedPrinter by remember { mutableStateOf(printers.firstOrNull()) }
    val accents = LocalZagrosAccents.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ZagrosSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "پیش‌نمایش چاپ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "فاکتور ${invoice.number} • ${invoice.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "بستن")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ZagrosSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PaperPreview(invoice = invoice, paper = paper)
                    Spacer(Modifier.height(ZagrosSpacing.lg))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ZagrosSpacing.lg),
                ) {
                    Text(
                        text = "عرض کاغذ",
                        style = MaterialTheme.typography.labelMedium,
                        color = accents.textSecondary,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        PaperWidth.entries.forEach { option ->
                            StatusPill(
                                text = option.label,
                                tone = if (paper == option) PillTone.PRIMARY else PillTone.NEUTRAL,
                                modifier = Modifier.clickable { paper = option },
                            )
                        }
                    }

                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "چاپگر",
                            style = MaterialTheme.typography.labelMedium,
                            color = accents.textSecondary,
                        )
                        Spacer(Modifier.width(ZagrosSpacing.sm))
                        if (selectedPrinter == null) {
                            StatusPill(text = "چاپگری یافت نشد", tone = PillTone.WARNING)
                        } else {
                            StatusPill(
                                text = selectedPrinter?.name ?: "ناشناس",
                                tone = PillTone.SUCCESS,
                                leadingDot = true,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "به‌روزرسانی",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = onLoadPrinters)
                                .padding(ZagrosSpacing.xs),
                        )
                    }

                    if (printers.size > 1) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Column(verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.xs)) {
                            printers.forEach { device ->
                                StatusPill(
                                    text = device.name ?: "چاپگر ناشناس",
                                    tone = if (device == selectedPrinter) PillTone.PRIMARY else PillTone.NEUTRAL,
                                    modifier = Modifier.clickable { selectedPrinter = device },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(ZagrosSpacing.lg))
                    Button(
                        onClick = { selectedPrinter?.let { onPrint(it, paper) } },
                        enabled = selectedPrinter != null && !isPrinting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(if (isPrinting) "در حال ارسال به چاپگر…" else "چاپ فاکتور")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperPreview(invoice: Invoice, paper: PaperWidth) {
    Surface(
        modifier = Modifier.width(paper.previewWidthDp.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.White,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "فاکتور فروش",
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "شماره: ${JalaliDate.toPersianDigits(invoice.number)}",
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "تاریخ: ${JalaliDate.formatIso(invoice.date)}",
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall,
            )

            PreviewSeparator(paper)

            PreviewLine("خریدار", invoice.customerName)
            if (invoice.customerPhone.isNotBlank()) {
                PreviewLine("تلفن", JalaliDate.toPersianDigits(invoice.customerPhone))
            }
            if (invoice.customerAddress.isNotBlank()) {
                PreviewLine("آدرس", invoice.customerAddress)
            }

            PreviewSeparator(paper)

            invoice.items.forEachIndexed { index, item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${JalaliDate.toPersianDigits((index + 1).toString())}. ${item.productName}",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${formatQty(item.quantity)} ${item.unit} × ${formatMoney(item.unitPrice)}",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = formatMoney(item.totalLine),
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            PreviewSeparator(paper)

            PreviewLine("جمع اقلام", formatMoney(invoice.subTotal))
            if (invoice.discount > 0) PreviewLine("تخفیف", formatMoney(invoice.discount))
            if (invoice.taxAmount > 0) PreviewLine("مالیات", formatMoney(invoice.taxAmount))

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "مبلغ نهایی",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatMoney(invoice.finalAmount),
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (invoice.note.isNotBlank()) {
                PreviewSeparator(paper)
                Text(
                    text = "یادداشت: ${invoice.note}",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PreviewSeparator(paper)
            Text(
                text = "با تشکر از خرید شما",
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${paper.millimeters}mm",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = IranSans,
            )
        }
    }
}

@Composable
private fun PreviewSeparator(paper: PaperWidth) {
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFCBD5E1)),
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PreviewLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = Color(0xFF475569),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = Color.Black,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
        )
    }
}


