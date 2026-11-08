package ir.zagroserp.pda.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosDimens
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatMoneyShort
import ir.zagroserp.pda.ui.util.formatPercent

/**
 * Counts up to [value] when the composable enters composition. Used by the hero tiles so
 * metrics feel alive without being distracting.
 */
@Composable
fun AnimatedCounter(
    value: Double,
    modifier: Modifier = Modifier,
    durationMillis: Int = 900,
    formatter: (Double) -> String = { formatMoneyShort(it) },
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatable.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing),
        )
    }
    Text(
        text = formatter(animatable.value.toDouble()),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Picks a text style that keeps a grouped amount on a single line inside a half-width tile. */
@Composable
private fun amountStyle(formatted: String): TextStyle = when {
    formatted.length <= 10 -> MaterialTheme.typography.headlineSmall
    formatted.length <= 14 -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp,
    colors: List<Color>? = null,
    trackColor: Color? = null,
    content: @Composable (() -> Unit)? = null,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = LinearOutSlowInEasing),
        label = "ring",
    )
    val ringColors = colors ?: listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor ?: Color(0x22808080),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(ringColors),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content?.invoke()
    }
}

/**
 * Primary dashboard metric.
 *
 * The headline is the **full** amount (grouped, never truncated); the abbreviated "میلیون" form
 * sits underneath as a quick glance value, and the caption carries the business context.
 */
@Composable
fun HeroMetricCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    emoji: String = "🎯",
    gradient: List<Color>,
    caption: String? = null,
    progress: Float? = null,
    valueFormatter: (Double) -> String = { formatMoney(it) },
    shortFormatter: (Double) -> String = { formatMoneyShort(it) },
) {
    val fullText = valueFormatter(value)
    val numberStyle = amountStyle(fullText)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ZagrosDimens.heroTileHeight),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient)),
        ) {
            // Decorative glow so the tiles do not read as flat rectangles.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 36.dp, y = (-36).dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-28).dp, y = 28.dp)
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ZagrosSpacing.lg),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(ZagrosSpacing.sm))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                    )
                }

                Column {
                    AnimatedCounter(
                        value = value,
                        formatter = valueFormatter,
                        style = numberStyle,
                        color = Color.White,
                    )
                    Text(
                        text = shortFormatter(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    if (caption != null) {
                        Spacer(Modifier.height(ZagrosSpacing.xs))
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (progress != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        GradientProgressBar(
                            progress = progress,
                            gradient = listOf(Color.White, Color.White.copy(alpha = 0.6f)),
                            trackColor = Color.White.copy(alpha = 0.25f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dashboard quick action. The icon sits in a gradient badge, a tinted blob and a gradient accent
 * bar give it depth, and the badge count / status pill carry the state.
 */
@Composable
fun GridActionTile(
    title: String,
    subtitle: String,
    emoji: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    badgeTone: PillTone = PillTone.DANGER,
    pillText: String? = null,
    pillTone: PillTone = PillTone.SUCCESS,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ZagrosDimens.actionTileHeight)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accents.border),
        shadowElevation = 2.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-34).dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(gradient.first().copy(alpha = 0.10f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ZagrosSpacing.md),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    GradientIconBadge(emoji = emoji, gradient = gradient, size = 46.dp)
                    if (badge > 0) {
                        CountBadge(count = badge, tone = badgeTone)
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pillText != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        StatusPill(text = pillText, tone = pillTone, leadingDot = true)
                    }
                }
            }

            // Gradient accent bar pinned to the bottom edge.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(gradient)),
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emoji: String = "",
    tone: PillTone = PillTone.NEUTRAL,
    gradient: List<Color>? = null,
) {
    val accents = LocalZagrosAccents.current
    val valueColor = when (tone) {
        PillTone.SUCCESS -> accents.success
        PillTone.WARNING -> accents.warning
        PillTone.DANGER -> accents.danger
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primary
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    val badgeGradient = gradient ?: accents.tileGradients.first()

    Surface(
        modifier = modifier.height(ZagrosDimens.statTileHeight),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accents.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ZagrosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji.isNotEmpty()) {
                GradientIconBadge(emoji = emoji, gradient = badgeGradient, size = 34.dp)
                Spacer(Modifier.width(ZagrosSpacing.sm))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Full-width metric row. Used instead of a three-across grid where the labels ("سفارش‌های
 * ناموفق", "پورسانت تخمینی") were being ellipsized into meaninglessness on a phone width.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.NEUTRAL,
    gradient: List<Color>? = null,
    onClick: (() -> Unit)? = null,
) {
    val accents = LocalZagrosAccents.current
    val valueColor = when (tone) {
        PillTone.SUCCESS -> accents.success
        PillTone.WARNING -> accents.warning
        PillTone.DANGER -> accents.danger
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primary
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    val badgeGradient = gradient ?: accents.tileGradients.first()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ZagrosSpacing.md, vertical = ZagrosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradientIconBadge(emoji = emoji, gradient = badgeGradient, size = 34.dp)
        Spacer(Modifier.width(ZagrosSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
        )
        if (onClick != null) {
            Spacer(Modifier.width(ZagrosSpacing.sm))
            Text(
                text = "‹",
                style = MaterialTheme.typography.titleMedium,
                color = accents.textSecondary,
            )
        }
    }
}

@Composable
fun OfflineBanner(
    pendingCount: Int,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false,
) {
    val accents = LocalZagrosAccents.current
    if (pendingCount <= 0 && isOnline) return
    val text = when {
        !isOnline && pendingCount > 0 -> "آفلاین — $pendingCount مورد در صف ارسال"
        !isOnline -> "آفلاین هستید؛ داده‌ها به‌صورت محلی ذخیره می‌شوند"
        else -> "$pendingCount مورد در انتظار همگام‌سازی"
    }
    val tone = if (isOnline) PillTone.WARNING else PillTone.DANGER
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (tone == PillTone.DANGER) accents.dangerContainer else accents.warningContainer,
            )
            .padding(horizontal = ZagrosSpacing.md, vertical = ZagrosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = if (isOnline) "🔄" else "📴", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(ZagrosSpacing.sm))
        Text(
            text = ir.zagroserp.pda.core.JalaliDate.toPersianDigits(text),
            style = MaterialTheme.typography.bodySmall,
            color = if (tone == PillTone.DANGER) accents.danger else accents.warning,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ProgressCard(
    title: String,
    progress: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    ZagrosCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp,
            ) {
                Text(
                    text = formatPercent(progress * 100.0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(ZagrosSpacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalZagrosAccents.current.textSecondary,
                    )
                }
            }
        }
    }
}
