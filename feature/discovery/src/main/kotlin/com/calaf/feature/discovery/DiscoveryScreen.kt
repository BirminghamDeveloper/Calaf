package com.calaf.feature.discovery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.activeFilterCount
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionBanner
import com.calaf.core.ui.ComplimentDialog
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.MatchMomentOverlay
import com.calaf.core.ui.NoticeCard
import com.calaf.core.ui.R as CoreR
import com.calaf.core.ui.ReportDialog
import com.calaf.core.ui.SwipeButtons
import com.calaf.core.ui.SwipeCardStack
import com.calaf.core.ui.SwipeDirection
import com.calaf.core.ui.SwipeStackController

/** The Discover tab. Gets its ViewModel from Hilt, so previews and tests use [DiscoveryScreen] directly. */
@Composable
fun DiscoveryRoute(uid: String, onPreferences: () -> Unit, onOpenChat: (String) -> Unit) {
    val model = hiltViewModel<DiscoveryViewModel, DiscoveryViewModel.Factory>(
        key = "discovery_$uid",
        creationCallback = { it.create(uid) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    LaunchedEffect(model) { model.discoverIfNeeded() }
    DiscoveryScreen(
        state, photoLoader = model::photo, matchPhotos = model::matchPhotos, onRefresh = { model.discover() },
        onInterest = model::interest, onBlock = model::block, onReport = model::report,
        onApplyFilters = model::applyFilters, onPreferences = onPreferences, onRetry = model::retry,
        onDismissMatch = model::dismissMatchMoment, onOpenChat = onOpenChat,
    )
}

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    matchPhotos: suspend (CommunityProfile) -> Pair<ByteArray?, ByteArray?>,
    onRefresh: () -> Unit,
    onInterest: (String, Boolean, String) -> Unit,
    onBlock: (String) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onApplyFilters: (PartnerPreference, () -> Unit) -> Unit,
    onPreferences: () -> Unit,
    onRetry: () -> Unit,
    onDismissMatch: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var blockUser by remember { mutableStateOf<String?>(null) }
    val controller = remember { SwipeStackController() }
    var complimentFor by remember { mutableStateOf<CommunityProfile?>(null) }
    // A note chosen in the compliment dialog, sent with the next like from the stack.
    var pendingNote by remember { mutableStateOf("") }
    val busy = state.action.busy
    // The open profile is looked up by id, so it closes by itself once that member leaves the stack.
    val moment = state.matchMoment
    if (moment != null) {
        MatchMomentOverlay(
            moment, yourName = state.profile?.name.orEmpty(), loadPhotos = matchPhotos,
            onMessage = { matchId ->
                onDismissMatch()
                onOpenChat(matchId)
            },
            onKeepBrowsing = onDismissMatch,
        )
        return
    }
    val detail = state.candidates.firstOrNull { it.id == detailId }
    BackHandler(enabled = detail != null) { detailId = null }
    Column(Modifier.fillMaxSize().testTag("discovery_list")) {
        ActionBanner(state.action, working = state.discovering, onRetry = onRetry)
        if (detail != null) {
            ProfileDetailScreen(
                detail, photoLoader = { photo -> photoLoader(detail.id, photo) }, enabled = !busy,
                onBack = { detailId = null },
                onInterest = { like ->
                    detailId = null
                    onInterest(detail.id, like, "")
                },
                onCompliment = { complimentFor = detail },
                onReport = { reportUser = detail.id }, onBlock = { blockUser = detail.id },
            )
            return@Column
        }
        Header(
            onRefresh, state.discovering, state.preferences.activeFilterCount,
            onFilters = { showFilters = true }, onPreferences = onPreferences,
        )
        if (state.profile == null) NoticeCard(stringResource(R.string.publish_first))
        when {
            state.candidates.isNotEmpty() -> Deck(
                state, controller, photoLoader, enabled = !busy,
                onInterest = { id, like ->
                    onInterest(id, like, if (like) pendingNote else "")
                    pendingNote = ""
                },
                onCompliment = { complimentFor = state.candidates.firstOrNull() },
                onOpenDetails = { detailId = state.candidates.firstOrNull()?.id },
                onReport = { reportUser = state.candidates.firstOrNull()?.id },
                onBlock = { blockUser = state.candidates.firstOrNull()?.id },
                modifier = Modifier.weight(1f),
            )
            state.discovering || !state.loaded -> Loading(Modifier.weight(1f))
            else -> EmptyState(onRefresh, onPreferences, Modifier.weight(1f))
        }
    }
    reportUser?.let { userId ->
        ReportDialog(onDismiss = { reportUser = null }) { category, details ->
            onReport(userId, category, details) { reportUser = null }
        }
    }
    complimentFor?.let { target ->
        ComplimentDialog(target.name, enabled = !busy, onDismiss = { complimentFor = null }, onSend = { note ->
            complimentFor = null
            if (detail != null && detail.id == target.id) {
                detailId = null
                onInterest(target.id, true, note)
            } else {
                // Swipe the top card away like a normal like, carrying the note.
                pendingNote = note
                controller.swipe(SwipeDirection.LIKE)
            }
        })
    }
    if (showFilters) {
        FilterSheet(
            current = state.preferences, busy = busy,
            onApply = { value -> onApplyFilters(value) { showFilters = false } },
            onDismiss = { showFilters = false },
        )
    }
    blockUser?.let { userId ->
        ConfirmDialog(
            enabled = !busy,
            onConfirm = { onBlock(userId); blockUser = null },
            onDismiss = { blockUser = null },
        )
    }
}

@Composable
private fun Header(
    onRefresh: () -> Unit,
    refreshing: Boolean,
    filterCount: Int,
    onFilters: () -> Unit,
    onPreferences: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CalafSpacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(CoreR.string.discover), style = MaterialTheme.typography.titleLarge)
        Row {
            TextButton(onClick = onFilters) {
                Text(
                    if (filterCount > 0) {
                        stringResource(R.string.disc_filters_count, filterCount)
                    } else {
                        stringResource(R.string.disc_filters)
                    },
                )
            }
            TextButton(onClick = onRefresh, enabled = !refreshing) { Text(stringResource(R.string.refresh)) }
            TextButton(onClick = onPreferences) { Text(stringResource(CoreR.string.preferences)) }
        }
    }
}

@Composable
private fun Deck(
    state: DiscoveryUiState,
    controller: SwipeStackController,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    enabled: Boolean,
    onInterest: (String, Boolean) -> Unit,
    onCompliment: () -> Unit,
    onOpenDetails: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier,
) {
    val like = stringResource(R.string.interest)
    val pass = stringResource(R.string.pass)
    Column(
        modifier.fillMaxWidth().padding(CalafSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
    ) {
        SwipeCardStack(
            items = state.candidates, itemKey = CommunityProfile::id,
            onSwiped = { profile, direction -> onInterest(profile.id, direction == SwipeDirection.LIKE) },
            controller = controller, likeLabel = like, passLabel = pass, enabled = enabled,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { profile ->
            ProfileCard(
                profile, photoLoader = { photo -> photoLoader(profile.id, photo) }, onOpenDetails = onOpenDetails,
            )
        }
        Text(stringResource(R.string.disc_swipe_hint), style = MaterialTheme.typography.bodySmall)
        SwipeButtons(
            onPass = { controller.swipe(SwipeDirection.PASS) }, onLike = { controller.swipe(SwipeDirection.LIKE) },
            passLabel = pass, likeLabel = like, enabled = enabled,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            reportLabel = stringResource(CoreR.string.report), blockLabel = stringResource(CoreR.string.block),
            onReport = onReport, onBlock = onBlock,
            complimentLabel = stringResource(CoreR.string.cmp_action), onCompliment = onCompliment,
        )
    }
}

@Composable
private fun Loading(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.disc_loading))
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit, onPreferences: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(CalafSpacing.large), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            Text(stringResource(R.string.disc_empty_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.empty_discovery), textAlign = TextAlign.Center)
            Button(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
            OutlinedButton(onClick = onPreferences) { Text(stringResource(CoreR.string.preferences)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoveryScreenPreview() {
    val demo = CommunityProfile(
        "a", "Amina (Demo)", 30, "London", "United Kingdom", occupation = "Teacher",
        photos = listOf(PublicPhoto("p1", blurred = false), PublicPhoto("p2", blurred = true)),
    )
    CalafTheme {
        DiscoveryScreen(
            DiscoveryUiState(profile = demo, candidates = listOf(demo, demo.copy(id = "b")), loaded = true),
            photoLoader = { _, _ -> null }, matchPhotos = { null to null }, onRefresh = {},
            onInterest = { _, _, _ -> }, onBlock = {},
            onReport = { _, _, _, _ -> }, onApplyFilters = { _, _ -> },
            onPreferences = {}, onRetry = {}, onDismissMatch = {}, onOpenChat = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoveryEmptyPreview() {
    CalafTheme {
        DiscoveryScreen(
            DiscoveryUiState(loaded = true), photoLoader = { _, _ -> null }, matchPhotos = { null to null },
            onRefresh = {}, onInterest = { _, _, _ -> }, onBlock = {}, onReport = { _, _, _, _ -> },
            onApplyFilters = { _, _ -> }, onPreferences = {}, onRetry = {},
            onDismissMatch = {}, onOpenChat = {},
        )
    }
}
