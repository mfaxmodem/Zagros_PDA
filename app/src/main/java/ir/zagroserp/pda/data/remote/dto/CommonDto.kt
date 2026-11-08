package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic error envelope described in section 1.1 of the API contract.
 * Every field except [error] is optional and must be parsed defensively.
 */
@Serializable
data class ApiErrorBody(
    @SerialName("error") val error: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("requires_company") val requiresCompany: Boolean = false,
    @SerialName("companies") val companies: List<CompanyDto> = emptyList(),
    @SerialName("retry_after") val retryAfter: Long? = null,
    @SerialName("requires_2fa") val requires2fa: Boolean = false,
    @SerialName("duplicate") val duplicate: Boolean = false,
    @SerialName("existing_customer_id") val existingCustomerId: String = "",
    // Present on the geofence rejection of `POST /visits`.
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    @SerialName("allowed_meters") val allowedMeters: Double? = null,
) {
    val displayMessage: String
        get() = error.ifBlank { message }.ifBlank { "خطای نامشخص" }
}

@Serializable
data class CompanyDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
)

@Serializable
data class BatchSummary(
    @SerialName("created") val created: Int = 0,
    @SerialName("duplicates") val duplicates: Int = 0,
    @SerialName("errors") val errors: Int = 0,
    @SerialName("blocked") val blocked: Int = 0,
)

@Serializable
data class OkResponse(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("message") val message: String = "",
)

@Serializable
data class MessageResponse(
    @SerialName("message") val message: String = "",
)

@Serializable
data class MarkReadResponse(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("updated") val updated: Int = 0,
)
