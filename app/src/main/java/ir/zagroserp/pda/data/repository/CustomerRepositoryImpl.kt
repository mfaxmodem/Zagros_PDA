package ir.zagroserp.pda.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.Defaults
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.data.local.dao.CustomerDao
import ir.zagroserp.pda.data.local.entity.CustomerEntity
import ir.zagroserp.pda.data.mapper.Mappers
import ir.zagroserp.pda.data.mapper.applyTo
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.dto.CreateCustomerRequest
import ir.zagroserp.pda.data.remote.dto.UpdateCustomerRequest
import ir.zagroserp.pda.domain.model.Customer
import ir.zagroserp.pda.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val customerDao: CustomerDao,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : CustomerRepository {

    override fun pagingCustomers(query: String): Flow<PagingData<Customer>> =
        Pager(
            config = PagingConfig(pageSize = Defaults.PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { customerDao.pagingSource(query.trim()) },
        ).flow
            .map { paging -> paging.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeCustomers(): Flow<List<Customer>> =
        customerDao.observeAll().map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeCustomer(id: String): Flow<Customer?> =
        customerDao.observeById(id).map { it?.toDomain() }.flowOn(dispatchers.io)

    override suspend fun getCustomer(id: String): Customer? = withContext(dispatchers.io) {
        customerDao.getById(id)?.toDomain()
    }

    override suspend fun findByNationalId(nationalId: String): Customer? =
        withContext(dispatchers.io) {
            val trimmed = nationalId.trim()
            if (trimmed.isEmpty()) null else customerDao.findByNationalId(trimmed)?.toDomain()
        }

    override suspend fun refreshCustomers(query: String?): AppResult<Int> {
        var page = 1
        var total = 0
        while (page <= MAX_PAGES) {
            val result = apiCall(json) {
                api.customers(query = query, page = page, pageSize = Defaults.MAX_PAGE_SIZE)
            }
            when (result) {
                is AppResult.Success -> {
                    val body = result.data
                    if (body.customers.isEmpty()) return AppResult.Success(total)
                    val entities = body.customers.map { dto ->
                        dto.toEntity(withContext(dispatchers.io) { customerDao.getById(dto.id) })
                    }
                    withContext(dispatchers.io) { customerDao.upsertAll(entities) }
                    total += entities.size
                    if (!body.hasMore || body.customers.size < Defaults.MAX_PAGE_SIZE) {
                        return AppResult.Success(total)
                    }
                    page++
                }

                is AppResult.Failure -> return result
            }
        }
        return AppResult.Success(total)
    }

    override suspend fun createCustomer(
        name: String,
        nationalId: String,
        phone: String?,
        address: String?,
        creditLimit: Double?,
        settlementDays: Int?,
        latitude: Double?,
        longitude: Double?,
    ): AppResult<Customer> {
        val request = CreateCustomerRequest(
            name = name.trim(),
            nationalId = nationalId.trim(),
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            address = address?.trim()?.takeIf { it.isNotEmpty() },
            creditLimit = creditLimit,
            settlementDays = settlementDays,
            latitude = latitude,
            longitude = longitude,
        )
        return when (val result = apiCall(json) { api.createCustomer(request) }) {
            is AppResult.Success -> {
                val entity = CustomerEntity(
                    id = result.data.id,
                    code = result.data.code,
                    name = result.data.name,
                    nationalId = result.data.nationalId,
                    phone = result.data.phone,
                    address = result.data.address,
                    creditLimit = result.data.creditLimit,
                    settlementDays = result.data.settlementDays,
                    lat = latitude ?: 0.0,
                    lng = longitude ?: 0.0,
                    active = result.data.active,
                )
                withContext(dispatchers.io) { customerDao.upsert(entity) }
                AppResult.Success(entity.toDomain())
            }

            is AppResult.Failure -> result
        }
    }

    /**
     * `PUT /mobile/visitor/customers/:id` is a **full replace** on the server: any field left out
     * of the body is cleared. Verified against the live API — sending only `latitude`/`longitude`
     * blanked the customer's phone and address.
     *
     * So every editable field is always sent, falling back to what we already know about the
     * customer. A partial update can therefore never destroy data.
     */
    override suspend fun updateCustomer(
        id: String,
        name: String?,
        phone: String?,
        address: String?,
        city: String?,
        province: String?,
        latitude: Double?,
        longitude: Double?,
    ): AppResult<Customer> {
        // Base the request on the freshest record we can get hold of.
        val base = withContext(dispatchers.io) { customerDao.getById(id) }
            ?: when (val fetched = apiCall(json) { api.customer(id) }) {
                is AppResult.Success -> fetched.data.toEntity()
                is AppResult.Failure -> return fetched
            }

        val request = UpdateCustomerRequest(
            name = (name ?: base.name).trim().takeIf { it.isNotEmpty() },
            phone = (phone ?: base.phone).trim(),
            address = (address ?: base.address).trim(),
            city = (city ?: base.city).trim(),
            province = (province ?: base.province).trim(),
            latitude = latitude ?: base.lat.takeIf { it != 0.0 },
            longitude = longitude ?: base.lng.takeIf { it != 0.0 },
        )
        return when (val result = apiCall(json) { api.updateCustomer(id, request) }) {
            is AppResult.Success -> {
                val existing = withContext(dispatchers.io) { customerDao.getById(id) }
                val entity = result.data.toEntity(existing)
                withContext(dispatchers.io) { customerDao.upsert(entity) }
                AppResult.Success(entity.toDomain())
            }

            is AppResult.Failure -> result
        }
    }

    private companion object {
        const val MAX_PAGES = 20
    }
}
