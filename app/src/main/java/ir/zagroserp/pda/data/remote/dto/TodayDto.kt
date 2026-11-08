package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TourDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("customers") val customers: List<MobileCustomerDto> = emptyList(),
)

@Serializable
data class TodayResponse(
    @SerialName("date") val date: String = "",
    @SerialName("tours") val tours: List<TourDto> = emptyList(),
    @SerialName("customers") val customers: List<MobileCustomerDto> = emptyList(),
    @SerialName("visited") val visited: List<VisitDto> = emptyList(),
)

@Serializable
data class GoalsResponse(
    @SerialName("target_value") val targetValue: Double = 0.0,
    @SerialName("achieved_value") val achievedValue: Double = 0.0,
    @SerialName("progress_percent") val progressPercent: Double = 0.0,
    @SerialName("commission_rate") val commissionRate: Double = 0.0,
    @SerialName("estimated_commission") val estimatedCommission: Double = 0.0,
    @SerialName("sales_today") val salesToday: Double = 0.0,
    @SerialName("order_count") val orderCount: Int = 0,
)

@Serializable
data class ReportResponse(
    @SerialName("from") val from: String = "",
    @SerialName("to") val to: String = "",
    @SerialName("order_count") val orderCount: Int = 0,
    @SerialName("total_sales") val totalSales: Double = 0.0,
    @SerialName("visit_count") val visitCount: Int = 0,
    @SerialName("assigned_customers") val assignedCustomers: Int = 0,
    @SerialName("debt_total") val debtTotal: Double = 0.0,
)

@Serializable
data class NotificationDto(
    @SerialName("id") val id: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)
