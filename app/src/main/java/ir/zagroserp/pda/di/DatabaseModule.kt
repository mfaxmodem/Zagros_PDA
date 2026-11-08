package ir.zagroserp.pda.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.zagroserp.pda.core.Db
import ir.zagroserp.pda.data.local.Migrations
import ir.zagroserp.pda.data.local.ZagrosDatabase
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.dao.InvoiceDao
import ir.zagroserp.pda.data.local.dao.LocationQueueDao
import ir.zagroserp.pda.data.local.dao.MessageDao
import ir.zagroserp.pda.data.local.dao.OrderDao
import ir.zagroserp.pda.data.local.dao.ProductDao
import ir.zagroserp.pda.data.local.dao.SyncStateDao
import ir.zagroserp.pda.data.local.dao.VisitDao
import ir.zagroserp.pda.data.local.dao.ZonePermissionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZagrosDatabase =
        Room.databaseBuilder(context, ZagrosDatabase::class.java, Db.NAME)
            .addMigrations(*Migrations.ALL)
            // Kept only as a last resort for development builds; every released version must ship
            // an explicit migration or an update would wipe the visitor's offline queue.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideProductDao(db: ZagrosDatabase): ProductDao = db.productDao()

    @Provides
    fun provideCustomerDao(db: ZagrosDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideVisitDao(db: ZagrosDatabase): VisitDao = db.visitDao()

    @Provides
    fun provideOrderDao(db: ZagrosDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideMessageDao(db: ZagrosDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideInvoiceDao(db: ZagrosDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideZonePermissionDao(db: ZagrosDatabase): ZonePermissionDao = db.zonePermissionDao()

    @Provides
    fun provideLocationQueueDao(db: ZagrosDatabase): LocationQueueDao = db.locationQueueDao()

    @Provides
    fun provideSyncStateDao(db: ZagrosDatabase): SyncStateDao = db.syncStateDao()
}
