package ir.zagroserp.pda.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "messages", indices = [Index(value = ["createdAt"]), Index(value = ["isRead"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "invoices", indices = [Index(value = ["customerId"]), Index(value = ["date"])])
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val number: String = "",
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val subTotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val note: String = "",
    val printedAt: String = "",
    val itemsJson: String = "[]",
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "zone_permissions", indices = [Index(value = ["customerId"]), Index(value = ["status"])])
data class ZonePermissionEntity(
    @PrimaryKey val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val tourId: String = "",
    val scope: String = "SAME_DAY",
    val validFrom: String = "",
    val validUntil: String = "",
    val reason: String = "",
    val notes: String = "",
    val status: String = "PENDING",
    val decisionNote: String = "",
    val consumedAt: String? = null,
    val isUsable: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
)

/** Offline queue entry for `POST /mobile/visitor/location` (+batch). */
@Entity(tableName = "pending_locations", indices = [Index(value = ["createdAt"])])
data class PendingLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double = 0.0,
    val recordedAt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
)

/**
 * Who the visitor is expected to see on a given day, as returned by `GET /mobile/visitor/today`.
 *
 * This table exists because the rest of the cache is **not** day-scoped: `customers` and `tours`
 * hold everything the visitor can reach (99 rows in production vs 1 customer assigned today).
 * Reading `customers` directly made "My Route" show the entire customer book, so the daily
 * assignment has to be stored explicitly.
 */
@Entity(tableName = "today_customers", primaryKeys = ["date", "customerId"])
data class TodayCustomerEntity(
    /** Gregorian `YYYY-MM-DD`, matching both the API and the visits table. */
    val date: String,
    val customerId: String,
    /** [DIRECT_TOUR_ID] when the customer is assigned directly rather than through a tour. */
    val tourId: String,
    val sortOrder: Int = 0,
) {
    companion object {
        /**
         * Sentinel instead of a nullable column: Room primary keys cannot be null, and a nullable
         * `tourId` would also break the join that lists the day's tours.
         */
        const val DIRECT_TOUR_ID = "__direct__"
    }
}

/** Delta-sync cursor. Single row, id = 0. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 0,
    val serverTime: String = "",
    val lastSyncAt: Long = 0L,
    val lastBootstrapAt: Long = 0L,
    val productCount: Int = 0,
)
