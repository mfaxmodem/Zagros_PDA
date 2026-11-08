package ir.zagroserp.pda.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle distance helpers used to enforce the check-in radius.
 *
 * The API also validates tour zones server-side; this is the *client* half of that rule, so the
 * visitor gets immediate feedback instead of a rejected request after the fact.
 */
object Geofence {

    /** Radius within which a check-in is accepted, in metres. */
    const val CHECK_IN_RADIUS_METERS = 50.0

    /** Fixes worse than this are too unreliable to judge a 50 m radius. */
    const val MAX_TRUSTED_ACCURACY_METERS = 100.0

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Haversine distance in metres. */
    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
    }

    fun isWithinCheckInRadius(distanceMeters: Double): Boolean =
        distanceMeters <= CHECK_IN_RADIUS_METERS
}

/** Outcome of the client-side check-in guard. */
sealed interface CheckInVerdict {
    /** No coordinates on record for the customer – distance cannot be judged, so allow it. */
    data object CustomerWithoutCoordinates : CheckInVerdict

    data class Allowed(val distanceMeters: Double) : CheckInVerdict

    data class TooFar(val distanceMeters: Double) : CheckInVerdict

    /** The fix came from a mock-location provider. */
    data object MockLocation : CheckInVerdict

    /** No usable fix at all. */
    data object NoLocation : CheckInVerdict

    val canProceed: Boolean
        get() = this is CustomerWithoutCoordinates || this is Allowed
}

/** Persian, user-facing explanation for a rejected check-in. */
fun CheckInVerdict.rejectionMessage(): String = when (this) {
    is CheckInVerdict.TooFar -> "شما ${JalaliDate.toPersianDigits(distanceMeters.toInt().toString())} متر از " +
        "مشتری فاصله دارید. ثبت بازدید فقط تا شعاع " +
        "${JalaliDate.toPersianDigits(Geofence.CHECK_IN_RADIUS_METERS.toInt().toString())} متر امکان‌پذیر است."

    CheckInVerdict.MockLocation ->
        "موقعیت مکانی دستگاه به‌عنوان موقعیت جعلی شناسایی شد. برای ثبت بازدید، " +
            "اپلیکیشن‌های تغییر موقعیت را ببندید و GPS واقعی را روشن کنید."

    CheckInVerdict.NoLocation ->
        "موقعیت مکانی در دسترس نیست. برای ثبت بازدید، دسترسی GPS لازم است."

    is CheckInVerdict.Allowed, CheckInVerdict.CustomerWithoutCoordinates -> ""
}

object CheckInGuard {

    /**
     * Decides whether a check-in may be recorded.
     *
     * Order of checks matters: a mocked fix is rejected before the distance is even considered,
     * because a fake position would trivially satisfy the radius.
     */
    fun evaluate(
        customerLat: Double?,
        customerLng: Double?,
        fixLat: Double?,
        fixLng: Double?,
        isMock: Boolean,
        accuracyMeters: Double,
        radiusMeters: Double = Geofence.CHECK_IN_RADIUS_METERS,
    ): CheckInVerdict {
        val hasCustomerCoordinates =
            customerLat != null && customerLng != null && (customerLat != 0.0 || customerLng != 0.0)

        // Customers the API has no coordinates for must remain visitable – the visitor can send
        // the coordinates afterwards from the customer profile.
        if (!hasCustomerCoordinates) return CheckInVerdict.CustomerWithoutCoordinates

        if (isMock) return CheckInVerdict.MockLocation

        if (fixLat == null || fixLng == null) return CheckInVerdict.NoLocation

        val distance = Geofence.distanceMeters(fixLat, fixLng, customerLat!!, customerLng!!)
        return if (distance <= radiusMeters) {
            CheckInVerdict.Allowed(distance)
        } else {
            CheckInVerdict.TooFar(distance)
        }
    }
}
