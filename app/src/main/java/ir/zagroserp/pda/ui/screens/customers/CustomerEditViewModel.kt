package ir.zagroserp.pda.ui.screens.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CustomerEditRules
import ir.zagroserp.pda.core.Geofence
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.location.LocationProvider
import ir.zagroserp.pda.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerEditUiState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val savedLatitude: Double? = null,
    val savedLongitude: Double? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isLocating: Boolean = false,
    val isMeasuring: Boolean = false,
    val distanceMeters: Double? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val hasCoordinates: Boolean
        get() = CustomerEditRules.hasCoordinates(latitude, longitude)

    /** Coordinates the server currently holds, used for the distance check. */
    val hasSavedCoordinates: Boolean
        get() = CustomerEditRules.hasCoordinates(savedLatitude, savedLongitude)

    val canSave: Boolean get() = !isSaving && !isLoading

    val distanceLabel: String?
        get() = distanceMeters?.let {
            "${ir.zagroserp.pda.core.JalaliDate.toPersianDigits(it.toInt().toString())} متر"
        }

    val withinRadius: Boolean?
        get() = distanceMeters?.let { CustomerEditRules.withinRadius(it) }

    val coordinatesLabel: String
        get() = CustomerEditRules.coordinateLabel(latitude, longitude)
}

/**
 * Edits a customer's contact details and position.
 *
 * `PUT /mobile/visitor/customers/:id` is a real PATCH, but the whole editable set is always sent:
 * it costs nothing, keeps the request self-describing, and means a field can never be cleared by
 * accident the way it could when the endpoint replaced the whole record.
 */
@HiltViewModel
class CustomerEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val customerId: String =
        savedStateHandle.get<String>(Routes.ARG_CUSTOMER_ID).orEmpty()

    private val _state = MutableStateFlow(CustomerEditUiState())
    val state: StateFlow<CustomerEditUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            var customer = customerRepository.getCustomer(customerId)
            if (customer == null) {
                customerRepository.refreshCustomers()
                customer = customerRepository.getCustomer(customerId)
            }

            if (customer == null) {
                _state.update {
                    it.copy(isLoading = false, error = "اطلاعات مشتری در دسترس نیست")
                }
                return@launch
            }
            applyCustomer(customer)
        }
    }

    private fun applyCustomer(customer: Customer) {
        _state.update {
            it.copy(
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                city = customer.city,
                province = customer.province,
                latitude = customer.lat.takeIf { v -> v != 0.0 },
                longitude = customer.lng.takeIf { v -> v != 0.0 },
                savedLatitude = customer.lat.takeIf { v -> v != 0.0 },
                savedLongitude = customer.lng.takeIf { v -> v != 0.0 },
                isLoading = false,
            )
        }
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameError = null, error = null) }

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phone = value, phoneError = null, error = null) }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value) }

    fun onCityChange(value: String) = _state.update { it.copy(city = value) }

    fun onProvinceChange(value: String) = _state.update { it.copy(province = value) }

    /** Fills the form with the visitor's current position. Does not save. */
    fun captureLocation() {
        if (_state.value.isLocating) return
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true, error = null, message = null) }

            val point = runCatching { locationProvider.freshFix() }.getOrNull()
            when {
                point == null -> _state.update {
                    it.copy(
                        isLocating = false,
                        error = "موقعیت مکانی در دسترس نیست؛ دسترسی GPS و روشن بودن آن را بررسی کنید",
                    )
                }

                point.isMock -> _state.update {
                    it.copy(
                        isLocating = false,
                        error = "موقعیت جعلی شناسایی شد و ثبت نشد",
                    )
                }

                else -> _state.update {
                    it.copy(
                        isLocating = false,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        distanceMeters = null,
                        message = CustomerEditRules.captureConfirmation(
                            latitude = point.latitude,
                            longitude = point.longitude,
                            accuracyMeters = point.accuracy,
                        ),
                        error = CustomerEditRules.accuracyWarning(point.accuracy),
                    )
                }
            }
        }
    }

    /** Measures the live distance to the position the server currently holds for the customer. */
    fun measureDistance() {
        if (_state.value.isMeasuring) return
        viewModelScope.launch {
            _state.update { it.copy(isMeasuring = true, error = null) }
            val current = _state.value
            val point = runCatching { locationProvider.freshFix() }.getOrNull()

            when {
                point == null -> _state.update {
                    it.copy(
                        isMeasuring = false,
                        error = "موقعیت مکانی در دسترس نیست؛ دسترسی GPS را بررسی کنید",
                    )
                }

                point.isMock -> _state.update {
                    it.copy(isMeasuring = false, error = "موقعیت جعلی شناسایی شد")
                }

                !current.hasSavedCoordinates -> _state.update {
                    it.copy(isMeasuring = false, message = "این مشتری مختصات ثبت‌شده ندارد")
                }

                else -> {
                    val distance = Geofence.distanceMeters(
                        point.latitude,
                        point.longitude,
                        current.savedLatitude!!,
                        current.savedLongitude!!,
                    )
                    _state.update {
                        it.copy(
                            isMeasuring = false,
                            distanceMeters = distance,
                            message = if (CustomerEditRules.withinRadius(distance)) {
                                "داخل شعاع مجاز هستید"
                            } else {
                                "خارج از شعاع مجاز هستید؛ ثبت بازدید رد می‌شود"
                            },
                        )
                    }
                }
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = _state.value
        val nameError = CustomerEditRules.validateName(snapshot.name)
        val phoneError = CustomerEditRules.validatePhone(snapshot.phone)
        if (nameError != null || phoneError != null) {
            _state.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }

        _state.update { it.copy(isSaving = true, error = null, message = null) }

        viewModelScope.launch {
            when (
                val result = customerRepository.updateCustomer(
                    id = customerId,
                    name = snapshot.name.trim(),
                    phone = snapshot.phone.trim(),
                    address = snapshot.address.trim(),
                    city = snapshot.city.trim(),
                    province = snapshot.province.trim(),
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                )
            ) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSaving = false, message = "تغییرات ذخیره شد") }
                    onSaved()
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isSaving = false, error = result.error.message)
                }
            }
        }
    }
}
