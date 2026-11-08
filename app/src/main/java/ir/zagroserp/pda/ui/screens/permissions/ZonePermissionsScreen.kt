package ir.zagroserp.pda.ui.screens.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.model.ZonePermission
import ir.zagroserp.pda.domain.repository.ZonePermissionRepository
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosCard
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZonePermissionsUiState(
    val permissions: List<ZonePermission> = emptyList(),
    val usableCount: Int = 0,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ZonePermissionsViewModel @Inject constructor(
    private val repository: ZonePermissionRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ZonePermissionsUiState> = combine(
        repository.observePermissions(),
        repository.observeUsableCount(),
        refreshing,
        error,
    ) { permissions, usable, isRefreshing, currentError ->
        ZonePermissionsUiState(
            permissions = permissions,
            usableCount = usable,
            isRefreshing = isRefreshing,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ZonePermissionsUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            when (val result = repository.refresh()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> error.value = result.error.message
            }
            refreshing.value = false
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            when (val result = repository.cancel(id)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> error.value = result.error.message
            }
        }
    }
}

@Composable
fun ZonePermissionsScreen(
    onBack: () -> Unit,
    viewModel: ZonePermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accents = LocalZagrosAccents.current

    ScreenScaffold(
        title = "مجوزهای خارج از محدوده",
        subtitle = "${state.usableCount} مجوز قابل استفاده",
        onBack = onBack,
    ) {
        if (state.permissions.isEmpty()) {
            EmptyState(
                title = "درخواستی ثبت نشده",
                subtitle = "برای ویزیت مشتریان خارج از محدوده‌ی تور، از صفحه‌ی مشتری درخواست بدهید.",
                emoji = "🔑",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(ZagrosSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
            ) {
                if (state.error != null) {
                    item { StatusPill(text = state.error!!, tone = PillTone.DANGER) }
                }
                items(state.permissions, key = { it.id }) { permission ->
                    ZagrosCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = permission.customerName.ifBlank { permission.customerId },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${permission.scopeLabel} • ${formatDate(permission.validFrom)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accents.textSecondary,
                                )
                            }
                            StatusPill(
                                text = permission.statusLabel,
                                tone = when (permission.status.uppercase()) {
                                    "APPROVED" -> PillTone.SUCCESS
                                    "PENDING" -> PillTone.WARNING
                                    "REJECTED", "EXPIRED", "CANCELLED" -> PillTone.DANGER
                                    else -> PillTone.NEUTRAL
                                },
                                leadingDot = true,
                            )
                        }

                        if (permission.isUsable) {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            StatusPill(text = "قابل استفاده", tone = PillTone.SUCCESS, leadingDot = true)
                        }

                        if (permission.reason.isNotBlank()) {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            Text(
                                text = permission.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = accents.textSecondary,
                            )
                        }

                        if (permission.status.uppercase() == "PENDING") {
                            Spacer(Modifier.height(ZagrosSpacing.sm))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { viewModel.cancel(permission.id) }) {
                                    Text("لغو درخواست")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
