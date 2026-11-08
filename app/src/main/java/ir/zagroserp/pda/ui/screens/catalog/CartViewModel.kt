package ir.zagroserp.pda.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CheckInVerdict
import ir.zagroserp.pda.core.ConnectivityObserver
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.location.LocationProvider
import ir.zagroserp.pda.domain.cart.CartStore
import ir.zagroserp.pda.domain.model.CartLine
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val customer: Customer? = null,
    val customers: List<Customer> = emptyList(),
    val customerQuery: String = "",
    val note: String = "",
    val orderDiscountText: String = "",
    val subtotal: Double = 0.0,
    val totalQuantity: Double = 0.0,
    val isOnline: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val lastOrderQueuedOffline: Boolean = false,
    val lastOrderMessage: String = "",
) {
    val orderDiscount: Double get() = orderDiscountText.toDoubleOrNull() ?: 0.0
    val total: Double get() = (subtotal - orderDiscount).coerceAtLeast(0.0)
    val canSubmit: Boolean get() = lines.isNotEmpty() && customer != null && !isSubmitting
}

@HiltViewModel
@Suppress("UNCHECKED_CAST")
class CartViewModel @Inject constructor(
    private val cartStore: CartStore,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val locationProvider: LocationProvider,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {

    private val submitting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /** Non-null while the "you are far from the customer" confirmation is on screen. */
    private val farWarning = MutableStateFlow<String?>(null)
    val farWarningState: StateFlow<String?> = farWarning

    private var pendingLocation: LocationPoint? = null
    private val customerQuery = MutableStateFlow("")
    private val lastQueued = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow("")

    val state: StateFlow<CartUiState> = combine(
        cartStore.lines,
        cartStore.customer,
        customerRepository.observeCustomers(),
        customerQuery,
        cartStore.note,
        cartStore.orderDiscount,
        submitting,
        error,
        connectivity.isOnline,
        lastQueued,
        lastMessage,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val lines = values[0] as List<CartLine>
        val customer = values[1] as Customer?
        val allCustomers = values[2] as List<Customer>
        val query = values[3] as String
        val note = values[4] as String
        val discount = values[5] as Double
        val isSubmitting = values[6] as Boolean
        val currentError = values[7] as String?
        val online = values[8] as Boolean
        val queued = values[9] as Boolean
        val message = values[10] as String

        CartUiState(
            lines = lines,
            customer = customer,
            customers = if (query.isBlank()) {
                allCustomers
            } else {
                allCustomers.filter {
                    it.name.contains(query, true) ||
                        it.phone.contains(query) ||
                        it.nationalId.contains(query)
                }
            },
            customerQuery = query,
            note = note,
            orderDiscountText = if (discount == 0.0) "" else discount.toLong().toString(),
            subtotal = lines.sumOf { it.lineTotal },
            totalQuantity = lines.sumOf { it.quantity },
            isOnline = online,
            isSubmitting = isSubmitting,
            error = currentError,
            lastOrderQueuedOffline = queued,
            lastOrderMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState(),
    )

    fun onCustomerQueryChange(value: String) {
        customerQuery.value = value
    }

    fun selectCustomer(customer: Customer) {
        cartStore.setCustomer(customer)
    }

    fun onNoteChange(value: String) = cartStore.setNote(value)

    fun onOrderDiscountChange(value: String) {
        val filtered = value.filter(Char::isDigit)
        cartStore.setOrderDiscount(filtered.toDoubleOrNull() ?: 0.0)
    }

    fun updateQuantity(line: CartLine, quantity: Double) = cartStore.updateQuantity(line, quantity)

    fun updatePrice(line: CartLine, price: Double) = cartStore.updatePrice(line, price)

    fun remove(line: CartLine) = cartStore.remove(line)

    fun clearCart() {
        cartStore.clear()
        lastQueued.value = false
        lastMessage.value = ""
    }

    /**
     * Phase one: validate, take a fresh fix and ask the server-side-equivalent guard whether the
     * visitor is close enough.
     *
     * A far order is **not blocked** — refusing the sale would cost more than the data quality it
     * protects. It is confirmed instead, and the location travels with the order so the server can
     * flag it for review.
     */
    fun submit(onSuccess: () -> Unit) {
        val snapshot = state.value
        val customer = snapshot.customer
        if (customer == null) {
            error.value = "برای ثبت سفارش، مشتری را انتخاب کنید"
            return
        }
        if (snapshot.lines.isEmpty()) {
            error.value = "سبد سفارش خالی است"
            return
        }

        viewModelScope.launch {
            submitting.value = true
            error.value = null
            val point = runCatching { locationProvider.freshFix() }.getOrNull()
            val verdict = orderRepository.evaluateOrderLocation(customer.id, point)
            submitting.value = false

            if (verdict is CheckInVerdict.TooFar) {
                pendingLocation = point
                farWarning.value = buildString {
                    append("شما ")
                    append(JalaliDate.toPersianDigits(verdict.distanceMeters.toInt().toString()))
                    append(" متر از این مشتری فاصله دارید.\n\n")
                    append("سفارش ثبت می‌شود ولی برای بازبینی مدیر علامت می‌خورد. ")
                    append("اگر همین حالا کنار مشتری نیستید، بهتر است ابتدا به محل مشتری بروید.")
                }
                return@launch
            }

            doSubmit(snapshot, point, onSuccess)
        }
    }

    /** Phase two: the visitor confirmed a far order. */
    fun confirmFarSubmit(onSuccess: () -> Unit) {
        val snapshot = state.value
        farWarning.value = null
        viewModelScope.launch { doSubmit(snapshot, pendingLocation, onSuccess) }
    }

    fun dismissFarWarning() {
        farWarning.value = null
        pendingLocation = null
    }

    private suspend fun doSubmit(
        snapshot: CartUiState,
        location: LocationPoint?,
        onSuccess: () -> Unit,
    ) {
        val customer = snapshot.customer ?: return
        submitting.value = true
        error.value = null

        val result = orderRepository.submitOrder(
            customerId = customer.id,
            customerName = customer.name,
            date = JalaliDate.todayIso(),
            lines = snapshot.lines,
            note = snapshot.note.takeIf { it.isNotBlank() },
            discount = snapshot.orderDiscount,
            location = location,
        )

        when (result) {
            is AppResult.Success -> {
                lastQueued.value = result.data.queuedOffline
                lastMessage.value = buildString {
                    if (result.data.softWarning.isNotBlank()) {
                        append(result.data.softWarning)
                    }
                    if (result.data.order.awaitingSupply) {
                        if (isNotEmpty()) append("\n")
                        append("در انتظار تأمین — کسری: ${result.data.order.shortageQty}")
                    }
                    if (result.data.order.number.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("شماره سفارش: ${result.data.order.number}")
                    }
                }
                submitting.value = false
                pendingLocation = null
                onSuccess()
            }

            is AppResult.Failure -> {
                error.value = result.error.message
                submitting.value = false
            }
        }
    }
}
