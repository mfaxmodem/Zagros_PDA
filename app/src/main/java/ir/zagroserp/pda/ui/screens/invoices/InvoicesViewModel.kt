package ir.zagroserp.pda.ui.screens.invoices

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.domain.repository.InvoiceRepository
import ir.zagroserp.pda.print.EscPosPrinter
import ir.zagroserp.pda.print.PaperWidth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoicesUiState(
    val invoices: List<Invoice> = emptyList(),
    val unprintedCount: Int = 0,
    val printers: List<BluetoothDevice> = emptyList(),
    val isLoading: Boolean = true,
    val isPrinting: Boolean = false,
    val error: String? = null,
    val printMessage: String? = null,
)

@HiltViewModel
@Suppress("UNCHECKED_CAST")
class InvoicesViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val printer: EscPosPrinter,
) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val printing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val printers = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val printMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<InvoicesUiState> = combine(
        invoiceRepository.observeInvoices(),
        invoiceRepository.observeUnprintedCount(),
        printers,
        loading,
        printing,
        error,
        printMessage,
    ) { values ->
        InvoicesUiState(
            invoices = values[0] as List<Invoice>,
            unprintedCount = values[1] as Int,
            printers = values[2] as List<BluetoothDevice>,
            isLoading = values[3] as Boolean,
            isPrinting = values[4] as Boolean,
            error = values[5] as String?,
            printMessage = values[6] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvoicesUiState(),
    )

    init {
        refresh()
    }

    fun hasBluetoothPermission(): Boolean = printer.hasPermission()

    fun onBluetoothDenied() {
        error.value = "برای چاپ فاکتور، دسترسی بلوتوث لازم است"
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            when (val result = invoiceRepository.refreshInvoices()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> error.value = result.error.message
            }
            loading.value = false
        }
    }

    fun loadPrinters() {
        printers.value = printer.pairedPrinters()
    }

    fun print(invoice: Invoice, device: BluetoothDevice, paper: PaperWidth) {
        if (printing.value) return
        printing.value = true
        printMessage.value = null
        viewModelScope.launch {
            val result = printer.print(device, invoice, paper)
            if (result.isSuccess) {
                val ack = invoiceRepository.markPrinted(invoice.id)
                printMessage.value = when (ack) {
                    is AppResult.Success -> "فاکتور روی ${paper.label} چاپ شد"
                    is AppResult.Failure ->
                        "چاپ انجام شد؛ تأیید سرور ناموفق بود و بعداً تلاش می‌شود"
                }
            } else {
                printMessage.value = "چاپ ناموفق بود: ${result.exceptionOrNull()?.message.orEmpty()}"
            }
            printing.value = false
        }
    }

    fun clearMessage() {
        printMessage.value = null
    }
}
