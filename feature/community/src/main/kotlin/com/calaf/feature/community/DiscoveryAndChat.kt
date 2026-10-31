package com.calaf.feature.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.*
import java.text.DateFormat
import java.util.Date
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
internal fun CandidateCard(profile: CommunityProfile, busy: Boolean, onInterest: (Boolean) -> Unit, onReport: () -> Unit, onBlock: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CalafSpacing.large), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
            Text(stringResource(R.string.candidate_title, profile.name, profile.age), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.location, profile.city, profile.country))
            if (profile.verification == VerificationStatus.EMAIL_ONLY) Text(stringResource(R.string.email_verified), style = MaterialTheme.typography.labelMedium)
            Text(profile.biography)
            if (profile.culture.isNotBlank()) Text(profile.culture)
            if (profile.languages.isNotEmpty()) Text(profile.languages.joinToString())
            val score = profile.compatibility
            val percentage = score.score
            Text(if (percentage == null) stringResource(R.string.compatibility_unknown) else stringResource(R.string.compatibility, percentage),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.compatibility_explanation), style = MaterialTheme.typography.bodySmall)
            if (score.strong.isNotEmpty()) Text(stringResource(R.string.strong_areas, score.strong.map { stringResource(it.label()) }.joinToString()))
            if (score.differences.isNotEmpty()) Text(stringResource(R.string.differences, score.differences.map { stringResource(it.label()) }.joinToString()))
            Row(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
                OutlinedButton(onClick = { onInterest(false) }, enabled = !busy) { Text(stringResource(R.string.pass)) }
                Button(onClick = { onInterest(true) }, enabled = !busy) { Text(stringResource(R.string.interest)) }
            }
            Row {
                TextButton(onClick = onReport, enabled = !busy) { Text(stringResource(R.string.report)) }
                TextButton(onClick = onBlock, enabled = !busy) { Text(stringResource(R.string.block)) }
            }
        }
    }
}

@Composable
internal fun ColumnScope.ChatScreen(uid: String, state: CommunityUiState, onSend: (String) -> Unit,
    onBack: () -> Unit, onUnmatch: (String) -> Unit, onBlock: (String) -> Unit, onReport: (String) -> Unit,
    onEarlier: () -> Unit, onRead: (ChatMessage) -> Unit) {
    val conversation = state.conversation ?: return
    var draft by rememberSaveable(conversation.id) { mutableStateOf("") }
    val list = rememberLazyListState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(conversation.id, state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty() && (list.firstVisibleItemIndex == 0 || state.messages.last().senderId == uid)) list.animateScrollToItem(0)
    }
    LaunchedEffect(list, state.messages, state.settings.readReceipts) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { list.layoutInfo.visibleItemsInfo.map { it.key } }.collect { keys ->
                state.messages.lastOrNull { it.id in keys && it.senderId != uid }?.let(onRead)
            }
        }
    }
    LaunchedEffect(state.notice) { if (state.notice == Notice.SENT) draft = "" }
    Column(Modifier.weight(1f).imePadding().padding(horizontal = CalafSpacing.medium)) {
        Text(conversation.name, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.chat_safety), style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            TextButton(onClick = { onReport(conversation.otherId) }) { Text(stringResource(R.string.report)) }
            TextButton(onClick = { onBlock(conversation.otherId) }) { Text(stringResource(R.string.block)) }
            TextButton(onClick = { onUnmatch(conversation.id) }) { Text(stringResource(R.string.unmatch)) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("chat_history"), state = list, reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
            items(state.messages.asReversed(), key = ChatMessage::id) { message ->
                Surface(color = if (message.senderId == uid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(CalafSpacing.medium)) {
                        Text(message.text)
                        if (message.senderId == uid && message.createdAt > 0 && conversation.otherReadAt >= message.createdAt)
                            Text(stringResource(R.string.message_read), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.sent_at, stringResource(if (message.senderId == uid) R.string.you else R.string.member),
                            if (message.createdAt == 0L) stringResource(R.string.please_wait) else DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAt))),
                            style = MaterialTheme.typography.labelSmall)
                        if (message.warnings.isNotEmpty()) Text(stringResource(R.string.warning_message), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.hasEarlierMessages) item {
                TextButton(onClick = onEarlier, enabled = !state.loadingHistory) {
                    Text(stringResource(if (state.loadingHistory) R.string.please_wait else R.string.earlier_messages))
                }
            }
        }
        OutlinedTextField(draft, { draft = it.take(2000) }, label = { Text(stringResource(R.string.message_hint)) },
            enabled = !state.busy, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        Button(onClick = { onSend(draft) }, enabled = draft.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.send)) }
    }
}

@Composable
internal fun ReportDialog(onDismiss: () -> Unit, onSubmit: (ReportCategory, String) -> Unit) {
    var category by rememberSaveable { mutableStateOf(ReportCategory.OTHER) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.report)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            ChoiceField(stringResource(R.string.report_category), category, ReportCategory.entries, { stringResource(it.label()) }) { category = it }
            OutlinedTextField(details, { details = it.take(2000) }, label = { Text(stringResource(R.string.report_details)) })
        } },
        confirmButton = { TextButton(onClick = { onSubmit(category, details) }) { Text(stringResource(R.string.report)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
private fun ReportCategory.label() = when (this) {
    ReportCategory.HARASSMENT -> R.string.harassment; ReportCategory.IMPERSONATION -> R.string.impersonation
    ReportCategory.FAKE_PROFILE -> R.string.fake_profile; ReportCategory.UNDERAGE -> R.string.underage
    ReportCategory.FINANCIAL_SCAM -> R.string.scam; ReportCategory.INAPPROPRIATE_CONTENT -> R.string.inappropriate; ReportCategory.OTHER -> R.string.other
}
private fun CompatibilityArea.label() = when (this) {
    CompatibilityArea.TIMELINE -> R.string.area_timeline; CompatibilityArea.WANTS_CHILDREN -> R.string.area_children
    CompatibilityArea.SMOKING -> R.string.area_smoking; CompatibilityArea.RELOCATION -> R.string.area_relocation
    CompatibilityArea.RELIGION -> R.string.area_religion; CompatibilityArea.LANGUAGES -> R.string.area_languages
    CompatibilityArea.VALUES -> R.string.area_values; CompatibilityArea.INTERESTS -> R.string.area_interests
}
