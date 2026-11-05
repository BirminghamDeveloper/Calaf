package com.calaf.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    /** Null until the match is found, and again once it is no longer active. */
    val conversation: Match? = null,
    val messages: List<ChatMessage> = emptyList(),
    val readReceipts: Boolean = false,
    val loadingHistory: Boolean = false,
    val hasEarlierMessages: Boolean = false,
    val action: ActionState = ActionState(),
)

/** One shot events. The screen reacts once and the ViewModel does not remember them. */
sealed interface ChatEvent {
    /** The match ended (unmatched, blocked, suspended or missing), so the screen should leave the chat. */
    data object Closed : ChatEvent
}

@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted("uid") private val uid: String,
    @Assisted("matchId") private val matchId: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("uid") uid: String, @Assisted("matchId") matchId: String): ChatViewModel
    }

    private val mutable = MutableStateFlow(ChatUiState())
    val state = mutable.asStateFlow()
    private val eventChannel = Channel<ChatEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var observers = listOf<Job>()
    private var chat: Job? = null
    private var history: Job? = null
    private var receipt: Job? = null
    private var closed = false
    private var lastReadMessage: String? = null
    private var pendingMessage: Triple<String, String, String>? = null
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observers.forEach(Job::cancel)
        actions.clearError()
        observers = listOf(
            viewModelScope.launch {
                repository.matches(uid).catch { actions.fail(it) }.collect { data ->
                    val active = data.firstOrNull { it.id == matchId && it.status == MatchStatus.ACTIVE }
                    if (active == null) close() else open(active)
                }
            },
            viewModelScope.launch {
                repository.settings(uid).catch { actions.fail(it) }.collect { data ->
                    mutable.update { it.copy(readReceipts = data.readReceipts) }
                }
            },
        )
    }

    fun stop() {
        observers.forEach(Job::cancel)
        stopMessages()
    }

    private fun open(match: Match) {
        mutable.update { it.copy(conversation = match) }
        if (chat == null) startMessages()
    }

    private fun startMessages() {
        chat = viewModelScope.launch {
            var first = true
            repository.messages(matchId).catch { actions.fail(it) }.collect { data ->
                mutable.update { old ->
                    old.copy(
                        messages = (old.messages + data).associateBy(ChatMessage::id).values
                            .sortedWith(compareBy<ChatMessage> { it.createdAt }.thenBy { it.id }),
                        hasEarlierMessages = if (first) data.size == PAGE_SIZE else old.hasEarlierMessages,
                    )
                }
                first = false
            }
        }
    }

    private fun stopMessages() {
        chat?.cancel()
        chat = null
        history?.cancel()
        receipt?.cancel()
        lastReadMessage = null
    }

    /** Clears the private conversation and tells the screen to leave, once. */
    private fun close() {
        stopMessages()
        mutable.update {
            it.copy(conversation = null, messages = emptyList(), loadingHistory = false, hasEarlierMessages = false)
        }
        if (!closed) {
            closed = true
            eventChannel.trySend(ChatEvent.Closed)
        }
    }

    fun loadEarlierMessages() {
        val current = state.value
        val beforeId = current.messages.firstOrNull()?.id ?: return
        if (current.conversation == null || current.loadingHistory || !current.hasEarlierMessages) return
        mutable.update { it.copy(loadingHistory = true, action = it.action.copy(error = null)) }
        history = viewModelScope.launch {
            try {
                val older = repository.earlierMessages(matchId, beforeId)
                if (state.value.conversation != null) {
                    mutable.update {
                        it.copy(
                            messages = (older + it.messages).distinctBy(ChatMessage::id),
                            hasEarlierMessages = older.size == PAGE_SIZE,
                        )
                    }
                }
            } catch (error: Exception) {
                actions.fail(error)
            } finally {
                mutable.update { it.copy(loadingHistory = false) }
            }
        }
    }

    /** Sends a read receipt for [message], only if the member turned receipts on and it is not their own. */
    fun markVisibleMessageRead(message: ChatMessage) {
        if (state.value.conversation == null) return
        if (!state.value.readReceipts || message.senderId == uid || lastReadMessage == message.id) return
        receipt?.cancel()
        receipt = viewModelScope.launch {
            try {
                repository.execute(CommunityCommand.Read(matchId, message.id))
                lastReadMessage = message.id
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Retry on the next visible message. Sending stays independent of read receipts.
            }
        }
    }

    /** A retry of the same text reuses the same message id, so a failed send never posts twice. */
    fun send(text: String) {
        if (state.value.conversation == null || text.isBlank() || text.length > MAX_MESSAGE) return
        val pending = pendingMessage?.takeIf { it.first == matchId && it.second == text }
            ?: Triple(matchId, text, UUID.randomUUID().toString()).also { pendingMessage = it }
        actions.run(CommunityCommand.Send(matchId, pending.third, text), onSuccess = { pendingMessage = null })
    }

    fun unmatch() = actions.run(CommunityCommand.Unmatch(matchId), onSuccess = ::close)

    fun block(userId: String) = actions.run(CommunityCommand.Block(userId), onSuccess = ::close)

    fun report(userId: String, category: ReportCategory, details: String, onSuccess: () -> Unit) =
        actions.run(CommunityCommand.Report(userId, category, details), onSuccess = onSuccess)

    fun dismissNotice() = actions.dismissNotice()

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_MESSAGE = 2000
    }
}
