package ir.zagroserp.pda.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.zagroserp.pda.core.WorkNames
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place that owns the background sync topology.
 *  * [OfflineSyncWorker] – pushes the offline queue (orders / visits / locations).
 *  * [DeltaSyncWorker]   – pulls `GET /mobile/visitor/sync` deltas.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        val queueWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val deltaWork = PeriodicWorkRequestBuilder<DeltaSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkNames.UNIQUE_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            queueWork,
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkNames.UNIQUE_DELTA,
            ExistingPeriodicWorkPolicy.KEEP,
            deltaWork,
        )
    }

    /** Fire-and-forget push of the offline queue, e.g. right after a check-in. */
    fun requestImmediateSync() {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${WorkNames.UNIQUE_SYNC}_now",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun requestDeltaSync() {
        val request = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${WorkNames.UNIQUE_DELTA}_now",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
