package ir.zagroserp.pda.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import ir.zagroserp.pda.data.local.entity.OrderEntity
import ir.zagroserp.pda.data.local.entity.OrderItemEntity
import ir.zagroserp.pda.data.local.entity.OrderWithCustomerName
import ir.zagroserp.pda.data.local.entity.PendingOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query(
        """
        SELECT o.*, c.name AS resolvedCustomerName
        FROM orders o
        LEFT JOIN customers c ON c.id = o.customerId
        WHERE (:status = '' OR o.status = :status)
        ORDER BY o.date DESC, o.createdAt DESC
        """,
    )
    fun pagingSource(status: String): PagingSource<Int, OrderWithCustomerName>

    @Query(
        """
        SELECT o.*, c.name AS resolvedCustomerName
        FROM orders o
        LEFT JOIN customers c ON c.id = o.customerId
        ORDER BY o.date DESC, o.createdAt DESC LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int = 20): Flow<List<OrderWithCustomerName>>

    @Query(
        """
        SELECT o.*, c.name AS resolvedCustomerName
        FROM orders o
        LEFT JOIN customers c ON c.id = o.customerId
        WHERE o.id = :id
        """,
    )
    fun observeById(id: String): Flow<OrderWithCustomerName?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE clientOrderId = :clientOrderId LIMIT 1")
    suspend fun findByClientOrderId(clientOrderId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY rowId ASC")
    fun observeItems(orderId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY rowId ASC")
    suspend fun getItems(orderId: String): List<OrderItemEntity>

    @Upsert
    suspend fun upsertOrder(order: OrderEntity)

    @Upsert
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItems(orderId: String)

    @Transaction
    suspend fun saveOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        deleteItems(order.id)
        if (items.isNotEmpty()) insertItems(items)
    }

    @Query("DELETE FROM orders")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(finalAmount), 0) FROM orders WHERE date = :date AND status != 'REJECTED' AND status != 'CANCELLED'")
    fun observeSalesForDate(date: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM orders WHERE date = :date")
    fun observeOrderCountForDate(date: String): Flow<Int>

    // ---- offline queue ----------------------------------------------------

    @Query("SELECT * FROM pending_orders ORDER BY createdAt ASC")
    fun observePending(): Flow<List<PendingOrderEntity>>

    @Query("SELECT * FROM pending_orders ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<PendingOrderEntity>

    @Query("SELECT COUNT(*) FROM pending_orders")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_orders WHERE status = 'ERROR'")
    fun observeErrorCount(): Flow<Int>

    @Upsert
    suspend fun enqueue(order: PendingOrderEntity)

    @Query("DELETE FROM pending_orders WHERE clientOrderId = :clientOrderId")
    suspend fun dequeue(clientOrderId: String)

    /** The server refused this entry on its merits – counts towards the drop budget. */
    @Query(
        """
        UPDATE pending_orders
        SET attemptCount = attemptCount + 1, lastError = :error, status = 'ERROR'
        WHERE clientOrderId = :clientOrderId
        """,
    )
    suspend fun markRejected(clientOrderId: String, error: String)

    /** Transport failure – records the reason without spending the rejection budget. */
    @Query("UPDATE pending_orders SET lastError = :error WHERE clientOrderId = :clientOrderId")
    suspend fun markTransportError(clientOrderId: String, error: String)

    @Query("SELECT * FROM pending_orders WHERE attemptCount >= :threshold")
    suspend fun getExhausted(threshold: Int): List<PendingOrderEntity>

    @Query("DELETE FROM pending_orders WHERE clientOrderId IN (:ids)")
    suspend fun dequeueAll(ids: List<String>)

    @Query("UPDATE pending_orders SET status = 'BLOCKED', serverOrderId = :serverId, serverNumber = :number WHERE clientOrderId = :clientOrderId")
    suspend fun markBlocked(clientOrderId: String, serverId: String, number: String)

    @Query("DELETE FROM pending_orders")
    suspend fun clearPending()
}
