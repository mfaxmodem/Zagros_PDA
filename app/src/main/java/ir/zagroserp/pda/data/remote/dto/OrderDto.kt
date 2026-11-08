package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItemRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("quantity") val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("uom_id") val uomId: String? = null,
)

/**
 * `POST /mobile/visitor/orders`.
 *
 * The location fields are **not** part of the published contract yet — they are sent so the
 * server can apply the same geofence rule it applies to visits. Until the backend accepts them
 * they are simply ignored, which is harmless. See `backend-requirements.md` P0-4.
 */
@Serializable
data class CreateOrderRequest(
    @SerialName("customer_id") val customerId: String,
    @SerialName("date") val date: String,
    @SerialName("items") val items: List<OrderItemRequest>,
    @SerialName("fiscal_year_id") val fiscalYearId: String? = null,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("note") val note: String? = null,
    @SerialName("client_order_id") val clientOrderId: String,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("accuracy") val accuracy: Double? = null,
    @SerialName("is_mock") val isMock: Boolean? = null,
)

@Serializable
data class OrderItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("batch_id") val batchId: String = "",
    @SerialName("quantity") val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("total_line") val totalLine: Double = 0.0,
)

@Serializable
data class AvailabilityWarningDto(
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("requested") val requested: Double = 0.0,
    @SerialName("available") val available: Double = 0.0,
    @SerialName("shortage") val shortage: Double = 0.0,
)

@Serializable
data class OrderResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("fiscal_year_id") val fiscalYearId: String = "",
    @SerialName("visitor_id") val visitorId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("number") val number: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("sub_total") val subTotal: Double = 0.0,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("final_amount") val finalAmount: Double = 0.0,
    @SerialName("status") val status: String = "",
    @SerialName("rejection_reason") val rejectionReason: String = "",
    @SerialName("note") val note: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("visitor_name") val visitorName: String = "",
    @SerialName("soft_warning") val softWarning: String = "",
    @SerialName("awaiting_supply") val awaitingSupply: Boolean = false,
    @SerialName("shortage_qty") val shortageQty: Double = 0.0,
    @SerialName("availability_warnings") val availabilityWarnings: List<AvailabilityWarningDto> = emptyList(),
    @SerialName("items") val items: List<OrderItemDto> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class OrderListResponse(
    @SerialName("orders") val orders: List<OrderResponseDto> = emptyList(),
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class OrderBatchRequest(
    @SerialName("orders") val orders: List<CreateOrderRequest>,
)

@Serializable
data class OrderBatchResult(
    @SerialName("client_order_id") val clientOrderId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("message") val message: String = "",
)

@Serializable
data class OrderBatchResponse(
    @SerialName("results") val results: List<OrderBatchResult> = emptyList(),
    @SerialName("summary") val summary: BatchSummary = BatchSummary(),
)
