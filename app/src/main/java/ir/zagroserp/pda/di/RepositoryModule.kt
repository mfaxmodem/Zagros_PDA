package ir.zagroserp.pda.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.zagroserp.pda.data.repository.AuthRepositoryImpl
import ir.zagroserp.pda.data.repository.CatalogRepositoryImpl
import ir.zagroserp.pda.data.repository.CustomerRepositoryImpl
import ir.zagroserp.pda.data.repository.InvoiceRepositoryImpl
import ir.zagroserp.pda.data.repository.LocationRepositoryImpl
import ir.zagroserp.pda.data.repository.MessageRepositoryImpl
import ir.zagroserp.pda.data.repository.OrderRepositoryImpl
import ir.zagroserp.pda.data.repository.ReportRepositoryImpl
import ir.zagroserp.pda.data.repository.SettingsRepositoryImpl
import ir.zagroserp.pda.data.repository.SyncRepositoryImpl
import ir.zagroserp.pda.data.repository.SystemAlertsRepositoryImpl
import ir.zagroserp.pda.data.repository.VisitRepositoryImpl
import ir.zagroserp.pda.data.repository.ZonePermissionRepositoryImpl
import ir.zagroserp.pda.domain.repository.AuthRepository
import ir.zagroserp.pda.domain.repository.CatalogRepository
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.domain.repository.InvoiceRepository
import ir.zagroserp.pda.domain.repository.LocationRepository
import ir.zagroserp.pda.domain.repository.MessageRepository
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.domain.repository.ReportRepository
import ir.zagroserp.pda.domain.repository.SettingsRepository
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.domain.repository.SystemAlertsRepository
import ir.zagroserp.pda.domain.repository.VisitRepository
import ir.zagroserp.pda.domain.repository.ZonePermissionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindVisitRepository(impl: VisitRepositoryImpl): VisitRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindSystemAlertsRepository(impl: SystemAlertsRepositoryImpl): SystemAlertsRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindZonePermissionRepository(impl: ZonePermissionRepositoryImpl): ZonePermissionRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
