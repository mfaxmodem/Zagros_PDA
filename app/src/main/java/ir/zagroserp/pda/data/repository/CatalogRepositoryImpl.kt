package ir.zagroserp.pda.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.Defaults
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.data.local.dao.ProductDao
import ir.zagroserp.pda.data.mapper.Mappers
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.domain.model.CategoryOffer
import ir.zagroserp.pda.domain.model.Discount
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import ir.zagroserp.pda.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val productDao: ProductDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : CatalogRepository {

    override fun pagingProducts(query: String, category: String): Flow<PagingData<Product>> =
        Pager(
            config = PagingConfig(
                pageSize = Defaults.PAGE_SIZE,
                initialLoadSize = Defaults.PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { productDao.pagingSource(query.trim(), category.trim()) },
        ).flow
            .map { paging -> paging.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeCategories(): Flow<List<String>> =
        productDao.observeCategories().flowOn(dispatchers.io)

    override fun pagingPromotions(query: String): Flow<PagingData<Product>> =
        Pager(
            config = PagingConfig(pageSize = Defaults.PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { productDao.pagingPromoSource(query.trim()) },
        ).flow
            .map { paging -> paging.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeProducts(): Flow<List<Product>> =
        productDao.observeAll().map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeProduct(id: String): Flow<Product?> =
        productDao.observeById(id).map { it?.toDomain() }.flowOn(dispatchers.io)

    override suspend fun getProduct(id: String): Product? = withContext(dispatchers.io) {
        productDao.getById(id)?.toDomain()
    }

    override suspend fun lookupByBarcode(barcode: String): AppResult<Product> {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Failure(ir.zagroserp.pda.core.AppError.Validation("بارکد وارد نشده است"))
        }
        // Local cache first – barcode lookups happen on the field with flaky connectivity.
        val local = withContext(dispatchers.io) { productDao.findByBarcode(trimmed) }
        if (local != null) return AppResult.Success(local.toDomain())

        return when (val result = apiCall(json) { api.lookupProduct(trimmed) }) {
            is AppResult.Success -> {
                val entity = result.data.toEntity()
                withContext(dispatchers.io) { productDao.upsert(entity) }
                AppResult.Success(entity.toDomain())
            }

            is AppResult.Failure -> result
        }
    }

    override suspend fun refreshProducts(): AppResult<Int> {
        var page = 1
        var total = 0
        while (page <= MAX_PAGES) {
            val result = apiCall(json) {
                api.products(page = page, pageSize = Defaults.MAX_PAGE_SIZE)
            }
            when (result) {
                is AppResult.Success -> {
                    val items = result.data
                    if (items.isEmpty()) return AppResult.Success(total)
                    val entities = items.map { it.toEntity() }
                    withContext(dispatchers.io) { productDao.upsertAll(entities) }
                    total += entities.size
                    if (items.size < Defaults.MAX_PAGE_SIZE) return AppResult.Success(total)
                    page++
                }

                is AppResult.Failure -> return result
            }
        }
        return AppResult.Success(total)
    }

    override suspend fun productOffers(productId: String): AppResult<List<CategoryOffer>> =
        when (val result = apiCall(json) { api.productCategories(productId) }) {
            is AppResult.Success -> AppResult.Success(result.data.categories.map { it.toDomain() })
            is AppResult.Failure -> {
                // Fall back to the cached product when offline.
                val cached = withContext(dispatchers.io) { productDao.getById(productId) }
                if (cached != null && result.error is ir.zagroserp.pda.core.AppError.Network) {
                    AppResult.Success(cached.toDomain().offers)
                } else {
                    result
                }
            }
        }

    override suspend fun productUoms(productId: String): AppResult<List<ProductUom>> =
        when (val result = apiCall(json) { api.productUoms(productId) }) {
            is AppResult.Success -> AppResult.Success(result.data.uoms.map { it.toDomain() })
            is AppResult.Failure -> {
                val cached = withContext(dispatchers.io) { productDao.getById(productId) }
                if (cached != null && result.error is ir.zagroserp.pda.core.AppError.Network) {
                    AppResult.Success(cached.toDomain().uoms)
                } else {
                    result
                }
            }
        }

    override fun observeDiscounts(): Flow<List<Discount>> =
        productDao.observeDiscounts()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun refreshDiscounts(): AppResult<Unit> =
        when (val result = apiCall(json) { api.discounts(pageSize = Defaults.MAX_PAGE_SIZE) }) {
            is AppResult.Success -> {
                withContext(dispatchers.io) {
                    productDao.upsertDiscounts(result.data.discounts.map { it.toEntity() })
                }
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    private companion object {
        const val MAX_PAGES = 20
    }
}
