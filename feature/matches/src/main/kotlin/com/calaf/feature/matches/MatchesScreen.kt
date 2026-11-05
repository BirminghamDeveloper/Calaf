package com.calaf.feature.matches

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.CalafListScreen
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.MatchMomentOverlay
import com.calaf.core.ui.ReportDialog

private sealed interface Pending {
    data class Block(val userId: String) : Pending
    data class Unmatch(val matchId: String) : Pending
}

/** The Matches tab. Gets its ViewModel from Hilt, so previews and tests use [MatchesScreen] directly. */
@Composable
fun MatchesRoute(uid: String, onOpenChat: (String) -> Unit) {
    val model = hiltViewModel<MatchesViewModel, MatchesViewModel.Factory>(
        key = "matches_$uid",
        creationCallback = { it.create(uid) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    MatchesScreen(
        state, onOpenChat, onReport = model::report, onBlock = model::block, onUnmatch = model::unmatch,
        onRetry = model::observe, photoLoader = model::photo, matchPhotos = model::matchPhotos,
        onLikeBack = model::likeBack, onPass = model::pass, onRefreshLikedYou = model::refreshLikedYou,
        onDismissMatch = model::dismissMatchMoment,
    )
}

/** Two tabs: your matches, and the members who liked you and are waiting for an answer. */
@Composable
fun MatchesScreen(
    state: MatchesUiState,
    onOpenChat: (String) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onBlock: (String) -> Unit,
    onUnmatch: (String) -> Unit,
    onRetry: () -> Unit,
    photoLoader: suspend (String, PublicPhoto) -> ByteArray?,
    matchPhotos: suspend (CommunityProfile) -> Pair<ByteArray?, ByteArray?>,
    onLikeBack: (CommunityProfile, String) -> Unit,
    onPass: (CommunityProfile) -> Unit,
    onRefreshLikedYou: () -> Unit,
    onDismissMatch: () -> Unit,
) {
    val moment = state.matchMoment
    if (moment != null) {
        MatchMomentOverlay(
            moment, yourName = "", loadPhotos = matchPhotos,
            onMessage = { matchId ->
                onDismissMatch()
                onOpenChat(matchId)
            },
            onKeepBrowsing = onDismissMatch,
        )
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(CoreR.string.matches)) })
            Tab(
                selected = tab == 1,
                onClick = {
                    tab = 1
                    onRefreshLikedYou()
                },
                text = { Text(stringResource(R.string.ly_tab, state.likedYou?.count ?: 0)) },
            )
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                MatchesList(state, { onOpenChat(it.id) }, onReport, onBlock, onUnmatch, onRetry)
            } else {
                LikedYouList(state, photoLoader, onLikeBack, onPass, onRefreshLikedYou, onRetry)
            }
        }
    }
}

@Composable
private fun MatchesList(
    state: MatchesUiState,
    onOpenConversation: (Match) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onBlock: (String) -> Unit,
    onUnmatch: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    val active = state.matches.filter { it.status == MatchStatus.ACTIVE }
    CalafListScreen(stringResource(CoreR.string.matches), state.action, "matches_list", onRetry = onRetry) {
        if (active.isEmpty()) item { Text(stringResource(R.string.empty_matches)) }
        items(active, key = Match::id) { match ->
            MatchCard(
                match, onOpen = { onOpenConversation(match) },
                onReport = { reportUser = match.otherId },
                onBlock = { pending = Pending.Block(match.otherId) },
                onUnmatch = { pending = Pending.Unmatch(match.id) },
            )
        }
    }
    reportUser?.let { userId ->
        ReportDialog(onDismiss = { reportUser = null }) { category, details ->
            onReport(userId, category, details) { reportUser = null }
        }
    }
    pending?.let { action ->
        ConfirmDialog(
            enabled = !state.action.busy,
            onConfirm = {
                when (action) {
                    is Pending.Block -> onBlock(action.userId)
                    is Pending.Unmatch -> onUnmatch(action.matchId)
                }
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun MatchCard(
    match: Match,
    onOpen: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onUnmatch: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CalafSpacing.medium)) {
            Text(
                match.name.ifBlank { stringResource(CoreR.string.member) },
                style = MaterialTheme.typography.titleLarge,
            )
            Row {
                Button(onClick = onOpen) {
                    CalafIcon(CalafIcons.Messages, contentDescription = null)
                    Text(stringResource(R.string.open_conversation), Modifier.padding(start = CalafSpacing.small))
                }
                // Safety actions stay one tap away. Only the less urgent Unmatch lives in the menu.
                IconButton(onClick = onReport) {
                    CalafIcon(CalafIcons.Report, stringResource(CoreR.string.report))
                }
                IconButton(onClick = onBlock) {
                    CalafIcon(CalafIcons.Block, stringResource(CoreR.string.block))
                }
                IconButton(onClick = { menu = true }) {
                    CalafIcon(CalafIcons.More, stringResource(R.string.more_options))
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(CoreR.string.unmatch)) },
                        onClick = { menu = false; onUnmatch() },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchesScreenPreview() {
    CalafTheme {
        MatchesScreen(
            MatchesUiState(listOf(Match("a_b", "b", "Hodan (Demo)", MatchStatus.ACTIVE))),
            onOpenChat = {}, onReport = { _, _, _, _ -> }, onBlock = {}, onUnmatch = {}, onRetry = {},
            photoLoader = { _, _ -> null }, matchPhotos = { null to null }, onLikeBack = { _, _ -> }, onPass = {},
            onRefreshLikedYou = {}, onDismissMatch = {},
        )
    }
}
