package ir.zagroserp.pda.ui.screens.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.model.OrderItem
import ir.zagroserp.pda.domain.model.OrderStatus
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressRow
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ThinDivider
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.navigation.Routes
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatDateTime
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatQty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: Order? = null,
    val items: List<OrderItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>(Routes.ARG_ORDER_ID).orEmpty()
    private val loading = MutableStateFlow(true)

    val state: StateFlow<OrderDetailUiState> = combine(
        orderRepository.observeOrder(orderId),
        orderRepository.observeOrderItems(orderId),
        loading,
    ) { order, items, isLoading ->
        OrderDetailUiState(order = order, items = items, isLoading = isLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrderDetailUiState(),
    )

    init {
        viewModelScope.launch {
            orderRepository.loadOrder(orderId)
            loading.value = false
        }
    }
}

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current
    val order = state.order

    ScreenScaffold(
        title = order?.number?.ifBlank { "جزئیات سفارش" } ?: "جزئیات سفارش",
        subtitle = order?.customerName,
        onBack = onBack,
    ) {
        when {
            state.isLoading && order == null -> LoadingBox()
            order == null -> LoadingBox(message = "سفارش یافت نشد")
            else -> LazyColumn(
                contentPadding = PaddingValues(ZagrosSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            ) {
                item {
                    ZagrosCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.customerName.ifBlank { "مشتری" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = formatDateTime(order.createdAt.ifBlank { order.date }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accents.textSecondary,
                                )
                            }
                            StatusPill(
                                text = order.status.label,
                                tone = when (order.status) {
                                    OrderStatus.APPROVED -> PillTone.SUCCESS
                                    OrderStatus.PENDING, OrderStatus.BLOCKED -> PillTone.WARNING
                                    OrderStatus.REJECTED, OrderStatus.CANCELLED -> PillTone.DANGER
                                    else -> PillTone.NEUTRAL
                                },
                                leadingDot = true,
                            )
                        }
                        if (order.rejectionReason.isNotBlank()) {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            StatusPill(text = order.rejectionReason, tone = PillTone.DANGER)
                        }
                        if (order.softWarning.isNotBlank()) {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            StatusPill(text = order.softWarning, tone = PillTone.WARNING)
                        }
                        if (order.awaitingSupply) {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            StatusPill(
                                text = "در انتظار تأمین — کسری: ${formatQty(order.shortageQty)}",
                                tone = PillTone.WARNING,
                            )
                        }
                    }
                }

                if (order.warnings.isNotEmpty()) {
                    item {
                        ZagrosCard {
                            SectionHeader(title = "هشدار موجودی")
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            order.warnings.forEach { warning ->
                                Text(
                                    text = "${warning.productName}: درخواست ${formatQty(warning.requested)} / موجود ${formatQty(warning.available)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accents.warning,
                                )
                            }
                        }
                    }
                }

                item {
                    ZagrosCard {
                        SectionHeader(
                            title = "اقلام سفارش",
                            subtitle = "${state.items.size} قلم",
                        )
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        state.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ZagrosSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName.ifBlank { item.productId },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "${formatQty(item.quantity)} × ${formatMoney(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accents.textSecondary,
                                    )
                                }
                                Text(
                                    text = formatMoney(item.totalLine),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            ThinDivider()
                        }
                    }
                }

                item {
                    ZagrosCard {
                        SectionHeader(title = "جمع‌بندی")
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        ProgressRow("جمع اقلام", formatMoney(order.subTotal))
                        Spacer(Modifier.height(ZagrosSpacing.xs))
                        ProgressRow("تخفیف", formatMoney(order.discount))
                        Spacer(Modifier.height(ZagrosSpacing.xs))
                        ProgressRow("مالیات", formatMoney(order.taxAmount))
                        Spacer(Modifier.height(ZagrosSpacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "مبلغ نهایی",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = formatMoney(order.finalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (order.note.isNotBlank()) {
                            Spacer(Modifier.height(ZagrosSpacing.md))
                            Text(
                                text = "یادداشت: ${order.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = accents.textSecondary,
                            )
                        }
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = "مبالغ مالیات و نهایی فقط توسط سرور محاسبه و تأیید می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
