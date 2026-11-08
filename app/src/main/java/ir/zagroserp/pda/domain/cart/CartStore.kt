package ir.zagroserp.pda.domain.cart

import ir.zagroserp.pda.domain.model.CartLine
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory order cart shared between the catalog, the cart screen and the customer detail
 * flow. Deliberately not persisted: an abandoned order should not silently reappear later.
 */
@Singleton
class CartStore @Inject constructor() {

    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())
    val lines: StateFlow<List<CartLine>> = _lines.asStateFlow()

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _orderDiscount = MutableStateFlow(0.0)
    val orderDiscount: StateFlow<Double> = _orderDiscount.asStateFlow()

    val subtotal: Double
        get() = _lines.value.sumOf { it.lineTotal }

    val totalQuantity: Double
        get() = _lines.value.sumOf { it.quantity }

    fun setCustomer(customer: Customer?) {
        _customer.value = customer
    }

    fun setNote(value: String) {
        _note.value = value
    }

    fun setOrderDiscount(value: Double) {
        _orderDiscount.value = value.coerceAtLeast(0.0)
    }

    /** Adds a product, merging with an existing line when the same product + packaging is present. */
    fun add(
        product: Product,
        quantity: Double,
        uom: ProductUom? = null,
        unitPrice: Double? = null,
        offerId: String? = null,
        offerName: String = "",
    ) {
        if (quantity <= 0) return
        val price = unitPrice ?: uom?.unitPrice ?: product.defaultOffer?.salePrice ?: product.sellPrice
        _lines.update { current ->
            val index = current.indexOfFirst {
                it.product.id == product.id && it.uomId == uom?.uomId && it.offerId == offerId
            }
            if (index >= 0) {
                val existing = current[index]
                current.toMutableList().apply {
                    this[index] = existing.copy(quantity = existing.quantity + quantity)
                }
            } else {
                current + CartLine(
                    product = product,
                    quantity = quantity,
                    unitPrice = price,
                    uomId = uom?.uomId,
                    uomName = uom?.unitName.orEmpty(),
                    offerId = offerId,
                    offerName = offerName,
                )
            }
        }
    }

    fun updateQuantity(line: CartLine, quantity: Double) {
        if (quantity <= 0) {
            remove(line)
            return
        }
        _lines.update { current ->
            current.map { if (it === line) it.copy(quantity = quantity) else it }
        }
    }

    fun updatePrice(line: CartLine, price: Double) {
        _lines.update { current ->
            current.map { if (it === line) it.copy(unitPrice = price.coerceAtLeast(0.0)) else it }
        }
    }

    fun updateLineDiscount(line: CartLine, discount: Double) {
        _lines.update { current ->
            current.map { if (it === line) it.copy(discount = discount.coerceAtLeast(0.0)) else it }
        }
    }

    fun remove(line: CartLine) {
        _lines.update { current -> current.filterNot { it === line } }
    }

    fun clear() {
        _lines.value = emptyList()
        _customer.value = null
        _note.value = ""
        _orderDiscount.value = 0.0
    }

    fun quantityOf(productId: String): Double =
        _lines.value.filter { it.product.id == productId }.sumOf { it.quantity }
}
