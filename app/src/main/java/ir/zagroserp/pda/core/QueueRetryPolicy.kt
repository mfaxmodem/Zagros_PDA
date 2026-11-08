package ir.zagroserp.pda.core

/**
 * Decides what happens to an offline queue entry that the server refuses.
 *
 * The server now enforces the visit geofence, so a queued visit can come back as a **permanent**
 * rejection (too far, mocked location, coarse accuracy). Before this policy existed such an entry
 * was retried on every sync for ever: it never left the queue, permanently inflated the "waiting
 * to send" counter, and burned a request each time.
 *
 * The distinction that matters is *semantic* rejection (the request reached the server and it said
 * no) versus *transport* failure (the request never got an answer). Only the former may count
 * towards giving up — a flaky connection must never discard a valid visit.
 */
object QueueRetryPolicy {

    /** Semantic rejections tolerated before the entry is dropped from the queue. */
    const val MAX_REJECTIONS = 3

    /**
     * `true` when retrying the same payload can never succeed.
     *
     * 400/403/404/409 are decisions about the content. 408 (timeout), 429 (rate limit) and 5xx are
     * transient by definition, and 401 is handled by the token authenticator.
     */
    fun isPermanent(error: AppError): Boolean = when (error) {
        is AppError.Validation,
        is AppError.Conflict,
        is AppError.Forbidden,
        is AppError.NotFound,
        -> true

        is AppError.Network,
        is AppError.Timeout,
        is AppError.Server,
        is AppError.RateLimited,
        is AppError.Unauthorized,
        is AppError.Unknown,
        -> false
    }

    /** `true` once an entry has been refused too many times to keep trying. */
    fun shouldDrop(rejectionCount: Int): Boolean = rejectionCount >= MAX_REJECTIONS

    /** Human readable reason shown in the alert raised when an entry is dropped. */
    fun dropReason(lastError: String): String =
        lastError.ifBlank { "سرور این مورد را نپذیرفت" }
}
