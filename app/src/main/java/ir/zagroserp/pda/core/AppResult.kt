package ir.zagroserp.pda.core

/**
 * Domain level failure model. Every failure carries a Persian, user-displayable message.
 */
/**
 * Normalised view of the polymorphic error envelope (API section 1.1), decoupled from the
 * transport layer so the core package does not depend on DTOs.
 */
data class ApiErrorEnvelope(
    val error: String = "",
    val requiresCompany: Boolean = false,
    val companies: List<Pair<String, String>> = emptyList(),
    val requires2fa: Boolean = false,
    val duplicate: Boolean = false,
    val existingCustomerId: String = "",
    val retryAfter: Long? = null,
    /** Geofence rejections carry the measured distance and the company's configured radius. */
    val distanceMeters: Double? = null,
    val allowedMeters: Double? = null,
)

sealed class AppError(
    override val message: String,
    cause: Throwable? = null,
    /** Parsed error envelope, when the server returned one. */
    open val envelope: ApiErrorEnvelope? = null,
) : Exception(message, cause) {

    /** Device is offline or the host is unreachable. */
    class Network(message: String = "اتصال اینترنت برقرار نیست", cause: Throwable? = null) :
        AppError(message, cause)

    /** Request timed out. */
    class Timeout(message: String = "زمان پاسخ سرور به پایان رسید", cause: Throwable? = null) :
        AppError(message, cause)

    /** 401 – no valid session, or 2FA required. */
    class Unauthorized(
        message: String = "نشست شما منقضی شده است، دوباره وارد شوید",
        override val envelope: ApiErrorEnvelope? = null,
    ) : AppError(message, envelope = envelope)

    /** 403 – account/company disabled. */
    class Forbidden(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** 404 – resource outside the user's company scope. */
    class NotFound(message: String, cause: Throwable? = null) : AppError(message, cause)

    /** 409 – state conflict (invalid transition, duplicate customer, ...). */
    class Conflict(
        message: String,
        val duplicate: Boolean = false,
        val existingCustomerId: String? = null,
        override val envelope: ApiErrorEnvelope? = null,
    ) : AppError(message, envelope = envelope)

    /** 429 – rate limited / account locked. */
    class RateLimited(
        message: String,
        val retryAfterSeconds: Long? = null,
        override val envelope: ApiErrorEnvelope? = null,
    ) : AppError(message, envelope = envelope)

    /** 400 – validation. */
    class Validation(
        message: String,
        cause: Throwable? = null,
        override val envelope: ApiErrorEnvelope? = null,
    ) : AppError(message, cause, envelope = envelope)

    /** Server side 5xx. */
    class Server(message: String = "خطای سرور، کمی بعد تلاش کنید", cause: Throwable? = null) :
        AppError(message, cause)

    /** Anything else. */
    class Unknown(message: String, cause: Throwable? = null) : AppError(message, cause)
}

/** Simple result wrapper used across the domain layer. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
