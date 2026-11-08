package ir.zagroserp.pda.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.domain.model.LocationPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface LocationProvider {
    fun hasPermission(): Boolean

    /** Whatever the device last recorded. May be stale – check [LocationPoint.ageMillis]. */
    suspend fun lastKnown(): LocationPoint?

    /** Actively requests a fix, falling back to the cache. */
    suspend fun current(): LocationPoint?

    /**
     * A fix trustworthy enough for the check-in radius: an actively requested high-accuracy
     * reading, or a cached one no older than [maxAgeMillis].
     *
     * Using the raw cache here was the hole in the geofence: a fix recorded somewhere else
     * earlier in the day would be compared against the customer's coordinates.
     */
    suspend fun freshFix(maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS): LocationPoint?

    companion object {
        const val DEFAULT_MAX_AGE_MILLIS = 60_000L
    }
}

@Singleton
class LocationProviderImpl @Inject constructor(
    private val client: FusedLocationProviderClient,
    @param:ApplicationContext private val context: Context,
) : LocationProvider {

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): LocationPoint? {
        if (!hasPermission()) return null
        return runCatching { client.lastLocation.await()?.toPoint() }.getOrNull()
    }

    override suspend fun current(): LocationPoint? {
        if (!hasPermission()) return null
        return requestCurrent() ?: lastKnown()
    }

    override suspend fun freshFix(maxAgeMillis: Long): LocationPoint? {
        if (!hasPermission()) return null
        val requested = requestCurrent()
        if (requested != null) return requested
        return lastKnown()?.takeIf { it.ageMillis <= maxAgeMillis }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrent(): LocationPoint? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        if (continuation.isActive) continuation.resume(location?.toPoint())
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }.onFailure {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
