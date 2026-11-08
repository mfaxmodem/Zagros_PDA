package ir.zagroserp.pda.data.remote.sse

import ir.zagroserp.pda.BuildConfig
import ir.zagroserp.pda.core.Api
import ir.zagroserp.pda.data.prefs.ServerConfigStore
import ir.zagroserp.pda.data.prefs.SessionStore
import ir.zagroserp.pda.data.remote.dto.NotificationDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import ir.zagroserp.pda.di.BaseClient
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live notification stream (`GET /mobile/visitor/messages/stream`).
 *
 * The token travels as a query parameter because SSE clients cannot set headers.
 * The API does not support `Last-Event-ID` replay, so reconnects are followed by a REST
 * gap-fill (`GET /mobile/visitor/messages?page=1`) performed by the repository.
 */
@Singleton
class MessageStreamClient @Inject constructor(
    @param:BaseClient private val client: OkHttpClient,
    private val sessionStore: SessionStore,
    private val serverConfig: ServerConfigStore,
    private val json: Json,
) {

    fun connect(): Flow<StreamEvent> = callbackFlow {
        var eventSource: EventSource? = null
        // Signals that the current connection died and a reconnect should be attempted.
        val reconnectSignal = Channel<Unit>(Channel.CONFLATED)

        fun open() {
            val token = sessionStore.accessTokenBlocking()
            if (token.isNullOrBlank()) {
                trySend(StreamEvent.Closed("no-session"))
                close()
                return
            }

            val url = serverConfig.baseUrlBlocking().trimEnd('/') + "/" + Api.MESSAGES_STREAM +
                "?token=" + token
            val request = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build()

            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    trySend(StreamEvent.Open)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    when (type) {
                        "ping" -> trySend(StreamEvent.Ping)
                        "notification", null, "" -> {
                            val notification = runCatching {
                                json.decodeFromString(NotificationDto.serializer(), data)
                            }.getOrNull()
                            if (notification != null) {
                                trySend(StreamEvent.Notification(notification))
                            }
                        }
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    trySend(StreamEvent.Closed("closed"))
                    reconnectSignal.trySend(Unit)
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?,
                ) {
                    trySend(StreamEvent.Failure(t?.message ?: "stream-failure"))
                    reconnectSignal.trySend(Unit)
                }
            }

            eventSource?.cancel()
            eventSource = EventSources.createFactory(client)
                .newEventSource(request, listener)
        }

        open()

        // The API has no `Last-Event-ID` replay, so a dropped connection is simply re-opened
        // after a fixed backoff. Reconnects are event driven – never on a timer – so a healthy
        // stream is left untouched.
        val reconnectJob = launch {
            for (signal in reconnectSignal) {
                delay(BuildConfig.SSE_RECONNECT_DELAY_SECONDS * 1000)
                open()
            }
        }

        awaitClose {
            reconnectJob.cancel()
            eventSource?.cancel()
            reconnectSignal.close()
        }
    }
}

sealed interface StreamEvent {
    data object Open : StreamEvent
    data object Ping : StreamEvent
    data class Notification(val dto: NotificationDto) : StreamEvent
    data class Closed(val reason: String) : StreamEvent
    data class Failure(val reason: String) : StreamEvent
}
