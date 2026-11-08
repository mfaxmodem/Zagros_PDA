package ir.zagroserp.pda.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.zagroserp.pda.core.AppResult
import ir.zagroserp.pda.domain.model.Notification
import ir.zagroserp.pda.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val messages: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isStreaming: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
) : ViewModel() {

    private val streaming = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<MessagesUiState> = combine(
        messageRepository.observeMessages(),
        messageRepository.observeUnreadCount(),
        streaming,
        error,
    ) { messages, unread, isStreaming, currentError ->
        MessagesUiState(
            messages = messages,
            unreadCount = unread,
            isStreaming = isStreaming,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MessagesUiState(),
    )

    init {
        refresh()
        observeStream()
    }

    fun refresh() {
        viewModelScope.launch {
            error.value = null
            when (val result = messageRepository.refreshMessages()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> error.value = result.error.message
            }
        }
    }

    /**
     * The API has no SSE replay, so every (re)connection is followed by a REST gap-fill.
     */
    private fun observeStream() {
        viewModelScope.launch {
            messageRepository.stream().collect { streaming.value = true }
        }
        viewModelScope.launch {
            // Periodic gap-fill keeps the list consistent after dropped connections.
            while (true) {
                kotlinx.coroutines.delay(60_000)
                messageRepository.refreshMessages()
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { messageRepository.markRead(id) }
    }

    /**
     * Syncs the read state to the server so a second device stops showing the same messages as
     * unread. Deliberately an explicit action rather than something that happens on screen open —
     * silently clearing the badge would hide alerts the visitor has not looked at yet.
     */
    fun markAllRead() {
        viewModelScope.launch {
            when (val result = messageRepository.markAllRead()) {
                is AppResult.Success -> error.value = null
                is AppResult.Failure -> error.value = result.error.message
            }
        }
    }
}
