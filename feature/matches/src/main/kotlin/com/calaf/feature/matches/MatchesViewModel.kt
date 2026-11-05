package com.calaf.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.Match
import com.calaf.core.model.ReportCategory
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

data class MatchesUiState(
    val matches: List<Match> = emptyList(),
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = MatchesViewModel.Factory::class)
class MatchesViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): MatchesViewModel
    }

    private val mutable = MutableStateFlow(MatchesUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observer?.cancel()
        actions.clearError()
        observer = viewModelScope.launch {
            repository.matches(uid).catch { actions.fail(it) }.collect { data ->
                mutable.update { it.copy(matches = data) }
            }
        }
    }

    fun stop() {
        observer?.cancel()
    }

    fun unmatch(matchId: String) = actions.run(CommunityCommand.Unmatch(matchId))

    fun block(userId: String) = actions.run(CommunityCommand.Block(userId))

    fun report(userId: String, category: ReportCategory, details: String, onSuccess: () -> Unit) =
        actions.run(CommunityCommand.Report(userId, category, details), onSuccess = onSuccess)

    fun dismissNotice() = actions.dismissNotice()
}
