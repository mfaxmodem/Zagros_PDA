package ir.zagroserp.pda.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["visitDate"]),
        Index(value = ["clientVisitId"], unique = true),
    ],
)
data class VisitEntity(
    @PrimaryKey val id: String,
    val companyId: String = "",
    val visitorId: String = "",
    val customerId: String = "",
    val visitDate: String = "",
    val visitType: String = "ROUTE",
    val status: String = "VISITED",
    val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val orderId: String = "",
    val clientVisitId: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val isPending: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
)

/** Offline queue entry for `POST /mobile/visitor/visits` (+batch). */
@Entity(tableName = "pending_visits", indices = [Index(value = ["createdAt"])])
data class PendingVisitEntity(
    @PrimaryKey val clientVisitId: String,
    val payloadJson: String,
    val customerId: String = "",
    val customerName: String = "",
    val status: String = "VISITED",
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastError: String = "",
)
