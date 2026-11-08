package ir.zagroserp.pda.ui.screens.dashboard
import androidx.compose.material.icons.filled.Settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.ui.components.GridActionTile
import ir.zagroserp.pda.ui.components.HeroMetricCard
import ir.zagroserp.pda.ui.components.OfflineBanner
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProgressCard
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatRow
import ir.zagroserp.pda.ui.components.ThinDivider
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.navigation.Routes
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatCount
import ir.zagroserp.pda.ui.util.formatMoney

/** How long the first back press stays "armed" before it is forgotten. */
private const val EXIT_CONFIRM_WINDOW_MS = 2_000L

private data class DashboardAction(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val route: String,
    val badge: Int = 0,
    val badgeTone: PillTone = PillTone.DANGER,
    val pill: String? = null,
    val pillTone: PillTone = PillTone.SUCCESS,
)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current
    val context = LocalContext.current

    // The dashboard is the root of the stack, so back here means "leave the app". Requiring a
    // second press avoids losing the session to a stray tap on a field device.
    var exitArmed by remember { mutableStateOf(false) }
    LaunchedEffect(exitArmed) {
        if (exitArmed) {
            delay(EXIT_CONFIRM_WINDOW_MS)
            exitArmed = false
        }
    }
    BackHandler {
        if (exitArmed) {
            (context as? Activity)?.finish()
        } else {
            exitArmed = true
            Toast.makeText(
                context,
                "برای خروج، دوباره دکمه‌ی بازگشت را بزنید",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val actions = listOf(
        DashboardAction(
            title = "مسیر امروز",
            subtitle = "${formatCount(state.pendingCount)} مشتری باقی‌مانده",
            emoji = "📍",
            route = Routes.TODAY_ROUTE,
            pill = if (state.customerCount == 0) {
                "مسیری تعیین نشده"
            } else {
                "${formatCount(state.visitedCount)} از ${formatCount(state.customerCount)} بازدید"
            },
            pillTone = if (state.pendingCount == 0 && state.customerCount > 0) PillTone.SUCCESS else PillTone.WARNING,
        ),
        DashboardAction(
            title = "کاتالوگ و ثبت سفارش",
            subtitle = "قیمت‌گذاری چندواحدی و تخفیف‌ها",
            emoji = "🛒",
            route = Routes.CATALOG,
        ),
        DashboardAction(
            title = "فاکتورهای تأییدشده",
            subtitle = "چاپ بلوتوثی فاکتور",
            emoji = "📦",
            route = Routes.INVOICES,
            badge = state.unprintedInvoices,
            badgeTone = PillTone.WARNING,
            pill = if (state.unprintedInvoices == 0) "همه چاپ شده" else "چاپ‌نشده",
            pillTone = if (state.unprintedInvoices == 0) PillTone.SUCCESS else PillTone.WARNING,
        ),
        DashboardAction(
            title = "گزارش عملکرد",
            subtitle = "فروش، بازدید و پورسانت",
            emoji = "📊",
            route = Routes.REPORTS,
        ),
        DashboardAction(
            title = "پیام‌ها و هشدارها",
            subtitle = if (state.unreadAlerts > 0) {
                "هشدار صف ارسال و اعلان‌های سرور"
            } else {
                "اعلان زنده‌ی سرور"
            },
            emoji = if (state.unreadAlerts > 0) "⚠️" else "🔔",
            route = Routes.MESSAGES,
            badge = state.unreadMessages,
            badgeTone = if (state.unreadAlerts > 0) PillTone.DANGER else PillTone.WARNING,
            pill = when {
                state.unreadAlerts > 0 -> "${formatCount(state.unreadAlerts)} هشدار فعال"
                state.unreadMessages > 0 -> "پیام جدید"
                else -> "بدون پیام جدید"
            },
            pillTone = when {
                state.unreadAlerts > 0 -> PillTone.DANGER
                state.unreadMessages > 0 -> PillTone.WARNING
                else -> PillTone.NEUTRAL
            },
        ),
        DashboardAction(
            title = "مجوز خارج از محدوده",
            subtitle = "درخواست ویزیت خارج از تور",
            emoji = "🔑",
            route = Routes.ZONE_PERMISSIONS,
            pill = if (state.usablePermissions > 0) {
                "${formatCount(state.usablePermissions)} مجوز فعال"
            } else {
                "بدون مجوز فعال"
            },
            pillTone = if (state.usablePermissions > 0) PillTone.SUCCESS else PillTone.NEUTRAL,
        ),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ),
        contentPadding = PaddingValues(
            start = ZagrosSpacing.lg,
            end = ZagrosSpacing.lg,
            top = ZagrosSpacing.lg,
            bottom = ZagrosSpacing.xxl,
        ),
        horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DashboardHeader(
                greeting = state.greeting,
                name = state.fullName.ifBlank { state.username },
                date = state.todayLabel,
                isOnline = state.isOnline,
                onSettings = { onNavigate(Routes.SETTINGS) },
            )
        }

        if (state.error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OfflineBanner(pendingCount = state.queue.total, isOnline = state.isOnline)
            }
        } else if (!state.isOnline || state.queue.total > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OfflineBanner(pendingCount = state.queue.total, isOnline = state.isOnline)
            }
        }

        item {
            HeroMetricCard(
                label = "فروش امروز",
                value = state.salesToday,
                emoji = "💳",
                gradient = accents.tileGradients[0],
                caption = "${formatCount(state.ordersToday)} سفارش ثبت‌شده",
            )
        }
        item {
            HeroMetricCard(
                label = "تحقق هدف",
                value = state.goals.achievedValue,
                emoji = "🎯",
                gradient = accents.tileGradients[1],
                caption = if (state.goals.targetValue > 0) {
                    "هدف ${formatMoney(state.goals.targetValue)}"
                } else {
                    "هدفی تعیین نشده"
                },
                progress = state.targetProgress,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ProgressCard(
                title = "پیشرفت مسیر امروز",
                progress = state.routeProgress,
                subtitle = "${formatCount(state.visitedCount)} بازدید از ${formatCount(state.customerCount)} مشتری",
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "دسترسی سریع",
                subtitle = "کارهای روزانه‌ی ویزیت",
            )
        }

        itemsIndexed(actions, key = { _, item -> item.route }) { index, action ->
            GridActionTile(
                title = action.title,
                subtitle = action.subtitle,
                emoji = action.emoji,
                gradient = accents.tileGradients[index % accents.tileGradients.size],
                badge = action.badge,
                badgeTone = action.badgeTone,
                pillText = action.pill,
                pillTone = action.pillTone,
                onClick = { onNavigate(action.route) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ZagrosCard(contentPadding = PaddingValues(vertical = ZagrosSpacing.xs)) {
                StatRow(
                    label = "سابقه‌ی سفارش‌ها",
                    value = "",
                    emoji = "🧾",
                    gradient = accents.tileGradients[5],
                    onClick = { onNavigate(Routes.ORDERS) },
                )
                ThinDivider()
                StatRow(
                    label = "پروفایل و تنظیمات",
                    value = "",
                    emoji = "⚙️",
                    gradient = accents.tileGradients[0],
                    onClick = { onNavigate(Routes.SETTINGS) },
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    greeting: String,
    name: String,
    date: String,
    isOnline: Boolean,
    onSettings: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ZagrosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).ifBlank { "و" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(ZagrosSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting${if (name.isBlank()) "" else "، $name"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) accents.success else accents.danger),
                )
                Spacer(Modifier.width(ZagrosSpacing.xs))
                Text(
                    text = if (isOnline) "$date • آنلاین" else "$date • آفلاین",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Settings,
                contentDescription = "تنظیمات",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

