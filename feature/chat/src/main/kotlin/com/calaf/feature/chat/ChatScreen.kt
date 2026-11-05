package com.calaf.feature.chat

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionBanner
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.Notice
import com.calaf.core.ui.ReportDialog
import java.text.DateFormat
import java.util.Date

private enum class Pending { BLOCK, UNMATCH }

/** One conversation. Gets its ViewModel from Hilt, so previews and tests use [ChatScreen] directly. */
@Composable
fun ChatRoute(uid: String, matchId: String, onClosed: () -> Unit, onBack: () -> Unit) {
    val model = hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
        key = "chat_${uid}_$matchId",
        creationCallback = { it.create(uid, matchId) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    LaunchedEffect(model) {
        model.events.collect { event ->
            when (event) {
                ChatEvent.Closed -> onClosed()
            }
        }
    }
    ChatScreen(
        uid, state, onSend = model::send, onBack = onBack, onUnmatch = model::unmatch, onBlock = model::block,
        onReport = model::report, onEarlier = model::loadEarlierMessages, onRead = model::markVisibleMessageRead,
    )
}

@Composable
fun ChatScreen(
    uid: String,
    state: ChatUiState,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onUnmatch: () -> Unit,
    onBlock: (String) -> Unit,
    onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    onEarlier: () -> Unit,
    onRead: (ChatMessage) -> Unit,
) {
    val conversation = state.conversation
    var draft by rememberSaveable(conversation?.id) { mutableStateOf("") }
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    val list = rememberLazyListState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(conversation?.id, state.messages.lastOrNull()?.id) {
        val last = state.messages.lastOrNull()
        if (last != null && (list.firstVisibleItemIndex == 0 || last.senderId == uid)) list.animateScrollToItem(0)
    }
    LaunchedEffect(list, state.messages, state.readReceipts) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { list.layoutInfo.visibleItemsInfo.map { it.key } }.collect { keys ->
                state.messages.lastOrNull { it.id in keys && it.senderId != uid }?.let(onRead)
            }
        }
    }
    LaunchedEffect(state.action.notice) { if (state.action.notice == Notice.SENT) draft = "" }
    Column(Modifier.fillMaxSize()) {
        ActionBanner(state.action)
        if (conversation == null) return@Column
        Column(Modifier.weight(1f).imePadding().padding(horizontal = CalafSpacing.medium)) {
            ChatHeader(
                conversation, onBack,
                onReport = { reportUser = conversation.otherId },
                onBlock = { pending = Pending.BLOCK },
                onUnmatch = { pending = Pending.UNMATCH },
            )
            Text(stringResource(R.string.chat_safety), style = MaterialTheme.typography.bodySmall)
            MessageList(uid, conversation, state, list, onEarlier, Modifier.weight(1f))
            OutlinedTextField(
                draft, { draft = it.take(MAX_MESSAGE) }, label = { Text(stringResource(R.string.message_hint)) },
                enabled = !state.action.busy, modifier = Modifier.fillMaxWidth(), maxLines = 4,
            )
            Button(
                onClick = { onSend(draft) }, enabled = draft.isNotBlank() && !state.action.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CalafIcon(CalafIcons.Send, contentDescription = null)
                Text(stringResource(R.string.send), Modifier.padding(start = CalafSpacing.small))
            }
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
                    Pending.BLOCK -> conversation?.let { onBlock(it.otherId) }
                    Pending.UNMATCH -> onUnmatch()
                }
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun ChatHeader(
    match: Match,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onUnmatch: () -> Unit,
) {
    Text(match.name, style = MaterialTheme.typography.titleLarge)
    Row {
        IconButton(onClick = onBack) { CalafIcon(CalafIcons.Back, stringResource(CoreR.string.back)) }
        IconButton(onClick = onReport) { CalafIcon(CalafIcons.Report, stringResource(CoreR.string.report)) }
        IconButton(onClick = onBlock) { CalafIcon(CalafIcons.Block, stringResource(CoreR.string.block)) }
        TextButton(onClick = onUnmatch) { Text(stringResource(CoreR.string.unmatch)) }
    }
}

@Composable
private fun MessageList(
    uid: String,
    conversation: Match,
    state: ChatUiState,
    list: androidx.compose.foundation.lazy.LazyListState,
    onEarlier: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier.fillMaxWidth().testTag("chat_history"), state = list, reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
    ) {
        items(state.messages.asReversed(), key = ChatMessage::id) { message ->
            val mine = message.senderId == uid
            Surface(
                color = if (mine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(CalafSpacing.medium)) {
                    Text(message.text)
                    if (mine && message.createdAt > 0 && conversation.otherReadAt >= message.createdAt) {
                        Text(stringResource(R.string.message_read), style = MaterialTheme.typography.labelSmall)
                    }
                    val time = if (message.createdAt == 0L) {
                        stringResource(R.string.please_wait)
                    } else {
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAt))
                    }
                    val sender = stringResource(if (mine) R.string.you else CoreR.string.member)
                    Text(stringResource(R.string.sent_at, sender, time), style = MaterialTheme.typography.labelSmall)
                    if (message.warnings.isNotEmpty()) {
                        Text(stringResource(R.string.warning_message), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (state.hasEarlierMessages) {
            item {
                TextButton(onClick = onEarlier, enabled = !state.loadingHistory) {
                    Text(stringResource(if (state.loadingHistory) R.string.please_wait else R.string.earlier_messages))
                }
            }
        }
    }
}

private const val MAX_MESSAGE = 2000

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    val match = Match("a_b", "b", "Hodan (Demo)", MatchStatus.ACTIVE)
    CalafTheme {
        ChatScreen(
            "a",
            ChatUiState(
                conversation = match,
                messages = listOf(
                    ChatMessage("1", "b", "Salaam, nice to meet you.", 1_700_000_000_000, emptyList()),
                    ChatMessage("2", "a", "Walaykum salaam!", 1_700_000_060_000, emptyList()),
                ),
            ),
            onSend = {}, onBack = {}, onUnmatch = {}, onBlock = {}, onReport = { _, _, _, _ -> },
            onEarlier = {}, onRead = {},
        )
    }
}
