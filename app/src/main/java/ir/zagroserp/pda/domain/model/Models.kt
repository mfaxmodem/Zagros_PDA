package ir.zagroserp.pda.domain.model

import ir.zagroserp.pda.core.JalaliDate

data class CategoryOffer(
    val id: String,
    val name: String,
    val salePrice: Double,
    val isDefaultSale: Boolean,
)

data class ProductUom(
    val id: String,
    val uomId: String,
    val unitName: String,
    val conversionFactor: Double,
    val unitPrice: Double,
    val barcode: String,
    val isDefaultForSale: Boolean,
) {
    /** Price expressed per base unit, handy for comparison across packagings. */
    val pricePerBaseUnit: Double
        get() = if (conversionFactor > 0) unitPrice / conversionFactor else unitPrice
}

data class Product(
    val id: String,
    val code: String,
    val name: String,
    val latinName: String = "",
    val barcode: String = "",
    val category: String = "",
    val unit: String = "",
    val sellPrice: Double = 0.0,
    val buyPrice: Double = 0.0,
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val hasPromotion: Boolean = false,
    val promoPercent: Double = 0.0,
    val totalQty: Double = 0.0,
    val reservedQty: Double = 0.0,
    val availableQty: Double = 0.0,
    val minStock: Double = 0.0,
    val taxMu: Double = 0.0,
    val offers: List<CategoryOffer> = emptyList(),
    val uoms: List<ProductUom> = emptyList(),
    val updatedAt: String = "",
) {
    val inStock: Boolean get() = availableQty > 0
    val isLowStock: Boolean get() = availableQty in 0.0001..minStock.coerceAtLeast(1.0)
    val defaultOffer: CategoryOffer? get() = offers.firstOrNull { it.isDefaultSale } ?: offers.firstOrNull()
    val defaultUom: ProductUom? get() = uoms.firstOrNull { it.isDefaultForSale } ?: uoms.firstOrNull()
}

data class Customer(
    val id: String,
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
    val isLocalOnly: Boolean = false,
) {
    val hasLocation: Boolean get() = lat != 0.0 || lng != 0.0

    /**
     * The API keeps `balance` in the customer domain and currently always returns `0`, so a
     * non-zero value is the only trustworthy signal. Showing "0 debt" for every customer would be
     * worse than showing nothing.
     */
    val hasKnownDebt: Boolean get() = balance > 0.0
}

data class Tour(
    val id: String,
    val name: String,
    val customers: List<Customer> = emptyList(),
)

enum class VisitStatus(val apiValue: String, val label: String) {
    PLANNED("PLANNED", "برنامه‌ریزی‌شده"),
    VISITED("VISITED", "بازدید شد"),
    NO_ANSWER("NO_ANSWER", "پاسخی داده نشد"),
    CLOSED("CLOSED", "بسته");

    companion object {
        fun from(value: String): VisitStatus =
            entries.firstOrNull { it.apiValue.equals(value, true) } ?: VISITED
    }
}

enum class VisitType(val apiValue: String, val label: String) {
    ROUTE("ROUTE", "مسیر"),
    EXTRA("EXTRA", "خارج از مسیر");

    companion object {
        fun from(value: String): VisitType =
            entries.firstOrNull { it.apiValue.equals(value, true) } ?: ROUTE
    }
}

data class Visit(
    val id: String,
    val customerId: String,
    val customerName: String = "",
    val visitDate: String = "",
    val visitType: VisitType = VisitType.ROUTE,
    val status: VisitStatus = VisitStatus.VISITED,
    val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val orderId: String = "",
    val clientVisitId: String = "",
    val isPending: Boolean = false,
    val createdAt: String = "",
)

enum class OrderStatus(val apiValue: String, val label: String) {
    PENDING("PENDING", "در انتظار تأیید"),
    APPROVED("APPROVED", "تأییدشده"),
    REJECTED("REJECTED", "ردشده"),
    CANCELLED("CANCELLED", "لغوشده"),
    BLOCKED("BLOCKED", "نیازمند بازبینی"),
    UNKNOWN("", "نامشخص");

    companion object {
        fun from(value: String): OrderStatus =
            entries.firstOrNull { it.apiValue.equals(value, true) } ?: UNKNOWN
    }
}

data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val totalLine: Double = 0.0,
    val uomId: String? = null,
    val uomName: String = "",
)

data class AvailabilityWarning(
    val productId: String,
    val productName: String,
    val requested: Double,
    val available: Double,
    val shortage: Double,
)

data class Order(
    val id: String,
    val number: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val date: String = "",
    val subTotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.UNKNOWN,
    val rawStatus: String = "",
    val rejectionReason: String = "",
    val note: String = "",
    val softWarning: String = "",
    val awaitingSupply: Boolean = false,
    val shortageQty: Double = 0.0,
    val warnings: List<AvailabilityWarning> = emptyList(),
    val items: List<OrderItem> = emptyList(),
    val clientOrderId: String = "",
    val isLocalOnly: Boolean = false,
    val createdAt: String = "",
)

/** A line in the in-memory order cart. */
data class CartLine(
    val product: Product,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val uomId: String? = null,
    val uomName: String = "",
    val offerId: String? = null,
    val offerName: String = "",
) {
    val lineTotal: Double get() = (quantity * unitPrice - discount).coerceAtLeast(0.0)
}

data class Notification(
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",
)

data class InvoiceLine(
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val discount: Double,
    val totalLine: Double,
)

data class Invoice(
    val id: String,
    val number: String = "",
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val subTotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val note: String = "",
    val printedAt: String = "",
    val items: List<InvoiceLine> = emptyList(),
) {
    val isPrinted: Boolean get() = printedAt.isNotBlank()
}

data class ZonePermission(
    val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val scope: String = "SAME_DAY",
    val validFrom: String = "",
    val validUntil: String = "",
    val reason: String = "",
    val notes: String = "",
    val status: String = "PENDING",
    val decisionNote: String = "",
    val isUsable: Boolean = false,
    val createdAt: String = "",
) {
    val statusLabel: String
        get() = when (status.uppercase()) {
            "PENDING" -> "در انتظار تأیید"
            "APPROVED" -> "تأییدشده"
            "REJECTED" -> "ردشده"
            "EXPIRED" -> "منقضی"
            "CANCELLED" -> "لغوشده"
            else -> status
        }

    val scopeLabel: String
        get() = when (scope.uppercase()) {
            "SINGLE_VISIT" -> "یک‌بار مصرف"
            "SAME_DAY" -> "همان روز"
            "DATE_RANGE" -> "بازه‌ی زمانی"
            else -> scope
        }
}

data class Discount(
    val id: String,
    val title: String = "",
    val type: String = "",
    val description: String = "",
    val value: Double = 0.0,
    val valueType: String = "",
    val validFrom: String = "",
    val validTo: String = "",
    val productIds: List<String> = emptyList(),
    val isActive: Boolean = true,
) {
    val displayValue: String
        get() = if (valueType.equals("percent", true)) "${trim(value)}٪" else trim(value)

    private fun trim(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
}

data class TodaySnapshot(
    val date: String = "",
    val tours: List<Tour> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val visited: List<Visit> = emptyList(),
) {
    val visitedCustomerIds: Set<String> get() = visited.map { it.customerId }.toSet()
    val pendingCustomers: List<Customer> get() = customers.filterNot { it.id in visitedCustomerIds }
    val progressPercent: Float
        get() = if (customers.isEmpty()) 0f else visited.size.toFloat() / customers.size * 100f

    val dateJalali: String get() = JalaliDate.formatIso(date)
}

data class Goals(
    val targetValue: Double = 0.0,
    val achievedValue: Double = 0.0,
    val progressPercent: Double = 0.0,
    val commissionRate: Double = 0.0,
    val estimatedCommission: Double = 0.0,
    val salesToday: Double = 0.0,
    val orderCount: Int = 0,
)

data class ReportSummary(
    val from: String = "",
    val to: String = "",
    val orderCount: Int = 0,
    val totalSales: Double = 0.0,
    val visitCount: Int = 0,
    val assignedCustomers: Int = 0,
    val debtTotal: Double = 0.0,
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double = 0.0,
    val recordedAt: String,
    /** True when Android reports the fix as coming from a mock provider. */
    val isMock: Boolean = false,
) {
    val isValid: Boolean
        get() = latitude in 25.0..40.5 && longitude in 44.0..63.5 &&
            !latitude.isNaN() && !longitude.isNaN()

    /** A fix precise enough to judge a 50 m radius. */
    val isReliable: Boolean
        get() = accuracy <= ir.zagroserp.pda.core.Geofence.MAX_TRUSTED_ACCURACY_METERS

    /**
     * How old the fix is. A cached fix from another place must not be used to judge a 50 m
     * radius, so callers filter on this before trusting [latitude]/[longitude].
     */
    val ageMillis: Long
        get() = runCatching {
            System.currentTimeMillis() - java.time.Instant.parse(recordedAt).toEpochMilli()
        }.getOrDefault(0L)

    val ageSeconds: Long get() = ageMillis / 1000
}

data class HealthInfo(
    val status: String = "",
    val serverVersion: String = "",
    val minAppVersion: String = "",
    val serverTime: String = "",
)

data class VisitorProfile(
    val visitorId: String = "",
    val userId: String = "",
    val name: String = "",
    val username: String = "",
)

data class PendingQueueSummary(
    val orders: Int = 0,
    val visits: Int = 0,
    val locations: Int = 0,
    val failedOrders: Int = 0,
) {
    val total: Int get() = orders + visits + locations
    val hasFailures: Boolean get() = failedOrders > 0
}

/** Outcome of one item inside a batch submission. */
data class BatchItemOutcome(
    val clientId: String,
    val status: String,
    val serverId: String = "",
    val message: String = "",
) {
    val isSuccess: Boolean get() = status == "created" || status == "duplicate"
    val isBlocked: Boolean get() = status == "blocked"
    val isError: Boolean get() = status == "error"
}

data class BatchOutcome(
    val items: List<BatchItemOutcome> = emptyList(),
    val created: Int = 0,
    val duplicates: Int = 0,
    val errors: Int = 0,
    val blocked: Int = 0,
) {
    val allFailed: Boolean get() = items.isNotEmpty() && created + duplicates + blocked == 0
}
