package ir.zagroserp.pda.ui.screens.catalog
import androidx.compose.material.icons.filled.Search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.domain.model.CartLine
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressRow
import ir.zagroserp.pda.ui.components.QuantityStepper
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatQty

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val farWarning by viewModel.farWarningState.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "سبد سفارش",
        subtitle = if (state.customer != null) state.customer!!.name else "مشتری انتخاب نشده",
        onBack = onBack,
        bottomBar = {
            if (state.lines.isNotEmpty()) {
                Column(modifier = Modifier.padding(ZagrosSpacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("جمع کل (تخمینی)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatMoney(state.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Button(
                        onClick = { viewModel.submit { showSuccess = true } },
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(if (state.isOnline) "ثبت سفارش" else "ذخیره و ارسال در پس‌زمینه")
                        }
                    }
                    if (!state.isOnline) {
                        Spacer(Modifier.height(ZagrosSpacing.xs))
                        Text(
                            text = "آفلاین هستید؛ سفارش به‌صورت محلی ذخیره و بعداً ارسال می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                }
            }
        },
    ) {
        if (state.lines.isEmpty()) {
            EmptyState(
                title = "سبد سفارش خالی است",
                subtitle = "از کاتالوگ، کالاها را به سبد اضافه کنید.",
                emoji = "🛒",
                actionLabel = "بازگشت به کاتالوگ",
                onAction = onBack,
            )
            return@ScreenScaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            modifier = Modifier.imePadding(),
        ) {
            item {
                ZagrosCard(onClick = { showCustomerPicker = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👤", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(ZagrosSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.customer?.name ?: "انتخاب مشتری",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = state.customer?.address?.ifBlank { "برای ثبت سفارش، مشتری الزامی است" }
                                    ?: "برای ثبت سفارش، مشتری الزامی است",
                                style = MaterialTheme.typography.bodySmall,
                                color = accents.textSecondary,
                                maxLines = 1,
                            )
                        }
                        Text(text = "تغییر", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }



            items(state.lines, key = { it.product.id + (it.uomId ?: "") }) { line ->
                CartLineCard(
                    line = line,
                    onQuantityChange = { viewModel.updateQuantity(line, it) },
                    onPriceChange = { viewModel.updatePrice(line, it) },
                    onRemove = { viewModel.remove(line) },
                )
            }

            item {
                ZagrosCard {
                    ProgressRow("جمع اقلام", formatMoney(state.subtotal))
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow("تعداد اقلام", formatQty(state.totalQuantity))
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.orderDiscountText,
                        onValueChange = viewModel::onOrderDiscountChange,
                        label = "تخفیف کل سفارش (ریال)",
                        placeholder = "۱,۲۵۰,۰۰۰",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        moneyFormat = true,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        label = "یادداشت سفارش",
                        singleLine = false,
                        maxLines = 3,
                    )
                }
            }

            if (state.error != null) {
                item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            query = state.customerQuery,
            onQueryChange = viewModel::onCustomerQueryChange,
            onSelect = {
                viewModel.selectCustomer(it)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false },
        )
    }

    // Non-blocking geofence warning: the sale is never refused, but the visitor is told that the
    // order will be flagged for review.
    farWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFarWarning,
            title = { Text("فاصله از مشتری") },
            text = { Text(warning) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmFarSubmit { showSuccess = true } },
                ) { Text("ثبت سفارش") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFarWarning) { Text("انصراف") }
            },
        )
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text(if (state.lastOrderQueuedOffline) "سفارش در صف ارسال" else "سفارش ثبت شد") },
            text = {
                Text(
                    text = if (state.lastOrderQueuedOffline) {
                        "اتصال برقرار نبود؛ سفارش به‌صورت محلی ذخیره شد و در پس‌زمینه ارسال می‌شود."
                    } else {
                        state.lastOrderMessage.ifBlank { "سفارش با موفقیت ثبت شد." }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccess = false
                    viewModel.clearCart()
                    onOrderPlaced()
                }) { Text("تأیید") }
            },
        )
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    ZagrosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (line.uomName.isNotBlank()) {
                        "بسته‌بندی: ${line.uomName}"
                    } else {
                        "واحد: ${line.product.unit.ifBlank { "عدد" }}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
                if (line.offerName.isNotBlank()) {
                    Text(
                        text = "عرضه: ${line.offerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accents.dangerContainer)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✕", style = MaterialTheme.typography.labelMedium, color = accents.danger)
            }
        }
        Spacer(Modifier.height(ZagrosSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityStepper(
                value = line.quantity.toInt(),
                onValueChange = { onQuantityChange(it.toDouble()) },
                buttonSize = 34.dp,
                label = null,
            )
            Spacer(Modifier.width(ZagrosSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "قیمت واحد",
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.textSecondary,
                )
                Text(
                    text = formatMoney(line.unitPrice),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "جمع خط",
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.textSecondary,
                )
                Text(
                    text = formatMoney(line.lineTotal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<ir.zagroserp.pda.domain.model.Customer>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (ir.zagroserp.pda.domain.model.Customer) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب مشتری") },
        text = {
            Column {
                ZagrosTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = "جست‌وجو",
                    leadingIcon = androidx.compose.material.icons.Icons.Filled.Search,
                )
                Spacer(Modifier.height(ZagrosSpacing.md))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(customers, key = { it.id }) { customer ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(customer) }
                                .padding(vertical = ZagrosSpacing.sm),
                        ) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = customer.address.ifBlank { customer.phone },
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalZagrosAccents.current.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        },
    )
}
