package ir.zagroserp.pda.data.remote

import ir.zagroserp.pda.core.ServerAddress
import ir.zagroserp.pda.data.remote.dto.HealthResponse
import ir.zagroserp.pda.data.remote.interceptor.BaseUrlInterceptor
import ir.zagroserp.pda.di.RefreshClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ProbeResult {
    data class Success(
        val serverVersion: String,
        val minAppVersion: String,
        val serverTime: String,
        val baseUrl: String,
    ) : ProbeResult

    data class InvalidUrl(val message: String) : ProbeResult
    data class HttpError(val code: Int) : ProbeResult
    data class InvalidResponse(val message: String) : ProbeResult
    data class Unreachable(val message: String) : ProbeResult
}

/**
 * "تست ارتباط" — calls the public `GET /mobile/visitor/health` on an arbitrary address so the
 * visitor can confirm the server before being allowed to log in. Uses a bare client with no auth
 * interceptor and no base-URL rewriting.
 */
@Singleton
class ServerProbe @Inject constructor(
    @param:RefreshClient private val client: OkHttpClient,
    private val json: Json,
) {

    suspend fun probe(rawAddress: String): ProbeResult = withContext(Dispatchers.IO) {
        val normalized = ServerAddress.normalize(rawAddress)
            ?: return@withContext ProbeResult.InvalidUrl(
                "آدرس نامعتبر است. نمونه: https://dalbara.ir یا http://192.168.1.50:8080",
            )

        val request = Request.Builder()
            .url(normalized + "mobile/visitor/health")
            .header("Accept", "application/json")
            // Keep the address the visitor typed instead of the saved one.
            .header(BaseUrlInterceptor.HEADER_SKIP, "1")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use ProbeResult.HttpError(response.code)
                }
                val body = response.body?.string().orEmpty()
                val health = runCatching {
                    json.decodeFromString(HealthResponse.serializer(), body)
                }.getOrNull()

                if (health == null || health.status.isBlank()) {
                    ProbeResult.InvalidResponse(
                        "پاسخ سرور قابل خواندن نبود؛ مطمئن شوید آدرس به سرویس زاگرس اشاره می‌کند",
                    )
                } else {
                    ProbeResult.Success(
                        serverVersion = health.serverVersion,
                        minAppVersion = health.minAppVersion,
                        serverTime = health.serverTime,
                        baseUrl = normalized,
                    )
                }
            }
        } catch (timeout: SocketTimeoutException) {
            ProbeResult.Unreachable("زمان پاسخ سرور به پایان رسید؛ آدرس و پورت را بررسی کنید")
        } catch (dns: UnknownHostException) {
            ProbeResult.Unreachable("آدرس سرور پیدا نشد؛ املای دامنه یا اتصال شبکه را بررسی کنید")
        } catch (io: IOException) {
            ProbeResult.Unreachable("ارتباط برقرار نشد: ${io.message ?: "خطای شبکه"}")
        } catch (t: Throwable) {
            ProbeResult.Unreachable(t.message ?: "خطای نامشخص در تست ارتباط")
        }
    }
}
