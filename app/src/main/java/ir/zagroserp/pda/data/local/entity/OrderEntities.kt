package ir.zagroserp.pda.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["status"]),
        Index(value = ["clientOrderId"]),
    ],
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val companyId: String = "",
    val fiscalYearId: String = "",
    val visitorId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val visitorName: String = "",
    val number: String = "",
    val date: String = "",
    val subTotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val status: String = "",
    val rejectionReason: String = "",
    val note: String = "",
    val softWarning: String = "",
    val awaitingSupply: Boolean = false,
    val shortageQty: Double = 0.0,
    val availabilityWarningsJson: String = "[]",
    val clientOrderId: String = "",
    val isLocalOnly: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["productId"])],
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val id: String = "",
    val orderId: String,
    val productId: String = "",
    @ColumnInfo(name = "product_name") val productName: String = "",
    val batchId: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val totalLine: Double = 0.0,
    val uomId: String? = null,
    val uomName: String = "",
)

/**
 * Order row joined with the customer's name.
 *
 * `GET /mobile/visitor/orders` (the list) does not always populate `customer_name` while
 * `GET /mobile/visitor/orders/:id` does — which is why a freshly synced order showed a
 * placeholder in the history but the real name once opened. Resolving the name from the local
 * customer table removes that dependency on the list payload.
 */
data class OrderWithCustomerName(
    @Embedded val order: OrderEntity,
    @ColumnInfo(name = "resolvedCustomerName") val resolvedCustomerName: String?,
)

/** Offline queue entry for `POST /mobile/visitor/orders` (+batch). */
@Entity(tableName = "pending_orders", indices = [Index(value = ["createdAt"])])
data class PendingOrderEntity(
    @PrimaryKey val clientOrderId: String,
    val payloadJson: String,
    val customerId: String = "",
    val customerName: String = "",
    val itemCount: Int = 0,
    val estimatedTotal: Double = 0.0,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastError: String = "",
    val serverOrderId: String? = null,
    val serverNumber: String? = null,
)
