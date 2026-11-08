package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.data.local.dao.MessageDao
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.domain.alerts.SystemAlerts
import ir.zagroserp.pda.domain.repository.SyncRepository
import ir.zagroserp.pda.domain.repository.SystemAlertsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Materialises [SystemAlerts] into the local message table.
 *
 * Reconcile semantics:
 *  * a condition that became true inserts (or refreshes) its alert,
 *  * a condition that cleared removes its alert,
 *  * an alert that is still true keeps its read flag, so acknowledging a problem does not make it
 *    pop back up as unread on the next evaluation.
 */
@Singleton
class SystemAlertsRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val syncRepository: SyncRepository,
    private val dispatchers: DispatcherProvider,
) : SystemAlertsRepository {

    override suspend fun reconcile() = withContext(dispatchers.io) {
        val summary = syncRepository.pendingSummary.first()
        val desired = SystemAlerts.derive(summary)

        if (desired.isEmpty()) {
            messageDao.clearSystemAlerts()
            return@withContext
        }

        val now = Instant.now().toString()
        val entities = desired.map { spec ->
            MessageEntity(
                id = spec.id,
                type = spec.type,
                title = spec.title,
                body = spec.body,
                isRead = false,
                createdAt = now,
            )
        }

        // Drop alerts whose condition no longer holds, then upsert the rest without losing the
        // local read flag.
        messageDao.deleteSystemAlertsExcept(desired.map { it.id })
        messageDao.upsertAllPreservingRead(entities)
    }
}
