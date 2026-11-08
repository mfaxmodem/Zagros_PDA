package ir.zagroserp.pda.ui.screens.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.cart.CartStore
import ir.zagroserp.pda.domain.model.CategoryOffer
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import ir.zagroserp.pda.domain.repository.CatalogRepository
import ir.zagroserp.pda.domain.repository.CustomerRepository
import ir.zagroserp.pda.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val query: String = "",
    val selectedCategory: String = "",
    val promotionsOnly: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val selectedProduct: Product? = null,
    val offers: List<CategoryOffer> = emptyList(),
    val uoms: List<ProductUom> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val cartCustomer: Customer? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val customerRepository: CustomerRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    /**
     * When the catalog is opened from a customer (or from a route row) the customer is seeded
     * into the cart straight away, so the cart never asks for it a second time.
     */
    private val requestedCustomerId: String? =
        savedStateHandle.get<String>(Routes.ARG_CUSTOMER_ID)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val promoFlow = MutableStateFlow(false)
    private val categoryFlow = MutableStateFlow("")

    /** Category chips, taken from whatever the catalog cache currently holds. */
    val categories: StateFlow<List<String>> = catalogRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: Flow<PagingData<Product>> = combine(
        queryFlow.debounce(250).distinctUntilChanged(),
        promoFlow,
        categoryFlow,
    ) { query, promo, category -> Triple(query, promo, category) }
        .flatMapLatest { (query, promo, category) ->
            if (promo) catalogRepository.pagingPromotions(query)
            else catalogRepository.pagingProducts(query, category)
        }
        .cachedIn(viewModelScope)

    val cartCount: StateFlow<Int> = cartStore.lines
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Product id -> quantity already in the basket (packaging and loose lines summed).
     * Drives the "in basket" marker on each catalog card.
     */
    val cartQuantities: StateFlow<Map<String, Double>> = cartStore.lines
        .map { lines ->
            lines.groupBy { it.product.id }
                .mapValues { (_, group) -> group.sumOf { it.quantity } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val cartCustomer: StateFlow<Customer?> = cartStore.customer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            seedCustomer()
            refresh()
        }
    }

    private suspend fun seedCustomer() {
        val id = requestedCustomerId ?: return
        val current = cartStore.customer.value
        if (current?.id == id) return

        // A different customer means the previous basket is no longer valid.
        cartStore.clear()
        customerRepository.getCustomer(id)?.let { cartStore.setCustomer(it) }
            ?: run {
                customerRepository.refreshCustomers()
                customerRepository.getCustomer(id)?.let { cartStore.setCustomer(it) }
            }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value, error = null) }
        queryFlow.value = value
    }

    fun togglePromotions() {
        val next = !_state.value.promotionsOnly
        _state.update { it.copy(promotionsOnly = next) }
        promoFlow.value = next
    }

    /** Empty category means "all". */
    fun selectCategory(category: String) {
        val next = if (_state.value.selectedCategory == category) "" else category
        _state.update { it.copy(selectedCategory = next) }
        categoryFlow.value = next
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            catalogRepository.refreshProducts().let { result ->
                if (result is AppResult.Failure) {
                    _state.update { it.copy(error = result.error.message) }
                } else {
                    _state.update { it.copy(error = null) }
                }
            }
            catalogRepository.refreshDiscounts()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun openProduct(product: Product) {
        _state.update {
            it.copy(
                selectedProduct = product,
                offers = product.offers,
                uoms = product.uoms,
                isLoadingDetail = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val offers = catalogRepository.productOffers(product.id)
            val uoms = catalogRepository.productUoms(product.id)
            _state.update { current ->
                current.copy(
                    offers = (offers as? AppResult.Success)?.data ?: current.offers,
                    uoms = (uoms as? AppResult.Success)?.data ?: current.uoms,
                    isLoadingDetail = false,
                )
            }
        }
    }

    fun closeProduct() {
        _state.update { it.copy(selectedProduct = null, offers = emptyList(), uoms = emptyList()) }
    }

    /**
     * Adds the packaging line and the loose-piece line separately. The packaged line carries the
     * `uom_id` so the server converts it to base units; the loose line stays on the base unit.
     */
    fun addToCart(
        product: Product,
        packCount: Int,
        uom: ProductUom?,
        looseCount: Int,
        looseUnitPrice: Double,
        offer: CategoryOffer?,
    ) {
        if (uom != null && packCount > 0) {
            cartStore.add(
                product = product,
                quantity = packCount.toDouble(),
                uom = uom,
                unitPrice = uom.unitPrice,
            )
        }
        if (looseCount > 0) {
            cartStore.add(
                product = product,
                quantity = looseCount.toDouble(),
                uom = null,
                unitPrice = looseUnitPrice,
                offerId = offer?.id,
                offerName = offer?.name.orEmpty(),
            )
        }
        _state.update {
            it.copy(selectedProduct = null, info = "${product.name} به سبد افزوده شد")
        }
    }

    fun selectCustomer(customer: Customer) {
        cartStore.setCustomer(customer)
    }

    fun clearInfo() = _state.update { it.copy(info = null, error = null) }
}
