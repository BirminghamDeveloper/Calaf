package com.calaf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.model.PhotoRepository
import com.calaf.core.ui.ActionBanner
import com.calaf.feature.onboarding.PhotoSection
import com.calaf.feature.onboarding.ProfileScreen
import com.calaf.feature.onboarding.ProfileUiState
import com.calaf.feature.onboarding.ProfileViewModel
import com.calaf.feature.onboarding.PublishViewModel

/** The Profile tab: the private profile editor, publishing, photos and any flavour specific controls. */
@Composable
internal fun ProfileTab(
    uid: String,
    profile: ProfileViewModel,
    profileState: ProfileUiState,
    onLogout: () -> Unit,
    photos: PhotoRepository,
    appMode: AppMode,
) {
    val publish = hiltViewModel<PublishViewModel>(key = "publish_$uid")
    val publishState by publish.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ActionBanner(publishState.action)
        Box(Modifier.weight(1f)) {
            ProfileScreen(
                profileState, profile::edit, profile::save, profile::editAgain, profile::load, onLogout,
                onPublish = { publish.publish(profileState.draft) },
                photos = {
                    appMode.AdminSection()
                    PhotoSection(uid, photos)
                },
            )
        }
    }
}
