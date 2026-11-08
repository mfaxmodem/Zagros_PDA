package ir.zagroserp.pda.ui.screens.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.SectionHeader
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing

/**
 * Edits a customer's contact details and geographical position.
 *
 * Replaces the previous one-tap "capture coordinates" action so every change to a customer lives
 * in a single, reviewable form.
 */
@Composable
fun CustomerEditScreen(
    onBack: () -> Unit,
    viewModel: CustomerEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    ScreenScaffold(
        title = "ویرایش مشتری",
        subtitle = state.name.ifBlank { null },
        onBack = onBack,
        bottomBar = {
            Column(modifier = Modifier.padding(ZagrosSpacing.lg)) {
                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = state.canSave && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) {
        if (state.isLoading) {
            LoadingBox(message = "در حال دریافت اطلاعات مشتری…")
            return@ScreenScaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            modifier = Modifier.imePadding(),
        ) {
            item {
                ZagrosCard {
                    SectionHeader(
                        title = "اطلاعات تماس",
                        subtitle = "شناسه ملی و اطلاعات اعتباری قابل ویرایش نیستند",
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "نام مشتری *",
                        isError = state.nameError != null,
                        errorText = state.nameError,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    ZagrosTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "تلفن همراه",
                        placeholder = "09120000000",
                        keyboardType = KeyboardType.Phone,
                        isError = state.phoneError != null,
                        errorText = state.phoneError,
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
                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.md)) {
                        ZagrosTextField(
                            value = state.city,
                            onValueChange = viewModel::onCityChange,
                            label = "شهر",
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next,
                        )
                        ZagrosTextField(
                            value = state.province,
                            onValueChange = viewModel::onProvinceChange,
                            label = "استان",
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Done,
                        )
                    }
                }
            }

            item {
                ZagrosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(title = "موقعیت مکانی")
                        }
                        StatusPill(
                            text = if (state.hasCoordinates) "ثبت‌شده" else "ثبت نشده",
                            tone = if (state.hasCoordinates) PillTone.SUCCESS else PillTone.WARNING,
                            leadingDot = true,
                        )
                    }

                    if (state.coordinatesLabel.isNotBlank()) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        Text(
                            text = state.coordinatesLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = ir.zagroserp.pda.ui.theme.IranSans,
                        )
                    }

                    Spacer(Modifier.height(ZagrosSpacing.md))
                    OutlinedButton(
                        onClick = viewModel::captureLocation,
                        enabled = !state.isLocating && !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (state.isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(ZagrosSpacing.sm))
                            Text("در حال دریافت موقعیت…")
                        } else {
                            Text("ثبت موقعیت فعلی من")
                        }
                    }

                    if (state.hasSavedCoordinates) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        OutlinedButton(
                            onClick = viewModel::measureDistance,
                            enabled = !state.isMeasuring && !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                if (state.isMeasuring) {
                                    "در حال اندازه‌گیری…"
                                } else {
                                    "اندازه‌گیری فاصله تا مشتری"
                                },
                            )
                        }
                    }

                    state.distanceLabel?.let { label ->
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        StatusPill(
                            text = "فاصله فعلی: $label (حد مجاز ۵۰ متر)",
                            tone = if (state.withinRadius == true) PillTone.SUCCESS else PillTone.DANGER,
                            leadingDot = true,
                        )
                    }

                    Spacer(Modifier.height(ZagrosSpacing.md))
                    Text(
                        text = "با ثبت مختصات، از این پس ثبت بازدید برای این مشتری فقط از " +
                            "شعاع ۵۰ متری امکان‌پذیر می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.textSecondary,
                    )
                }
            }

            if (state.error != null) {
                item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
            }
            if (state.message != null) {
                item { StatusPill(text = state.message!!, tone = PillTone.SUCCESS) }
            }

            item { Spacer(Modifier.height(ZagrosSpacing.xxl)) }
        }
    }
}
