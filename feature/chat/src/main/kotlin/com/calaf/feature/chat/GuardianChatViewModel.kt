package com.calaf.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.GuardianMatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuardianChatUiState(
    /** Who shared the conversation and with whom they are talking. Null until loaded or if it is no longer shared. */
    val shared: GuardianMatch? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loaded: Boolean = false,
    /** The conversation is not shared with this member (any more). */
    val unavailable: Boolean = false,
    val error: CommunityError? = null,
)

/** A guardian reading one conversation a member shared with them. There is deliberately nothing here to send with. */
@HiltViewModel(assistedFactory = GuardianChatViewModel.Factory::class)
class GuardianChatViewModel @AssistedInject constructor(
    @Assisted private val matchId: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(matchId: String): GuardianChatViewModel
    }

    private val mutable = MutableStateFlow(GuardianChatUiState())
    val state = mutable.asStateFlow()
    private var observers = listOf<Job>()

    fun observe() {
        observers.forEach(Job::cancel)
        mutable.update { it.copy(error = null, unavailable = false) }
        observers = listOf(
            viewModelScope.launch {
                try {
                    val shared = repository.guardianMatches().firstOrNull { it.matchId == matchId }
                    mutable.update { it.copy(shared = shared, unavailable = shared == null && it.loaded) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    fail(error)
                }
            },
            viewModelScope.launch {
                repository.guardianMessages(matchId).catch { fail(it) }.collect { data ->
                    mutable.update { it.copy(messages = data, loaded = true, unavailable = false) }
                }
            },
        )
    }

    fun stop() {
        observers.forEach(Job::cancel)
    }

    private fun fail(error: Throwable) {
        if (error is CancellationException) throw error
        val reason = (error as? CommunityException)?.reason ?: CommunityError.UNKNOWN
        mutable.update {
            // A permission error means the owner stopped sharing, or the guardian was removed.
            if (reason == CommunityError.PERMISSION) {
                it.copy(unavailable = true, messages = emptyList(), loaded = true)
            } else {
                it.copy(error = reason)
            }
        }
    }
}
