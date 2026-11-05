package com.calaf.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CommunityRepository
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

data class DiscoveryUiState(
    /** The member's own published profile. Null until they publish, which discovery needs. */
    val profile: CommunityProfile? = null,
    val candidates: List<CommunityProfile> = emptyList(),
    val cursor: String? = null,
    val discovering: Boolean = false,
    val loaded: Boolean = false,
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = DiscoveryViewModel.Factory::class)
class DiscoveryViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): DiscoveryViewModel
    }

    private val mutable = MutableStateFlow(DiscoveryUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observer?.cancel()
        actions.clearError()
        observer = viewModelScope.launch {
            repository.profile(uid).catch { actions.fail(it) }.collect { data ->
                mutable.update { it.copy(profile = data) }
            }
        }
    }

    fun stop() {
        observer?.cancel()
    }

    /** Loads the first page, or the next one when [more] is true. */
    fun discover(more: Boolean = false) {
        if (state.value.discovering) return
        mutable.update { it.copy(discovering = true, action = it.action.copy(error = null)) }
        viewModelScope.launch {
            try {
                val page = repository.discover(if (more) state.value.cursor else null)
                mutable.update {
                    it.copy(
                        candidates = if (more) {
                            (it.candidates + page.profiles).distinctBy(CommunityProfile::id)
                        } else {
                            page.profiles
                        },
                        cursor = page.cursor,
                        loaded = true,
                    )
                }
            } catch (error: Exception) {
                actions.fail(error)
            } finally {
                mutable.update { it.copy(discovering = false) }
            }
        }
    }

    /** Likes or passes. Either way the member is not shown again in this list. */
    fun interest(userId: String, like: Boolean) =
        actions.run(CommunityCommand.Interest(userId, like), onResult = { removeCandidate(userId) })

    fun block(userId: String) = actions.run(CommunityCommand.Block(userId), onResult = { removeCandidate(userId) })

    fun report(userId: String, category: ReportCategory, details: String, onSuccess: () -> Unit) =
        actions.run(CommunityCommand.Report(userId, category, details), onSuccess = onSuccess)

    fun retry() {
        observe()
        discover()
    }

    fun dismissNotice() = actions.dismissNotice()

    private fun removeCandidate(userId: String) {
        mutable.update { state -> state.copy(candidates = state.candidates.filterNot { it.id == userId }) }
    }
}
