package ir.zagroserp.pda.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.zagroserp.pda.data.local.entity.PendingVisitEntity
import ir.zagroserp.pda.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE visitDate = :date ORDER BY createdAt DESC")
    fun observeByDate(date: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits ORDER BY visitDate DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE customerId = :customerId ORDER BY visitDate DESC")
    fun observeForCustomer(customerId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE visitDate = :date AND customerId = :customerId LIMIT 1")
    suspend fun findForCustomerOnDate(customerId: String, date: String): VisitEntity?

    @Query("SELECT customerId FROM visits WHERE visitDate = :date")
    fun observeVisitedCustomerIds(date: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM visits WHERE visitDate = :date")
    fun observeCountForDate(date: String): Flow<Int>

    @Upsert
    suspend fun upsertAll(visits: List<VisitEntity>)

    @Upsert
    suspend fun upsert(visit: VisitEntity)

    @Query("DELETE FROM visits")
    suspend fun clear()

    // ---- offline queue ----------------------------------------------------

    @Query("SELECT * FROM pending_visits ORDER BY createdAt ASC")
    fun observePending(): Flow<List<PendingVisitEntity>>

    @Query("SELECT * FROM pending_visits ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<PendingVisitEntity>

    @Query("SELECT COUNT(*) FROM pending_visits")
    fun observePendingCount(): Flow<Int>

    @Upsert
    suspend fun enqueue(visit: PendingVisitEntity)

    @Query("DELETE FROM pending_visits WHERE clientVisitId = :clientVisitId")
    suspend fun dequeue(clientVisitId: String)

    /** The server refused this entry on its merits – counts towards the drop budget. */
    @Query("UPDATE pending_visits SET attemptCount = attemptCount + 1, lastError = :error WHERE clientVisitId = :clientVisitId")
    suspend fun markRejected(clientVisitId: String, error: String)

    /** Transport failure – records the reason without spending the rejection budget. */
    @Query("UPDATE pending_visits SET lastError = :error WHERE clientVisitId = :clientVisitId")
    suspend fun markTransportError(clientVisitId: String, error: String)

    @Query("SELECT * FROM pending_visits WHERE attemptCount >= :threshold")
    suspend fun getExhausted(threshold: Int): List<PendingVisitEntity>

    @Query("DELETE FROM pending_visits WHERE clientVisitId IN (:ids)")
    suspend fun dequeueAll(ids: List<String>)

    @Query("DELETE FROM pending_visits")
    suspend fun clearPending()
}
