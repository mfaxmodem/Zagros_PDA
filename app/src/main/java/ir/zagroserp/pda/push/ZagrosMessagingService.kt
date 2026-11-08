package ir.zagroserp.pda.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Receives push messages from FCM.
 *
 * Two things happen for every message: a system notification is posted straight away so the
 * visitor sees it without opening the app, and the authoritative message list is pulled from the
 * server so the in-app list and unread badge stay consistent.
 */
@AndroidEntryPoint
class ZagrosMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    @Inject
    lateinit var pushTokenManager: PushTokenManager

    /**
     * `FirebaseMessagingService` has no `goAsync()`, so the work runs in an application-lifetime
     * scope. It is deliberately idempotent: the token is persisted *before* the server call, and
     * `PushTokenManager.syncToken()` re-registers any token that is stored but not yet accepted —
     * so a process death in the middle self-heals on the next app start.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { pushTokenManager.onNewToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.toDomainNotification() ?: return
        scope.launch {
            // Post first: the visitor must see the alert even if the list refresh fails.
            runCatching { pushNotificationHelper.show(notification) }
            runCatching { messageRepository.refreshMessages() }
        }
    }
}

/**
 * Maps an FCM payload onto the app's notification model.
 *
 * The server may send either a `notification` block or a data-only payload, so both shapes are
 * accepted and the data block wins for identifiers.
 */
internal fun RemoteMessage.toDomainNotification(): Notification? {
    val payload = data
    val title = notification?.title ?: payload["title"].orEmpty()
    val body = notification?.body ?: payload["body"].orEmpty()
    if (title.isBlank() && body.isBlank()) return null

    return Notification(
        id = payload["id"]
            ?: payload["notification_id"]
            ?: notification?.tag
            ?: "push-${System.currentTimeMillis()}",
        type = payload["type"].orEmpty(),
        title = title,
        body = body,
        isRead = false,
        createdAt = payload["created_at"] ?: Instant.now().toString(),
    )
}
