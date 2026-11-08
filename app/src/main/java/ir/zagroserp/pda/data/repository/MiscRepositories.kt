package ir.zagroserp.pda.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import ir.zagroserp.pda.core.AppError
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.ConnectivityObserver
import ir.zagroserp.pda.core.Defaults
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.dao.InvoiceDao
import ir.zagroserp.pda.data.local.dao.LocationQueueDao
import ir.zagroserp.pda.data.local.dao.OrderDao
import ir.zagroserp.pda.data.local.dao.ZonePermissionDao
import ir.zagroserp.pda.data.local.entity.PendingLocationEntity
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.prefs.ThemeMode
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.dto.CreateZonePermissionRequest
import ir.zagroserp.pda.data.remote.dto.LocationBatchRequest
import ir.zagroserp.pda.data.remote.dto.LocationRequest
import ir.zagroserp.pda.domain.model.Goals
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.model.ReportSummary
import ir.zagroserp.pda.domain.model.ZonePermission
import ir.zagroserp.pda.domain.repository.InvoiceRepository
import ir.zagroserp.pda.domain.repository.LocationRepository
import ir.zagroserp.pda.domain.repository.ReportRepository
import ir.zagroserp.pda.domain.repository.SettingsRepository
import ir.zagroserp.pda.domain.repository.ZonePermissionRepository
import ir.zagroserp.pda.location.LocationUploadPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val invoiceDao: InvoiceDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : InvoiceRepository {

    override fun pagingInvoices(): Flow<PagingData<Invoice>> =
        Pager(
            config = PagingConfig(pageSize = Defaults.PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { invoiceDao.pagingSource() },
        ).flow
            .map { paging -> paging.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeInvoices(): Flow<List<Invoice>> =
        invoiceDao.observeAll().map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeUnprintedCount(): Flow<Int> = invoiceDao.observeUnprintedCount()
        .flowOn(dispatchers.io)

    override suspend fun refreshInvoices(): AppResult<Unit> {
        var page = 1
        while (page <= MAX_PAGES) {
            val result = apiCall(json) {
                api.approvedInvoices(page = page, pageSize = Defaults.MAX_PAGE_SIZE)
            }
            when (result) {
                is AppResult.Success -> {
                    val body = result.data
                    withContext(dispatchers.io) {
                        invoiceDao.upsertAll(body.invoices.map { it.toEntity() })
                    }
                    if (!body.hasMore || body.invoices.isEmpty()) return AppResult.Success(Unit)
                    page++
                }

                is AppResult.Failure -> return result
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun markPrinted(id: String): AppResult<String> =
        when (val result = apiCall(json) { api.markInvoicePrinted(id) }) {
            is AppResult.Success -> {
                val printedAt = result.data.printedAt.ifBlank { Instant.now().toString() }
                withContext(dispatchers.io) { invoiceDao.markPrinted(id, printedAt) }
                AppResult.Success(printedAt)
            }

            is AppResult.Failure -> {
                // Printing succeeded locally; only the server acknowledgement failed.
                val local = Instant.now().toString()
                withContext(dispatchers.io) { invoiceDao.markPrinted(id, local) }
                AppResult.Failure(result.error)
            }
        }

    private companion object {
        const val MAX_PAGES = 20
    }
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val orderDao: OrderDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : ReportRepository {

    override suspend fun goals(): AppResult<Goals> =
        when (val result = apiCall(json) { api.goals() }) {
            is AppResult.Success -> AppResult.Success(
                Goals(
                    targetValue = result.data.targetValue,
                    achievedValue = result.data.achievedValue,
                    progressPercent = result.data.progressPercent,
                    commissionRate = result.data.commissionRate,
                    estimatedCommission = result.data.estimatedCommission,
                    salesToday = result.data.salesToday,
                    orderCount = result.data.orderCount,
                ),
            )

            is AppResult.Failure -> result
        }

    override suspend fun report(from: String?, to: String?): AppResult<ReportSummary> =
        when (val result = apiCall(json) { api.reports(from, to) }) {
            is AppResult.Success -> AppResult.Success(
                ReportSummary(
                    from = result.data.from,
                    to = result.data.to,
                    orderCount = result.data.orderCount,
                    totalSales = result.data.totalSales,
                    visitCount = result.data.visitCount,
                    assignedCustomers = result.data.assignedCustomers,
                    debtTotal = result.data.debtTotal,
                ),
            )

            is AppResult.Failure -> result
        }

    override fun observeSalesToday(date: String): Flow<Double> =
        orderDao.observeSalesForDate(date).flowOn(dispatchers.io)

    override fun observeOrdersToday(date: String): Flow<Int> =
        orderDao.observeOrderCountForDate(date).flowOn(dispatchers.io)
}

@Singleton
class ZonePermissionRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val zonePermissionDao: ZonePermissionDao,
    private val customerDao: CustomerDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : ZonePermissionRepository {

    override fun observePermissions(): Flow<List<ZonePermission>> =
        zonePermissionDao.observeAll().map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeUsableCount(): Flow<Int> = zonePermissionDao.observeUsableCount()
        .flowOn(dispatchers.io)

    override suspend fun refresh(): AppResult<Unit> =
        when (val result = apiCall(json) { api.zonePermissions() }) {
            is AppResult.Success -> {
                withContext(dispatchers.io) {
                    val entities = result.data.map { dto ->
                        dto.toEntity(customerDao.getById(dto.customerId)?.name.orEmpty())
                    }
                    zonePermissionDao.upsertAll(entities)
                }
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    override suspend fun request(
        customerId: String,
        scope: String,
        reason: String,
        targetDate: String?,
        until: String?,
        notes: String?,
    ): AppResult<ZonePermission> {
        if (reason.trim().length < 3) {
            return AppResult.Failure(AppError.Validation("دلیل درخواست باید حداقل ۳ کاراکتر باشد"))
        }
        val request = CreateZonePermissionRequest(
            customerId = customerId,
            scope = scope,
            reason = reason.trim(),
            targetDate = targetDate,
            until = until,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        return when (val result = apiCall(json) { api.createZonePermission(request) }) {
            is AppResult.Success -> {
                val name = withContext(dispatchers.io) { customerDao.getById(customerId)?.name.orEmpty() }
                val entity = result.data.toEntity(name)
                withContext(dispatchers.io) { zonePermissionDao.upsert(entity) }
                AppResult.Success(entity.toDomain())
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun cancel(id: String): AppResult<Unit> =
        when (val result = apiCall(json) { api.cancelZonePermission(id) }) {
            is AppResult.Success -> {
                withContext(dispatchers.io) { zonePermissionDao.delete(id) }
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val locationQueueDao: LocationQueueDao,
    private val connectivity: ConnectivityObserver,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : LocationRepository {

    @Volatile
    private var lastPoint: LocationPoint? = null

    @Volatile
    private var lastFlushAt: Long = 0L

    override fun observePendingCount(): Flow<Int> = locationQueueDao.observePendingCount()
        .flowOn(dispatchers.io)

    override suspend fun record(point: LocationPoint) {
        if (!point.isValid) return
        lastPoint = point

        // Always persist first: the field device drops connectivity constantly, and a queued point
        // is never lost.
        val queueSize = withContext(dispatchers.io) {
            locationQueueDao.insert(
                PendingLocationEntity(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    accuracy = point.accuracy,
                    recordedAt = point.recordedAt,
                ),
            )
            locationQueueDao.prunePoisoned()
            locationQueueDao.count()
        }

        // Upload in batches. Sending each fix on its own cost one HTTPS request per 15 seconds,
        // which was the dominant battery and data cost of tracking.
        if (!connectivity.current()) return

        val now = System.currentTimeMillis()
        val elapsed = if (lastFlushAt == 0L) Long.MAX_VALUE else now - lastFlushAt
        if (!LocationUploadPolicy.shouldFlush(queueSize, elapsed)) return

        lastFlushAt = now
        flush()
    }

    override suspend fun flushNow(): AppResult<Int> {
        lastFlushAt = System.currentTimeMillis()
        return flush()
    }

    override suspend fun sendNow(point: LocationPoint): AppResult<Unit> {
        if (!point.isValid) {
            return AppResult.Failure(AppError.Validation("مختصات موقعیت نامعتبر است"))
        }
        val request = LocationRequest(
            latitude = point.latitude,
            longitude = point.longitude,
            accuracy = point.accuracy,
            recordedAt = point.recordedAt,
        )
        return when (val result = apiCall(json) { api.sendLocation(request) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun flush(): AppResult<Int> {
        val pending = withContext(dispatchers.io) {
            locationQueueDao.getPending(MAX_BATCH)
        }
        if (pending.isEmpty()) return AppResult.Success(0)

        val payload = pending.map {
            LocationRequest(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                recordedAt = it.recordedAt,
            )
        }
        return when (val result = apiCall(json) { api.sendLocationsBatch(LocationBatchRequest(payload)) }) {
            is AppResult.Success -> {
                withContext(dispatchers.io) {
                    locationQueueDao.delete(pending.map { it.id })
                }
                AppResult.Success(pending.size)
            }

            is AppResult.Failure -> {
                withContext(dispatchers.io) {
                    locationQueueDao.markAttempt(pending.map { it.id })
                }
                result
            }
        }
    }

    override suspend fun lastKnown(): LocationPoint? = lastPoint

    private companion object {
        const val MAX_BATCH = 500
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val sessionStore: SessionStore,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = sessionStore.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = sessionStore.setThemeMode(mode)
}