package ir.zagroserp.pda.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.ConnectivityObserver
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.domain.model.Goals
import ir.zagroserp.pda.domain.model.PendingQueueSummary
import ir.zagroserp.pda.domain.model.TodaySnapshot
import ir.zagroserp.pda.domain.repository.InvoiceRepository
import ir.zagroserp.pda.domain.repository.MessageRepository
import ir.zagroserp.pda.domain.repository.ReportRepository
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.domain.repository.VisitRepository
import ir.zagroserp.pda.domain.repository.ZonePermissionRepository
import ir.zagroserp.pda.work.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val fullName: String = "",
    val username: String = "",
    val today: TodaySnapshot = TodaySnapshot(),
    val goals: Goals = Goals(),
    val queue: PendingQueueSummary = PendingQueueSummary(),
    val unreadMessages: Int = 0,
    val unreadAlerts: Int = 0,
    val usablePermissions: Int = 0,
    val unprintedInvoices: Int = 0,
    val salesToday: Double = 0.0,
    val ordersToday: Int = 0,
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val visitedCount: Int get() = today.visited.size
    val customerCount: Int get() = today.customers.size
    val pendingCount: Int get() = today.pendingCustomers.size
    val routeProgress: Float
        get() = if (customerCount == 0) 0f else visitedCount.toFloat() / customerCount
    val targetProgress: Float
        get() = when {
            goals.targetValue > 0 -> (goals.achievedValue / goals.targetValue).toFloat().coerceIn(0f, 1f)
            else -> (goals.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
        }

    val greeting: String
        get() {
            val hour = java.time.LocalTime.now().hour
            return when (hour) {
                in 5..11 -> "صبح بخیر"
                in 12..16 -> "روز بخیر"
                in 17..20 -> "عصر بخیر"
                else -> "شب بخیر"
            }
        }

    val todayLabel: String
        get() {
            val jalali = JalaliDate.todayJalali()
            return "${JalaliDate.weekdayName(JalaliDate.todayIso())} ${jalali.formatLong()}"
        }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val reportRepository: ReportRepository,
    private val syncRepository: SyncRepository,
    private val messageRepository: MessageRepository,
    private val invoiceRepository: InvoiceRepository,
    private val zonePermissionRepository: ZonePermissionRepository,
    private val sessionStore: SessionStore,
    private val connectivity: ConnectivityObserver,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val todayIso = JalaliDate.todayIso()

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val goalsState = MutableStateFlow(Goals())

    val state: StateFlow<DashboardUiState> = combine(
        sessionStore.session,
        visitRepository.observeToday(todayIso),
        goalsState,
        syncRepository.pendingSummary,
        messageRepository.observeUnreadCount(),
        messageRepository.observeUnreadAlertCount(),
        zonePermissionRepository.observeUsableCount(),
        invoiceRepository.observeUnprintedCount(),
        reportRepository.observeSalesToday(todayIso),
        reportRepository.observeOrdersToday(todayIso),
        connectivity.isOnline,
        refreshing,
        error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val session = values[0] as ir.zagroserp.pda.data.prefs.Session?
        DashboardUiState(
            fullName = session?.fullName.orEmpty(),
            username = session?.username.orEmpty(),
            today = values[1] as TodaySnapshot,
            goals = values[2] as Goals,
            queue = values[3] as PendingQueueSummary,
            unreadMessages = values[4] as Int,
            unreadAlerts = values[5] as Int,
            usablePermissions = values[6] as Int,
            unprintedInvoices = values[7] as Int,
            salesToday = values[8] as Double,
            ordersToday = values[9] as Int,
            isOnline = values[10] as Boolean,
            isRefreshing = values[11] as Boolean,
            error = values[12] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null

            visitRepository.refreshToday(todayIso).let { result ->
                if (result is AppResult.Failure) error.value = result.error.message
            }
            reportRepository.goals().let { result ->
                when (result) {
                    is AppResult.Success -> goalsState.value = result.data
                    is AppResult.Failure -> if (error.value == null) error.value = result.error.message
                }
            }
            messageRepository.refreshMessages()
            zonePermissionRepository.refresh()
            invoiceRepository.refreshInvoices()
            syncScheduler.requestImmediateSync()

            refreshing.value = false
        }
    }

    fun dismissError() {
        error.value = null
    }
}
