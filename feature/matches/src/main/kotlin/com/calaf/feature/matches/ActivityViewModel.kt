package com.calaf.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.Match
import com.calaf.core.model.MemberNotification
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityUiState(
    val notifications: List<MemberNotification> = emptyList(),
    val matches: List<Match> = emptyList(),
    val action: ActionState = ActionState(),
) {
    val unreadCount: Int get() = notifications.count { !it.read }
}

@HiltViewModel(assistedFactory = ActivityViewModel.Factory::class)
class ActivityViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): ActivityViewModel
    }

    private val mutable = MutableStateFlow(ActivityUiState())
    val state = mutable.asStateFlow()
    private var observers = listOf<Job>()
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observers.forEach(Job::cancel)
        actions.clearError()
        observers = listOf(
            viewModelScope.launch {
                repository.notifications(uid).catch { actions.fail(it) }.collect { data ->
                    mutable.update { it.copy(notifications = data) }
                }
            },
            viewModelScope.launch {
                repository.matches(uid).catch { actions.fail(it) }.collect { data ->
                    mutable.update { it.copy(matches = data) }
                }
            },
        )
    }

    fun stop() {
        observers.forEach(Job::cancel)
    }

    fun markRead(notification: MemberNotification) =
        actions.run(CommunityCommand.ReadNotification(notification.id, notification.createdAt))

    fun dismissNotice() = actions.dismissNotice()
}
