package ir.zagroserp.pda.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CheckInGuard
import ir.zagroserp.pda.core.CheckInVerdict
import ir.zagroserp.pda.core.Defaults
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.core.QueueRetryPolicy
import ir.zagroserp.pda.core.SyncLimits
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.dao.OrderDao
import ir.zagroserp.pda.data.local.dao.ProductDao
import ir.zagroserp.pda.data.local.entity.OrderWithCustomerName
import ir.zagroserp.pda.data.local.entity.PendingOrderEntity
import ir.zagroserp.pda.data.mapper.Mappers
import ir.zagroserp.pda.data.mapper.applyTo
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.dto.CreateOrderRequest
import ir.zagroserp.pda.data.remote.dto.OrderBatchRequest
import ir.zagroserp.pda.data.remote.dto.OrderItemRequest
import ir.zagroserp.pda.data.remote.dto.OrderResponseDto
import ir.zagroserp.pda.domain.model.BatchItemOutcome
import ir.zagroserp.pda.domain.model.BatchOutcome
import ir.zagroserp.pda.domain.model.CartLine
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.model.OrderItem
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.domain.repository.PendingOrderItem
import ir.zagroserp.pda.domain.repository.SubmitOrderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val orderDao: OrderDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val rejectionRecorder: QueueRejectionRecorder,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : OrderRepository {

    override fun pagingOrders(status: String): Flow<PagingData<Order>> =
        Pager(
            config = PagingConfig(pageSize = Defaults.PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { orderDao.pagingSource(status.trim()) },
        ).flow
            .map { paging -> paging.map { it.resolve() } }
            .flowOn(dispatchers.io)

    override fun observeRecentOrders(limit: Int): Flow<List<Order>> =
        orderDao.observeRecent(limit).map { list -> list.map { it.resolve() } }
            .flowOn(dispatchers.io)

    override fun observeOrder(id: String): Flow<Order?> =
        orderDao.observeById(id).map { it?.resolve() }.flowOn(dispatchers.io)

    /**
     * Prefers the name stored on the order and falls back to the local customer record, so the
     * history list is never blank just because the list endpoint omitted `customer_name`.
     */
    private fun OrderWithCustomerName.resolve(): Order {
        val order = order.toDomain()
        return if (order.customerName.isBlank() && !resolvedCustomerName.isNullOrBlank()) {
            order.copy(customerName = resolvedCustomerName)
        } else {
            order
        }
    }

    override fun observeOrderItems(orderId: String): Flow<List<OrderItem>> =
        orderDao.observeItems(orderId).map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observePendingOrders(): Flow<List<PendingOrderItem>> =
        orderDao.observePending().map { list ->
            list.map {
                PendingOrderItem(
                    clientOrderId = it.clientOrderId,
                    customerId = it.customerId,
                    customerName = it.customerName,
                    itemCount = it.itemCount,
                    estimatedTotal = it.estimatedTotal,
                    status = it.status,
                    lastError = it.lastError,
                    createdAt = it.createdAt,
                )
            }
        }.flowOn(dispatchers.io)

    override suspend fun refreshOrders(status: String?): AppResult<Unit> {
        var page = 1
        while (page <= MAX_PAGES) {
            val result = apiCall(json) {
                api.orders(
                    status = status?.takeIf { it.isNotBlank() },
                    page = page,
                    pageSize = Defaults.MAX_PAGE_SIZE,
                )
            }
            when (result) {
                is AppResult.Success -> {
                    val body = result.data
                    withContext(dispatchers.io) {
                        body.orders.forEach { saveOrder(it) }
                    }
                    if (!body.hasMore || body.orders.isEmpty()) return AppResult.Success(Unit)
                    page++
                }

                is AppResult.Failure -> return result
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun loadOrder(id: String): AppResult<Order> =
        when (val result = apiCall(json) { api.order(id) }) {
            is AppResult.Success -> {
                val order = withContext(dispatchers.io) { saveOrder(result.data) }
                AppResult.Success(order)
            }

            is AppResult.Failure -> {
                val cached = withContext(dispatchers.io) {
                    orderDao.getById(id)?.let { entity ->
                        entity.toDomain(orderDao.getItems(id))
                    }
                }
                if (cached != null) AppResult.Success(cached) else result
            }
        }

    override suspend fun evaluateOrderLocation(
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

    override suspend fun submitOrder(
        customerId: String,
        customerName: String,
        date: String,
        lines: List<CartLine>,
        note: String?,
        discount: Double,
        location: LocationPoint?,
    ): AppResult<SubmitOrderResult> {
        if (lines.isEmpty()) {
            return AppResult.Failure(
                ir.zagroserp.pda.core.AppError.Validation("سبد سفارش خالی است"),
            )
        }

        val clientOrderId = UUID.randomUUID().toString().take(64)
        val request = CreateOrderRequest(
            customerId = customerId,
            date = date,
            discount = discount,
            note = note?.takeIf { it.isNotBlank() },
            clientOrderId = clientOrderId,
            // Sent so the server can flag an order taken outside the customer's radius.
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracy = location?.accuracy,
            isMock = location?.isMock,
            items = lines.map { line ->
                OrderItemRequest(
                    productId = line.product.id,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    discount = line.discount,
                    uomId = line.uomId,
                )
            },
        )

        val estimatedTotal = lines.sumOf { it.lineTotal }
        withContext(dispatchers.io) {
            orderDao.enqueue(
                PendingOrderEntity(
                    clientOrderId = clientOrderId,
                    payloadJson = Mappers.json.encodeToString(CreateOrderRequest.serializer(), request),
                    customerId = customerId,
                    customerName = customerName,
                    itemCount = lines.size,
                    estimatedTotal = estimatedTotal,
                    status = "PENDING",
                ),
            )
        }

        val sync = syncPending()
        val outcome = (sync as? AppResult.Success)?.data

        val failed = outcome?.items?.firstOrNull { it.clientId == clientOrderId && it.isError }
        if (failed != null) {
            return AppResult.Failure(
                ir.zagroserp.pda.core.AppError.Validation(failed.message.ifBlank { "ثبت سفارش ناموفق بود" }),
            )
        }

        val created = outcome?.items?.firstOrNull { it.clientId == clientOrderId && it.isSuccess }
        val blocked = outcome?.items?.firstOrNull { it.clientId == clientOrderId && it.isBlocked }

        return if (created != null || blocked != null) {
            val serverId = created?.serverId ?: blocked?.serverId.orEmpty()
            val order = withContext(dispatchers.io) {
                val entity = orderDao.getById(serverId)
                val warnings = entity?.let { Mappers.decodeWarnings(it.availabilityWarningsJson) }
                SubmitOrderResult(
                    order = entity?.toDomain(orderDao.getItems(serverId)) ?: Order(
                        id = serverId,
                        customerId = customerId,
                        customerName = customerName,
                        date = date,
                        finalAmount = estimatedTotal,
                        clientOrderId = clientOrderId,
                    ),
                    queuedOffline = false,
                    softWarning = entity?.softWarning.orEmpty(),
                ).also { result ->
                    if (result.softWarning.isBlank() && warnings?.isNotEmpty() == true) {
                        // Availability warnings are surfaced through the order model itself.
                    }
                }
            }
            AppResult.Success(order)
        } else {
            // Still offline: the order is safely queued and will sync in the background.
            AppResult.Success(
                SubmitOrderResult(
                    order = Order(
                        id = clientOrderId,
                        customerId = customerId,
                        customerName = customerName,
                        date = date,
                        finalAmount = estimatedTotal,
                        clientOrderId = clientOrderId,
                        isLocalOnly = true,
                        status = ir.zagroserp.pda.domain.model.OrderStatus.PENDING,
                    ),
                    queuedOffline = true,
                ),
            )
        }
    }

    override suspend fun syncPending(): AppResult<BatchOutcome> {
        val pending = withContext(dispatchers.io) { orderDao.getPending(SyncLimits.ORDER_BATCH_MAX) }
        if (pending.isEmpty()) return AppResult.Success(BatchOutcome())

        val payload = pending.mapNotNull { entry ->
            runCatching {
                Mappers.json.decodeFromString(CreateOrderRequest.serializer(), entry.payloadJson)
            }.getOrNull()
        }
        if (payload.isEmpty()) return AppResult.Success(BatchOutcome())

        return when (val result = apiCall(json) { api.createOrdersBatch(OrderBatchRequest(payload)) }) {
            is AppResult.Success -> {
                val items = result.data.results.map {
                    BatchItemOutcome(it.clientOrderId, it.status, it.orderId, it.message)
                }
                withContext(dispatchers.io) {
                    items.forEach { item ->
                        when {
                            item.isSuccess || item.isBlocked -> {
                                orderDao.dequeue(item.clientId)
                                if (item.serverId.isNotBlank()) {
                                    val local = orderDao.findByClientOrderId(item.clientId)
                                    if (local != null && local.id != item.serverId) {
                                        orderDao.upsertOrder(
                                            local.copy(id = item.serverId, isLocalOnly = false),
                                        )
                                        val movedItems = orderDao.getItems(local.id)
                                            .map { it.copy(orderId = item.serverId) }
                                        orderDao.deleteItems(local.id)
                                        if (movedItems.isNotEmpty()) orderDao.insertItems(movedItems)
                                    }
                                }
                            }

                            item.isError -> orderDao.markRejected(item.clientId, item.message)
                        }
                    }
                    dropExhaustedOrders()
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
                        pending.forEach {
                            orderDao.markRejected(it.clientOrderId, result.error.message)
                        }
                        dropExhaustedOrders()
                    } else {
                        pending.forEach {
                            orderDao.markTransportError(it.clientOrderId, result.error.message)
                        }
                    }
                }
                result
            }
        }
    }

    /**
     * Removes orders the server has permanently refused and raises an alert for each. Silently
     * dropping them would leave the visitor believing an order was placed.
     */
    private suspend fun dropExhaustedOrders() {
        val exhausted = orderDao.getExhausted(QueueRetryPolicy.MAX_REJECTIONS)
        if (exhausted.isEmpty()) return
        exhausted.forEach { entry ->
            rejectionRecorder.recordOrderRejection(
                clientOrderId = entry.clientOrderId,
                customerName = entry.customerName,
                reason = entry.lastError,
            )
        }
        orderDao.dequeueAll(exhausted.map { it.clientOrderId })
    }

    override suspend fun pendingCount(): Int = withContext(dispatchers.io) {
        orderDao.getPending(SyncLimits.ORDER_BATCH_MAX).size
    }

    private suspend fun saveOrder(dto: OrderResponseDto): Order {
        val existing = orderDao.getById(dto.id)
        val entity = dto.toEntity(existing)
        val items = dto.items.map { it.toEntity(dto.id) }
        orderDao.saveOrderWithItems(entity, items)
        return entity.toDomain(items)
    }

    private companion object {
        const val MAX_PAGES = 20
    }
}
