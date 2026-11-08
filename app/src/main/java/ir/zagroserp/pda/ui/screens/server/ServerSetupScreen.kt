package ir.zagroserp.pda.ui.screens.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.core.ServerAddress
import ir.zagroserp.pda.data.remote.ProbeResult
import ir.zagroserp.pda.ui.components.BrandHeader
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ServerRadarIcon
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ThinDivider
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.IranSans
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

private val quickAddresses = listOf(
    "https://dalbara.ir" to "دامنه‌ی ابری",
    "http://192.168.1.50:8080" to "سرور داخلی",
    "http://10.0.2.2:8080" to "شبیه‌ساز",
)

/**
 * Second stop after the permissions step: the visitor points the install at their company's
 * server and proves the connection works before the login form unlocks.
 */
@Composable
fun ServerSetupScreen(
    onReady: () -> Unit,
    viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BrandHeader(
            title = "اتصال به سرور شرکت",
            subtitle = "آدرس سرویس زاگرس سازمان خود را وارد کنید",
        ) {
            ServerRadarIcon(size = 88.dp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(ZagrosSpacing.lg),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, accents.border),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(ZagrosSpacing.xl)) {
                    ZagrosTextField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = "آدرس سرور",
                        placeholder = "https://dalbara.ir  یا  http://192.168.1.50:8080",
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        fontFamily = IranSans,
                        enabled = !state.isTesting && !state.isSaving,
                    )

                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    Text(
                        text = "بخش /api/v1 در صورت نبود، خودکار اضافه می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )

                    Spacer(Modifier.height(ZagrosSpacing.md))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
                    ) {
                        quickAddresses.forEach { (address, label) ->
                            QuickAddressChip(
                                label = label,
                                selected = state.address.trim().trimEnd('/') == address,
                                enabled = !state.isTesting && !state.isSaving,
                                onClick = { viewModel.useAddress(address) },
                            )
                        }
                    }

                    if (state.address.isNotBlank()) {
                        Spacer(Modifier.height(ZagrosSpacing.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                text = if (state.isSecure) "HTTPS امن" else "HTTP بدون رمزنگاری",
                                tone = if (state.isSecure) PillTone.SUCCESS else PillTone.WARNING,
                                leadingDot = true,
                            )
                            if (state.normalized.isNotBlank()) {
                                Spacer(Modifier.width(ZagrosSpacing.sm))
                                Text(
                                    text = ServerAddress.display(state.normalized),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accents.textSecondary,
                                    // A host/port must stay readable in Latin digits.
                                    fontFamily = IranSans,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(ZagrosSpacing.xl))

                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        enabled = !state.isTesting && !state.isSaving && state.address.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        if (state.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(ZagrosSpacing.sm))
                            Text("در حال تست ارتباط…")
                        } else {
                            Text("تست ارتباط", fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = state.error != null) {
                        Column {
                            Spacer(Modifier.height(ZagrosSpacing.md))
                            StatusPill(text = state.error.orEmpty(), tone = PillTone.DANGER)
                        }
                    }

                    AnimatedVisibility(visible = state.probe is ProbeResult.Success) {
                        Column {
                            Spacer(Modifier.height(ZagrosSpacing.md))
                            ProbeSuccessCard(
                                serverVersion = state.serverVersion,
                                minAppVersion = state.minAppVersion,
                                baseUrl = ServerAddress.display(state.normalized),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.probe != null && state.probe !is ProbeResult.Success,
                    ) {
                        Column {
                            Spacer(Modifier.height(ZagrosSpacing.md))
                            StatusPill(
                                text = probeFailureMessage(state.probe),
                                tone = PillTone.DANGER,
                            )
                        }
                    }

                    Spacer(Modifier.height(ZagrosSpacing.xl))

                    Button(
                        onClick = { viewModel.saveAndContinue(onReady) },
                        enabled = state.canContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = "ورود",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (!state.canContinue) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = "برای فعال شدن دکمه‌ی ورود، ابتدا تست ارتباط باید موفق شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(ZagrosSpacing.xl))
        }
    }
}

/**
 * Interactive address shortcut. Explicitly bordered and tinted so it reads as a control rather
 * than a static label.
 */
@Composable
private fun QuickAddressChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ZagrosSpacing.md, vertical = ZagrosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else accents.success),
            )
            Spacer(Modifier.width(ZagrosSpacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProbeSuccessCard(
    serverVersion: String,
    minAppVersion: String,
    baseUrl: String,
) {
    val accents = LocalZagrosAccents.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accents.successContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(ZagrosSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accents.success),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(ZagrosSpacing.sm))
                Text(
                    text = "ارتباط با سرور برقرار شد",
                    style = MaterialTheme.typography.titleSmall,
                    color = accents.success,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(ZagrosSpacing.sm))
            ThinDivider()
            Spacer(Modifier.height(ZagrosSpacing.sm))
            InfoRow("آدرس", baseUrl)
            InfoRow("نسخه‌ی سرور", JalaliDate.toPersianDigits(serverVersion.ifBlank { "—" }))
            if (minAppVersion.isNotBlank()) {
                InfoRow("حداقل نسخه‌ی اپ", JalaliDate.toPersianDigits(minAppVersion))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalZagrosAccents.current.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = IranSans,
        )
    }
}

private fun probeFailureMessage(probe: ProbeResult?): String = when (probe) {
    is ProbeResult.InvalidUrl -> probe.message
    is ProbeResult.HttpError -> "سرور پاسخ داد ولی خطا برگرداند (کد ${probe.code})"
    is ProbeResult.InvalidResponse -> probe.message
    is ProbeResult.Unreachable -> probe.message
    else -> "تست ارتباط ناموفق بود"
}
