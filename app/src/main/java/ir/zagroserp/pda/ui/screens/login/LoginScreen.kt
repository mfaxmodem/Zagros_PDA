package ir.zagroserp.pda.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.domain.repository.AuthRepository
import ir.zagroserp.pda.domain.repository.LoginOutcome
import ir.zagroserp.pda.ui.components.BrandHeader
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val otp: String = "",
    val requires2Fa: Boolean = false,
    val requiresCompany: Boolean = false,
    val companies: List<Pair<String, String>> = emptyList(),
    val selectedCompanyId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
) {
    val canSubmit: Boolean
        get() = username.length >= 3 && password.length >= 8 && !isLoading &&
            (!requires2Fa || otp.length >= 4) &&
            (!requiresCompany || selectedCompanyId != null)
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onOtpChange(value: String) =
        _state.update { it.copy(otp = value.filter(Char::isDigit), error = null) }

    fun onCompanySelected(id: String) =
        _state.update { it.copy(selectedCompanyId = id, error = null) }

    fun submit(onLoggedIn: () -> Unit) {
        if (!_state.value.canSubmit) return
        val snapshot = _state.value
        _state.update { it.copy(isLoading = true, error = null, info = null) }

        viewModelScope.launch {
            val outcome = authRepository.login(
                username = snapshot.username,
                password = snapshot.password,
                otp = snapshot.otp.takeIf { it.isNotBlank() },
                companyId = snapshot.selectedCompanyId,
            )
            when (outcome) {
                is LoginOutcome.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    onLoggedIn()
                }

                LoginOutcome.Requires2Fa -> _state.update {
                    it.copy(
                        isLoading = false,
                        requires2Fa = true,
                        info = "کد دومرحله‌ای را وارد کنید",
                        error = null,
                    )
                }

                is LoginOutcome.RequiresCompany -> _state.update {
                    it.copy(
                        isLoading = false,
                        requiresCompany = true,
                        companies = outcome.companies,
                        info = "شرکت مورد نظر را انتخاب کنید",
                        error = null,
                    )
                }

                is LoginOutcome.Failed -> _state.update {
                    it.copy(isLoading = false, error = outcome.message)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BrandHeader(
            title = "ورود ویزیتور",
            subtitle = "با نام کاربری سازمانی خود وارد شوید",
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 30))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🧭", style = MaterialTheme.typography.displaySmall)
            }
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
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = "نام کاربری یا ایمیل",
                        placeholder = "visitor1",
                        imeAction = ImeAction.Next,
                    )
                    Spacer(Modifier.height(ZagrosSpacing.lg))
                    ZagrosTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "رمز عبور",
                        placeholder = "••••••••",
                        singleLine = true,
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.requires2Fa) ImeAction.Next else ImeAction.Done,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "پنهان" else "نمایش",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                    )

                    AnimatedVisibility(visible = state.requires2Fa) {
                        Column {
                            Spacer(Modifier.height(ZagrosSpacing.lg))
                            ZagrosTextField(
                                value = state.otp,
                                onValueChange = viewModel::onOtpChange,
                                label = "کد تأیید دومرحله‌ای",
                                placeholder = "۱۲۳۴۵۶",
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.requiresCompany && state.companies.isNotEmpty(),
                    ) {
                        Column {
                            Spacer(Modifier.height(ZagrosSpacing.lg))
                            Text(
                                text = "انتخاب شرکت",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            state.companies.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { viewModel.onCompanySelected(id) }
                                        .padding(vertical = ZagrosSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = state.selectedCompanyId == id,
                                        onClick = { viewModel.onCompanySelected(id) },
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(ZagrosSpacing.md))
                        StatusPill(text = state.error.orEmpty(), tone = PillTone.DANGER)
                    } else if (state.info != null) {
                        Spacer(Modifier.height(ZagrosSpacing.md))
                        StatusPill(text = state.info.orEmpty(), tone = PillTone.WARNING)
                    }

                    Spacer(Modifier.height(ZagrosSpacing.xl))
                    Button(
                        onClick = { viewModel.submit(onLoggedIn) },
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (state.isLoading) {
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
                }
            }

            Spacer(Modifier.height(ZagrosSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accents.textSecondary),
                )
                Spacer(Modifier.width(ZagrosSpacing.sm))
                Text(
                    text = "در صورت فراموشی رمز عبور با مدیر سیستم تماس بگیرید",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }
            Spacer(Modifier.height(ZagrosSpacing.xxl))
        }
    }
}
