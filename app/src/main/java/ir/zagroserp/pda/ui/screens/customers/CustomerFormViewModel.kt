package ir.zagroserp.pda.ui.screens.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppError
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerFormUiState(
    val name: String = "",
    val nationalId: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: String = "",
    val settlementDays: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasLocationPermission: Boolean = false,
    val isLocating: Boolean = false,
    val isSubmitting: Boolean = false,
    val nameError: String? = null,
    val nationalIdError: String? = null,
    val locationError: String? = null,
    /** Set when the national id already belongs to a customer the visitor can reach. */
    val duplicate: DuplicateCustomer? = null,
    val error: String? = null,
    val createdCustomerId: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && name.trim().length >= 2 && nationalId.trim().length >= 5
}

data class DuplicateCustomer(
    val existingId: String,
    val existingName: String,
    /** True when the conflict was reported by the server rather than the local cache. */
    val fromServer: Boolean,
)

@HiltViewModel
class CustomerFormViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CustomerFormUiState(hasLocationPermission = locationProvider.hasPermission()),
    )
    val state: StateFlow<CustomerFormUiState> = _state.asStateFlow()

    init {
        if (locationProvider.hasPermission()) captureLocation()
    }

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameError = null, error = null) }

    fun onNationalIdChange(value: String) = _state.update {
        it.copy(
            nationalId = value.filter { ch -> ch.isDigit() },
            nationalIdError = null,
            error = null,
            duplicate = null,
        )
    }

    fun dismissDuplicate() = _state.update { it.copy(duplicate = null) }

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value.filter(Char::isDigit)) }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value) }

    fun onCreditLimitChange(value: String) =
        _state.update { it.copy(creditLimit = value.filter(Char::isDigit)) }

    fun onSettlementDaysChange(value: String) =
        _state.update { it.copy(settlementDays = value.filter(Char::isDigit)) }

    fun onLocationDenied() = _state.update {
        it.copy(
            hasLocationPermission = false,
            isLocating = false,
            locationError = "بدون دسترسی موقعیت مکانی، ثبت مشتری خارج از محدوده ممکن است رد شود",
        )
    }

    fun captureLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true, hasLocationPermission = true, locationError = null) }
            val point = locationProvider.current()
            _state.update {
                if (point != null) {
                    it.copy(
                        isLocating = false,
                        latitude = point.latitude,
                        longitude = point.longitude,
                    )
                } else {
                    it.copy(
                        isLocating = false,
                        locationError = "دریافت موقعیت مکانی ناموفق بود؛ GPS را روشن کنید",
                    )
                }
            }
        }
    }

    fun submit() {
        val snapshot = _state.value
        val nameError = if (snapshot.name.trim().length < 2) "نام باید حداقل ۲ کاراکتر باشد" else null
        val nationalIdError =
            if (snapshot.nationalId.trim().length < 5) "شناسه ملی نامعتبر است" else null
        if (nameError != null || nationalIdError != null) {
            _state.update { it.copy(nameError = nameError, nationalIdError = nationalIdError) }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null, duplicate = null) }

        viewModelScope.launch {
            // Local pre-check first: if the customer is already cached there is no reason to make
            // the visitor wait for a round trip to be told so.
            val cached = customerRepository.findByNationalId(snapshot.nationalId)
            if (cached != null) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        duplicate = DuplicateCustomer(
                            existingId = cached.id,
                            existingName = cached.name,
                            fromServer = false,
                        ),
                    )
                }
                return@launch
            }

            val result = customerRepository.createCustomer(
                name = snapshot.name,
                nationalId = snapshot.nationalId,
                phone = snapshot.phone.takeIf { it.isNotBlank() },
                address = snapshot.address.takeIf { it.isNotBlank() },
                creditLimit = snapshot.creditLimit.toDoubleOrNull(),
                settlementDays = snapshot.settlementDays.toIntOrNull(),
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
            )
            when (result) {
                is AppResult.Success -> _state.update {
                    it.copy(isSubmitting = false, createdCustomerId = result.data.id)
                }

                is AppResult.Failure -> {
                    val error = result.error
                    if (error is AppError.Conflict && error.duplicate) {
                        val existingId = error.existingCustomerId.orEmpty()
                        // The server is authoritative; try to resolve the name for the message.
                        val existing = existingId.takeIf { it.isNotBlank() }
                            ?.let { customerRepository.getCustomer(it) }
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                duplicate = DuplicateCustomer(
                                    existingId = existingId,
                                    existingName = existing?.name.orEmpty(),
                                    fromServer = true,
                                ),
                            )
                        }
                    } else {
                        _state.update { it.copy(isSubmitting = false, error = error.message) }
                    }
                }
            }
        }
    }
}
