package ir.zagroserp.pda.data.remote.api

import ir.zagroserp.pda.core.Api
import ir.zagroserp.pda.data.remote.dto.CreateCustomerRequest
import ir.zagroserp.pda.data.remote.dto.CreateOrderRequest
import ir.zagroserp.pda.data.remote.dto.CreateVisitRequest
import ir.zagroserp.pda.data.remote.dto.CreateZonePermissionRequest
import ir.zagroserp.pda.data.remote.dto.CustomerListResponse
import ir.zagroserp.pda.data.remote.dto.CustomerResponseDto
import ir.zagroserp.pda.data.remote.dto.DeviceDeleteRequest
import ir.zagroserp.pda.data.remote.dto.DeviceRequest
import ir.zagroserp.pda.data.remote.dto.DeviceResponseDto
import ir.zagroserp.pda.data.remote.dto.DiscountsResponse
import ir.zagroserp.pda.data.remote.dto.GoalsResponse
import ir.zagroserp.pda.data.remote.dto.HealthResponse
import ir.zagroserp.pda.data.remote.dto.InvoiceListResponse
import ir.zagroserp.pda.data.remote.dto.LocationBatchRequest
import ir.zagroserp.pda.data.remote.dto.LocationBatchResponse
import ir.zagroserp.pda.data.remote.dto.LocationRequest
import ir.zagroserp.pda.data.remote.dto.MarkReadResponse
import ir.zagroserp.pda.data.remote.dto.LoginRequest
import ir.zagroserp.pda.data.remote.dto.LoginResponse
import ir.zagroserp.pda.data.remote.dto.MessageResponse
import ir.zagroserp.pda.data.remote.dto.MobileCustomerDto
import ir.zagroserp.pda.data.remote.dto.MobileProductDto
import ir.zagroserp.pda.data.remote.dto.NotificationDto
import ir.zagroserp.pda.data.remote.dto.OkResponse
import ir.zagroserp.pda.data.remote.dto.OrderBatchRequest
import ir.zagroserp.pda.data.remote.dto.OrderBatchResponse
import ir.zagroserp.pda.data.remote.dto.OrderListResponse
import ir.zagroserp.pda.data.remote.dto.OrderResponseDto
import ir.zagroserp.pda.data.remote.dto.ProductCategoriesResponse
import ir.zagroserp.pda.data.remote.dto.ProductResponseDto
import ir.zagroserp.pda.data.remote.dto.ProductStockDto
import ir.zagroserp.pda.data.remote.dto.PrintedResponse
import ir.zagroserp.pda.data.remote.dto.ProductUomsResponse
import ir.zagroserp.pda.data.remote.dto.ProfileResponse
import ir.zagroserp.pda.data.remote.dto.RefreshRequest
import ir.zagroserp.pda.data.remote.dto.RefreshResponse
import ir.zagroserp.pda.data.remote.dto.ReportResponse
import ir.zagroserp.pda.data.remote.dto.SyncDeltaDto
import ir.zagroserp.pda.data.remote.dto.TodayResponse
import ir.zagroserp.pda.data.remote.dto.UpdateCustomerRequest
import ir.zagroserp.pda.data.remote.dto.VisitBatchRequest
import ir.zagroserp.pda.data.remote.dto.VisitBatchResponse
import ir.zagroserp.pda.data.remote.dto.VisitDto
import ir.zagroserp.pda.data.remote.dto.ZonePermissionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VisitorApi {

    // ---- Auth -------------------------------------------------------------

    @POST(Api.LOGIN)
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST(Api.REFRESH)
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST(Api.LOGOUT)
    suspend fun logout(): Response<MessageResponse>

    // ---- Bootstrap --------------------------------------------------------

    @GET(Api.HEALTH)
    suspend fun health(): Response<HealthResponse>

    @GET(Api.PROFILE)
    suspend fun profile(): Response<ProfileResponse>

    @GET(Api.TODAY)
    suspend fun today(@Query("date") date: String? = null): Response<TodayResponse>

    @GET(Api.GOALS)
    suspend fun goals(): Response<GoalsResponse>

    @GET(Api.REPORTS)
    suspend fun reports(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<ReportResponse>

    // ---- Catalog ----------------------------------------------------------

    @GET(Api.PRODUCTS)
    suspend fun products(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<List<ProductResponseDto>>

    @GET(Api.PRODUCTS_PROMO)
    suspend fun productsPromo(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<List<MobileProductDto>>

    @GET(Api.PRODUCT_LOOKUP)
    suspend fun lookupProduct(@Query("barcode") barcode: String): Response<MobileProductDto>

    @GET(Api.PRODUCT_STOCK)
    suspend fun productStock(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<List<ProductStockDto>>

    @GET("${Api.PRODUCTS}/{id}/categories")
    suspend fun productCategories(@Path("id") productId: String): Response<ProductCategoriesResponse>

    @GET("${Api.PRODUCTS}/{id}/uoms")
    suspend fun productUoms(@Path("id") productId: String): Response<ProductUomsResponse>

    @GET(Api.DISCOUNTS)
    suspend fun discounts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<DiscountsResponse>

    // ---- Customers --------------------------------------------------------

    @GET(Api.CUSTOMERS)
    suspend fun customers(
        @Query("query") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<CustomerListResponse>

    @GET("${Api.CUSTOMERS}/{id}")
    suspend fun customer(@Path("id") id: String): Response<MobileCustomerDto>

    @POST(Api.CUSTOMERS)
    suspend fun createCustomer(@Body body: CreateCustomerRequest): Response<CustomerResponseDto>

    @PUT("${Api.CUSTOMERS}/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body body: UpdateCustomerRequest,
    ): Response<MobileCustomerDto>

    // ---- Devices ----------------------------------------------------------

    @POST(Api.DEVICES)
    suspend fun registerDevice(@Body body: DeviceRequest): Response<DeviceResponseDto>

    @DELETE(Api.DEVICES)
    suspend fun deleteDevice(@Query("device_token") token: String): Response<OkResponse>

    @DELETE(Api.DEVICES)
    suspend fun deleteDeviceBody(@Body body: DeviceDeleteRequest): Response<OkResponse>

    // ---- Visits -----------------------------------------------------------

    @POST(Api.VISITS)
    suspend fun createVisit(@Body body: CreateVisitRequest): Response<VisitDto>

    @GET(Api.VISITS)
    suspend fun visits(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<List<VisitDto>>

    @POST(Api.VISITS_BATCH)
    suspend fun createVisitsBatch(@Body body: VisitBatchRequest): Response<VisitBatchResponse>

    // ---- Orders -----------------------------------------------------------

    @POST(Api.ORDERS)
    suspend fun createOrder(@Body body: CreateOrderRequest): Response<OrderResponseDto>

    @GET(Api.ORDERS)
    suspend fun orders(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<OrderListResponse>

    @GET("${Api.ORDERS}/{id}")
    suspend fun order(@Path("id") id: String): Response<OrderResponseDto>

    @POST(Api.ORDERS_BATCH)
    suspend fun createOrdersBatch(@Body body: OrderBatchRequest): Response<OrderBatchResponse>

    // ---- Invoices ---------------------------------------------------------

    @GET(Api.APPROVED_INVOICES)
    suspend fun approvedInvoices(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<InvoiceListResponse>

    @POST("${Api.APPROVED_INVOICES}/{id}/printed")
    suspend fun markInvoicePrinted(@Path("id") id: String): Response<PrintedResponse>

    // ---- Messages ---------------------------------------------------------

    @GET(Api.MESSAGES)
    suspend fun messages(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Response<List<NotificationDto>>

    @PUT("${Api.MESSAGES}/read")
    suspend fun markAllMessagesRead(): Response<MarkReadResponse>

    @PUT("${Api.MESSAGES}/{id}/read")
    suspend fun markMessageRead(@Path("id") id: String): Response<MarkReadResponse>

    // ---- Zone permissions -------------------------------------------------

    @POST(Api.ZONE_PERMISSIONS)
    suspend fun createZonePermission(@Body body: CreateZonePermissionRequest): Response<ZonePermissionDto>

    @GET(Api.ZONE_PERMISSIONS)
    suspend fun zonePermissions(): Response<List<ZonePermissionDto>>

    @PUT("${Api.ZONE_PERMISSIONS}/{id}/cancel")
    suspend fun cancelZonePermission(@Path("id") id: String): Response<OkResponse>

    // ---- Sync -------------------------------------------------------------

    @GET(Api.SYNC)
    suspend fun sync(@Query("since") since: String? = null): Response<SyncDeltaDto>

    // ---- Location ---------------------------------------------------------

    @POST(Api.LOCATION)
    suspend fun sendLocation(@Body body: LocationRequest): Response<MessageResponse>

    @POST(Api.LOCATIONS_BATCH)
    suspend fun sendLocationsBatch(@Body body: LocationBatchRequest): Response<LocationBatchResponse>
}
