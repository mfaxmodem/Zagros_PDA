package ir.zagroserp.pda.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressCard
import ir.zagroserp.pda.ui.components.ProgressRow
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatTile
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatCount
import ir.zagroserp.pda.ui.util.formatDate
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatPercent

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "گزارش عملکرد",
        subtitle = if (state.report.from.isNotBlank()) {
            "${formatDate(state.report.from)} تا ${formatDate(state.report.to)}"
        } else {
            "۳۰ روز اخیر"
        },
        onBack = onBack,
    ) {
        if (state.isLoading && state.report.totalSales == 0.0) {
            LoadingBox()
            return@ScreenScaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
        ) {
            if (state.error != null) {
                item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                    items(ReportRange.entries, key = { it.name }) { range ->
                        FilterChip(
                            selected = state.range == range,
                            onClick = { viewModel.setRange(range) },
                            label = { Text(range.label) },
                        )
                    }
                }
            }

            item {
                ProgressCard(
                    title = "تحقق هدف فروش",
                    progress = state.goalProgress,
                    subtitle = "فروش ${formatMoney(state.goals.achievedValue)} از هدف ${formatMoney(state.goals.targetValue)}",
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    StatTile(
                        label = "فروش کل",
                        value = formatMoney(state.report.totalSales),
                        emoji = "💳",
                        tone = PillTone.SUCCESS,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "تعداد سفارش",
                        value = formatCount(state.report.orderCount),
                        emoji = "🧾",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    StatTile(
                        label = "بازدید",
                        value = formatCount(state.report.visitCount),
                        emoji = "📍",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "مشتریان تخصیصی",
                        value = formatCount(state.report.assignedCustomers),
                        emoji = "👥",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(title = "پورسانت و اهداف")
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    ProgressRow("نرخ پورسانت", formatPercent(state.goals.commissionRate))
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow("پورسانت تخمینی", formatMoney(state.goals.estimatedCommission))
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow("فروش امروز", formatMoney(state.goals.salesToday))
                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    ProgressRow("سفارش‌های امروز", formatCount(state.goals.orderCount))
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(title = "وضعیت مالی مشتریان")
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    ProgressRow("مجموع بدهی", formatMoney(state.report.debtTotal))
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Text(
                        text = "ارقام این صفحه تخمینی هستند و پس از تأیید نهایی سرور قطعی می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ir.zagroserp.pda.ui.theme.LocalZagrosAccents.current.textSecondary,
                    )
                }
            }
        }
    }
}
