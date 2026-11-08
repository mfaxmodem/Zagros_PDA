package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisitDto(
    @SerialName("id") val id: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("visitor_id") val visitorId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("visit_date") val visitDate: String = "",
    @SerialName("visit_type") val visitType: String = "ROUTE",
    @SerialName("status") val status: String = "VISITED",
    @SerialName("notes") val notes: String = "",
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("order_id") val orderId: String = "",
    @SerialName("client_visit_id") val clientVisitId: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/**
 * `accuracy` and `is_mock` were added to the server contract so the geofence can be enforced
 * server-side: a coarse fix is rejected and a mocked fix is refused outright. The app always
 * sends both, because a client-only check is trivially bypassed on a rooted device.
 */
@Serializable
data class CreateVisitRequest(
    @SerialName("customer_id") val customerId: String,
    @SerialName("visit_type") val visitType: String = "ROUTE",
    @SerialName("status") val status: String = "VISITED",
    @SerialName("notes") val notes: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("accuracy") val accuracy: Double? = null,
    @SerialName("is_mock") val isMock: Boolean? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("client_visit_id") val clientVisitId: String,
)

@Serializable
data class VisitBatchRequest(
    @SerialName("visits") val visits: List<CreateVisitRequest>,
)

@Serializable
data class VisitBatchResult(
    @SerialName("client_visit_id") val clientVisitId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("visit_id") val visitId: String = "",
    @SerialName("message") val message: String = "",
)

@Serializable
data class VisitBatchResponse(
    @SerialName("results") val results: List<VisitBatchResult> = emptyList(),
    @SerialName("summary") val summary: BatchSummary = BatchSummary(),
)
