package ir.zagroserp.pda.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CheckInVerdict
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.core.rejectionMessage
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Tour
import ir.zagroserp.pda.domain.repository.ReportRepository
import ir.zagroserp.pda.domain.repository.VisitRepository
import ir.zagroserp.pda.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RouteFilter(val label: String) {
    ALL("همه"),
    PENDING("باقی‌مانده"),
    VISITED("بازدیدشده"),
}

data class RouteCustomerRowState(
    val customer: Customer,
    val isVisited: Boolean,
    val isCheckingIn: Boolean = false,
    val lastStatus: String = "",
)

data class TodayRouteUiState(
    val tours: List<Tour> = emptyList(),
    val selectedTourId: String? = null,
    val filter: RouteFilter = RouteFilter.ALL,
    val customers: List<RouteCustomerRowState> = emptyList(),
    val visitedCount: Int = 0,
    val salesToday: Double = 0.0,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val info: String? = null,
) {
    val customerCount: Int get() = customers.size
    val pendingCount: Int get() = customers.count { !it.isVisited }
    val visibleCustomers: List<RouteCustomerRowState>
        get() = when (filter) {
            RouteFilter.ALL -> customers
            RouteFilter.PENDING -> customers.filter { !it.isVisited }
            RouteFilter.VISITED -> customers.filter { it.isVisited }
        }
}

@HiltViewModel
@Suppress("UNCHECKED_CAST")
class TodayRouteViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val reportRepository: ReportRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val todayIso = JalaliDate.todayIso()
    private val filter = MutableStateFlow(RouteFilter.ALL)
    private val selectedTour = MutableStateFlow<String?>(null)
    private val refreshing = MutableStateFlow(false)
    private val checkingIn = MutableStateFlow<Set<String>>(emptySet())
    private val error = MutableStateFlow<String?>(null)
    private val info = MutableStateFlow<String?>(null)

    val state: StateFlow<TodayRouteUiState> = combine(
        visitRepository.observeToday(todayIso),
        filter,
        selectedTour,
        refreshing,
        checkingIn,
        error,
        reportRepository.observeSalesToday(todayIso),
        info,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val today = values[0] as ir.zagroserp.pda.domain.model.TodaySnapshot
        val currentFilter = values[1] as RouteFilter
        val tourId = values[2] as String?
        val isRefreshing = values[3] as Boolean
        val busy = values[4] as Set<String>
        val currentError = values[5] as String?
        val sales = values[6] as Double
        val currentInfo = values[7] as String?

        val visitedIds = today.visitedCustomerIds
        val tourCustomerIds = tourId
            ?.let { id -> today.tours.firstOrNull { it.id == id }?.customers?.map { it.id }?.toSet() }
        val customers = today.customers
            .filter { tourCustomerIds == null || it.id in tourCustomerIds }
            .map { customer ->
                RouteCustomerRowState(
                    customer = customer,
                    isVisited = customer.id in visitedIds,
                    isCheckingIn = customer.id in busy,
                    lastStatus = today.visited.firstOrNull { it.customerId == customer.id }
                        ?.status?.apiValue.orEmpty(),
                )
            }
        TodayRouteUiState(
            tours = today.tours,
            selectedTourId = tourId,
            filter = currentFilter,
            customers = customers,
            visitedCount = today.visited.size,
            salesToday = sales,
            isRefreshing = isRefreshing,
            error = currentError,
            info = currentInfo,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayRouteUiState(),
    )

    init {
        refresh()
    }

    fun setFilter(value: RouteFilter) {
        filter.value = value
    }

    fun selectTour(tourId: String?) {
        selectedTour.value = tourId
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            info.value = null
            visitRepository.refreshToday(todayIso).let { result ->
                if (result is AppResult.Failure) error.value = result.error.message
            }
            refreshing.value = false
        }
    }

    /**
     * Records the check-in. Coordinates are attached whenever they are available so the server
     * can enforce the tour geofence and out-of-zone permissions.
     */
    fun checkIn(customerId: String, status: String = "VISITED") {
        if (checkingIn.value.contains(customerId)) return
        checkingIn.value = checkingIn.value + customerId
        viewModelScope.launch {
            // A fresh high-accuracy fix only – a cached reading from another place must not be
            // used to judge a 50 m radius.
            val point = runCatching { locationProvider.freshFix() }.getOrNull()

            // Ask the guard first so the visitor is told exactly why a check-in is refused
            // (too far / no customer coordinates / mock fix) rather than getting a bare error.
            val verdict = visitRepository.evaluateCheckIn(customerId, point)
            if (!verdict.canProceed) {
                error.value = verdict.rejectionMessage()
                info.value = null
                checkingIn.value = checkingIn.value - customerId
                return@launch
            }

            visitRepository.checkIn(
                customerId = customerId,
                status = status,
                notes = null,
                location = point,
                orderId = null,
            ).let { result ->
                when (result) {
                    is AppResult.Success -> {
                        val visit = result.data
                        val detail = when (verdict) {
                            is CheckInVerdict.Allowed -> " • فاصله: " +
                                "${JalaliDate.toPersianDigits(verdict.distanceMeters.toInt().toString())} متر"

                            CheckInVerdict.CustomerWithoutCoordinates ->
                                " • مختصات این مشتری ثبت نشده"

                            else -> ""
                        }
                        info.value = if (visit.isPending) {
                            "بازدید ثبت شد و در صف ارسال قرار گرفت$detail"
                        } else {
                            "بازدید با موفقیت ثبت شد$detail"
                        }
                        error.value = null
                    }

                    is AppResult.Failure -> {
                        error.value = result.error.message
                        info.value = null
                    }
                }
            }
            checkingIn.value = checkingIn.value - customerId
        }
    }
}
