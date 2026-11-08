package ir.zagroserp.pda.ui.screens.route
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressRow
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatCount
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun TodayRouteScreen(
    onCustomerClick: (String) -> Unit,
    onCreateOrder: (String) -> Unit,
    viewModel: TodayRouteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ZagrosSpacing.lg, vertical = ZagrosSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مسیر امروز",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${JalaliDate.weekdayName(JalaliDate.todayIso())} • ${JalaliDate.todayJalali().formatLong()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
            if (state.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                        contentDescription = "به‌روزرسانی",
                    )
                }
            }
        }

        ZagrosCard(modifier = Modifier.padding(horizontal = ZagrosSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatCount(state.visitedCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(ZagrosSpacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    ProgressRow(
                        label = "بازدید انجام‌شده",
                        value = "${formatCount(state.visitedCount)} از ${formatCount(state.customerCount)}",
                    )
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow(label = "باقی‌مانده", value = formatCount(state.pendingCount))
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow(
                        label = "فروش امروز",
                        value = formatMoney(state.salesToday),
                    )
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(ZagrosSpacing.md))
            StatusPill(
                text = state.error!!,
                tone = PillTone.DANGER,
                modifier = Modifier.padding(horizontal = ZagrosSpacing.lg),
            )
        } else if (state.info != null) {
            Spacer(Modifier.height(ZagrosSpacing.md))
            StatusPill(
                text = state.info!!,
                tone = PillTone.SUCCESS,
                modifier = Modifier.padding(horizontal = ZagrosSpacing.lg),
            )
        }

        Spacer(Modifier.height(ZagrosSpacing.lg))

        if (state.tours.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = ZagrosSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedTourId == null,
                        onClick = { viewModel.selectTour(null) },
                        label = { Text("همه‌ی مسیرها") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
                items(state.tours, key = { it.id }) { tour ->
                    FilterChip(
                        selected = state.selectedTourId == tour.id,
                        onClick = { viewModel.selectTour(tour.id) },
                        label = { Text(tour.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(ZagrosSpacing.md))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = ZagrosSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
        ) {
            RouteFilter.entries.forEach { filter ->
                item(key = filter.name) {
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
        }

        Spacer(Modifier.height(ZagrosSpacing.sm))

        if (state.customers.isEmpty()) {
            EmptyState(
                title = "مشتری‌ای برای امروز ثبت نشده",
                subtitle = "پس از تخصیص تور توسط مدیر، مشتریان اینجا نمایش داده می‌شوند.",
                emoji = "🗺️",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = ZagrosSpacing.lg,
                    end = ZagrosSpacing.lg,
                    top = ZagrosSpacing.sm,
                    bottom = ZagrosSpacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            ) {
                item {
                    SectionHeader(
                        title = "مشتریان مسیر",
                        subtitle = "${formatCount(state.visibleCustomers.size)} مورد",
                    )
                }
                items(state.visibleCustomers, key = { it.customer.id }) { row ->
                    RouteCustomerRow(
                        row = row,
                        onOpen = { onCustomerClick(row.customer.id) },
                        onCheckIn = { viewModel.checkIn(row.customer.id) },
                        onOrder = { onCreateOrder(row.customer.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteCustomerRow(
    row: RouteCustomerRowState,
    onOpen: () -> Unit,
    onCheckIn: () -> Unit,
    onOrder: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    ZagrosCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(ZagrosSpacing.sm))
                    StatusPill(
                        text = if (row.isVisited) "بازدید شد" else "در انتظار",
                        tone = if (row.isVisited) PillTone.SUCCESS else PillTone.WARNING,
                        leadingDot = true,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.customer.address.ifBlank { "بدون آدرس ثبت‌شده" },
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                    maxLines = 1,
                )
                if (row.customer.phone.isNotBlank()) {
                    Text(
                        text = JalaliDate.toPersianDigits(row.customer.phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )
                }
                if (row.customer.hasKnownDebt) {
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    Text(
                        text = "بدهی: ${formatMoney(row.customer.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.danger,
                    )
                }
            }
        }
        Spacer(Modifier.height(ZagrosSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
            RouteActionButton(
                label = if (row.isVisited) "بازدید دوباره" else "ثبت بازدید",
                emoji = "✅",
                enabled = !row.isCheckingIn,
                onClick = onCheckIn,
                modifier = Modifier.weight(1f),
            )
            RouteActionButton(
                label = "سفارش",
                emoji = "🛒",
                enabled = true,
                onClick = onOrder,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RouteActionButton(
    label: String,
    emoji: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = LocalZagrosAccents.current
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else accents.border,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = ZagrosSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(ZagrosSpacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    accents.textSecondary
                },
            )
        }
    }
}
