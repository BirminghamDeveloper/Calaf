package com.calaf.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.ProfileDraft
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PublishUiState(val action: ActionState = ActionState())

/** Publishes the member's reviewed profile so other members can discover it. */
@HiltViewModel
class PublishViewModel @Inject constructor(repository: CommunityRepository) : ViewModel() {
    private val mutable = MutableStateFlow(PublishUiState())
    val state = mutable.asStateFlow()
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun publish(profile: ProfileDraft) = actions.run(CommunityCommand.Publish(profile))

    fun dismissNotice() = actions.dismissNotice()
}
