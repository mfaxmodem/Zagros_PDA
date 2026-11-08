package ir.zagroserp.pda.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["code"]),
        Index(value = ["name"]),
        Index(value = ["category"]),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val companyId: String = "",
    val code: String = "",
    val name: String = "",
    val latinName: String = "",
    val printName: String = "",
    val barcode: String = "",
    val category: String = "",
    val unit: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val warehouseId: String = "",
    val imageUrl: String = "",
    val weightKg: Double = 0.0,
    val volumeM3: Double = 0.0,
    val minStock: Double = 0.0,
    val maxStock: Double = 0.0,
    val sstid: String = "",
    val taxMu: Double = 0.0,
    val isActive: Boolean = true,
    val hasPromotion: Boolean = false,
    val promoPercent: Double = 0.0,
    val totalQty: Double = 0.0,
    val reservedQty: Double = 0.0,
    val availableQty: Double = 0.0,
    @ColumnInfo(name = "stock_updated_at") val stockUpdatedAt: String = "",
    val categoriesJson: String = "[]",
    val uomsJson: String = "[]",
    val updatedAt: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
) {
    val hasStock: Boolean get() = availableQty > 0
}

@Entity(tableName = "discounts", indices = [Index(value = ["isActive"])])
data class DiscountEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val type: String = "",
    val description: String = "",
    val value: Double = 0.0,
    val valueType: String = "",
    val validFrom: String = "",
    val validTo: String = "",
    val productsJson: String = "[]",
    val isActive: Boolean = true,
    val cachedAt: Long = System.currentTimeMillis(),
)
