package ir.zagroserp.pda.ui.screens.messages
import androidx.compose.material.icons.filled.Refresh

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import ir.zagroserp.pda.domain.alerts.SystemAlerts
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatRelative

@Composable
fun MessagesScreen(
    onOpenOrders: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    ScreenScaffold(
        title = "پیام‌ها و هشدارها",
        subtitle = if (state.unreadCount > 0) "${state.unreadCount} پیام خوانده‌نشده" else "همه خوانده شده",
        onBack = onBack,
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (state.isStreaming) "زنده" else "قطع",
                    tone = if (state.isStreaming) PillTone.SUCCESS else PillTone.WARNING,
                    leadingDot = true,
                )
                if (state.unreadCount > 0) {
                    androidx.compose.material3.TextButton(onClick = viewModel::markAllRead) {
                        Text("همه خوانده شد", style = MaterialTheme.typography.labelMedium)
                    }
                }
                androidx.compose.material3.IconButton(onClick = viewModel::refresh) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                        contentDescription = "به‌روزرسانی",
                    )
                }
            }
        },
    ) {
        if (state.messages.isEmpty()) {
            EmptyState(
                title = "پیامی وجود ندارد",
                subtitle = "اعلان‌های تخصیص مسیر و هشدارها اینجا نمایش داده می‌شوند.",
                emoji = "🔔",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(ZagrosSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            ) {
                if (state.error != null) {
                    item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
                }
                items(state.messages, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        onClick = {
                            viewModel.markRead(message.id)
                            // Queue alerts are only useful if they lead somewhere.
                            if (message.type == SystemAlerts.TYPE_QUEUE_FAILED ||
                                message.type == SystemAlerts.TYPE_QUEUE_PENDING
                            ) {
                                onOpenOrders()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: Notification, onClick: () -> Unit) {
    val accents = LocalZagrosAccents.current
    ZagrosCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isRead) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emojiFor(message.type), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(ZagrosSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.title.ifBlank { message.type },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (!message.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                if (message.body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )
                }
                Spacer(Modifier.height(ZagrosSpacing.xs))
                Text(
                    text = formatRelative(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.textSecondary,
                )
            }
        }
    }
}

private fun emojiFor(type: String): String = when (type.uppercase()) {
    SystemAlerts.TYPE_QUEUE_FAILED -> "⚠️"
    SystemAlerts.TYPE_QUEUE_PENDING -> "☁️"
    "ROUTE_ASSIGNED" -> "🗺️"
    "ORDER_APPROVED" -> "✅"
    "ORDER_REJECTED" -> "❌"
    "ZONE_PERMISSION" -> "🔑"
    "INVOICE" -> "📦"
    else -> "🔔"
}
