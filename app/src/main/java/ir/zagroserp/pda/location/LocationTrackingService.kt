package ir.zagroserp.pda.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import ir.zagroserp.pda.MainActivity
import ir.zagroserp.pda.R
import ir.zagroserp.pda.core.Notifications
import ir.zagroserp.pda.domain.model.LocationPoint
import ir.zagroserp.pda.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Foreground service that records the visitor's live position.
 *
 * Battery and data are treated as first-class constraints:
 *  * the tracking profile is **adaptive** – high accuracy while the visitor is actually looking
 *    at the app, a coarse, slow profile once it is backgrounded;
 *  * fixes are written to the offline queue and uploaded in **batches** (see
 *    [LocationUploadPolicy]) instead of one request per fix.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isForegroundProfile = true

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                scope.launch { locationRepository.record(location.toPoint()) }
            }
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = applyProfile(foreground = true)
        override fun onStop(owner: LifecycleOwner) = applyProfile(foreground = false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        runCatching {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        applyProfile(foreground = true)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        runCatching { ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver) }
        runCatching { fusedLocationClient.removeLocationUpdates(callback) }
        scope.cancel()
        super.onDestroy()
    }

    private fun applyProfile(foreground: Boolean) {
        if (!hasLocationPermission()) return
        isForegroundProfile = foreground
        val profile = if (foreground) FOREGROUND_PROFILE else BACKGROUND_PROFILE

        runCatching {
            fusedLocationClient.removeLocationUpdates(callback)
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(profile.priority, profile.intervalMs)
                    .setMinUpdateIntervalMillis(profile.minUpdateIntervalMs)
                    .setMinUpdateDistanceMeters(profile.minDistanceMeters)
                    .setMaxUpdateDelayMillis(profile.maxUpdateDelayMs)
                    .setWaitForAccurateLocation(false)
                    .build(),
                callback,
                mainLooper,
            )
        }
        updateNotification(foreground)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startForegroundCompat() {
        val notification = buildNotification(isForegroundProfile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.LOCATION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(Notifications.LOCATION_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(foreground: Boolean) {
        runCatching {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(Notifications.LOCATION_NOTIFICATION_ID, buildNotification(foreground))
        }
    }

    private fun buildNotification(foreground: Boolean) =
        NotificationCompat.Builder(this, Notifications.LOCATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.location_service_title))
            .setContentText(
                getString(
                    if (foreground) {
                        R.string.location_service_text
                    } else {
                        R.string.location_service_text_background
                    },
                ),
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(Notifications.LOCATION_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    Notifications.LOCATION_CHANNEL_ID,
                    getString(R.string.location_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.location_channel_description)
                    setShowBadge(false)
                },
            )
        }
    }

    private data class TrackingProfile(
        val priority: Int,
        val intervalMs: Long,
        val minUpdateIntervalMs: Long,
        val minDistanceMeters: Float,
        val maxUpdateDelayMs: Long,
    )

    companion object {
        /** App visible: the visitor may be checking in, so accuracy matters. */
        private val FOREGROUND_PROFILE = TrackingProfile(
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs = 20_000L,
            minUpdateIntervalMs = 10_000L,
            minDistanceMeters = 10f,
            maxUpdateDelayMs = 40_000L,
        )

        /**
         * App backgrounded (in the car, walking between visits): a coarse fix every two minutes is
         * plenty for a route history and costs a fraction of the battery. `BALANCED_POWER_ACCURACY`
         * lets the OS use wifi/cell positioning instead of keeping the GPS chip hot.
         */
        private val BACKGROUND_PROFILE = TrackingProfile(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs = 120_000L,
            minUpdateIntervalMs = 60_000L,
            minDistanceMeters = 30f,
            maxUpdateDelayMs = 300_000L,
        )

        /** Read by the settings screen to show whether tracking is actually live. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}

@Suppress("DEPRECATION")
fun Location.toPoint(): LocationPoint = LocationPoint(
    latitude = latitude,
    longitude = longitude,
    accuracy = if (hasAccuracy()) accuracy.toDouble() else 0.0,
    recordedAt = Instant.ofEpochMilli(time).toString(),
    // `isMock` is API 31+; `isFromMockProvider` covers the older devices we still support.
    isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        isFromMockProvider
    },
)
