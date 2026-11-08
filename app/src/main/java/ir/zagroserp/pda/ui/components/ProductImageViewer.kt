package ir.zagroserp.pda.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

/**
 * Full-screen product photo viewer: pinch to zoom, drag to pan, double tap to toggle zoom.
 * Opened by tapping a product image in the catalog grid or the detail sheet.
 */
@Composable
fun ProductImageViewer(
    imageUrl: String?,
    title: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120)),
        ) {
            // One painter drives both the image and the loading/error indicators. Using a second
            // `AsyncImage` here previously left the spinner spinning forever, because the painter
            // whose state was inspected was never attached to the composition.
            val painter = rememberAsyncImagePainter(model = imageUrl)
            val state = painter.state
            val isLoading = state is AsyncImagePainter.State.Loading
            val isError = state is AsyncImagePainter.State.Error

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, bottom = 96.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    reset()
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    }
                    .transformable(
                        state = rememberTransformableState { zoomChange, panChange, _ ->
                            val next = (scale * zoomChange).coerceIn(1f, 6f)
                            scale = next
                            offset = if (next <= 1.01f) Offset.Zero else offset + panChange
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl.isNullOrBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🖼️",
                            style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "برای این کالا تصویری ثبت نشده است",
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                } else {
                    Image(
                        painter = painter,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                            ),
                    )
                    when {
                        isLoading -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                        )

                        isError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚠️", style = androidx.compose.material3.MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "بارگذاری تصویر ناموفق بود",
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        text = "برای بزرگنمایی دو بار لمس کنید",
                        color = Color.White.copy(alpha = 0.6f),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "بستن", tint = Color.White)
                    }
                }
            }

            if (scale > 1f) {
                Text(
                    text = "بزرگنمایی ${"%.1f".format(scale)}×",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                )
            }
        }
    }
}


