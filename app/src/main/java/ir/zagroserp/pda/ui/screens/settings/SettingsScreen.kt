package ir.zagroserp.pda.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.data.prefs.ThemeMode
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.IranSans
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatDateTime
import ir.zagroserp.pda.ui.util.formatMoney

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeServer: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    // The service may have been restarted by the system since this screen was last shown.
    LaunchedEffect(Unit) { viewModel.refreshTrackingState() }

    ScreenScaffold(
        title = "تنظیمات",
        subtitle = "پروفایل، همگام‌سازی و ظاهر برنامه",
        onBack = onBack,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
        ) {
            item {
                ZagrosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.fullName.ifBlank { "ویزیتور" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = state.username.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                color = accents.textSecondary,
                            )
                        }
                        StatusPill(
                            text = if (state.isOnline) "آنلاین" else "آفلاین",
                            tone = if (state.isOnline) PillTone.SUCCESS else PillTone.DANGER,
                            leadingDot = true,
                        )
                    }
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(title = "ظاهر برنامه")
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        ThemeMode.entries.forEach { mode ->
                            StatusPill(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "سیستم"
                                    ThemeMode.LIGHT -> "روشن"
                                    ThemeMode.DARK -> "تیره"
                                },
                                tone = if (state.themeMode == mode) PillTone.PRIMARY else PillTone.NEUTRAL,
                                modifier = Modifier.clickable { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(
                        title = "سرور شرکت",
                        subtitle = "هر شرکت روی سرور خودش نصب می‌شود",
                    )
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    SettingRow("آدرس فعلی", state.baseUrl, latinValue = true)
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    OutlinedButton(
                        onClick = onChangeServer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text("تغییر آدرس سرور")
                    }
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(title = "همگام‌سازی")
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    SettingRow("آخرین همگام‌سازی", formatDateTime(state.lastSyncAtIso))
                    SettingRow("نسخه‌ی سرور", state.serverVersion.ifBlank { "—" })
                    SettingRow("نسخه‌ی اپ", BuildConfig.VERSION_NAME, latinValue = true)
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = !state.isSyncing,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(if (state.isSyncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون")
                        }
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("پاک‌سازی داده‌ها")
                        }
                    }
                }
            }

            item {
                ZagrosCard {
                    SectionHeader(
                        title = "صف آفلاین",
                        subtitle = "مواردی که هنوز به سرور نرسیده‌اند",
                    )
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    SettingRow("سفارش در صف", persian(state.pending.orders))
                    SettingRow("بازدید در صف", persian(state.pending.visits))
                    SettingRow("موقعیت در صف", persian(state.pending.locations))
                    if (state.pending.hasFailures) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        StatusPill(
                            text = "${persian(state.pending.failedOrders)} سفارش با خطای ارسال",
                            tone = PillTone.DANGER,
                        )
                    }
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Button(
                        onClick = viewModel::retryQueue,
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(if (state.isSyncing) "در حال ارسال…" else "ارسال / تلاش مجدد ارسال")
                    }
                }
            }

            item {
                val push by viewModel.pushState.collectAsStateWithLifecycle()

                ZagrosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(
                                title = "اعلان فوری (FCM)",
                                subtitle = "دریافت اعلان حتی وقتی اپ بسته است",
                            )
                        }
                        StatusPill(
                            text = push.statusLabel,
                            tone = when {
                                push.hasToken && push.registered -> PillTone.SUCCESS
                                push.hasToken -> PillTone.WARNING
                                else -> PillTone.DANGER
                            },
                            leadingDot = true,
                        )
                    }

                    if (push.tokenPreview.isNotBlank()) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        SettingRow("توکن دستگاه", push.tokenPreview, latinValue = true)
                    }

                    if (push.error != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = push.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalZagrosAccents.current.danger,
                        )
                    }

                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    Text(
                        text = "این قابلیت به Google Play Services روی دستگاه وابسته است. " +
                            "اگر روی این دستگاه فعال نشد، اعلان‌ها فقط وقتی اپ باز است دریافت می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalZagrosAccents.current.textSecondary,
                    )
                }
            }

            item {
                ZagrosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(
                                title = "ردیابی موقعیت",
                                subtitle = "ارسال زنده‌ی مسیر شما به پنل مدیریت",
                            )
                        }
                        StatusPill(
                            text = if (state.isTrackingLocation) "فعال" else "غیرفعال",
                            tone = if (state.isTrackingLocation) PillTone.SUCCESS else PillTone.DANGER,
                            leadingDot = true,
                        )
                    }

                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    SettingRow("در صف ارسال", persian(state.pending.locations))
                    if (state.lastLocationLabel.isNotBlank()) {
                        SettingRow("آخرین موقعیت ارسالی", state.lastLocationLabel, latinValue = true)
                    }

                    Spacer(Modifier.height(ZagrosSpacing.xs))
                    Text(
                        text = "برای صرفه‌جویی در باتری، موقعیت‌ها دسته‌ای و حداکثر هر یک دقیقه ارسال " +
                            "می‌شوند. وقتی اپ در پس‌زمینه است دقت کاهش می‌یابد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalZagrosAccents.current.textSecondary,
                    )

                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        Button(
                            onClick = {
                                viewModel.startLocationTracking()
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            enabled = !state.isTrackingLocation,
                            shape = MaterialTheme.shapes.small,
                        ) { Text("شروع ردیابی") }
                        TextButton(
                            onClick = viewModel::stopLocationTracking,
                            enabled = state.isTrackingLocation,
                        ) { Text("توقف") }
                    }

                }
            }

            if (state.message != null) {
                item { StatusPill(text = state.message!!, tone = PillTone.SUCCESS) }
            }
            if (state.error != null) {
                item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
            }

            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accents.dangerContainer,
                        contentColor = accents.danger,
                    ),
                ) {
                    Text("خروج از حساب")
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("خروج از حساب") },
            text = { Text("توکن دستگاه حذف می‌شود. موارد در صف ارسال باقی می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout(onLoggedOut)
                }) { Text("خروج") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("انصراف") }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("پاک‌سازی داده‌های محلی") },
            text = { Text("تمام کالاها، مشتریان و فاکتورهای کش‌شده حذف می‌شوند. صف آفلاین پاک نمی‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearLocalData()
                }) { Text("پاک کن") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("انصراف") }
            },
        )
    }
}

private fun persian(value: Int): String =
    ir.zagroserp.pda.core.JalaliDate.toPersianDigits(value.toString())

@Composable
private fun SettingRow(label: String, value: String, latinValue: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ZagrosSpacing.xs),
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
            // Hosts, ports and version strings keep Latin digits.
            fontFamily = if (latinValue) IranSans else null,
        )
    }
}
