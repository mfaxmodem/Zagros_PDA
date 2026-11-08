package ir.zagroserp.pda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.dao.InvoiceDao
import ir.zagroserp.pda.data.local.dao.LocationQueueDao
import ir.zagroserp.pda.data.local.dao.MessageDao
import ir.zagroserp.pda.data.local.dao.OrderDao
import ir.zagroserp.pda.data.local.dao.ProductDao
import ir.zagroserp.pda.data.local.dao.SyncStateDao
import ir.zagroserp.pda.data.local.dao.VisitDao
import ir.zagroserp.pda.data.local.dao.ZonePermissionDao
import ir.zagroserp.pda.data.local.entity.CustomerEntity
import ir.zagroserp.pda.data.local.entity.DiscountEntity
import ir.zagroserp.pda.data.local.entity.InvoiceEntity
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.data.local.entity.OrderEntity
import ir.zagroserp.pda.data.local.entity.OrderItemEntity
import ir.zagroserp.pda.data.local.entity.PendingLocationEntity
import ir.zagroserp.pda.data.local.entity.PendingOrderEntity
import ir.zagroserp.pda.data.local.entity.PendingVisitEntity
import ir.zagroserp.pda.data.local.entity.ProductEntity
import ir.zagroserp.pda.data.local.entity.SyncStateEntity
import ir.zagroserp.pda.data.local.entity.TodayCustomerEntity
import ir.zagroserp.pda.data.local.entity.TourCustomerEntity
import ir.zagroserp.pda.data.local.entity.TourEntity
import ir.zagroserp.pda.data.local.entity.VisitEntity
import ir.zagroserp.pda.data.local.entity.ZonePermissionEntity

@Database(
    entities = [
        ProductEntity::class,
        DiscountEntity::class,
        CustomerEntity::class,
        TourEntity::class,
        TourCustomerEntity::class,
        VisitEntity::class,
        PendingVisitEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PendingOrderEntity::class,
        MessageEntity::class,
        InvoiceEntity::class,
        ZonePermissionEntity::class,
        PendingLocationEntity::class,
        SyncStateEntity::class,
        TodayCustomerEntity::class,
    ],
    // v2 adds `today_customers`. A destructive fallback is deliberately NOT relied upon for this
    // step: it drops every table, which would silently discard queued offline orders and visits.
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ZagrosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun visitDao(): VisitDao
    abstract fun orderDao(): OrderDao
    abstract fun messageDao(): MessageDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun zonePermissionDao(): ZonePermissionDao
    abstract fun locationQueueDao(): LocationQueueDao
    abstract fun syncStateDao(): SyncStateDao
}
