package com.calaf.feature.discovery

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.CalafListScreen
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.ReportDialog

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
    DiscoveryScreen(
        state, onRefresh = { model.discover() }, onLoadMore = { model.discover(true) },
        onInterest = model::interest, onBlock = model::block, onReport = model::report,
        onPreferences = onPreferences, onRetry = model::retry,
    )
}

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onInterest: (String, Boolean) -> Unit,
    onBlock: (String) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onPreferences: () -> Unit,
    onRetry: () -> Unit,
) {
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var blockUser by remember { mutableStateOf<String?>(null) }
    val busy = state.action.busy
    CalafListScreen(
        stringResource(CoreR.string.discover), state.action, "discovery_list",
        working = state.discovering, onRetry = onRetry,
    ) {
        item {
            Row {
                Button(onClick = onRefresh, enabled = !state.discovering) { Text(stringResource(R.string.refresh)) }
                TextButton(onClick = onPreferences) { Text(stringResource(CoreR.string.preferences)) }
            }
        }
        if (state.profile == null) item { Text(stringResource(R.string.publish_first)) }
        if (state.candidates.isEmpty()) item { Text(stringResource(R.string.empty_discovery)) }
        items(state.candidates, key = CommunityProfile::id) { candidate ->
            CandidateCard(
                candidate, busy, { onInterest(candidate.id, it) },
                onReport = { reportUser = candidate.id }, onBlock = { blockUser = candidate.id },
            )
        }
        if (state.cursor != null) {
            item {
                TextButton(onClick = onLoadMore, enabled = !state.discovering) {
                    Text(stringResource(R.string.load_more))
                }
            }
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

@Preview(showBackground = true)
@Composable
private fun DiscoveryScreenPreview() {
    val demo = CommunityProfile(
        "a", "Amina (Demo)", 30, "London", "United Kingdom", biography = "Fictional preview profile.",
    )
    CalafTheme {
        DiscoveryScreen(
            DiscoveryUiState(candidates = listOf(demo), loaded = true),
            onRefresh = {}, onLoadMore = {}, onInterest = { _, _ -> }, onBlock = {},
            onReport = { _, _, _, _ -> }, onPreferences = {}, onRetry = {},
        )
    }
}
