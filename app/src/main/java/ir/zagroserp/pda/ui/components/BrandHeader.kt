package ir.zagroserp.pda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

private val HeaderGradient = listOf(Color(0xFF312E81), Color(0xFF4F46E5), Color(0xFF6366F1))

/**
 * Dark branded header used by the onboarding screens.
 *
 * Deliberately a *solid* dark gradient: an earlier version faded into the light background, which
 * left white text sitting on near-white pixels and unreadable.
 */
@Composable
fun BrandHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(HeaderGradient))
            .padding(
                start = ZagrosSpacing.xl,
                end = ZagrosSpacing.xl,
                top = ZagrosSpacing.xxl,
                bottom = ZagrosSpacing.xxl,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Spacer(Modifier.height(ZagrosSpacing.lg))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(ZagrosSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Concentric signal arcs around a centre dot – the "connect to server" mark. */
@Composable
fun ServerRadarIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 84.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 30))
            .background(Color.White.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val stroke = this.size.minDimension * 0.075f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val white = Color.White

            // Three expanding arcs, opened toward the top – a radar sweep.
            listOf(0.30f, 0.52f, 0.74f).forEachIndexed { index, factor ->
                drawArc(
                    color = white.copy(alpha = 0.95f - index * 0.22f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - this.size.width * factor / 2f,
                        center.y - this.size.height * factor / 2f,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width * factor,
                        this.size.height * factor,
                    ),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            drawCircle(color = white, radius = stroke * 1.15f, center = center)
            drawLine(
                color = white.copy(alpha = 0.75f),
                start = Offset(center.x, center.y + stroke * 1.2f),
                end = Offset(center.x, this.size.height * 0.92f),
                strokeWidth = stroke * 0.8f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Rounded gradient tile holding an emoji – used by the dashboard action tiles. */
@Composable
fun GradientIconBadge(
    emoji: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 32))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}


