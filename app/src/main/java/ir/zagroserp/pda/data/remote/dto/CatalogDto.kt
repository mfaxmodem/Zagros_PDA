package ir.zagroserp.pda.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Category supply of a product – each supply carries its own sale price (API 4.33). */
@Serializable
data class CategoryOfferDto(
    @SerialName("id") val id: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("is_default_sale") val isDefaultSale: Boolean = false,
)

/** Multi unit of measure packaging of a product (API 4.34). */
@Serializable
data class UomDto(
    @SerialName("id") val id: String = "",
    @SerialName("uom_id") val uomId: String = "",
    @SerialName("unit_name") val unitName: String = "",
    @SerialName("conversion_factor") val conversionFactor: Double = 1.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("barcode") val barcode: String = "",
    @SerialName("is_default_for_sale") val isDefaultForSale: Boolean = false,
)

/** Full product as returned by `GET /mobile/visitor/products`. */
@Serializable
data class ProductResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("latin_name") val latinName: String = "",
    @SerialName("print_name") val printName: String = "",
    @SerialName("barcode") val barcode: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("unit") val unit: String = "",
    @SerialName("buy_price") val buyPrice: Double = 0.0,
    @SerialName("sell_price") val sellPrice: Double = 0.0,
    @SerialName("warehouse_id") val warehouseId: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("volume_m3") val volumeM3: Double = 0.0,
    @SerialName("min_stock") val minStock: Double = 0.0,
    @SerialName("max_stock") val maxStock: Double = 0.0,
    @SerialName("sstid") val sstid: String = "",
    @SerialName("tax_mu") val taxMu: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("total_qty") val totalQty: Double = 0.0,
    @SerialName("reserved_qty") val reservedQty: Double = 0.0,
    @SerialName("available_qty") val availableQty: Double = 0.0,
    @SerialName("stock_updated_at") val stockUpdatedAt: String = "",
    @SerialName("categories") val categories: List<CategoryOfferDto> = emptyList(),
    @SerialName("uoms") val uoms: List<UomDto> = emptyList(),
)

/** Slimmer product used by `products-promo`, `products/lookup` and `/sync`. */
@Serializable
data class MobileProductDto(
    @SerialName("id") val id: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("unit") val unit: String = "",
    @SerialName("sell_price") val sellPrice: Double = 0.0,
    @SerialName("has_promotion") val hasPromotion: Boolean = false,
    @SerialName("promo_percent") val promoPercent: Double = 0.0,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("categories") val categories: List<CategoryOfferDto> = emptyList(),
    @SerialName("uoms") val uoms: List<UomDto> = emptyList(),
)

@Serializable
data class ProductStockDto(
    @SerialName("product_id") val productId: String = "",
    @SerialName("total_qty") val totalQty: Double = 0.0,
    @SerialName("reserved_qty") val reservedQty: Double = 0.0,
    @SerialName("available_qty") val availableQty: Double = 0.0,
    @SerialName("stock_updated_at") val stockUpdatedAt: String = "",
)

@Serializable
data class ProductCategoriesResponse(
    @SerialName("categories") val categories: List<CategoryOfferDto> = emptyList(),
)

@Serializable
data class ProductUomsResponse(
    @SerialName("uoms") val uoms: List<UomDto> = emptyList(),
)

@Serializable
data class DiscountsResponse(
    @SerialName("discounts") val discounts: List<DiscountDto> = emptyList(),
)

@Serializable
data class DiscountDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("value") val value: Double = 0.0,
    @SerialName("value_type") val valueType: String = "",
    @SerialName("valid_from") val validFrom: String = "",
    @SerialName("valid_to") val validTo: String = "",
    @SerialName("products") val products: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
)
