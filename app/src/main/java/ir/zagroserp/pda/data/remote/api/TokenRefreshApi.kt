package ir.zagroserp.pda.data.remote.api

import ir.zagroserp.pda.core.Api
import ir.zagroserp.pda.data.remote.dto.RefreshRequest
import ir.zagroserp.pda.data.remote.dto.RefreshResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Minimal surface used by [ir.zagroserp.pda.data.remote.interceptor.TokenAuthenticator].
 * Served by a dedicated OkHttp client so it can never recurse into the authenticator.
 */
interface TokenRefreshApi {

    @POST(Api.REFRESH)
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>
}
