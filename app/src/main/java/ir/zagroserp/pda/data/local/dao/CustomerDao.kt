package ir.zagroserp.pda.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import ir.zagroserp.pda.data.local.entity.CustomerEntity
import ir.zagroserp.pda.data.local.entity.TodayCustomerEntity
import ir.zagroserp.pda.data.local.entity.TourCustomerEntity
import ir.zagroserp.pda.data.local.entity.TourEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query(
        """
        SELECT * FROM customers
        WHERE :query = '' OR name LIKE '%' || :query || '%'
            OR phone LIKE '%' || :query || '%'
            OR nationalId LIKE '%' || :query || '%'
            OR address LIKE '%' || :query || '%'
        ORDER BY name ASC
        """,
    )
    fun pagingSource(query: String): PagingSource<Int, CustomerEntity>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE nationalId = :nationalId LIMIT 1")
    suspend fun findByNationalId(nationalId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM customers WHERE isLocalOnly = 1")
    suspend fun clearLocalOnly()

    @Query("DELETE FROM customers")
    suspend fun clear()

    @Query("UPDATE customers SET id = :serverId, isLocalOnly = 0, pendingSyncId = NULL WHERE id = :localId")
    suspend fun remapLocalId(localId: String, serverId: String)

    // ---- tours ------------------------------------------------------------

    @Query("SELECT * FROM tours ORDER BY sortOrder ASC, name ASC")
    fun observeTours(): Flow<List<TourEntity>>

    @Query("SELECT * FROM tour_customers ORDER BY tourId ASC, sortOrder ASC")
    fun observeAllTourCustomers(): Flow<List<TourCustomerEntity>>

    @Query("SELECT * FROM tours WHERE id = :id")
    fun observeTour(id: String): Flow<TourEntity?>

    @Query(
        """
        SELECT c.* FROM customers c
        INNER JOIN tour_customers tc ON tc.customerId = c.id
        WHERE tc.tourId = :tourId
        ORDER BY tc.sortOrder ASC
        """,
    )
    fun observeTourCustomers(tourId: String): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT c.* FROM customers c
        INNER JOIN tour_customers tc ON tc.customerId = c.id
        WHERE tc.tourId = :tourId
        ORDER BY tc.sortOrder ASC
        """,
    )
    suspend fun getTourCustomers(tourId: String): List<CustomerEntity>

    @Upsert
    suspend fun upsertTours(tours: List<TourEntity>)

    @Upsert
    suspend fun upsertTourCustomers(refs: List<TourCustomerEntity>)

    @Query("DELETE FROM tour_customers WHERE tourId = :tourId")
    suspend fun clearTourCustomers(tourId: String)

    // ---- today's assignment ------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodayCustomers(rows: List<TodayCustomerEntity>)

    @Query("DELETE FROM today_customers WHERE date = :date")
    suspend fun clearToday(date: String)

    /** Drops previous days so the table cannot grow without bound on a long-lived install. */
    @Query("DELETE FROM today_customers WHERE date != :date")
    suspend fun clearOtherDays(date: String)

    @Query("DELETE FROM today_customers")
    suspend fun clearAllToday()

    @Query(
        """
        SELECT c.* FROM customers c
        INNER JOIN today_customers tc ON tc.customerId = c.id
        WHERE tc.date = :date
        ORDER BY tc.sortOrder ASC
        """,
    )
    fun observeTodayCustomers(date: String): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT t.* FROM tours t
        INNER JOIN today_customers tc ON tc.tourId = t.id
        WHERE tc.date = :date
        GROUP BY t.id
        ORDER BY t.sortOrder ASC, t.name ASC
        """,
    )
    fun observeTodayTours(date: String): Flow<List<TourEntity>>

    @Query("SELECT * FROM today_customers WHERE date = :date ORDER BY sortOrder ASC")
    fun observeTodayLinks(date: String): Flow<List<TodayCustomerEntity>>

    @Query("SELECT COUNT(*) FROM today_customers WHERE date = :date")
    suspend fun todayCount(date: String): Int

    @Query("DELETE FROM tour_customers")
    suspend fun clearAllTourCustomers()

    @Query("DELETE FROM tours")
    suspend fun clearTours()
}
