package com.calaf.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class Notice { SAVED, MATCHED, REPORTED, BLOCKED, UNMATCHED, SENT, INVITATION_ACCEPTED, REVOKED, DELETION_REQUESTED }
data class CommunityUiState(
    val profile: CommunityProfile? = null, val matches: List<Match> = emptyList(),
    val settings: PrivacySettings = PrivacySettings(), val preferences: PartnerPreference = PartnerPreference(),
    val guardians: List<Guardian> = emptyList(), val subscription: Subscription = Subscription(),
    val candidates: List<CommunityProfile> = emptyList(), val cursor: String? = null,
    val discovering: Boolean = false, val discoveryLoaded: Boolean = false,
    val messages: List<ChatMessage> = emptyList(), val conversation: Match? = null,
    val busy: Boolean = false, val error: CommunityError? = null, val notice: Notice? = null,
    val invitationCode: String? = null, val exportJson: String? = null,
    val loadingHistory: Boolean = false, val hasEarlierMessages: Boolean = false,
    val notifications: List<MemberNotification> = emptyList(),
)
class CommunityViewModel(private val uid: String, private val repository: CommunityRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CommunityUiState())
    val state = mutable.asStateFlow()
    private var observers = listOf<Job>()
    private var chat: Job? = null
    private var history: Job? = null
    private var receipt: Job? = null
    private var lastReadMessage: String? = null
    private var pendingMessage: Triple<String, String, String>? = null
    fun stop() { observers.forEach(Job::cancel); chat?.cancel(); history?.cancel(); receipt?.cancel() }
    fun observe() {
        observers.forEach(Job::cancel)
        mutable.update { it.copy(error = null) }
        observers = listOf(
            watch(repository.profile(uid)) { data -> copy(profile = data) },
            watch(repository.matches(uid)) { data ->
                val active = data.firstOrNull { match -> match.id == conversation?.id && match.status == MatchStatus.ACTIVE }
                copy(matches = data, conversation = active, messages = if (active == null) emptyList() else messages)
            },
            watch(repository.settings(uid)) { data -> copy(settings = data) },
            watch(repository.preferences(uid)) { data -> copy(preferences = data) },
            watch(repository.guardians(uid)) { data -> copy(guardians = data) },
            watch(repository.subscription(uid)) { data -> copy(subscription = data) },
            watch(repository.notifications(uid)) { data -> copy(notifications = data) },
        )
    }
    private fun <T> watch(flow: Flow<T>, reducer: CommunityUiState.(T) -> CommunityUiState) = viewModelScope.launch {
        flow.catch { error -> fail(error) }.collect { data -> mutable.update { it.reducer(data) } }
    }
    private fun fail(error: Throwable) {
        if (error is CancellationException) throw error
        mutable.update { it.copy(error = (error as? CommunityException)?.reason ?: CommunityError.UNKNOWN) }
    }
    fun discover(more: Boolean = false) {
        if (state.value.discovering) return
        mutable.update { it.copy(discovering = true, error = null) }
        viewModelScope.launch {
            try {
                val page = repository.discover(if (more) state.value.cursor else null)
                mutable.update { it.copy(candidates = if (more) (it.candidates + page.profiles).distinctBy(CommunityProfile::id) else page.profiles,
                    cursor = page.cursor, discoveryLoaded = true) }
            } catch (error: Exception) { fail(error) }
            finally { mutable.update { it.copy(discovering = false) } }
        }
    }
    fun openChat(match: Match) {
        closeChat()
        mutable.update { it.copy(conversation = match, messages = emptyList(), error = null, notice = null) }
        chat = viewModelScope.launch {
            var first = true
            repository.messages(match.id).catch { fail(it) }.collect { data ->
                if (state.value.conversation?.id != match.id) return@collect
                mutable.update { old -> old.copy(messages = (old.messages + data).associateBy(ChatMessage::id).values
                    .sortedWith(compareBy<ChatMessage> { it.createdAt }.thenBy { it.id }),
                    hasEarlierMessages = if (first) data.size == 50 else old.hasEarlierMessages) }
                first = false
            }
        }
    }
    fun closeChat() {
        chat?.cancel(); history?.cancel(); receipt?.cancel(); lastReadMessage = null
        mutable.update { it.copy(conversation = null, messages = emptyList(), loadingHistory = false, hasEarlierMessages = false) }
    }
    fun loadEarlierMessages() {
        val current = state.value
        val matchId = current.conversation?.id ?: return
        val beforeId = current.messages.firstOrNull()?.id ?: return
        if (current.loadingHistory || !current.hasEarlierMessages) return
        mutable.update { it.copy(loadingHistory = true, error = null) }
        history = viewModelScope.launch {
            try {
                val older = repository.earlierMessages(matchId, beforeId)
                if (state.value.conversation?.id == matchId) mutable.update { it.copy(
                    messages = (older + it.messages).distinctBy(ChatMessage::id), hasEarlierMessages = older.size == 50) }
            } catch (error: Exception) { fail(error) }
            finally { mutable.update { it.copy(loadingHistory = false) } }
        }
    }
    fun markVisibleMessageRead(message: ChatMessage) {
        val matchId = state.value.conversation?.id ?: return
        if (!state.value.settings.readReceipts || message.senderId == uid || lastReadMessage == message.id) return
        receipt?.cancel()
        receipt = viewModelScope.launch {
            try { repository.execute(CommunityCommand.Read(matchId, message.id)); lastReadMessage = message.id }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Retry on the next visible message; sending remains independent. */ }
        }
    }
    fun send(text: String) {
        val id = state.value.conversation?.id ?: return
        if (text.isBlank() || text.length > 2000) return
        val pending = pendingMessage?.takeIf { it.first == id && it.second == text }
            ?: Triple(id, text, UUID.randomUUID().toString()).also { pendingMessage = it }
        act(CommunityCommand.Send(id, pending.third, text)) { pendingMessage = null }
    }
    fun dismissNotice() { mutable.update { it.copy(notice = null) } }
    fun act(command: CommunityCommand, onSuccess: () -> Unit = {}) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val result = repository.execute(command)
                mutable.update { old ->
                    val removeId = when (command) {
                        is CommunityCommand.Interest -> command.userId
                        is CommunityCommand.Block -> command.userId
                        else -> null
                    }
                    val notice = when {
                        result.matched -> Notice.MATCHED
                        command is CommunityCommand.Report -> Notice.REPORTED
                        command is CommunityCommand.Block -> Notice.BLOCKED
                        command is CommunityCommand.Unmatch -> Notice.UNMATCHED
                        command is CommunityCommand.Send -> Notice.SENT
                        command is CommunityCommand.AcceptGuardian -> Notice.INVITATION_ACCEPTED
                        command is CommunityCommand.RevokeGuardian || command == CommunityCommand.RevokeSessions -> Notice.REVOKED
                        command == CommunityCommand.RequestDeletion -> Notice.DELETION_REQUESTED
                        else -> Notice.SAVED
                    }
                    old.copy(notice = notice, candidates = old.candidates.filterNot { it.id == removeId },
                        invitationCode = result.invitationCode ?: old.invitationCode, exportJson = result.exportJson ?: old.exportJson)
                }
                if (command is CommunityCommand.Unmatch || command is CommunityCommand.Block) closeChat()
                onSuccess()
            } catch (error: Exception) { fail(error) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
