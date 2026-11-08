package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.core.QueueRetryPolicy
import ir.zagroserp.pda.data.local.dao.MessageDao
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.domain.alerts.SystemAlerts
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records a message when a queued entry is dropped because the server will never accept it.
 *
 * Silently discarding the entry would mean the visitor believes a visit was registered when it
 * was not — the worst possible outcome for a field app. The message surfaces in the alerts list
 * and in the dashboard badge until it is acknowledged.
 */
@Singleton
class QueueRejectionRecorder @Inject constructor(
    private val messageDao: MessageDao,
) {

    suspend fun recordVisitRejection(
        clientVisitId: String,
        customerName: String,
        reason: String,
    ) = record(
        id = SystemAlerts.visitRejectionId(clientVisitId),
        kind = "بازدید",
        subject = customerName,
        reason = reason,
    )

    suspend fun recordOrderRejection(
        clientOrderId: String,
        customerName: String,
        reason: String,
    ) = record(
        id = SystemAlerts.orderRejectionId(clientOrderId),
        kind = "سفارش",
        subject = customerName,
        reason = reason,
    )

    private suspend fun record(id: String, kind: String, subject: String, reason: String) {
        val spec = SystemAlerts.rejected(
            id = id,
            kind = kind,
            subject = subject,
            reason = QueueRetryPolicy.dropReason(reason),
        )
        messageDao.upsertAll(
            listOf(
                MessageEntity(
                    id = spec.id,
                    type = spec.type,
                    title = spec.title,
                    body = spec.body,
                    isRead = false,
                    createdAt = Instant.now().toString(),
                ),
            ),
        )
    }
}
