package com.calaf.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.Match
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PrivacySettings
import com.calaf.core.model.Subscription
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: PrivacySettings = PrivacySettings(),
    val preferences: PartnerPreference = PartnerPreference(),
    val subscription: Subscription = Subscription(),
    /** Members this member has blocked. */
    val blocked: List<Match> = emptyList(),
    val exportJson: String? = null,
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = SettingsViewModel.Factory::class)
class SettingsViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): SettingsViewModel
    }

    private val mutable = MutableStateFlow(SettingsUiState())
    val state = mutable.asStateFlow()
    private var observers = listOf<Job>()
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observers.forEach(Job::cancel)
        actions.clearError()
        observers = listOf(
            watch(repository.settings(uid)) { copy(settings = it) },
            watch(repository.preferences(uid)) { copy(preferences = it) },
            watch(repository.subscription(uid)) { copy(subscription = it) },
            watch(repository.matches(uid)) { data -> copy(blocked = data.filter(Match::blockedByMe)) },
        )
    }

    private fun <T> watch(flow: Flow<T>, reducer: SettingsUiState.(T) -> SettingsUiState) = viewModelScope.launch {
        flow.catch { actions.fail(it) }.collect { data -> mutable.update { it.reducer(data) } }
    }

    fun stop() {
        observers.forEach(Job::cancel)
    }

    fun savePreferences(value: PartnerPreference) = actions.run(CommunityCommand.Preferences(value))

    fun savePrivacy(value: PrivacySettings) = actions.run(CommunityCommand.Privacy(value))

    fun revokeSessions() = actions.run(CommunityCommand.RevokeSessions)

    fun requestDeletion() = actions.run(CommunityCommand.RequestDeletion)

    fun exportData() = actions.run(
        CommunityCommand.ExportData,
        onResult = { result -> mutable.update { it.copy(exportJson = result.exportJson ?: it.exportJson) } },
    )

    fun dismissNotice() = actions.dismissNotice()
}
