package ir.zagroserp.pda.ui.screens.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.ui.components.ErrorState
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressRow
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatDate
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreateOrder: () -> Unit,
    onViewOrders: () -> Unit,
    onViewPermissions: () -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = state.customer?.name ?: "جزئیات مشتری",
        subtitle = state.customer?.code?.takeIf { it.isNotBlank() }?.let { "کد: $it" },
        onBack = onBack,
    ) {
        when {
            state.isLoading && state.customer == null -> LoadingBox()
            state.customer == null -> ErrorState(
                message = state.error ?: "مشتری یافت نشد",
                onRetry = viewModel::load,
            )

            else -> {
                val customer = state.customer!!
                LazyColumn(
                    contentPadding = PaddingValues(ZagrosSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    item {
                        ZagrosCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = customer.name.take(1).ifBlank { "م" },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.width(ZagrosSpacing.lg))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (customer.nationalId.isNotBlank()) {
                                        Text(
                                            text = "شناسه ملی: ${JalaliDate.toPersianDigits(customer.nationalId)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalZagrosAccents.current.textSecondary,
                                        )
                                    }
                                    Spacer(Modifier.height(ZagrosSpacing.xs))
                                    StatusPill(
                                        text = if (customer.active) "فعال" else "غیرفعال",
                                        tone = if (customer.active) PillTone.SUCCESS else PillTone.DANGER,
                                        leadingDot = true,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ZagrosCard {
                            SectionHeader(title = "اطلاعات مالی")
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            // `balance` is not populated by the API yet, so the debt rows only
                            // appear when the server actually reports something.
                            if (customer.hasKnownDebt) {
                                ProgressRow(
                                    "بدهی جاری",
                                    formatMoney(customer.balance),
                                )
                                Spacer(Modifier.height(ZagrosSpacing.xs))
                            }
                            if (customer.creditLimit > 0) {
                                ProgressRow("سقف اعتبار", formatMoney(customer.creditLimit))
                                Spacer(Modifier.height(ZagrosSpacing.xs))
                            }
                            if (customer.settlementDays > 0) {
                                ProgressRow(
                                    "مهلت تسویه",
                                    "${JalaliDate.toPersianDigits(customer.settlementDays.toString())} روز",
                                )
                                Spacer(Modifier.height(ZagrosSpacing.xs))
                            }
                            if (!customer.hasKnownDebt && customer.creditLimit == 0.0) {
                                Text(
                                    text = "اطلاعات مالی این مشتری از سرور دریافت نشده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalZagrosAccents.current.textSecondary,
                                )
                            }
                        }
                    }

                    item {
                        ZagrosCard {
                            SectionHeader(title = "تماس و موقعیت")
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            ProgressRow("تلفن", customer.phone.ifBlank { "—" })
                            Spacer(Modifier.height(ZagrosSpacing.xs))
                            ProgressRow("آدرس", customer.address.ifBlank { "—" })
                            Spacer(Modifier.height(ZagrosSpacing.xs))
                            ProgressRow(
                                "مختصات",
                                if (customer.hasLocation) {
                                    "${customer.lat} , ${customer.lng}"
                                } else {
                                    "ثبت نشده"
                                },
                            )
                            if (customer.hasLocation) {
                                Spacer(Modifier.height(ZagrosSpacing.md))
                                Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                                    DetailAction(
                                        label = "تماس",
                                        emoji = "📞",
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${customer.phone}")
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                    DetailAction(
                                        label = "مسیریابی",
                                        emoji = "🧭",
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        val uri = Uri.parse(
                                            "geo:${customer.lat},${customer.lng}?q=" +
                                                Uri.encode(customer.name),
                                        )
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    }
                                }

                                // Verifiable proof that the 50 m check-in rule is active for
                                // this customer: measures the live distance and says whether a
                                // visit would be accepted.
                                Spacer(Modifier.height(ZagrosSpacing.sm))
                                OutlinedButton(
                                    onClick = viewModel::checkDistance,
                                    enabled = !state.isCheckingDistance,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(
                                        if (state.isCheckingDistance) {
                                            "در حال اندازه‌گیری فاصله…"
                                        } else {
                                            "بررسی فاصله تا مشتری"
                                        },
                                    )
                                }
                                state.distanceLabel?.let { label ->
                                    Spacer(Modifier.height(ZagrosSpacing.sm))
                                    StatusPill(
                                        text = "فاصله فعلی: $label" +
                                            " (حد مجاز ${JalaliDate.toPersianDigits("50")} متر)",
                                        tone = if (state.withinRadius == true) {
                                            PillTone.SUCCESS
                                        } else {
                                            PillTone.DANGER
                                        },
                                        leadingDot = true,
                                    )
                                }
                            }

                            // Editing lives in one place: contact details and position together.
                            Spacer(Modifier.height(ZagrosSpacing.md))
                            Button(
                                onClick = onEdit,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text("ویرایش اطلاعات تماس و موقعیت")
                            }
                        }
                    }

                    item {
                        ZagrosCard {
                            SectionHeader(
                                title = "سابقه‌ی بازدید",
                                subtitle = "${state.visits.size} بازدید ثبت‌شده",
                            )
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            if (state.visits.isEmpty()) {
                                Text(
                                    text = "بازدیدی ثبت نشده است",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalZagrosAccents.current.textSecondary,
                                )
                            } else {
                                state.visits.take(5).forEach { visit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = ZagrosSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = formatDate(visit.visitDate),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        StatusPill(
                                            text = visit.status.label,
                                            tone = when (visit.status.apiValue) {
                                                "VISITED" -> PillTone.SUCCESS
                                                "NO_ANSWER" -> PillTone.WARNING
                                                else -> PillTone.NEUTRAL
                                            },
                                        )
                                        if (visit.isPending) {
                                            Spacer(Modifier.width(ZagrosSpacing.xs))
                                            StatusPill(text = "در صف", tone = PillTone.WARNING)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                            PrimaryRowAction(
                                label = "ثبت سفارش جدید",
                                emoji = "🛒",
                                onClick = onCreateOrder,
                            )
                            PrimaryRowAction(
                                label = "سابقه‌ی سفارش‌ها",
                                emoji = "🧾",
                                onClick = onViewOrders,
                            )
                            PrimaryRowAction(
                                label = "درخواست مجوز خارج از محدوده",
                                emoji = "🔑",
                                onClick = { showPermissionDialog = true },
                            )
                            PrimaryRowAction(
                                label = "مشاهده‌ی مجوزها",
                                emoji = "📋",
                                onClick = onViewPermissions,
                            )
                        }
                    }

                    if (state.error != null) {
                        item {
                            StatusPill(text = state.error!!, tone = PillTone.DANGER)
                        }
                    }
                    if (state.message != null) {
                        item {
                            StatusPill(text = state.message!!, tone = PillTone.SUCCESS)
                        }
                    }
                }
            }
        }
    }

    // No confirmation dialog: the API now returns `lat`/`lng`, so this card only appears for
    // customers that genuinely have no coordinates — there is nothing to overwrite.
    if (showPermissionDialog) {
        ZonePermissionRequestDialog(
            customerName = state.customer?.name.orEmpty(),
            onDismiss = { showPermissionDialog = false },
            onSubmit = { scope, reason, days ->
                showPermissionDialog = false
                viewModel.requestZonePermission(scope, reason, days)
            },
        )
    }
}

@Composable
private fun DetailAction(
    label: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = ZagrosSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(ZagrosSpacing.xs))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PrimaryRowAction(
    label: String,
    emoji: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = ZagrosSpacing.lg, vertical = ZagrosSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(ZagrosSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ZonePermissionRequestDialog(
    customerName: String,
    onDismiss: () -> Unit,
    onSubmit: (scope: String, reason: String, days: Int) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("SAME_DAY") }
    var days by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درخواست مجوز خارج از محدوده") },
        text = {
            Column {
                Text(
                    text = "مشتری: $customerName",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(ZagrosSpacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                    listOf(
                        "SINGLE_VISIT" to "یک‌بار مصرف",
                        "SAME_DAY" to "همان روز",
                        "DATE_RANGE" to "بازه‌ی چندروزه",
                    ).forEach { (value, label) ->
                        StatusPill(
                            text = label,
                            tone = if (scope == value) PillTone.PRIMARY else PillTone.NEUTRAL,
                            modifier = Modifier.clickable { scope = value },
                        )
                    }
                }
                Spacer(Modifier.height(ZagrosSpacing.md))
                ir.zagroserp.pda.ui.components.ZagrosTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "دلیل درخواست",
                    singleLine = false,
                    maxLines = 3,
                )
                if (scope == "DATE_RANGE") {
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تعداد روز:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(ZagrosSpacing.sm))
                        listOf(1, 3, 7).forEach { option ->
                            StatusPill(
                                text = JalaliDate.toPersianDigits(option.toString()),
                                tone = if (days == option) PillTone.PRIMARY else PillTone.NEUTRAL,
                                modifier = Modifier
                                    .padding(end = ZagrosSpacing.xs)
                                    .clickable { days = option },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(scope, reason, days) },
                enabled = reason.trim().length >= 3,
            ) { Text("ارسال درخواست") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}