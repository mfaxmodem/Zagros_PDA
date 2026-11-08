package ir.zagroserp.pda.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.zagroserp.pda.data.local.entity.DiscountEntity
import ir.zagroserp.pda.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1
          AND (:category = '' OR category = :category)
          AND (
            :query = '' OR name LIKE '%' || :query || '%'
            OR code LIKE '%' || :query || '%'
            OR barcode LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
          )
        ORDER BY name ASC
        """,
    )
    fun pagingSource(query: String, category: String): PagingSource<Int, ProductEntity>

    /**
     * The API exposes no category listing endpoint, so the filter chips are derived from the
     * products already cached from `GET /mobile/visitor/products`.
     */
    @Query(
        """
        SELECT DISTINCT category FROM products
        WHERE isActive = 1 AND category != ''
        ORDER BY category ASC
        """,
    )
    fun observeCategories(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1 AND hasPromotion = 1 AND (
            :query = '' OR name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
        """,
    )
    fun pagingPromoSource(query: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND availableQty > 0 ORDER BY name ASC")
    fun observeInStock(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("UPDATE products SET totalQty = :total, reservedQty = :reserved, availableQty = :available, stock_updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: String, total: Double, reserved: Double, available: Double, updatedAt: String)

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM products")
    suspend fun clear()

    // ---- discounts --------------------------------------------------------

    @Query("SELECT * FROM discounts WHERE isActive = 1 ORDER BY title ASC")
    fun observeDiscounts(): Flow<List<DiscountEntity>>

    @Upsert
    suspend fun upsertDiscounts(discounts: List<DiscountEntity>)

    @Query("DELETE FROM discounts")
    suspend fun clearDiscounts()
}
