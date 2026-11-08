package ir.zagroserp.pda.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.prefs.ThemeMode
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.model.PendingQueueSummary
import ir.zagroserp.pda.domain.repository.AuthRepository
import ir.zagroserp.pda.domain.repository.LocationRepository
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.domain.repository.SettingsRepository
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.domain.repository.VisitRepository
import ir.zagroserp.pda.location.LocationProvider
import ir.zagroserp.pda.location.LocationTrackingService
import ir.zagroserp.pda.push.PushState
import ir.zagroserp.pda.push.PushTokenManager
import ir.zagroserp.pda.work.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SettingsUiState(
    val fullName: String = "",
    val username: String = "",
    val baseUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pending: PendingQueueSummary = PendingQueueSummary(),
    val lastSyncAtIso: String = "",
    val serverVersion: String = "",
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isTrackingLocation: Boolean = false,
    val lastLocationLabel: String = "",
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
@Suppress("UNCHECKED_CAST")
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val serverConfigStore: ServerConfigStore,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val orderRepository: OrderRepository,
    private val visitRepository: VisitRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val pushTokenManager: PushTokenManager,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    /**
     * Kept out of the main `combine` on purpose: it changes rarely and would only widen an
     * already large flow combination.
     */
    val pushState: StateFlow<PushState> = pushTokenManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PushState(),
    )

    private val syncing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val serverVersion = MutableStateFlow("")
    private val trackingLocation = MutableStateFlow(LocationTrackingService.isRunning)
    private val lastLocation = MutableStateFlow<LocationPoint?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        sessionStore.session,
        settingsRepository.themeMode,
        syncRepository.pendingSummary,
        syncRepository.observeLastSyncAt(),
        serverVersion,
        serverConfigStore.baseUrl,
        syncing,
        message,
        error,
        trackingLocation,
        lastLocation,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val session = values[0] as ir.zagroserp.pda.data.prefs.Session?
        val lastSync = values[3] as Long
        val point = values[10] as LocationPoint?
        SettingsUiState(
            fullName = session?.fullName.orEmpty(),
            username = session?.username.orEmpty(),
            baseUrl = values[5] as String,
            themeMode = values[1] as ThemeMode,
            pending = values[2] as PendingQueueSummary,
            lastSyncAtIso = if (lastSync > 0) Instant.ofEpochMilli(lastSync).toString() else "",
            serverVersion = values[4] as String,
            isSyncing = values[6] as Boolean,
            message = values[7] as String?,
            error = values[8] as String?,
            isTrackingLocation = values[9] as Boolean,
            lastLocationLabel = point?.let {
                "${it.latitude} , ${it.longitude}"
            }.orEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    init {
        viewModelScope.launch {
            authRepository.health().let { result ->
                if (result is AppResult.Success) serverVersion.value = result.data.serverVersion
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncing.value = true
            message.value = null
            error.value = null
            syncScheduler.requestImmediateSync()

            when (val delta = syncRepository.syncDelta()) {
                is AppResult.Success -> message.value = "همگام‌سازی با موفقیت انجام شد"
                is AppResult.Failure -> error.value = delta.error.message
            }
            syncing.value = false
        }
    }

    /**
     * Manual "send what is queued" – the escape hatch when the visitor wants to force the
     * offline queue out instead of waiting for WorkManager's 15-minute window.
     *
     * Each queue is drained independently and the per-queue outcome is reported, so a failure in
     * one does not hide progress in the others.
     */
    fun retryQueue() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            message.value = null
            error.value = null

            val before = state.value.pending
            val failures = mutableListOf<String>()

            when (val result = orderRepository.syncPending()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> failures += "سفارش‌ها: ${result.error.message}"
            }
            when (val result = visitRepository.syncPending()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> failures += "بازدیدها: ${result.error.message}"
            }
            when (val result = locationRepository.flush()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> failures += "موقعیت‌ها: ${result.error.message}"
            }

            val after = state.value.pending
            val sent = (before.total - after.total).coerceAtLeast(0)

            if (failures.isEmpty()) {
                message.value = if (sent > 0) {
                    "${JalaliDate.toPersianDigits(sent.toString())} مورد با موفقیت ارسال شد"
                } else {
                    "چیزی برای ارسال باقی نمانده است"
                }
            } else {
                error.value = buildString {
                    append("ارسال کامل نشد. ")
                    append(failures.joinToString(" | "))
                    if (after.hasFailures) {
                        append(" (${JalaliDate.toPersianDigits(after.failedOrders.toString())} سفارش ناموفق)")
                    }
                }
            }
            syncing.value = false
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            syncRepository.clearLocalData()
            message.value = "داده‌های محلی پاک شد"
        }
    }

    fun startLocationTracking() {
        LocationTrackingService.start(context)
        trackingLocation.value = true
        message.value = "ردیابی موقعیت فعال شد"
    }

    fun stopLocationTracking() {
        LocationTrackingService.stop(context)
        trackingLocation.value = false
        message.value = "ردیابی موقعیت متوقف شد"
    }

    /** Refreshes the "is tracking live" indicator, e.g. after returning to the screen. */
    fun refreshTrackingState() {
        trackingLocation.value = LocationTrackingService.isRunning
    }



    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            LocationTrackingService.stop(context)
            authRepository.logout()
            onLoggedOut()
        }
    }
}
