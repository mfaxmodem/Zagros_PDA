package ir.zagroserp.pda.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatCount

@Composable
fun ZagrosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(ZagrosSpacing.lg),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val accents = LocalZagrosAccents.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accents.border),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalZagrosAccents.current.textSecondary,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

enum class PillTone { NEUTRAL, SUCCESS, WARNING, DANGER, PRIMARY }

@Composable
fun StatusPill(
    text: String,
    tone: PillTone = PillTone.NEUTRAL,
    modifier: Modifier = Modifier,
    leadingDot: Boolean = false,
) {
    val accents = LocalZagrosAccents.current
    val (bg, fg) = when (tone) {
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to accents.textSecondary
        PillTone.SUCCESS -> accents.successContainer to accents.success
        PillTone.WARNING -> accents.warningContainer to accents.warning
        PillTone.DANGER -> accents.dangerContainer to accents.danger
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leadingDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    tone: PillTone = PillTone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val accents = LocalZagrosAccents.current
    val valueColor = when (tone) {
        PillTone.SUCCESS -> accents.success
        PillTone.WARNING -> accents.warning
        PillTone.DANGER -> accents.danger
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primary
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accents.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    message: String = "در حال دریافت اطلاعات…",
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
            Spacer(Modifier.height(ZagrosSpacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalZagrosAccents.current.textSecondary,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    emoji: String = "📭",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(ZagrosSpacing.xxl),
        ) {
            Text(text = emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(ZagrosSpacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(ZagrosSpacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = LocalZagrosAccents.current.textSecondary,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(ZagrosSpacing.lg))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(ZagrosSpacing.xxl),
        ) {
            Text(text = "⚠️", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(ZagrosSpacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(ZagrosSpacing.lg))
                TextButton(onClick = onRetry) { Text("تلاش دوباره") }
            }
        }
    }
}

@Composable
fun ZagrosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    maxLines: Int = if (singleLine) 1 else 4,
    /** Groups digits in threes while typing – for money fields. */
    moneyFormat: Boolean = false,
    /** Overrides the typography family, e.g. to keep URLs and version strings in Latin digits. */
    fontFamily: FontFamily? = null,
) {
    val accents = LocalZagrosAccents.current
    val effectiveTransformation = if (moneyFormat) {
        ThousandSeparatorTransformation()
    } else {
        visualTransformation
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = effectiveTransformation,
            textStyle = fontFamily?.let { MaterialTheme.typography.bodyLarge.copy(fontFamily = it) }
                ?: LocalTextStyle.current,
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = accents.textSecondary) }
            },
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = accents.border,
            ),
        )
        AnimatedVisibility(visible = isError && errorText != null) {
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = accents.danger,
                modifier = Modifier.padding(start = ZagrosSpacing.md, top = ZagrosSpacing.xs),
            )
        }
    }
}

@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    gradient: List<Color>? = null,
    trackColor: Color? = null,
) {
    val colors = gradient ?: listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor ?: LocalZagrosAccents.current.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors)),
        )
    }
}

@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = contentDescription, tint = LocalContentColor.current)
    }
}

@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, tone: PillTone = PillTone.DANGER) {
    if (count <= 0) return
    val accents = LocalZagrosAccents.current
    val bg = when (tone) {
        PillTone.DANGER -> accents.danger
        PillTone.WARNING -> accents.warning
        PillTone.SUCCESS -> accents.success
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primary
        PillTone.NEUTRAL -> accents.textSecondary
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProgressRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalZagrosAccents.current.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalZagrosAccents.current.border),
    )
}

@Composable
fun LinearLoading(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = LocalZagrosAccents.current.border,
    )
}

@Composable
fun TagLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, LocalZagrosAccents.current.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = LocalZagrosAccents.current.textSecondary,
        )
    }
}

@Composable
fun RowSpacer(width: androidx.compose.ui.unit.Dp) = Spacer(Modifier.width(width))
