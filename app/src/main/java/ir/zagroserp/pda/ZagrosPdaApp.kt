package ir.zagroserp.pda

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.di.ApplicationScope
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.domain.repository.SystemAlertsRepository
import ir.zagroserp.pda.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ZagrosPdaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var serverConfigStore: ServerConfigStore

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var systemAlertsRepository: SystemAlertsRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // The OkHttp interceptors read the token and the server address without suspending, so the
        // in-memory mirrors must be hydrated before the first request of this process.
        applicationScope.launch {
            serverConfigStore.warmUp()
            sessionStore.warmUp()
        }

        // System alerts mirror the offline queue: they appear when a queue condition becomes true
        // and disappear when it clears. Running this reactively means no caller has to remember
        // to trigger it after a sync attempt.
        applicationScope.launch {
            systemAlertsRepository.reconcile()
            syncRepository.pendingSummary
                .distinctUntilChanged()
                .collect { systemAlertsRepository.reconcile() }
        }

        syncScheduler.schedulePeriodicSync()
    }
}
