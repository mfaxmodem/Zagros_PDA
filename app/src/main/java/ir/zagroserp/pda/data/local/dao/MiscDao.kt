package ir.zagroserp.pda.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import ir.zagroserp.pda.data.local.entity.InvoiceEntity
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.data.local.entity.PendingLocationEntity
import ir.zagroserp.pda.data.local.entity.SyncStateEntity
import ir.zagroserp.pda.data.local.entity.ZonePermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND id LIKE 'local-%'")
    fun observeUnreadAlertCount(): Flow<Int>

    @Query("DELETE FROM messages WHERE id LIKE 'local-alert-%'")
    suspend fun clearSystemAlerts()

    @Query("DELETE FROM messages WHERE id LIKE 'local-alert-%' AND id NOT IN (:keepIds)")
    suspend fun deleteSystemAlertsExcept(keepIds: List<String>)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    /**
     * The API exposes no "mark as read" endpoint, so the read flag is device-local. Any refresh
     * from the server must therefore not clobber it – see [upsertAll].
     */
    @Query("SELECT id FROM messages WHERE isRead = 1")
    suspend fun readIds(): List<String>

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY date DESC, number DESC")
    fun pagingSource(): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoices ORDER BY date DESC, number DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM invoices WHERE printedAt = ''")
    fun observeUnprintedCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(invoices: List<InvoiceEntity>)

    @Query("UPDATE invoices SET printedAt = :printedAt WHERE id = :id")
    suspend fun markPrinted(id: String, printedAt: String)

    @Query("DELETE FROM invoices")
    suspend fun clear()
}

@Dao
interface ZonePermissionDao {

    @Query("SELECT * FROM zone_permissions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ZonePermissionEntity>>

    @Query("SELECT COUNT(*) FROM zone_permissions WHERE isUsable = 1")
    fun observeUsableCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(permissions: List<ZonePermissionEntity>)

    @Upsert
    suspend fun upsert(permission: ZonePermissionEntity)

    @Query("DELETE FROM zone_permissions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM zone_permissions")
    suspend fun clear()
}

@Dao
interface LocationQueueDao {

    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<PendingLocationEntity>

    @Query("SELECT COUNT(*) FROM pending_locations")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun pendingCount(): Int

    @Upsert
    suspend fun enqueue(location: PendingLocationEntity)

    /** Returns the new row id so a successful immediate upload can remove exactly that row. */
    @Insert
    suspend fun insert(location: PendingLocationEntity): Long

    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun count(): Int

    @Query("UPDATE pending_locations SET attemptCount = attemptCount + 1 WHERE id IN (:ids)")
    suspend fun markAttempt(ids: List<Long>)

    @Query("DELETE FROM pending_locations WHERE attemptCount > 10")
    suspend fun prunePoisoned()

    @Query("DELETE FROM pending_locations")
    suspend fun clear()
}

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = 0")
    fun observe(): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncStateEntity?

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("UPDATE sync_state SET serverTime = :serverTime, lastSyncAt = :at WHERE id = 0")
    suspend fun updateCursor(serverTime: String, at: Long)
}
