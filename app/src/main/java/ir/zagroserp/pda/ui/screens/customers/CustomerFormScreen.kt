package ir.zagroserp.pda.ui.screens.customers

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

@Composable
fun CustomerFormScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CustomerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) viewModel.captureLocation() else viewModel.onLocationDenied()
    }

    LaunchedEffect(state.createdCustomerId) {
        state.createdCustomerId?.let { onCreated(it) }
    }

    ScreenScaffold(
        title = "ثبت مشتری جدید",
        subtitle = "موقعیت مکانی برای اعتبارسنجی محدوده‌ی تور لازم است",
        onBack = onBack,
        bottomBar = {
            Column(modifier = Modifier.padding(ZagrosSpacing.lg)) {
                Button(
                    onClick = {
                        if (state.hasLocationPermission) {
                            viewModel.submit()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("ثبت مشتری")
                    }
                }
            }
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            modifier = Modifier.imePadding(),
        ) {
            item {
                ZagrosCard {
                    Text(
                        text = "اطلاعات هویتی",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "نام فروشگاه / مشتری *",
                        isError = state.nameError != null,
                        errorText = state.nameError,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.nationalId,
                        onValueChange = viewModel::onNationalIdChange,
                        label = "شناسه ملی *",
                        keyboardType = KeyboardType.Number,
                        isError = state.nationalIdError != null,
                        errorText = state.nationalIdError,
                    )
                }
            }

            item {
                ZagrosCard {
                    Text(
                        text = "اطلاعات تماس",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "تلفن همراه",
                        keyboardType = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = "آدرس",
                        singleLine = false,
                        maxLines = 3,
                        imeAction = ImeAction.Done,
                    )
                }
            }

            item {
                ZagrosCard {
                    Text(
                        text = "اعتبار و تسویه",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.creditLimit,
                        onValueChange = viewModel::onCreditLimitChange,
                        label = "سقف اعتبار (ریال)",
                        placeholder = "۱,۲۵۰,۰۰۰",
                        keyboardType = KeyboardType.Number,
                        moneyFormat = true,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.settlementDays,
                        onValueChange = viewModel::onSettlementDaysChange,
                        label = "مهلت تسویه (روز)",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    )
                }
            }

            item {
                ZagrosCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "موقعیت مکانی",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        when {
                            state.isLocating -> StatusPill(text = "در حال دریافت…", tone = PillTone.WARNING)
                            state.latitude != null -> StatusPill(
                                text = "ثبت شد",
                                tone = PillTone.SUCCESS,
                                leadingDot = true,
                            )
                            else -> StatusPill(text = "نامشخص", tone = PillTone.NEUTRAL)
                        }
                    }
                    if (state.latitude != null && state.longitude != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = "عرض: ${state.latitude}   طول: ${state.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                    } else {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = "برای اعتبارسنجی محدوده‌ی تور، موقعیت مکانی لازم است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                    }
                    if (state.locationError != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        StatusPill(text = state.locationError!!, tone = PillTone.WARNING)
                    }
                }
            }

            if (state.error != null) {
                item {
                    StatusPill(text = state.error!!, tone = PillTone.DANGER)
                }
            }
        }
    }

    // The server refuses a duplicate national id with 409; the local cache catches the obvious
    // cases first. Either way the visitor gets a clear choice instead of a bare error.
    state.duplicate?.let { duplicate ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicate,
            title = { Text("این مشتری از قبل ثبت شده است") },
            text = {
                Column {
                    Text(
                        text = if (duplicate.existingName.isNotBlank()) {
                            "مشتری «${duplicate.existingName}» با همین کد ملی ثبت شده است."
                        } else {
                            "مشتری‌ای با این کد ملی از قبل ثبت شده است."
                        },
                    )
                    if (duplicate.existingId.isBlank()) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = "برای جلوگیری از ثبت تکراری، کد ملی را اصلاح کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalZagrosAccents.current.textSecondary,
                        )
                    }
                }
            },
            confirmButton = {
                if (duplicate.existingId.isNotBlank()) {
                    TextButton(onClick = {
                        viewModel.dismissDuplicate()
                        onCreated(duplicate.existingId)
                    }) { Text("مشاهده‌ی مشتری موجود") }
                } else {
                    TextButton(onClick = viewModel::dismissDuplicate) { Text("باشه") }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicate) { Text("ویرایش کد ملی") }
            },
        )
    }
}
