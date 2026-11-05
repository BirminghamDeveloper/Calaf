package com.calaf.feature.matches

import com.calaf.core.ui.IllustrationKind
import com.calaf.core.ui.EmptyState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.PublicPhoto
import com.calaf.core.ui.CalafListScreen
import com.calaf.core.ui.ComplimentBanner
import com.calaf.core.ui.MemberPhoto
import com.calaf.core.ui.NoticeCard

/**
 * Members who liked you. In the demo the list is simulated and says so. In the live app it stays locked, showing only
 * how many people liked you, until premium exists.
 */
@Composable
internal fun LikedYouList(
    state: MatchesUiState,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    onLikeBack: (CommunityProfile, String) -> Unit,
    onPass: (CommunityProfile) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val liked = state.likedYou
    CalafListScreen(
        stringResource(R.string.ly_title), state.action, "liked_you_list",
        working = state.loadingLikedYou, onRetry = onRetry,
    ) {
        if (liked?.simulated == true) item { NoticeCard(stringResource(R.string.ly_demo)) }
        when {
            liked == null -> Unit
            liked.locked -> item { NoticeCard(stringResource(R.string.ly_locked, liked.count)) }
            liked.profiles.isEmpty() -> item { EmptyState(IllustrationKind.Calm, stringResource(R.string.ly_empty)) }
        }
        items(liked?.profiles.orEmpty(), key = CommunityProfile::id) { profile ->
            LikedYouCard(profile, photoLoader, !state.action.busy, onLikeBack, onPass)
        }
        item {
            TextButton(onClick = onRefresh, enabled = !state.loadingLikedYou) {
                Text(stringResource(R.string.ly_refresh))
            }
        }
    }
}

@Composable
private fun LikedYouCard(
    profile: CommunityProfile,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    enabled: Boolean,
    onLikeBack: (CommunityProfile, String) -> Unit,
    onPass: (CommunityProfile) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
                MemberPhoto(
                    profile, profile.photos.firstOrNull(), stringResource(R.string.ly_photo, profile.name),
                    loader = { photo -> photoLoader(profile.id, photo) },
                    modifier = Modifier.size(PHOTO_SIZE).clip(MaterialTheme.shapes.medium),
                )
                Column {
                    Text(
                        stringResource(R.string.ly_name_age, profile.name, profile.age),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.ly_location, profile.city, profile.country))
                }
            }
            profile.compliment?.let { ComplimentBanner(profile.name, it) }
            Row(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
                OutlinedButton(onClick = { onPass(profile) }, enabled = enabled) {
                    Text(stringResource(R.string.ly_pass))
                }
                Button(onClick = { onLikeBack(profile, "") }, enabled = enabled) {
                    Text(stringResource(R.string.ly_like_back))
                }
            }
        }
    }
}

private val PHOTO_SIZE = 88.dp
