package ir.zagroserp.pda.ui.screens.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.ErrorState
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun CustomerListScreen(
    onCustomerClick: (String) -> Unit,
    onAddCustomer: () -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customers = viewModel.customers.collectAsLazyPagingItems()

    ScreenScaffold(
        title = "مشتریان",
        subtitle = "تخصیص مستقیم و مشتریان تورهای شما",
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "به‌روزرسانی")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomer,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("مشتری جدید") },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(ZagrosSpacing.lg)) {
                ZagrosTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "جست‌وجو",
                    placeholder = "نام، تلفن، شناسه ملی یا آدرس",
                    leadingIcon = Icons.Filled.Search,
                )
            }

            when {
                customers.loadState.refresh is LoadState.Loading && customers.itemCount == 0 ->
                    LoadingBox(modifier = Modifier.weight(1f))

                customers.loadState.refresh is LoadState.Error -> ErrorState(
                    message = state.error ?: "بارگذاری فهرست مشتریان ناموفق بود.",
                    modifier = Modifier.weight(1f),
                    onRetry = viewModel::refresh,
                )

                customers.itemCount == 0 -> EmptyState(
                    title = "مشتری‌ای یافت نشد",
                    subtitle = "عبارت جست‌وجو را تغییر دهید یا مشتری جدید ثبت کنید.",
                    emoji = "🔍",
                    modifier = Modifier.weight(1f),
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = ZagrosSpacing.lg,
                        end = ZagrosSpacing.lg,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    items(
                        count = customers.itemCount,
                        key = { index -> customers.peek(index)?.id ?: index },
                    ) { index ->
                        customers[index]?.let { customer ->
                            CustomerRow(
                                customer = customer,
                                onClick = { onCustomerClick(customer.id) },
                            )
                        }
                    }
                    if (customers.loadState.append is LoadState.Loading) {
                        item { LoadingBox(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: Customer, onClick: () -> Unit) {
    val accents = LocalZagrosAccents.current
    ZagrosCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(ZagrosSpacing.sm))
                    if (customer.isLocalOnly) {
                        StatusPill(text = "محلی", tone = PillTone.WARNING)
                    }
                }
                if (customer.phone.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = JalaliDate.toPersianDigits(customer.phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )
                }
                if (customer.address.isNotBlank()) {
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            // Only rendered when the server actually reports a debt – the API returns 0 for
            // every customer today, and a "0" badge would be misleading.
            if (customer.hasKnownDebt) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(customer.balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = accents.danger,
                    )
                    Text(
                        text = "بدهی",
                        style = MaterialTheme.typography.labelSmall,
                        color = accents.textSecondary,
                    )
                }
            }
        }
    }
}
