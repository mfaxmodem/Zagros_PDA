package ir.zagroserp.pda.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.domain.repository.AuthRepository
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.ui.components.GradientProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val progress: Float = 0f,
    val message: String = "بررسی نسخه و اتصال…",
    val updateRequired: Boolean = false,
    val serverVersion: String = "",
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val sessionStore: SessionStore,
    private val serverConfigStore: ServerConfigStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun boot(
        onLoggedIn: () -> Unit,
        onNeedsLogin: () -> Unit,
        onNeedsServerSetup: () -> Unit,
        onNeedsPermissions: () -> Unit,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(progress = 0.2f)

            // Hydrate the non-suspending mirrors the OkHttp interceptors read, so the very first
            // request of this process already carries the right host and token.
            serverConfigStore.warmUp()
            sessionStore.warmUp()

            // 0) Fresh install: permissions first, then the server address.
            if (!sessionStore.onboarded.first()) {
                _state.value = _state.value.copy(
                    progress = 0.7f,
                    message = "آماده‌سازی دسترسی‌ها",
                )
                delay(350)
                onNeedsPermissions()
                return@launch
            }

            if (!serverConfigStore.isConfigured.first()) {
                _state.value = _state.value.copy(
                    progress = 0.7f,
                    message = "آدرس سرور شرکت را وارد کنید",
                )
                delay(350)
                onNeedsServerSetup()
                return@launch
            }

            // 1) Version handshake – public endpoint, works before login.
            val health = authRepository.health()
            if (health is AppResult.Success) {
                val minVersion = health.data.minAppVersion
                _state.value = _state.value.copy(
                    progress = 0.4f,
                    serverVersion = health.data.serverVersion,
                )
                if (isUpdateRequired(BuildConfig.VERSION_NAME, minVersion)) {
                    _state.value = _state.value.copy(
                        updateRequired = true,
                        message = "نسخه‌ی جدید اپ لازم است (حداقل $minVersion)",
                    )
                    delay(1500)
                    return@launch
                }
            } else {
                _state.value = _state.value.copy(
                    message = "اتصال به سرور برقرار نشد؛ حالت آفلاین",
                )
            }

            val loggedIn = sessionStore.isLoggedIn.first()
            if (!loggedIn) {
                _state.value = _state.value.copy(progress = 1f, message = "ورود به حساب")
                delay(120)
                onNeedsLogin()
                return@launch
            }

            _state.value = _state.value.copy(progress = 0.7f, message = "همگام‌سازی داده‌ها…")
            authRepository.refreshIfNeeded()
            runCatching { syncRepository.syncDelta() }

            _state.value = _state.value.copy(progress = 1f, message = "آماده")
            delay(120)
            onLoggedIn()
        }
    }

    /** Semver-ish comparison that tolerates suffixes such as `1.2.0-beta`. */
    private fun isUpdateRequired(current: String, minimum: String): Boolean {
        if (minimum.isBlank()) return false
        val a = current.split(".", "-").mapNotNull { it.toIntOrNull() }
        val b = minimum.split(".", "-").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }
}

@Composable
fun SplashScreen(
    onLoggedIn: () -> Unit,
    onNeedsLogin: () -> Unit,
    onNeedsServerSetup: () -> Unit,
    onNeedsPermissions: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.boot(onLoggedIn, onNeedsLogin, onNeedsServerSetup, onNeedsPermissions)
    }

    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(600),
        label = "splash",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4F46E5), Color(0xFF4338CA), Color(0xFF0F172A)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🧭", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "زاگرس PDA",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "دستیار فروش و ویزیت میدانی",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(48.dp))
            GradientProgressBar(
                progress = progress,
                gradient = listOf(Color.White, Color(0xFFA5B4FC)),
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            if (state.updateRequired) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "لطفاً از بازار/سایت رسمی، نسخه‌ی جدید را نصب کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
