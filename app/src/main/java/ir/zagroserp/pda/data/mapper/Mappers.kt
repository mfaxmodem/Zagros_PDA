package ir.zagroserp.pda.data.mapper

import ir.zagroserp.pda.data.local.entity.CustomerEntity
import ir.zagroserp.pda.data.local.entity.DiscountEntity
import ir.zagroserp.pda.data.local.entity.InvoiceEntity
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.data.local.entity.OrderEntity
import ir.zagroserp.pda.data.local.entity.OrderItemEntity
import ir.zagroserp.pda.data.local.entity.ProductEntity
import ir.zagroserp.pda.data.local.entity.TourEntity
import ir.zagroserp.pda.data.local.entity.VisitEntity
import ir.zagroserp.pda.data.local.entity.ZonePermissionEntity
import ir.zagroserp.pda.data.remote.dto.AvailabilityWarningDto
import ir.zagroserp.pda.data.remote.dto.CategoryOfferDto
import ir.zagroserp.pda.data.remote.dto.DiscountDto
import ir.zagroserp.pda.data.remote.dto.InvoiceItemDto
import ir.zagroserp.pda.data.remote.dto.MobileCustomerDto
import ir.zagroserp.pda.data.remote.dto.MobileInvoiceDto
import ir.zagroserp.pda.data.remote.dto.MobileProductDto
import ir.zagroserp.pda.data.remote.dto.NotificationDto
import ir.zagroserp.pda.data.remote.dto.OrderItemDto
import ir.zagroserp.pda.data.remote.dto.OrderResponseDto
import ir.zagroserp.pda.data.remote.dto.ProductResponseDto
import ir.zagroserp.pda.data.remote.dto.ProductStockDto
import ir.zagroserp.pda.data.remote.dto.TourDto
import ir.zagroserp.pda.data.remote.dto.UomDto
import ir.zagroserp.pda.data.remote.dto.VisitDto
import ir.zagroserp.pda.data.remote.dto.ZonePermissionDto
import ir.zagroserp.pda.domain.model.AvailabilityWarning
import ir.zagroserp.pda.domain.model.CategoryOffer
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Discount
import ir.zagroserp.pda.domain.model.Invoice
import ir.zagroserp.pda.domain.model.InvoiceLine
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.domain.model.Order
import ir.zagroserp.pda.domain.model.OrderItem
import ir.zagroserp.pda.domain.model.OrderStatus
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import ir.zagroserp.pda.domain.model.Tour
import ir.zagroserp.pda.domain.model.Visit
import ir.zagroserp.pda.domain.model.VisitStatus
import ir.zagroserp.pda.domain.model.VisitType
import ir.zagroserp.pda.domain.model.ZonePermission
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * JSON blob codecs for Room columns. Keeping them here means no other layer ever has to
 * know that `categories` / `uoms` / `availability_warnings` are stored as JSON text.
 */
object Mappers {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val offerSerializer = ListSerializer(CategoryOfferDto.serializer())
    private val uomSerializer = ListSerializer(UomDto.serializer())
    private val stringSerializer = ListSerializer(String.serializer())
    private val warningSerializer = ListSerializer(AvailabilityWarningDto.serializer())
    private val invoiceItemSerializer = ListSerializer(InvoiceItemDto.serializer())

    fun encodeOffers(offers: List<CategoryOfferDto>): String =
        runCatching { json.encodeToString(offerSerializer, offers) }.getOrDefault("[]")

    fun encodeUoms(uoms: List<UomDto>): String =
        runCatching { json.encodeToString(uomSerializer, uoms) }.getOrDefault("[]")

    fun encodeStrings(values: List<String>): String =
        runCatching { json.encodeToString(stringSerializer, values) }.getOrDefault("[]")

    fun encodeWarnings(values: List<AvailabilityWarningDto>): String =
        runCatching { json.encodeToString(warningSerializer, values) }.getOrDefault("[]")

    fun encodeInvoiceItems(values: List<InvoiceItemDto>): String =
        runCatching { json.encodeToString(invoiceItemSerializer, values) }.getOrDefault("[]")

    fun decodeOffers(raw: String): List<CategoryOfferDto> =
        runCatching { json.decodeFromString(offerSerializer, raw) }.getOrDefault(emptyList())

    fun decodeUoms(raw: String): List<UomDto> =
        runCatching { json.decodeFromString(uomSerializer, raw) }.getOrDefault(emptyList())

    fun decodeStrings(raw: String): List<String> =
        runCatching { json.decodeFromString(stringSerializer, raw) }.getOrDefault(emptyList())

    fun decodeWarnings(raw: String): List<AvailabilityWarningDto> =
        runCatching { json.decodeFromString(warningSerializer, raw) }.getOrDefault(emptyList())

    fun decodeInvoiceItems(raw: String): List<InvoiceItemDto> =
        runCatching { json.decodeFromString(invoiceItemSerializer, raw) }.getOrDefault(emptyList())
}

// ---- catalog --------------------------------------------------------------

fun CategoryOfferDto.toDomain() = CategoryOffer(
    id = id,
    name = category,
    salePrice = salePrice,
    isDefaultSale = isDefaultSale,
)

fun UomDto.toDomain() = ProductUom(
    id = id,
    uomId = uomId.ifBlank { id },
    unitName = unitName.ifBlank { "بسته" },
    conversionFactor = if (conversionFactor > 0) conversionFactor else 1.0,
    unitPrice = unitPrice,
    barcode = barcode,
    isDefaultForSale = isDefaultForSale,
)

fun ProductResponseDto.toEntity() = ProductEntity(
    id = id,
    companyId = companyId,
    code = code,
    name = name,
    latinName = latinName,
    printName = printName,
    barcode = barcode,
    category = category,
    unit = unit,
    buyPrice = buyPrice,
    sellPrice = sellPrice,
    warehouseId = warehouseId,
    imageUrl = imageUrl,
    weightKg = weightKg,
    volumeM3 = volumeM3,
    minStock = minStock,
    maxStock = maxStock,
    sstid = sstid,
    taxMu = taxMu,
    isActive = isActive,
    totalQty = totalQty,
    reservedQty = reservedQty,
    availableQty = availableQty,
    stockUpdatedAt = stockUpdatedAt,
    categoriesJson = Mappers.encodeOffers(categories),
    uomsJson = Mappers.encodeUoms(uoms),
    updatedAt = updatedAt,
)

fun MobileProductDto.toEntity(existing: ProductEntity? = null) = ProductEntity(
    id = id,
    companyId = existing?.companyId.orEmpty(),
    code = code.ifBlank { existing?.code.orEmpty() },
    name = name.ifBlank { existing?.name.orEmpty() },
    latinName = existing?.latinName.orEmpty(),
    printName = existing?.printName.orEmpty(),
    barcode = existing?.barcode.orEmpty(),
    category = category.ifBlank { existing?.category.orEmpty() },
    unit = unit.ifBlank { existing?.unit.orEmpty() },
    buyPrice = existing?.buyPrice ?: 0.0,
    sellPrice = if (sellPrice > 0) sellPrice else existing?.sellPrice ?: 0.0,
    warehouseId = existing?.warehouseId.orEmpty(),
    imageUrl = imageUrl.ifBlank { existing?.imageUrl.orEmpty() },
    weightKg = existing?.weightKg ?: 0.0,
    volumeM3 = existing?.volumeM3 ?: 0.0,
    minStock = existing?.minStock ?: 0.0,
    maxStock = existing?.maxStock ?: 0.0,
    sstid = existing?.sstid.orEmpty(),
    taxMu = existing?.taxMu ?: 0.0,
    isActive = true,
    hasPromotion = hasPromotion,
    promoPercent = promoPercent,
    totalQty = existing?.totalQty ?: 0.0,
    reservedQty = existing?.reservedQty ?: 0.0,
    availableQty = existing?.availableQty ?: 0.0,
    stockUpdatedAt = existing?.stockUpdatedAt.orEmpty(),
    categoriesJson = Mappers.encodeOffers(categories),
    uomsJson = Mappers.encodeUoms(uoms),
    updatedAt = updatedAt.ifBlank { existing?.updatedAt.orEmpty() },
    cachedAt = existing?.cachedAt ?: System.currentTimeMillis(),
)

fun ProductEntity.toDomain() = Product(
    id = id,
    code = code,
    name = name,
    latinName = latinName,
    barcode = barcode,
    category = category,
    unit = unit,
    sellPrice = sellPrice,
    buyPrice = buyPrice,
    imageUrl = imageUrl,
    isActive = isActive,
    hasPromotion = hasPromotion,
    promoPercent = promoPercent,
    totalQty = totalQty,
    reservedQty = reservedQty,
    availableQty = availableQty,
    minStock = minStock,
    taxMu = taxMu,
    offers = Mappers.decodeOffers(categoriesJson).map { it.toDomain() },
    uoms = Mappers.decodeUoms(uomsJson).map { it.toDomain() },
    updatedAt = updatedAt,
)

fun ProductStockDto.applyTo(entity: ProductEntity): ProductEntity = entity.copy(
    totalQty = totalQty,
    reservedQty = reservedQty,
    availableQty = availableQty,
    stockUpdatedAt = stockUpdatedAt,
)

fun DiscountDto.toEntity() = DiscountEntity(
    id = id,
    title = title,
    type = type,
    description = description,
    value = value,
    valueType = valueType,
    validFrom = validFrom,
    validTo = validTo,
    productsJson = Mappers.encodeStrings(products),
    isActive = isActive,
)

fun DiscountEntity.toDomain() = Discount(
    id = id,
    title = title,
    type = type,
    description = description,
    value = value,
    valueType = valueType,
    validFrom = validFrom,
    validTo = validTo,
    productIds = Mappers.decodeStrings(productsJson),
    isActive = isActive,
)

// ---- customers ------------------------------------------------------------

fun MobileCustomerDto.toEntity(existing: CustomerEntity? = null) = CustomerEntity(
    id = id,
    code = code.ifBlank { existing?.code.orEmpty() },
    name = name.ifBlank { existing?.name.orEmpty() },
    phone = phone.ifBlank { existing?.phone.orEmpty() },
    address = address.ifBlank { existing?.address.orEmpty() },
    nationalId = nationalId.ifBlank { existing?.nationalId.orEmpty() },
    balance = balance,
    creditLimit = if (creditLimit > 0) creditLimit else existing?.creditLimit ?: 0.0,
    lat = if (lat != 0.0) lat else existing?.lat ?: 0.0,
    lng = if (lng != 0.0) lng else existing?.lng ?: 0.0,
    city = city.ifBlank { existing?.city.orEmpty() },
    province = province.ifBlank { existing?.province.orEmpty() },
    active = active,
    settlementDays = if (settlementDays > 0) settlementDays else existing?.settlementDays ?: 0,
    updatedAt = updatedAt.ifBlank { existing?.updatedAt.orEmpty() },
    isLocalOnly = false,
    pendingSyncId = null,
    cachedAt = existing?.cachedAt ?: System.currentTimeMillis(),
)

fun CustomerEntity.toDomain() = Customer(
    id = id,
    code = code,
    name = name,
    phone = phone,
    address = address,
    nationalId = nationalId,
    balance = balance,
    creditLimit = creditLimit,
    lat = lat,
    lng = lng,
    city = city,
    province = province,
    active = active,
    settlementDays = settlementDays,
    isLocalOnly = isLocalOnly,
)

fun MobileCustomerDto.toDomain() = Customer(
    id = id,
    code = code,
    name = name,
    phone = phone,
    address = address,
    nationalId = nationalId,
    balance = balance,
    creditLimit = creditLimit,
    lat = lat,
    lng = lng,
    city = city,
    province = province,
    active = active,
    settlementDays = settlementDays,
)

fun TourDto.toEntity(sortOrder: Int) = TourEntity(id = id, name = name, sortOrder = sortOrder)

fun TourDto.toDomain() = Tour(id = id, name = name, customers = customers.map { it.toDomain() })

// ---- visits ---------------------------------------------------------------

fun VisitDto.toEntity(existing: VisitEntity? = null) = VisitEntity(
    id = id,
    companyId = companyId,
    visitorId = visitorId,
    customerId = customerId,
    visitDate = visitDate.take(10),
    visitType = visitType,
    status = status,
    notes = notes,
    latitude = latitude,
    longitude = longitude,
    orderId = orderId,
    clientVisitId = clientVisitId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPending = false,
    cachedAt = existing?.cachedAt ?: System.currentTimeMillis(),
)

fun VisitDto.toDomain(customerName: String = "") = Visit(
    id = id,
    customerId = customerId,
    customerName = customerName,
    visitDate = visitDate.take(10),
    visitType = VisitType.from(visitType),
    status = VisitStatus.from(status),
    notes = notes,
    latitude = latitude,
    longitude = longitude,
    orderId = orderId,
    clientVisitId = clientVisitId,
    isPending = false,
    createdAt = createdAt,
)

fun VisitEntity.toDomain(customerName: String = "") = Visit(
    id = id,
    customerId = customerId,
    customerName = customerName,
    visitDate = visitDate,
    visitType = VisitType.from(visitType),
    status = VisitStatus.from(status),
    notes = notes,
    latitude = latitude,
    longitude = longitude,
    orderId = orderId,
    clientVisitId = clientVisitId,
    isPending = isPending,
    createdAt = createdAt,
)

// ---- orders ---------------------------------------------------------------

fun OrderItemDto.toEntity(orderId: String) = OrderItemEntity(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    batchId = batchId,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    totalLine = totalLine,
)

fun OrderItemEntity.toDomain() = OrderItem(
    id = id,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    totalLine = totalLine,
    uomId = uomId,
    uomName = uomName,
)

fun AvailabilityWarningDto.toDomain() = AvailabilityWarning(
    productId = productId,
    productName = productName,
    requested = requested,
    available = available,
    shortage = shortage,
)

fun OrderResponseDto.toEntity(existing: OrderEntity? = null) = OrderEntity(
    id = id,
    companyId = companyId,
    fiscalYearId = fiscalYearId,
    visitorId = visitorId,
    customerId = customerId,
    customerName = customerName.ifBlank { existing?.customerName.orEmpty() },
    visitorName = visitorName.ifBlank { existing?.visitorName.orEmpty() },
    number = number,
    date = date.take(10),
    subTotal = subTotal,
    discount = discount,
    taxAmount = taxAmount,
    finalAmount = finalAmount,
    status = status,
    rejectionReason = rejectionReason,
    note = note,
    softWarning = softWarning,
    awaitingSupply = awaitingSupply,
    shortageQty = shortageQty,
    availabilityWarningsJson = Mappers.encodeWarnings(availabilityWarnings),
    clientOrderId = existing?.clientOrderId.orEmpty(),
    isLocalOnly = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
    cachedAt = existing?.cachedAt ?: System.currentTimeMillis(),
)

fun OrderEntity.toDomain(items: List<OrderItemEntity> = emptyList()) = Order(
    id = id,
    number = number,
    customerId = customerId,
    customerName = customerName,
    date = date,
    subTotal = subTotal,
    discount = discount,
    taxAmount = taxAmount,
    finalAmount = finalAmount,
    status = OrderStatus.from(status),
    rawStatus = status,
    rejectionReason = rejectionReason,
    note = note,
    softWarning = softWarning,
    awaitingSupply = awaitingSupply,
    shortageQty = shortageQty,
    warnings = Mappers.decodeWarnings(availabilityWarningsJson).map { it.toDomain() },
    items = items.map { it.toDomain() },
    clientOrderId = clientOrderId,
    isLocalOnly = isLocalOnly,
    createdAt = createdAt,
)

// ---- misc -----------------------------------------------------------------

fun NotificationDto.toEntity() = MessageEntity(
    id = id,
    type = type,
    title = title,
    body = body,
    isRead = isRead,
    createdAt = createdAt,
)

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = type,
    title = title,
    body = body,
    isRead = isRead,
    createdAt = createdAt,
)

fun MessageEntity.toDomain() = Notification(
    id = id,
    type = type,
    title = title,
    body = body,
    isRead = isRead,
    createdAt = createdAt,
)

fun InvoiceItemDto.toDomain() = InvoiceLine(
    productName = productName,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    discount = discount,
    totalLine = totalLine,
)

fun MobileInvoiceDto.toEntity() = InvoiceEntity(
    id = id,
    number = number,
    date = date.take(10),
    customerId = customerId,
    customerName = customerName,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
    subTotal = subTotal,
    discount = discount,
    taxAmount = taxAmount,
    finalAmount = finalAmount,
    note = note,
    printedAt = printedAt,
    itemsJson = Mappers.encodeInvoiceItems(items),
)

fun InvoiceEntity.toDomain() = Invoice(
    id = id,
    number = number,
    date = date,
    customerId = customerId,
    customerName = customerName,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
    subTotal = subTotal,
    discount = discount,
    taxAmount = taxAmount,
    finalAmount = finalAmount,
    note = note,
    printedAt = printedAt,
    items = Mappers.decodeInvoiceItems(itemsJson).map { it.toDomain() },
)

fun ZonePermissionDto.toEntity(customerName: String = "") = ZonePermissionEntity(
    id = id,
    customerId = customerId,
    customerName = customerName,
    tourId = tourId,
    scope = scope,
    validFrom = validFrom,
    validUntil = validUntil,
    reason = reason,
    notes = notes,
    status = status,
    decisionNote = decisionNote,
    consumedAt = consumedAt,
    isUsable = isUsable,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ZonePermissionEntity.toDomain() = ZonePermission(
    id = id,
    customerId = customerId,
    customerName = customerName,
    scope = scope,
    validFrom = validFrom,
    validUntil = validUntil,
    reason = reason,
    notes = notes,
    status = status,
    decisionNote = decisionNote,
    isUsable = isUsable,
    createdAt = createdAt,
)
