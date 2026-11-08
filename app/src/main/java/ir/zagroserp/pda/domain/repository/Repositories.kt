package ir.zagroserp.pda.domain.repository

import androidx.paging.PagingData
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.CheckInVerdict
import ir.zagroserp.pda.data.prefs.Session
import ir.zagroserp.pda.data.prefs.ThemeMode
import ir.zagroserp.pda.domain.model.BatchOutcome
import ir.zagroserp.pda.domain.model.CartLine
import ir.zagroserp.pda.domain.model.CategoryOffer
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Discount
import ir.zagroserp.pda.domain.model.Goals
import ir.zagroserp.pda.domain.model.HealthInfo
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.model.PendingQueueSummary
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import ir.zagroserp.pda.domain.model.ReportSummary
import ir.zagroserp.pda.domain.model.TodaySnapshot
import ir.zagroserp.pda.domain.model.Visit
import ir.zagroserp.pda.domain.model.ZonePermission
import kotlinx.coroutines.flow.Flow

sealed interface LoginOutcome {
    data class Success(val session: Session) : LoginOutcome
    data object Requires2Fa : LoginOutcome
    data class RequiresCompany(val companies: List<Pair<String, String>>) : LoginOutcome
    data class Failed(val message: String) : LoginOutcome
}

interface AuthRepository {
    val session: Flow<Session?>
    val isLoggedIn: Flow<Boolean>
    suspend fun login(
        username: String,
        password: String,
        otp: String? = null,
        companyId: String? = null,
    ): LoginOutcome

    suspend fun logout()
    suspend fun health(): AppResult<HealthInfo>
    suspend fun loadProfile(): AppResult<Unit>
    suspend fun registerPushToken(token: String): AppResult<Unit>
    suspend fun unregisterPushToken(token: String): AppResult<Unit>
    suspend fun refreshIfNeeded()
}

interface CatalogRepository {
    fun pagingProducts(query: String, category: String = ""): Flow<PagingData<Product>>
    fun pagingPromotions(query: String): Flow<PagingData<Product>>

    /** Categories derived from the cached catalog, for the filter chips. */
    fun observeCategories(): Flow<List<String>>

    fun observeProducts(): Flow<List<Product>>
    fun observeProduct(id: String): Flow<Product?>
    suspend fun getProduct(id: String): Product?
    suspend fun lookupByBarcode(barcode: String): AppResult<Product>
    suspend fun refreshProducts(): AppResult<Int>
    suspend fun productOffers(productId: String): AppResult<List<CategoryOffer>>
    suspend fun productUoms(productId: String): AppResult<List<ProductUom>>
    fun observeDiscounts(): Flow<List<Discount>>
    suspend fun refreshDiscounts(): AppResult<Unit>
}

interface CustomerRepository {
    fun pagingCustomers(query: String): Flow<PagingData<Customer>>
    fun observeCustomers(): Flow<List<Customer>>
    fun observeCustomer(id: String): Flow<Customer?>
    suspend fun getCustomer(id: String): Customer?

    /**
     * Fast local lookup used before creating a customer, so an obvious duplicate is reported
     * without a round trip. The server remains authoritative.
     */
    suspend fun findByNationalId(nationalId: String): Customer?
    suspend fun refreshCustomers(query: String? = null): AppResult<Int>
    suspend fun createCustomer(
        name: String,
        nationalId: String,
        phone: String?,
        address: String?,
        creditLimit: Double?,
        settlementDays: Int?,
        latitude: Double?,
        longitude: Double?,
    ): AppResult<Customer>

    suspend fun updateCustomer(
        id: String,
        name: String?,
        phone: String?,
        address: String?,
        city: String?,
        province: String?,
        latitude: Double?,
        longitude: Double?,
    ): AppResult<Customer>
}

interface VisitRepository {
    fun observeToday(date: String): Flow<TodaySnapshot>
    suspend fun refreshToday(date: String? = null): AppResult<TodaySnapshot>
    fun observeVisitsForCustomer(customerId: String): Flow<List<Visit>>
    fun observeVisitedCustomerIds(date: String): Flow<List<String>>
    fun observePendingVisits(): Flow<List<Visit>>
    /**
     * Records a visit. The location is validated against the customer's coordinates first:
     * a fix outside [ir.zagroserp.pda.core.Geofence.CHECK_IN_RADIUS_METERS] or one flagged as
     * mocked is rejected. Customers the API has no coordinates for are always visitable.
     */
    suspend fun checkIn(
        customerId: String,
        status: String,
        notes: String?,
        location: LocationPoint?,
        orderId: String?,
        visitType: String = "ROUTE",
    ): AppResult<Visit>

    /** Dry-run of the check-in guard, for showing the visitor the reason before/after a visit. */
    suspend fun evaluateCheckIn(
        customerId: String,
        location: LocationPoint?,
    ): ir.zagroserp.pda.core.CheckInVerdict

    suspend fun syncPending(): AppResult<BatchOutcome>
}

interface OrderRepository {
    fun pagingOrders(status: String): Flow<PagingData<Order>>
    fun observeRecentOrders(limit: Int = 20): Flow<List<Order>>
    fun observeOrder(id: String): Flow<Order?>
    fun observeOrderItems(orderId: String): Flow<List<ir.zagroserp.pda.domain.model.OrderItem>>
    fun observePendingOrders(): Flow<List<PendingOrderItem>>
    suspend fun refreshOrders(status: String? = null): AppResult<Unit>
    suspend fun loadOrder(id: String): AppResult<Order>
    /**
     * Places an order. [location] is forwarded to the server so the same geofence rule that
     * guards a visit can also flag an order taken far from the customer.
     */
    suspend fun submitOrder(
        customerId: String,
        customerName: String,
        date: String,
        lines: List<CartLine>,
        note: String?,
        discount: Double,
        location: LocationPoint? = null,
    ): AppResult<SubmitOrderResult>

    /** Client-side distance check used to warn before submitting. */
    suspend fun evaluateOrderLocation(
        customerId: String,
        location: LocationPoint?,
    ): CheckInVerdict

    suspend fun syncPending(): AppResult<BatchOutcome>
    suspend fun pendingCount(): Int
}

data class SubmitOrderResult(
    val order: Order,
    val queuedOffline: Boolean,
    val softWarning: String = "",
)

data class PendingOrderItem(
    val clientOrderId: String,
    val customerId: String,
    val customerName: String,
    val itemCount: Int,
    val estimatedTotal: Double,
    val status: String,
    val lastError: String,
    val createdAt: Long,
)

interface SyncRepository {
    val pendingSummary: Flow<PendingQueueSummary>
    fun observeLastSyncAt(): Flow<Long>
    suspend fun syncDelta(): AppResult<Unit>
    suspend fun bootstrap(): AppResult<Unit>
    suspend fun fullResync(): AppResult<Unit>
    suspend fun clearLocalData()
}

interface MessageRepository {
    fun observeMessages(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>

    /** Unread alerts the app generated itself (queue problems), as opposed to server messages. */
    fun observeUnreadAlertCount(): Flow<Int>

    suspend fun refreshMessages(): AppResult<Unit>

    /** Marks one message read locally and, best effort, on the server. */
    suspend fun markRead(id: String)

    /** Marks every message read so the unread badge stays consistent across devices. */
    suspend fun markAllRead(): AppResult<Unit>

    fun stream(): Flow<Notification>
}

/**
 * Keeps the app-generated alerts in the message list in sync with the current offline queue
 * state: an alert appears when a condition becomes true and disappears when it clears.
 */
interface SystemAlertsRepository {
    suspend fun reconcile()
}

interface InvoiceRepository {
    fun pagingInvoices(): Flow<PagingData<Invoice>>
    fun observeInvoices(): Flow<List<Invoice>>
    fun observeUnprintedCount(): Flow<Int>
    suspend fun refreshInvoices(): AppResult<Unit>
    suspend fun markPrinted(id: String): AppResult<String>
}

interface ReportRepository {
    suspend fun goals(): AppResult<Goals>
    suspend fun report(from: String?, to: String?): AppResult<ReportSummary>
    fun observeSalesToday(date: String): Flow<Double>
    fun observeOrdersToday(date: String): Flow<Int>
}

interface ZonePermissionRepository {
    fun observePermissions(): Flow<List<ZonePermission>>
    fun observeUsableCount(): Flow<Int>
    suspend fun refresh(): AppResult<Unit>
    suspend fun request(
        customerId: String,
        scope: String,
        reason: String,
        targetDate: String?,
        until: String?,
        notes: String?,
    ): AppResult<ZonePermission>

    suspend fun cancel(id: String): AppResult<Unit>
}

interface LocationRepository {
    fun observePendingCount(): Flow<Int>

    /** Queues a point locally, then pushes it immediately when the device is online. */
    suspend fun record(point: LocationPoint)

    /** Single-shot `POST /mobile/visitor/location` for near real-time tracking. */
    suspend fun sendNow(point: LocationPoint): AppResult<Unit>

    /** Uploads whatever is queued via `POST /mobile/visitor/locations/batch`. */
    suspend fun flush(): AppResult<Int>

    /** Same as [flush] but also resets the batching window – for background jobs and manual retry. */
    suspend fun flushNow(): AppResult<Int>

    suspend fun lastKnown(): LocationPoint?
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
