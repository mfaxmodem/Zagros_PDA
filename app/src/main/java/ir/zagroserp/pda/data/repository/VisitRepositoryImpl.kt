package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.core.AppError
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CheckInGuard
import ir.zagroserp.pda.core.CheckInVerdict
import ir.zagroserp.pda.core.rejectionMessage
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.core.JalaliDate
import ir.zagroserp.pda.core.QueueRetryPolicy
import ir.zagroserp.pda.core.SyncLimits
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.dao.VisitDao
import ir.zagroserp.pda.data.local.entity.PendingVisitEntity
import ir.zagroserp.pda.data.local.entity.TodayCustomerEntity
import ir.zagroserp.pda.data.local.entity.TourCustomerEntity
import ir.zagroserp.pda.data.local.entity.VisitEntity
import ir.zagroserp.pda.data.mapper.Mappers
import ir.zagroserp.pda.data.mapper.applyTo
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.dto.CreateVisitRequest
import ir.zagroserp.pda.data.remote.dto.VisitBatchRequest
import ir.zagroserp.pda.domain.model.BatchItemOutcome
import ir.zagroserp.pda.domain.model.BatchOutcome
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.model.TodaySnapshot
import ir.zagroserp.pda.domain.model.Tour
import ir.zagroserp.pda.domain.model.Visit
import ir.zagroserp.pda.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val visitDao: VisitDao,
    private val customerDao: CustomerDao,
    private val rejectionRecorder: QueueRejectionRecorder,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : VisitRepository {

    /**
     * Day-scoped view of the route. Reads the `today_customers` assignment rather than the whole
     * cached customer book — the cache holds every customer the visitor can reach (about 99 in
     * production) while a single day typically assigns one or two.
     */
    override fun observeToday(date: String): Flow<TodaySnapshot> = combine(
        customerDao.observeTodayTours(date),
        customerDao.observeTodayCustomers(date),
        customerDao.observeTodayLinks(date),
        visitDao.observeByDate(date),
    ) { tours, customers, links, visits ->
        val byId = customers.associateBy { it.id }
        TodaySnapshot(
            date = date,
            tours = tours.map { tour ->
                Tour(
                    id = tour.id,
                    name = tour.name,
                    customers = links
                        .filter { it.tourId == tour.id }
                        .mapNotNull { byId[it.customerId]?.toDomain() },
                )
            },
            customers = customers.map { it.toDomain() },
            visited = visits.map { entity ->
                entity.toDomain(byId[entity.customerId]?.name.orEmpty())
            },
        )
    }.flowOn(dispatchers.io)

    override suspend fun refreshToday(date: String?): AppResult<TodaySnapshot> {
        // The day is keyed by the date the caller asked for, so the value the UI observes and the
        // value written here can never drift apart over a timezone difference.
        val day = date?.take(10)?.takeIf { it.isNotBlank() } ?: JalaliDate.todayIso()
        val result = apiCall(json) { api.today(day) }
        return when (result) {
            is AppResult.Success -> {
                val body = result.data
                withContext(dispatchers.io) {
                    // Master data — keeps the full customer book for the Customers tab.
                    val customers = body.customers.map { dto ->
                        dto.toEntity(customerDao.getById(dto.id))
                    }
                    if (customers.isNotEmpty()) customerDao.upsertAll(customers)

                    customerDao.upsertTours(body.tours.mapIndexed { index, dto -> dto.toEntity(index) })
                    body.tours.forEach { tour ->
                        customerDao.clearTourCustomers(tour.id)
                        customerDao.upsertTourCustomers(
                            tour.customers.mapIndexed { index, customer ->
                                TourCustomerEntity(tour.id, customer.id, index)
                            },
                        )
                    }

                    // Day-scoped assignment: tour members keep their tour, the rest are direct.
                    val rows = TodayAssignmentBuilder.build(
                        date = day,
                        tours = body.tours.map { tour -> tour.id to tour.customers.map { it.id } },
                        flatCustomerIds = body.customers.map { it.id },
                    )

                    customerDao.clearToday(day)
                    if (rows.isNotEmpty()) customerDao.upsertTodayCustomers(rows)
                    customerDao.clearOtherDays(day)

                    visitDao.upsertAll(body.visited.map { it.toEntity() })
                }
                AppResult.Success(
                    TodaySnapshot(
                        date = body.date.ifBlank { day },
                        tours = body.tours.map { it.toDomain() },
                        customers = body.customers.map { it.toDomain() },
                        visited = body.visited.map { it.toDomain() },
                    ),
                )
            }

            is AppResult.Failure -> result
        }
    }

    override fun observeVisitsForCustomer(customerId: String): Flow<List<Visit>> =
        visitDao.observeForCustomer(customerId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeVisitedCustomerIds(date: String): Flow<List<String>> =
        visitDao.observeVisitedCustomerIds(date).flowOn(dispatchers.io)

    override fun observePendingVisits(): Flow<List<Visit>> =
        visitDao.observePending().map { list ->
            list.map { pending ->
                Visit(
                    id = pending.clientVisitId,
                    customerId = pending.customerId,
                    customerName = pending.customerName,
                    status = ir.zagroserp.pda.domain.model.VisitStatus.from(pending.status),
                    clientVisitId = pending.clientVisitId,
                    isPending = true,
                    createdAt = java.time.Instant.ofEpochMilli(pending.createdAt).toString(),
                )
            }
        }.flowOn(dispatchers.io)

    override suspend fun checkIn(
        customerId: String,
        status: String,
        notes: String?,
        location: LocationPoint?,
        orderId: String?,
        visitType: String,
    ): AppResult<Visit> {
        val customer = withContext(dispatchers.io) { customerDao.getById(customerId) }

        // Client-side half of the geofence rule: fail fast with a clear reason instead of letting
        // the server reject the visit after it has been queued.
        val verdict = evaluateCheckIn(customerId, location)
        if (!verdict.canProceed) {
            return AppResult.Failure(AppError.Validation(verdict.rejectionMessage()))
        }

        val clientVisitId = UUID.randomUUID().toString()
        val today = ir.zagroserp.pda.core.JalaliDate.todayIso()
        val request = CreateVisitRequest(
            customerId = customerId,
            visitType = visitType,
            status = status,
            notes = notes?.takeIf { it.isNotBlank() },
            latitude = location?.latitude,
            longitude = location?.longitude,
            // The server enforces the geofence itself; a coarse or mocked fix must be visible to it.
            accuracy = location?.accuracy,
            isMock = location?.isMock,
            orderId = orderId,
            clientVisitId = clientVisitId,
        )

        // Always persist locally first: the field device is frequently offline.
        withContext(dispatchers.io) {
            visitDao.upsert(
                VisitEntity(
                    id = clientVisitId,
                    customerId = customerId,
                    visitDate = today,
                    visitType = visitType,
                    status = status,
                    notes = notes.orEmpty(),
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    orderId = orderId.orEmpty(),
                    clientVisitId = clientVisitId,
                    createdAt = java.time.Instant.now().toString(),
                    isPending = true,
                ),
            )
            visitDao.enqueue(
                PendingVisitEntity(
                    clientVisitId = clientVisitId,
                    payloadJson = Mappers.json.encodeToString(
                        CreateVisitRequest.serializer(),
                        request,
                    ),
                    customerId = customerId,
                    customerName = customer?.name.orEmpty(),
                    status = status,
                ),
            )
        }

        val outcome = syncPending()
        val success = outcome is AppResult.Success && outcome.data.items.none {
            it.clientId == clientVisitId && it.isError
        }

        return if (success) {
            AppResult.Success(
                Visit(
                    id = clientVisitId,
                    customerId = customerId,
                    customerName = customer?.name.orEmpty(),
                    visitDate = today,
                    status = ir.zagroserp.pda.domain.model.VisitStatus.from(status),
                    clientVisitId = clientVisitId,
                    createdAt = java.time.Instant.now().toString(),
                ),
            )
        } else {
            AppResult.Success(
                Visit(
                    id = clientVisitId,
                    customerId = customerId,
                    customerName = customer?.name.orEmpty(),
                    visitDate = today,
                    status = ir.zagroserp.pda.domain.model.VisitStatus.from(status),
                    clientVisitId = clientVisitId,
                    isPending = true,
                    createdAt = java.time.Instant.now().toString(),
                ),
            )
        }
    }

    /**
     * Exposed separately so the UI can explain *why* a check-in would be accepted or refused
     * (distance, missing customer coordinates, mock fix) instead of only failing silently.
     */
    override suspend fun evaluateCheckIn(
        customerId: String,
        location: LocationPoint?,
    ): CheckInVerdict {
        val customer = withContext(dispatchers.io) { customerDao.getById(customerId) }
        return CheckInGuard.evaluate(
            customerLat = customer?.lat,
            customerLng = customer?.lng,
            fixLat = location?.latitude,
            fixLng = location?.longitude,
            isMock = location?.isMock == true,
            accuracyMeters = location?.accuracy ?: 0.0,
        )
    }

    override suspend fun syncPending(): AppResult<BatchOutcome> {
        val pending = withContext(dispatchers.io) {
            visitDao.getPending(SyncLimits.VISIT_BATCH_MAX)
        }
        if (pending.isEmpty()) return AppResult.Success(BatchOutcome())

        val payload = pending.mapNotNull { entry ->
            runCatching {
                Mappers.json.decodeFromString(CreateVisitRequest.serializer(), entry.payloadJson)
            }.getOrNull()
        }
        if (payload.isEmpty()) return AppResult.Success(BatchOutcome())

        return when (val result = apiCall(json) { api.createVisitsBatch(VisitBatchRequest(payload)) }) {
            is AppResult.Success -> {
                val items = result.data.results.map {
                    BatchItemOutcome(it.clientVisitId, it.status, it.visitId, it.message)
                }
                withContext(dispatchers.io) {
                    items.forEach { item ->
                        when {
                            item.isSuccess || item.isBlocked -> visitDao.dequeue(item.clientId)
                            item.isError -> visitDao.markRejected(item.clientId, item.message)
                        }
                    }
                    dropExhaustedVisits()
                }
                AppResult.Success(
                    BatchOutcome(
                        items = items,
                        created = result.data.summary.created,
                        duplicates = result.data.summary.duplicates,
                        errors = result.data.summary.errors,
                        blocked = result.data.summary.blocked,
                    ),
                )
            }

            is AppResult.Failure -> {
                withContext(dispatchers.io) {
                    if (QueueRetryPolicy.isPermanent(result.error)) {
                        // The batch was refused on its merits (e.g. the geofence), so every entry
                        // spends one rejection rather than waiting for a network that is fine.
                        pending.forEach {
                            visitDao.markRejected(it.clientVisitId, result.error.message)
                        }
                        dropExhaustedVisits()
                    } else {
                        pending.forEach {
                            visitDao.markTransportError(it.clientVisitId, result.error.message)
                        }
                    }
                }
                result
            }
        }
    }

    /**
     * Removes entries the server has permanently refused and raises an alert for each, so a
     * rejected visit is never silently lost.
     */
    private suspend fun dropExhaustedVisits() {
        val exhausted = visitDao.getExhausted(QueueRetryPolicy.MAX_REJECTIONS)
        if (exhausted.isEmpty()) return
        exhausted.forEach { entry ->
            rejectionRecorder.recordVisitRejection(
                clientVisitId = entry.clientVisitId,
                customerName = entry.customerName,
                reason = entry.lastError,
            )
        }
        visitDao.dequeueAll(exhausted.map { it.clientVisitId })
    }
}
