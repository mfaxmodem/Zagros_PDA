package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileCustomerDto(
    @SerialName("id") val id: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("national_id") val nationalId: String = "",
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("credit_limit") val creditLimit: Double = 0.0,
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lng") val lng: Double = 0.0,
    @SerialName("city") val city: String = "",
    @SerialName("province") val province: String = "",
    @SerialName("active") val active: Boolean = true,
    @SerialName("settlement_days") val settlementDays: Int = 0,
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class CustomerListResponse(
    @SerialName("customers") val customers: List<MobileCustomerDto> = emptyList(),
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class CustomerResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("national_id") val nationalId: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("credit_limit") val creditLimit: Double = 0.0,
    @SerialName("settlement_days") val settlementDays: Int = 0,
    @SerialName("active") val active: Boolean = true,
)

@Serializable
data class CreateCustomerRequest(
    @SerialName("name") val name: String,
    @SerialName("national_id") val nationalId: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("settlement_days") val settlementDays: Int? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

/**
 * `PUT /mobile/visitor/customers/:id`.
 *
 * The server treats this as a **full replace**, not a patch: omitted fields are cleared. Always
 * populate every editable field (see [ir.zagroserp.pda.data.repository.CustomerRepositoryImpl.updateCustomer]).
 */
@Serializable
data class UpdateCustomerRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("province") val province: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)
