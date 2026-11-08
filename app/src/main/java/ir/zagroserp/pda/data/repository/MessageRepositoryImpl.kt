package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.core.Defaults
import ir.zagroserp.pda.core.DispatcherProvider
import ir.zagroserp.pda.data.local.dao.MessageDao
import ir.zagroserp.pda.data.local.entity.MessageEntity
import ir.zagroserp.pda.data.mapper.toDomain
import ir.zagroserp.pda.data.mapper.toEntity
import ir.zagroserp.pda.data.remote.api.VisitorApi
import ir.zagroserp.pda.data.remote.apiCall
import ir.zagroserp.pda.data.remote.sse.MessageStreamClient
import ir.zagroserp.pda.data.remote.sse.StreamEvent
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: VisitorApi,
    private val messageDao: MessageDao,
    private val streamClient: MessageStreamClient,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
) : MessageRepository {

    override fun observeMessages(): Flow<List<Notification>> =
        messageDao.observeAll().map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeUnreadCount(): Flow<Int> = messageDao.observeUnreadCount()
        .flowOn(dispatchers.io)

    override fun observeUnreadAlertCount(): Flow<Int> = messageDao.observeUnreadAlertCount()
        .flowOn(dispatchers.io)

    override suspend fun refreshMessages(): AppResult<Unit> =
        when (val result = apiCall(json) { api.messages(pageSize = Defaults.MAX_PAGE_SIZE) }) {
            is AppResult.Success -> {
                withContext(dispatchers.io) {
                    messageDao.upsertAllPreservingRead(result.data.map { it.toEntity() })
                }
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    /**
     * Local first so the badge updates instantly, then best effort on the server. A failure is
     * ignored: the read flag is already correct on this device, and the next successful call or
     * `markAllRead` reconciles the other devices.
     */
    override suspend fun markRead(id: String) {
        withContext(dispatchers.io) { messageDao.markRead(id) }
        runCatching { api.markMessageRead(id) }
    }

    override suspend fun markAllRead(): AppResult<Unit> {
        withContext(dispatchers.io) { messageDao.markAllRead() }
        return when (val result = apiCall(json) { api.markAllMessagesRead() }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    /**
     * SSE notifications are persisted immediately so the UI reacts without a round-trip.
     * On every (re)connect the repository asks the caller to gap-fill via [refreshMessages].
     */
    override fun stream(): Flow<Notification> = streamClient.connect()
        .filter { it is StreamEvent.Notification }
        .map { event ->
            val dto = (event as StreamEvent.Notification).dto
            messageDao.upsertAllPreservingRead(listOf(dto.toEntity()))
            dto.toDomain()
        }
        .flowOn(dispatchers.io)
}

/**
 * Upserts server messages without losing the locally recorded read flag.
 *
 * `GET /mobile/visitor/messages` always reports `is_read` as stored server-side and there is no
 * mark-as-read endpoint, so a plain upsert would make every read message unread again on the next
 * refresh — which is exactly what made the dashboard badge come back.
 */
internal suspend fun MessageDao.upsertAllPreservingRead(messages: List<MessageEntity>) {
    if (messages.isEmpty()) return
    val alreadyRead = readIds().toSet()
    upsertAll(
        messages.map { entity ->
            if (!entity.isRead && entity.id in alreadyRead) entity.copy(isRead = true) else entity
        },
    )
}
