package ir.zagroserp.pda.data.remote

import ir.zagroserp.pda.core.ApiErrorEnvelope
import ir.zagroserp.pda.core.AppError
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.data.remote.dto.ApiErrorBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Executes a Retrofit call and normalises every outcome into [AppResult].
 * Handles the polymorphic error envelope, network failures and 401/403/404/409/429 semantics.
 */
suspend fun <T> apiCall(
    json: Json,
    block: suspend () -> Response<T>,
): AppResult<T> = withContext(Dispatchers.IO) {
    try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null) {
                AppResult.Failure(AppError.Server("پاسخ خالی از سرور دریافت شد"))
            } else {
                AppResult.Success(body)
            }
        } else {
            AppResult.Failure(response.toAppError(json))
        }
    } catch (ce: CancellationException) {
        throw ce
    } catch (io: IOException) {
        AppResult.Failure(io.toAppError())
    } catch (http: HttpException) {
        AppResult.Failure(AppError.Server(http.message(), http))
    } catch (t: Throwable) {
        AppResult.Failure(AppError.Unknown(t.message ?: "خطای نامشخص رخ داد", t))
    }
}

/** Same as [apiCall] but tolerates an empty/absent body for 2xx responses. */
suspend fun apiCallUnit(
    json: Json,
    block: suspend () -> Response<*>,
): AppResult<Unit> = withContext(Dispatchers.IO) {
    try {
        val response = block()
        if (response.isSuccessful) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(response.toAppError(json))
        }
    } catch (ce: CancellationException) {
        throw ce
    } catch (io: IOException) {
        AppResult.Failure(io.toAppError())
    } catch (t: Throwable) {
        AppResult.Failure(AppError.Unknown(t.message ?: "خطای نامشخص رخ داد", t))
    }
}

fun <T> Response<T>.toAppError(json: Json): AppError {
    val raw = runCatching { errorBody()?.string() }.getOrNull()
    val parsed = raw?.takeIf { it.isNotBlank() }?.let {
        runCatching { json.decodeFromString(ApiErrorBody.serializer(), it) }.getOrNull()
    }

    val envelope = parsed?.let {
        ApiErrorEnvelope(
            error = it.error,
            requiresCompany = it.requiresCompany,
            companies = it.companies.map { company -> company.id to company.name },
            requires2fa = it.requires2fa,
            duplicate = it.duplicate,
            existingCustomerId = it.existingCustomerId,
            retryAfter = it.retryAfter,
            distanceMeters = it.distanceMeters,
            allowedMeters = it.allowedMeters,
        )
    }

    val message = parsed?.displayMessage?.takeIf { it.isNotBlank() }
    return when (code()) {
        400 -> AppError.Validation(message ?: "اطلاعات ارسالی نامعتبر است", envelope = envelope)
        401 -> AppError.Unauthorized(message ?: "نام کاربری یا رمز عبور اشتباه است", envelope)
        403 -> AppError.Forbidden(message ?: "دسترسی شما به این بخش مجاز نیست")
        404 -> AppError.NotFound(message ?: "موردی یافت نشد")
        409 -> AppError.Conflict(
            message = message ?: "تضاد در وضعیت رکورد",
            duplicate = parsed?.duplicate == true,
            existingCustomerId = parsed?.existingCustomerId?.takeIf { it.isNotBlank() },
            envelope = envelope,
        )
        422 -> AppError.Validation(message ?: "اطلاعات ارسالی نامعتبر است", envelope = envelope)
        429 -> AppError.RateLimited(
            message = message ?: "تعداد درخواست‌ها زیاد است، کمی بعد تلاش کنید",
            retryAfterSeconds = parsed?.retryAfter,
            envelope = envelope,
        )
        in 500..599 -> AppError.Server(message ?: "خطای سرور، کمی بعد تلاش کنید")
        else -> AppError.Unknown(message ?: "خطای نامشخص (کد ${code()})")
    }
}

fun IOException.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout(cause = this)
    is UnknownHostException -> AppError.Network(cause = this)
    else -> AppError.Network(
        message = if (message?.contains("timeout", ignoreCase = true) == true) {
            "زمان پاسخ سرور به پایان رسید"
        } else {
            "ارتباط با سرور برقرار نشد"
        },
        cause = this,
    )
}
