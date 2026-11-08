package ir.zagroserp.pda.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

/**
 * Quantity control with circular gradient −/+ buttons. Used by the order sheet (packaging and
 * loose-piece counters) and by the cart rows.
 *
 * @param label caption rendered above the counter, e.g. the packaging name from the API `uoms`.
 */
@Composable
fun QuantityStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    min: Int = 0,
    max: Int = 9_999,
    enabled: Boolean = true,
    buttonSize: Dp = 40.dp,
    gradient: List<Color>? = null,
) {
    val accents = LocalZagrosAccents.current
    val colors = gradient ?: listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    val canDecrease = enabled && value > min
    val canIncrease = enabled && value < max

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accents.textSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = ZagrosSpacing.xs),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.xs),
        ) {
            StepperButton(
                isPlus = false,
                enabled = canDecrease,
                size = buttonSize,
                gradient = colors,
            ) { onValueChange((value - 1).coerceAtLeast(min)) }

            Box(
                modifier = Modifier
                    .widthIn(min = buttonSize + 16.dp)
                    .height(buttonSize)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ir.zagroserp.pda.core.JalaliDate.toPersianDigits(value.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StepperButton(
                isPlus = true,
                enabled = canIncrease,
                size = buttonSize,
                gradient = colors,
            ) { onValueChange((value + 1).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun StepperButton(
    isPlus: Boolean,
    enabled: Boolean,
    size: Dp,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    val background by animateColorAsState(
        targetValue = if (enabled) Color.Transparent else accents.border,
        label = "stepper-bg",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) Brush.linearGradient(gradient) else Brush.linearGradient(
                listOf(background, background),
            ))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size / 2.6f)) {
            val stroke = size.toPx() / 11f
            val color = Color.White
            val midY = this.size.height / 2f
            drawLine(
                color = color,
                start = Offset(0f, midY),
                end = Offset(this.size.width, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (isPlus) {
                val midX = this.size.width / 2f
                drawLine(
                    color = color,
                    start = Offset(midX, 0f),
                    end = Offset(midX, this.size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}


