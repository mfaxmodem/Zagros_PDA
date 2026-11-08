package ir.zagroserp.pda.ui.screens.invoices

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatDate
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun InvoicesScreen(
    onBack: () -> Unit,
    viewModel: InvoicesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var previewInvoice by remember { mutableStateOf<Invoice?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) {
            viewModel.loadPrinters()
        } else {
            viewModel.onBluetoothDenied()
        }
    }

    LaunchedEffect(previewInvoice) {
        if (previewInvoice != null) viewModel.loadPrinters()
    }

    fun openPreview(invoice: Invoice) {
        if (viewModel.hasBluetoothPermission()) {
            previewInvoice = invoice
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
            )
        }
    }

    ScreenScaffold(
        title = "فاکتورهای تأییدشده",
        subtitle = "${state.unprintedCount} فاکتور چاپ‌نشده",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
    ) {
        if (state.invoices.isEmpty() && !state.isLoading) {
            EmptyState(
                title = "فاکتوری تأیید نشده است",
                subtitle = "فاکتورهای تأییدشده‌ی شما پس از همگام‌سازی اینجا نمایش داده می‌شوند.",
                emoji = "📦",
                actionLabel = "همگام‌سازی",
                onAction = viewModel::refresh,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(ZagrosSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            ) {
                if (state.error != null) {
                    item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
                }
                items(state.invoices, key = { it.id }) { invoice ->
                    InvoiceCard(invoice = invoice, onPrint = { openPreview(invoice) })
                }
            }
        }
    }

    previewInvoice?.let { invoice ->
        PrintPreviewDialog(
            invoice = invoice,
            printers = state.printers,
            isPrinting = state.isPrinting,
            onDismiss = { previewInvoice = null },
            onLoadPrinters = viewModel::loadPrinters,
            onPrint = { device, paper ->
                viewModel.print(invoice, device, paper)
            },
        )
    }

    if (state.printMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("چاپ فاکتور") },
            text = { Text(state.printMessage!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) { Text("تأیید") }
            },
        )
    }

}

@Composable
private fun InvoiceCard(
    invoice: Invoice,
    onPrint: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    ZagrosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.customerName.ifBlank { "مشتری" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatDate(invoice.date)} • ${invoice.number}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
                Text(
                    text = "${invoice.items.size} قلم",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
            StatusPill(
                text = if (invoice.isPrinted) "چاپ‌شده" else "چاپ‌نشده",
                tone = if (invoice.isPrinted) PillTone.SUCCESS else PillTone.WARNING,
                leadingDot = true,
            )
        }
        Spacer(Modifier.height(ZagrosSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatMoney(invoice.finalAmount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onPrint, shape = MaterialTheme.shapes.small) {
                Text("پیش‌نمایش و چاپ")
            }
        }
    }
}
