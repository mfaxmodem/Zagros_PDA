package ir.zagroserp.pda.ui.screens.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.ui.components.BrandHeader
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

/**
 * First stop after the splash on a fresh install: collect the runtime permissions the field work
 * depends on, in the order Android requires (foreground location before background location).
 */
@Composable
fun PermissionsScreen(
    onDone: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onRequestFinished()
    }

    fun requestNext() {
        val permissions = viewModel.permissionsToRequest()
        if (permissions.isEmpty()) {
            viewModel.onRequestFinished()
        } else {
            launcher.launch(permissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        BrandHeader(
            title = "دسترسی‌های لازم",
            subtitle = "برای کار میدانی، زاگرس به چند دسترسی نیاز دارد",
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 30))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🔐", style = MaterialTheme.typography.displaySmall)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
        ) {
            items(state.rows, key = { it.kind.name }) { row ->
                ZagrosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (row.granted) accents.successContainer else accents.warningContainer,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    row.granted -> "✓"
                                    row.kind.required -> "!"
                                    else -> "؟"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (row.granted) accents.success else accents.warning,
                            )
                        }
                        Spacer(Modifier.width(ZagrosSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = row.kind.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Spacer(Modifier.width(ZagrosSpacing.sm))
                                StatusPill(
                                    text = when {
                                        row.granted -> "فعال"
                                        row.kind.required -> "الزامی"
                                        else -> "اختیاری"
                                    },
                                    tone = when {
                                        row.granted -> PillTone.SUCCESS
                                        row.kind.required -> PillTone.DANGER
                                        else -> PillTone.NEUTRAL
                                    },
                                    leadingDot = true,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = row.kind.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = accents.textSecondary,
                            )
                        }
                    }
                }
            }

            if (state.requestedOnce && !state.requiredGranted) {
                item {
                    StatusPill(
                        text = "بدون دسترسی موقعیت مکانی، ثبت بازدید و ایجاد مشتری کار نمی‌کند",
                        tone = PillTone.DANGER,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(ZagrosSpacing.lg),
        ) {
            Button(
                onClick = { requestNext() },
                enabled = !state.allGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = if (state.requestedOnce) "بررسی دوباره‌ی دسترسی‌ها" else "اعطای دسترسی‌ها",
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(ZagrosSpacing.sm))
            TextButton(
                onClick = { viewModel.markOnboarded(onDone) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.requiredGranted || state.requestedOnce,
            ) {
                Text(
                    text = if (state.requiredGranted) "ادامه" else "ادامه بدون دسترسی الزامی",
                    color = if (state.requiredGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        accents.textSecondary
                    },
                )
            }
        }
    }
}
