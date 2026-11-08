package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.DispatcherProvider
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
import ir.zagroserp.pda.data.local.entity.SyncStateEntity
import ir.zagroserp.pda.data.local.entity.TourCustomerEntity
import ir.zagroserp.pda.data.mapper.applyTo
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.domain.model.PendingQueueSummary
import ir.zagroserp.pda.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val db: ZagrosDatabase,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val visitDao: VisitDao,
    private val orderDao: OrderDao,
    private val messageDao: MessageDao,
    private val invoiceDao: InvoiceDao,
    private val zonePermissionDao: ZonePermissionDao,
    private val locationQueueDao: LocationQueueDao,
    private val syncStateDao: SyncStateDao,
    private val sessionStore: SessionStore,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : SyncRepository {

    override val pendingSummary: Flow<PendingQueueSummary> = combine(
        orderDao.observePendingCount(),
        visitDao.observePendingCount(),
        locationQueueDao.observePendingCount(),
        orderDao.observeErrorCount(),
    ) { orders, visits, locations, failedOrders ->
        PendingQueueSummary(
            orders = orders,
            visits = visits,
            locations = locations,
            failedOrders = failedOrders,
        )
    }.flowOn(dispatchers.io)

    override fun observeLastSyncAt(): Flow<Long> = syncStateDao.observe()
        .map { it?.lastSyncAt ?: 0L }
        .flowOn(dispatchers.io)

    override suspend fun syncDelta(): AppResult<Unit> {
        val since = withContext(dispatchers.io) { syncStateDao.get()?.serverTime }
        return applyDelta(since)
    }

    override suspend fun bootstrap(): AppResult<Unit> = applyDelta(null)

    override suspend fun fullResync(): AppResult<Unit> {
        withContext(dispatchers.io) {
            productDao.clear()
            customerDao.clear()
            customerDao.clearTours()
            customerDao.clearAllTourCustomers()
            customerDao.clearAllToday()
            visitDao.clear()
            orderDao.clear()
            invoiceDao.clear()
            messageDao.clear()
            zonePermissionDao.clear()
            syncStateDao.upsert(SyncStateEntity())
        }
        return applyDelta(null)
    }

    override suspend fun clearLocalData() = withContext(dispatchers.io) {
        productDao.clear()
        customerDao.clear()
        customerDao.clearTours()
        customerDao.clearAllTourCustomers()
            customerDao.clearAllToday()
        visitDao.clear()
        orderDao.clear()
        invoiceDao.clear()
        messageDao.clear()
        zonePermissionDao.clear()
        locationQueueDao.clear()
        syncStateDao.upsert(SyncStateEntity())
    }

    private suspend fun applyDelta(since: String?): AppResult<Unit> {
        val result = apiCall(json) { api.sync(since?.takeIf { it.isNotBlank() }) }
        return when (result) {
            is AppResult.Success -> {
                val delta = result.data
                withContext(dispatchers.io) {
                    // 1) Tombstones first so re-added products are not wiped afterwards.
                    if (delta.deletedIds.products.isNotEmpty()) {
                        productDao.deleteByIds(delta.deletedIds.products)
                    }

                    // 2) Products (MobileProduct has no image_url; keep the cached one).
                    if (delta.products.isNotEmpty()) {
                        val existing = delta.products
                            .map { it.id }
                            .chunked(200)
                            .flatMap { productDao.getByIds(it) }
                            .associateBy { it.id }
                        productDao.upsertAll(delta.products.map { it.toEntity(existing[it.id]) })
                    }

                    // 3) Stock snapshot merges into the product rows.
                    if (delta.productStock.isNotEmpty()) {
                        val current = delta.productStock
                            .map { it.productId }
                            .chunked(200)
                            .flatMap { productDao.getByIds(it) }
                            .associateBy { it.id }
                        val merged = delta.productStock.mapNotNull { stock ->
                            current[stock.productId]?.let { stock.applyTo(it) }
                        }
                        if (merged.isNotEmpty()) productDao.upsertAll(merged)
                    }

                    // 4) Master data.
                    if (delta.customers.isNotEmpty()) {
                        customerDao.upsertAll(
                            delta.customers.map { dto ->
                                dto.toEntity(customerDao.getById(dto.id))
                            },
                        )
                    }

                    if (delta.tours.isNotEmpty()) {
                        customerDao.upsertTours(delta.tours.mapIndexed { index, tour ->
                            tour.toEntity(index)
                        })
                        delta.tours.forEach { tour ->
                            customerDao.clearTourCustomers(tour.id)
                            customerDao.upsertTourCustomers(
                                tour.customers.mapIndexed { index, customer ->
                                    TourCustomerEntity(tour.id, customer.id, index)
                                },
                            )
                        }
                    }

                    if (delta.discounts.isNotEmpty()) {
                        productDao.upsertDiscounts(delta.discounts.map { it.toEntity() })
                    }

                    if (delta.visits.isNotEmpty()) {
                        visitDao.upsertAll(delta.visits.map { it.toEntity() })
                    }

                    if (delta.notifications.isNotEmpty()) {
                        // Preserve the local read flag – see upsertAllPreservingRead.
                        messageDao.upsertAllPreservingRead(delta.notifications.map { it.toEntity() })
                    }

                    if (delta.approvedInvoices.isNotEmpty()) {
                        invoiceDao.upsertAll(delta.approvedInvoices.map { it.toEntity() })
                    }

                    // 5) Advance the cursor only after everything has been persisted.
                    val cursor = delta.serverTime.ifBlank { since.orEmpty() }
                    val current = syncStateDao.get()
                    syncStateDao.upsert(
                        SyncStateEntity(
                            id = 0,
                            serverTime = cursor,
                            lastSyncAt = System.currentTimeMillis(),
                            lastBootstrapAt = current?.lastBootstrapAt ?: System.currentTimeMillis(),
                            productCount = productDao.count(),
                        ),
                    )
                    sessionStore.saveServerTime(cursor)
                }
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }
    }
}
