package ir.zagroserp.pda.location

/**
 * Decides when accumulated location fixes are pushed to the API.
 *
 * Uploading every fix individually cost one HTTPS request per 15 seconds — roughly 1,900
 * requests per 8-hour shift, each waking the radio for a ~110 byte payload. Batching onto
 * `POST /mobile/visitor/locations/batch` (the API accepts up to 500 points per call) brings that
 * down to about 480 requests for the same shift, with several points per request, while keeping
 * the panel's live map no more than a minute stale.
 *
 * The larger win is battery, not bytes: every request is a radio wake-up, and 1,900 of them per
 * shift keep the cellular radio in its high-power state for a significant part of the day.
 */
object LocationUploadPolicy {

    /** Never let the panel's map go staler than this. */
    const val MIN_INTERVAL_MS = 60_000L

    /** ...but do not sit on a large backlog either. */
    const val MIN_QUEUE_SIZE = 30

    fun shouldFlush(queueSize: Int, millisSinceLastFlush: Long): Boolean =
        queueSize >= MIN_QUEUE_SIZE || millisSinceLastFlush >= MIN_INTERVAL_MS

    /** Rough request count for a shift, used to sanity check the policy. */
    fun estimatedRequests(shiftHours: Int, fixIntervalSeconds: Int, batchIntervalSeconds: Int): Int {
        val fixes = shiftHours * 3600 / fixIntervalSeconds
        return minOf(fixes, shiftHours * 3600 / batchIntervalSeconds + 1)
    }
}
