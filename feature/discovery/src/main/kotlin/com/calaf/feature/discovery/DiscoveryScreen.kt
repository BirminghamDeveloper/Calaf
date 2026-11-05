package com.calaf.feature.discovery

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
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionBanner
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.NoticeCard
import com.calaf.core.ui.R as CoreR
import com.calaf.core.ui.ReportDialog
import com.calaf.core.ui.SwipeButtons
import com.calaf.core.ui.SwipeCardStack
import com.calaf.core.ui.SwipeDirection
import com.calaf.core.ui.SwipeStackController

/** The Discover tab. Gets its ViewModel from Hilt, so previews and tests use [DiscoveryScreen] directly. */
@Composable
fun DiscoveryRoute(uid: String, onPreferences: () -> Unit) {
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
        state, photoLoader = model::photo, onRefresh = { model.discover() },
        onInterest = model::interest, onBlock = model::block, onReport = model::report,
        onPreferences = onPreferences, onRetry = model::retry,
    )
}

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    onRefresh: () -> Unit,
    onInterest: (String, Boolean) -> Unit,
    onBlock: (String) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onPreferences: () -> Unit,
    onRetry: () -> Unit,
) {
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var blockUser by remember { mutableStateOf<String?>(null) }
    val controller = remember { SwipeStackController() }
    val busy = state.action.busy
    Column(Modifier.fillMaxSize().testTag("discovery_list")) {
        ActionBanner(state.action, working = state.discovering, onRetry = onRetry)
        Header(onRefresh, state.discovering, onPreferences)
        if (state.profile == null) NoticeCard(stringResource(R.string.publish_first))
        when {
            state.candidates.isNotEmpty() -> Deck(
                state, controller, photoLoader, enabled = !busy,
                onInterest = onInterest,
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
    blockUser?.let { userId ->
        ConfirmDialog(
            enabled = !busy,
            onConfirm = { onBlock(userId); blockUser = null },
            onDismiss = { blockUser = null },
        )
    }
}

@Composable
private fun Header(onRefresh: () -> Unit, refreshing: Boolean, onPreferences: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CalafSpacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(CoreR.string.discover), style = MaterialTheme.typography.titleLarge)
        Row {
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
        ) { profile -> ProfileCard(profile, photoLoader = { photo -> photoLoader(profile.id, photo) }) }
        Text(stringResource(R.string.disc_swipe_hint), style = MaterialTheme.typography.bodySmall)
        SwipeButtons(
            controller, passLabel = pass, likeLabel = like, enabled = enabled,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            reportLabel = stringResource(CoreR.string.report), blockLabel = stringResource(CoreR.string.block),
            onReport = onReport, onBlock = onBlock,
        )
    }
}

@Composable
private fun Loading(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.disc_loading))
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit, onPreferences: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(CalafSpacing.large), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
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
            photoLoader = { _, _ -> null }, onRefresh = {}, onInterest = { _, _ -> },
            onBlock = {},
            onReport = { _, _, _, _ -> }, onPreferences = {}, onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoveryEmptyPreview() {
    CalafTheme {
        DiscoveryScreen(
            DiscoveryUiState(loaded = true), photoLoader = { _, _ -> null }, onRefresh = {},
            onInterest = { _, _ -> }, onBlock = {}, onReport = { _, _, _, _ -> }, onPreferences = {}, onRetry = {},
        )
    }
}
