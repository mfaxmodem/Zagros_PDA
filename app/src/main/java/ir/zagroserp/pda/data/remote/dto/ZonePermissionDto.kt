package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZonePermissionDto(
    @SerialName("id") val id: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("visitor_id") val visitorId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("tour_id") val tourId: String = "",
    @SerialName("requested_by_user_id") val requestedByUserId: String = "",
    @SerialName("scope") val scope: String = "SAME_DAY",
    @SerialName("valid_from") val validFrom: String = "",
    @SerialName("valid_until") val validUntil: String = "",
    @SerialName("reason") val reason: String = "",
    @SerialName("notes") val notes: String = "",
    @SerialName("status") val status: String = "PENDING",
    @SerialName("decided_by_user_id") val decidedByUserId: String = "",
    @SerialName("decided_at") val decidedAt: String? = null,
    @SerialName("decision_note") val decisionNote: String = "",
    @SerialName("consumed_visit_id") val consumedVisitId: String? = null,
    @SerialName("consumed_at") val consumedAt: String? = null,
    @SerialName("is_usable") val isUsable: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class CreateZonePermissionRequest(
    @SerialName("customer_id") val customerId: String,
    @SerialName("scope") val scope: String,
    @SerialName("reason") val reason: String,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("until") val until: String? = null,
    @SerialName("notes") val notes: String? = null,
)
