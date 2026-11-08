package ir.zagroserp.pda.ui.screens.orders
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.model.OrderStatus
import ir.zagroserp.pda.domain.repository.PendingOrderItem
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatCount
import ir.zagroserp.pda.ui.util.formatDate
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val orders = viewModel.orders.collectAsLazyPagingItems()
    val accents = LocalZagrosAccents.current

    ScreenScaffold(
        title = "سابقه‌ی سفارش‌ها",
        subtitle = "${formatCount(state.totalPending)} سفارش در صف ارسال",
        onBack = onBack,
        actions = {
            androidx.compose.material3.IconButton(onClick = viewModel::refresh) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                    contentDescription = "به‌روزرسانی",
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = ZagrosSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = state.statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("همه") },
                    )
                }
                items(
                    OrderStatus.entries.filter { it != OrderStatus.UNKNOWN },
                    key = { it.name },
                ) { status ->
                    FilterChip(
                        selected = state.statusFilter == status.apiValue,
                        onClick = { viewModel.setStatusFilter(status.apiValue) },
                        label = { Text(status.label) },
                    )
                }
            }

            when {
                orders.loadState.refresh is androidx.paging.LoadState.Loading &&
                    orders.itemCount == 0 -> LoadingBox()

                orders.itemCount == 0 && state.pendingOrders.isEmpty() -> EmptyState(
                    title = "سفارشی ثبت نشده است",
                    subtitle = "سفارش‌های ثبت‌شده و در صف اینجا نمایش داده می‌شوند.",
                    emoji = "🧾",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = ZagrosSpacing.lg,
                        end = ZagrosSpacing.lg,
                        top = ZagrosSpacing.md,
                        bottom = ZagrosSpacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    if (state.pendingOrders.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "در صف ارسال",
                                subtitle = "به‌صورت خودکار در پس‌زمینه ارسال می‌شود",
                            )
                        }
                        items(state.pendingOrders, key = { it.clientOrderId }) { pending ->
                            PendingOrderCard(pending)
                        }
                        item {
                            SectionHeader(
                                title = "سفارش‌های ثبت‌شده",
                                subtitle = "${formatCount(orders.itemCount)} مورد",
                            )
                        }
                    }

                    items(orders.itemCount, key = { index -> orders.peek(index)?.id ?: index }) { index ->
                        orders[index]?.let { order ->
                            OrderCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.danger,
                    modifier = Modifier.padding(ZagrosSpacing.lg),
                )
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    val accents = LocalZagrosAccents.current
    ZagrosCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.customerName.ifBlank { "مشتری" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatDate(order.date)} • ${order.number.ifBlank { "بدون شماره" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
            StatusPill(
                text = order.status.label,
                tone = when (order.status) {
                    OrderStatus.APPROVED -> PillTone.SUCCESS
                    OrderStatus.PENDING -> PillTone.WARNING
                    OrderStatus.BLOCKED -> PillTone.WARNING
                    OrderStatus.REJECTED, OrderStatus.CANCELLED -> PillTone.DANGER
                    else -> PillTone.NEUTRAL
                },
                leadingDot = true,
            )
        }
        Spacer(Modifier.height(ZagrosSpacing.sm))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatMoney(order.finalAmount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            if (order.items.isNotEmpty()) {
                Text(
                    text = "${formatCount(order.items.size)} قلم",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
        }
        if (order.awaitingSupply) {
            Spacer(Modifier.height(ZagrosSpacing.sm))
            StatusPill(
                text = "در انتظار تأمین — کسری: ${order.shortageQty}",
                tone = PillTone.WARNING,
            )
        }
        if (order.rawStatus == "REJECTED" && order.rejectionReason.isNotBlank()) {
            Spacer(Modifier.height(ZagrosSpacing.sm))
            StatusPill(text = order.rejectionReason, tone = PillTone.DANGER)
        }
    }
}

@Composable
private fun PendingOrderCard(pending: PendingOrderItem) {
    val accents = LocalZagrosAccents.current
    ZagrosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pending.customerName.ifBlank { "مشتری محلی" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatCount(pending.itemCount)} قلم • ${formatMoney(pending.estimatedTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
            StatusPill(
                text = when (pending.status) {
                    "ERROR" -> "خطا"
                    "BLOCKED" -> "نیازمند بازبینی"
                    else -> "در صف"
                },
                tone = when (pending.status) {
                    "ERROR" -> PillTone.DANGER
                    "BLOCKED" -> PillTone.WARNING
                    else -> PillTone.WARNING
                },
                leadingDot = true,
            )
        }
        if (pending.lastError.isNotBlank()) {
            Spacer(Modifier.height(ZagrosSpacing.sm))
            Text(
                text = pending.lastError,
                style = MaterialTheme.typography.bodySmall,
                color = accents.danger,
            )
        }
    }
}
