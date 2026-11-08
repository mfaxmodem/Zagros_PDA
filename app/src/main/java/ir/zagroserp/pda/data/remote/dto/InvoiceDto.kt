package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceItemDto(
    @SerialName("product_name") val productName: String = "",
    @SerialName("quantity") val quantity: Double = 0.0,
    @SerialName("unit") val unit: String = "",
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("total_line") val totalLine: Double = 0.0,
)

@Serializable
data class MobileInvoiceDto(
    @SerialName("id") val id: String = "",
    @SerialName("number") val number: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_address") val customerAddress: String = "",
    @SerialName("sub_total") val subTotal: Double = 0.0,
    @SerialName("discount") val discount: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    @SerialName("final_amount") val finalAmount: Double = 0.0,
    @SerialName("note") val note: String = "",
    @SerialName("printed_at") val printedAt: String = "",
    @SerialName("items") val items: List<InvoiceItemDto> = emptyList(),
)

@Serializable
data class InvoiceListResponse(
    @SerialName("invoices") val invoices: List<MobileInvoiceDto> = emptyList(),
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 50,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class PrintedResponse(
    @SerialName("printed_at") val printedAt: String = "",
)
