package com.calaf.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.Guardian
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

data class GuardianUiState(
    val guardians: List<Guardian> = emptyList(),
    val invitationCode: String? = null,
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = GuardianViewModel.Factory::class)
class GuardianViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): GuardianViewModel
    }

    private val mutable = MutableStateFlow(GuardianUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observer?.cancel()
        actions.clearError()
        observer = viewModelScope.launch {
            repository.guardians(uid).catch { actions.fail(it) }.collect { data ->
                mutable.update { it.copy(guardians = data) }
            }
        }
    }

    fun stop() {
        observer?.cancel()
    }

    fun invite() = actions.run(
        CommunityCommand.InviteGuardian,
        onResult = { result ->
            mutable.update { it.copy(invitationCode = result.invitationCode ?: it.invitationCode) }
        },
    )

    fun accept(code: String) = actions.run(CommunityCommand.AcceptGuardian(code))

    fun revoke(id: String) = actions.run(CommunityCommand.RevokeGuardian(id))

    fun dismissNotice() = actions.dismissNotice()
}
