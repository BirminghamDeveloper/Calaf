package com.calaf.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.GuardianMatch
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.ui.NoticeCard
import com.calaf.core.ui.labelRes
import com.calaf.core.ui.R as CoreR
import java.time.ZoneId

/** A guardian reading a conversation shared with them. Read only: there is no way to write here. */
@Composable
fun GuardianChatRoute(matchId: String, onBack: () -> Unit) {
    val model = hiltViewModel<GuardianChatViewModel, GuardianChatViewModel.Factory>(
        key = "guardian_chat_$matchId",
        creationCallback = { it.create(matchId) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    GuardianChatScreen(state, onBack)
}

@Composable
fun GuardianChatScreen(state: GuardianChatUiState, onBack: () -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val items = remember(state.messages) { ChatTimeline.items(state.messages, System.currentTimeMillis(), zone) }
    val shared = state.shared
    Column(Modifier.fillMaxSize().padding(horizontal = CalafSpacing.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { CalafIcon(CalafIcons.Back, stringResource(CoreR.string.back)) }
            Text(stringResource(R.string.chat_menu_guardian), style = MaterialTheme.typography.titleLarge)
        }
        if (shared != null) {
            NoticeCard(stringResource(R.string.chat_guardian_readonly, shared.ownerName, shared.otherName))
        }
        state.error?.let { NoticeCard(stringResource(it.labelRes()), error = true) }
        when {
            state.unavailable -> NoticeCard(stringResource(R.string.chat_guardian_stopped), error = true)
            state.loaded && state.messages.isEmpty() -> Text(stringResource(R.string.chat_guardian_empty))
        }
        // Messages by the member are on the right, as they appear to the member. No status ticks: they are theirs.
        val owner = shared?.ownerId.orEmpty()
        val view = Match("", "", "", MatchStatus.ACTIVE)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("guardian_chat_history"), state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
        ) {
            items(items, key = ::itemKey) { item ->
                when (item) {
                    is ChatItem.Day -> DaySeparator(item)
                    is ChatItem.Message -> Bubble(item.message, owner, view, showStatus = false)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuardianChatPreview() {
    CalafTheme {
        GuardianChatScreen(
            GuardianChatUiState(
                shared = GuardianMatch("m", "owner", "Hassan (Demo)", "Amina (Demo)"),
                messages = listOf(
                    ChatMessage("1", "owner", "Salaam", 1_700_000_000_000, emptyList()),
                    ChatMessage("2", "other", "Walaykum salaam", 1_700_000_060_000, emptyList()),
                ),
                loaded = true,
            ),
            onBack = {},
        )
    }
}
