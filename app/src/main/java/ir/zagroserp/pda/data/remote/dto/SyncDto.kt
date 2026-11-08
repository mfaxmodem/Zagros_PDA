package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeletedIdsDto(
    @SerialName("products") val products: List<String> = emptyList(),
    @SerialName("customers") val customers: List<String> = emptyList(),
)

/**
 * Delta sync payload. `customers`, `tours` and `discounts` are master data returned
 * independently of the cursor; the rest is filtered on `updated_at > since`.
 */
@Serializable
data class SyncDeltaDto(
    @SerialName("server_time") val serverTime: String = "",
    @SerialName("products") val products: List<MobileProductDto> = emptyList(),
    @SerialName("product_stock") val productStock: List<ProductStockDto> = emptyList(),
    @SerialName("customers") val customers: List<MobileCustomerDto> = emptyList(),
    @SerialName("tours") val tours: List<TourDto> = emptyList(),
    @SerialName("discounts") val discounts: List<DiscountDto> = emptyList(),
    @SerialName("approved_invoices") val approvedInvoices: List<MobileInvoiceDto> = emptyList(),
    @SerialName("visits") val visits: List<VisitDto> = emptyList(),
    @SerialName("notifications") val notifications: List<NotificationDto> = emptyList(),
    @SerialName("deleted_ids") val deletedIds: DeletedIdsDto = DeletedIdsDto(),
)
