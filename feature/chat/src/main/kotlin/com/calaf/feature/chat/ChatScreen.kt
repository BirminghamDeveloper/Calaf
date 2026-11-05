package com.calaf.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.calaf.core.model.MessageSafety
import com.calaf.core.model.ReportCategory
import com.calaf.core.model.SafetyWarning
import com.calaf.core.ui.ActionBanner
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.Notice
import com.calaf.core.ui.NoticeCard
import com.calaf.core.ui.ReportDialog
import com.calaf.core.ui.sendingMessageRes

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
        uid, state, ChatActions(
            onSend = model::send, onBack = onBack, onUnmatch = model::unmatch, onBlock = model::block,
            onReport = model::report, onEarlier = model::loadEarlierMessages, onRead = model::markVisibleMessageRead,
            onShareWithGuardian = model::shareWithGuardian, onInviteGuardian = model::inviteGuardian,
        ),
    )
}

/** Everything the chat can ask for, so the screen takes one parameter instead of ten. */
class ChatActions(
    val onSend: (String) -> Unit,
    val onBack: () -> Unit,
    val onUnmatch: () -> Unit,
    val onBlock: (String) -> Unit,
    val onReport: (String, ReportCategory, String, () -> Unit) -> Unit,
    val onEarlier: () -> Unit,
    val onRead: (ChatMessage) -> Unit,
    val onShareWithGuardian: (Boolean) -> Unit,
    val onInviteGuardian: () -> Unit,
)

@Composable
fun ChatScreen(uid: String, state: ChatUiState, actions: ChatActions) {
    val conversation = state.conversation
    var draft by rememberSaveable(conversation?.id) { mutableStateOf("") }
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    var guardianDialog by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(conversation?.id, state.messages.lastOrNull()?.id) {
        val last = state.messages.lastOrNull()
        if (last != null && (list.firstVisibleItemIndex == 0 || last.senderId == uid)) list.animateScrollToItem(0)
    }
    LaunchedEffect(list, state.messages, state.readReceipts) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { list.layoutInfo.visibleItemsInfo.map { it.key } }.collect { keys ->
                state.messages.lastOrNull { it.id in keys && it.senderId != uid }?.let(actions.onRead)
            }
        }
    }
    LaunchedEffect(state.action.notice) { if (state.action.notice == Notice.SENT) draft = "" }
    Column(Modifier.fillMaxSize()) {
        ActionBanner(state.action)
        if (conversation == null) return@Column
        Column(Modifier.weight(1f).imePadding().padding(horizontal = CalafSpacing.medium)) {
            ChatHeader(
                conversation, actions.onBack,
                onReport = { reportUser = conversation.otherId },
                onBlock = { pending = Pending.BLOCK },
                onUnmatch = { pending = Pending.UNMATCH },
                onGuardian = { guardianDialog = true },
            )
            GuardianBanners(conversation)
            Text(stringResource(R.string.chat_safety), style = MaterialTheme.typography.bodySmall)
            MessageList(uid, conversation, state, list, actions.onEarlier, Modifier.weight(1f))
            Composer(draft, { draft = it }, state.action.busy) { actions.onSend(draft) }
        }
    }
    reportUser?.let { userId ->
        ReportDialog(onDismiss = { reportUser = null }) { category, details ->
            actions.onReport(userId, category, details) { reportUser = null }
        }
    }
    pending?.let { action ->
        ConfirmDialog(
            enabled = !state.action.busy,
            onConfirm = {
                when (action) {
                    Pending.BLOCK -> conversation?.let { actions.onBlock(it.otherId) }
                    Pending.UNMATCH -> actions.onUnmatch()
                }
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
    if (guardianDialog && conversation != null) {
        GuardianShareDialog(
            shared = conversation.sharedWithGuardian, hasGuardian = state.hasGuardian, code = state.guardianCode,
            enabled = !state.action.busy, onShare = actions.onShareWithGuardian,
            onInvite = actions.onInviteGuardian, onDismiss = { guardianDialog = false },
        )
    }
}

/** Who can read this conversation besides the two members, always shown so nobody is read over without knowing. */
@Composable
private fun GuardianBanners(conversation: Match) {
    if (conversation.sharedWithGuardian) NoticeCard(stringResource(R.string.chat_guardian_mine))
    if (conversation.otherSharesWithGuardian) {
        NoticeCard(stringResource(R.string.chat_guardian_theirs, conversation.name.ifBlank { "?" }))
    }
}

@Composable
private fun Composer(draft: String, onChange: (String) -> Unit, busy: Boolean, onSend: () -> Unit) {
    val warnings = MessageSafety.warnings(draft)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        OutlinedTextField(
            draft, { onChange(it.take(MAX_MESSAGE)) }, label = { Text(stringResource(R.string.message_hint)) },
            enabled = !busy, modifier = Modifier.fillMaxWidth(), maxLines = 4,
        )
        warnings.forEach { warning -> warning.hint() }
        Button(onClick = onSend, enabled = draft.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
            CalafIcon(CalafIcons.Send, contentDescription = null)
            Text(stringResource(R.string.send), Modifier.padding(start = CalafSpacing.small))
        }
    }
}

@Composable
private fun SafetyWarning.hint() {
    Text(
        stringResource(sendingMessageRes()), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private const val MAX_MESSAGE = 2000

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    val match = Match("a_b", "b", "Hodan (Demo)", MatchStatus.ACTIVE, otherReadAt = 1_700_000_050_000)
    CalafTheme {
        ChatScreen(
            "a",
            ChatUiState(
                conversation = match,
                messages = listOf(
                    ChatMessage("1", "b", "Salaam, nice to meet you.", 1_700_000_000_000, emptyList()),
                    ChatMessage("2", "a", "Walaykum salaam!", 1_700_000_060_000, emptyList()),
                    ChatMessage(
                        "3", "b", "Call me on 07700 900123", 1_700_000_120_000,
                        listOf(SafetyWarning.PERSONAL_INFORMATION),
                    ),
                    ChatMessage("4", "b", "Thank you for sharing that.", 1_700_000_180_000, emptyList(), isDemo = true),
                ),
            ),
            ChatActions({}, {}, {}, {}, { _, _, _, _ -> }, {}, {}, {}, {}),
        )
    }
}
