package ir.zagroserp.pda.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.domain.repository.PendingOrderItem
import ir.zagroserp.pda.domain.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val statusFilter: String? = null,
    val pendingOrders: List<PendingOrderItem> = emptyList(),
    val totalPending: Int = 0,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    syncRepository: SyncRepository,
) : ViewModel() {

    private val statusFilter = MutableStateFlow<String?>(null)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val orders: Flow<PagingData<Order>> = statusFilter
        .flatMapLatest { status -> orderRepository.pagingOrders(status.orEmpty()) }
        .cachedIn(viewModelScope)

    val state: StateFlow<OrdersUiState> = combine(
        statusFilter,
        orderRepository.observePendingOrders(),
        syncRepository.pendingSummary.map { it.orders },
        refreshing,
        error,
    ) { filter, pending, total, isRefreshing, currentError ->
        OrdersUiState(
            statusFilter = filter,
            pendingOrders = pending,
            totalPending = total,
            isRefreshing = isRefreshing,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdersUiState(),
    )

    init {
        refresh()
    }

    fun setStatusFilter(status: String?) {
        statusFilter.value = status
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            when (val result = orderRepository.refreshOrders(statusFilter.value)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> error.value = result.error.message
            }
            orderRepository.syncPending()
            refreshing.value = false
        }
    }
}
