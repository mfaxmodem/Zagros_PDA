package ir.zagroserp.pda.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.repository.LocationRepository
import ir.zagroserp.pda.domain.repository.OrderRepository
import ir.zagroserp.pda.domain.repository.VisitRepository

/**
 * Drains the offline queue in the background respecting the API batch limits
 * (50 orders / 100 visits / 500 locations per request).
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orderRepository: OrderRepository,
    private val visitRepository: VisitRepository,
    private val locationRepository: LocationRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var retryNeeded = false

        when (val visits = visitRepository.syncPending()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> retryNeeded = true
        }

        when (val orders = orderRepository.syncPending()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> retryNeeded = true
        }

        when (val locations = locationRepository.flushNow()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> retryNeeded = true
        }

        return if (retryNeeded) Result.retry() else Result.success()
    }
}
