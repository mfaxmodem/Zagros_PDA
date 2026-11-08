package ir.zagroserp.pda.ui.screens.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.Geofence
import ir.zagroserp.pda.core.Timestamps
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Visit
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.domain.repository.VisitRepository
import ir.zagroserp.pda.domain.repository.ZonePermissionRepository
import ir.zagroserp.pda.location.LocationProvider
import ir.zagroserp.pda.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailUiState(
    val customer: Customer? = null,
    val visits: List<Visit> = emptyList(),
    val isLoading: Boolean = true,
    val isCheckingDistance: Boolean = false,
    val distanceMeters: Double? = null,
    val error: String? = null,
    val message: String? = null,
) {
    val distanceLabel: String?
        get() = distanceMeters?.let {
            "${ir.zagroserp.pda.core.JalaliDate.toPersianDigits(it.toInt().toString())} متر"
        }

    /** Whether a check-in from the currently measured position would be accepted. */
    val withinRadius: Boolean?
        get() = distanceMeters?.let { it <= Geofence.CHECK_IN_RADIUS_METERS }
}

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    private val visitRepository: VisitRepository,
    private val zonePermissionRepository: ZonePermissionRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val customerId: String =
        savedStateHandle.get<String>(Routes.ARG_CUSTOMER_ID).orEmpty()


    private val loading = MutableStateFlow(true)
    private val checkingDistance = MutableStateFlow(false)
    private val distanceMeters = MutableStateFlow<Double?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<CustomerDetailUiState> = combine(
        customerRepository.observeCustomer(customerId),
        visitRepository.observeVisitsForCustomer(customerId),
        loading,
        error,
        message,
        checkingDistance,
        distanceMeters,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CustomerDetailUiState(
            customer = values[0] as Customer?,
            visits = values[1] as List<Visit>,
            isLoading = values[2] as Boolean,
            error = values[3] as String?,
            message = values[4] as String?,
            isCheckingDistance = values[5] as Boolean,
            distanceMeters = values[6] as Double?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerDetailUiState(),
    )

    init {
        load()
    }

    /**
     * Measures the live distance to the customer and reports whether a check-in would be
     * accepted. Gives the visitor (and support) a way to confirm the geofence is actually
     * active for a given customer.
     */
    fun checkDistance() {
        if (checkingDistance.value) return
        viewModelScope.launch {
            checkingDistance.value = true
            error.value = null
            message.value = null

            val customer = state.value.customer
            val point = runCatching { locationProvider.freshFix() }.getOrNull()

            when {
                customer == null -> error.value = "اطلاعات مشتری در دسترس نیست"

                !customer.hasLocation -> {
                    distanceMeters.value = null
                    message.value = "مختصات این مشتری ثبت نشده؛ ثبت بازدید محدودیتی ندارد"
                }

                point == null -> {
                    distanceMeters.value = null
                    error.value = "موقعیت مکانی در دسترس نیست. دسترسی GPS را بررسی کنید."
                }

                point.isMock -> {
                    distanceMeters.value = null
                    error.value = "موقعیت مکانی جعلی شناسایی شد؛ فاصله قابل اندازه‌گیری نیست."
                }

                else -> {
                    val distance = Geofence.distanceMeters(
                        point.latitude,
                        point.longitude,
                        customer.lat,
                        customer.lng,
                    )
                    distanceMeters.value = distance
                    message.value = if (Geofence.isWithinCheckInRadius(distance)) {
                        "داخل شعاع مجاز هستید؛ ثبت بازدید پذیرفته می‌شود"
                    } else {
                        "خارج از شعاع مجاز هستید؛ ثبت بازدید رد می‌شود"
                    }
                }
            }
            checkingDistance.value = false
        }
    }

    fun load() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val local = customerRepository.getCustomer(customerId)
            if (local == null) {
                // Not cached yet – pull it from the API.
                when (val result = customerRepository.refreshCustomers()) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> error.value = result.error.message
                }
            }
            loading.value = false
        }
    }

    /**
     * `target_date` and `until` are RFC3339 timestamps in the API contract, not plain dates.
     * Sending `2026-09-19` made the server answer 400 "بدنه درخواست نامعتبر است".
     */
    fun requestZonePermission(scope: String, reason: String, days: Int) {
        viewModelScope.launch {
            message.value = null
            error.value = null

            val start = Timestamps.startOfDayUtc(0)
            val isRange = scope.equals("DATE_RANGE", ignoreCase = true)

            when (
                val result = zonePermissionRepository.request(
                    customerId = customerId,
                    scope = scope,
                    reason = reason,
                    // DATE_RANGE needs both ends; the single-visit scopes only need the target day.
                    targetDate = start,
                    until = if (isRange) Timestamps.startOfDayUtc(days.toLong()) else null,
                    notes = null,
                )
            ) {
                is AppResult.Success -> message.value = "درخواست مجوز ثبت شد و در انتظار تأیید است"
                is AppResult.Failure -> error.value = result.error.message
            }
        }
    }
}