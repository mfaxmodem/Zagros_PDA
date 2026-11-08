package ir.zagroserp.pda.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["nationalId"]),
        Index(value = ["name"]),
        Index(value = ["phone"]),
    ],
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val code: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val nationalId: String = "",
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val city: String = "",
    val province: String = "",
    val active: Boolean = true,
    val settlementDays: Int = 0,
    val updatedAt: String = "",
    val isLocalOnly: Boolean = false,
    val pendingSyncId: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "tours")
data class TourEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val sortOrder: Int = 0,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "tour_customers",
    primaryKeys = ["tourId", "customerId"],
    indices = [Index(value = ["customerId"]), Index(value = ["tourId"])],
)
data class TourCustomerEntity(
    val tourId: String,
    val customerId: String,
    val sortOrder: Int = 0,
)
