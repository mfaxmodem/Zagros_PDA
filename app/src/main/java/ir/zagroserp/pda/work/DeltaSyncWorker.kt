package ir.zagroserp.pda.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.domain.repository.MessageRepository
import ir.zagroserp.pda.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first

/**
 * Pulls the delta cursor forward and gap-fills notifications that were missed while the
 * SSE stream was disconnected.
 */
@HiltWorker
class DeltaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val messageRepository: MessageRepository,
    private val sessionStore: SessionStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!sessionStore.isLoggedIn.first()) return Result.success()

        return when (val result = syncRepository.syncDelta()) {
            is AppResult.Success -> {
                messageRepository.refreshMessages()
                Result.success()
            }

            is AppResult.Failure -> Result.retry()
        }
    }
}
