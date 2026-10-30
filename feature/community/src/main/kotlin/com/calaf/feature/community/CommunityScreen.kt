package com.calaf.feature.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.*

internal enum class Page(val label: Int) {
    DISCOVER(R.string.discover), MATCHES(R.string.matches), MESSAGES(R.string.messages), PROFILE(R.string.profile),
    SETTINGS(R.string.settings), PREFERENCES(R.string.preferences), PRIVACY(R.string.privacy), GUARDIAN(R.string.guardian),
    VERIFICATION(R.string.verification), PREMIUM(R.string.premium), SAFETY(R.string.safety), GUIDELINES(R.string.guidelines),
    BLOCKED(R.string.blocked), SECURITY(R.string.security), ABOUT(R.string.about), ACTIVITY(R.string.activity_title)
}

@Composable
fun CommunityScreen(uid: String, repository: CommunityRepository, profileContent: @Composable ((ProfileDraft) -> Unit) -> Unit) {
    val model: CommunityViewModel = viewModel(key = "community_$uid", factory = viewModelFactory {
        initializer { CommunityViewModel(uid, repository) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) { model.observe(); onDispose { model.stop() } }
    var page by rememberSaveable { mutableStateOf(Page.PROFILE) }
    var reportUser by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<CommunityCommand?>(null) }
    val mainPages = listOf(Page.DISCOVER, Page.MATCHES, Page.MESSAGES, Page.PROFILE)
    Scaffold(bottomBar = {
        NavigationBar { mainPages.forEach { destination ->
            NavigationBarItem(selected = page == destination, onClick = { model.closeChat(); page = destination },
                icon = { Text(stringResource(destination.label).take(1)) }, label = { Text(stringResource(destination.label)) })
        } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = CalafSpacing.medium), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.brand), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { model.closeChat(); page = Page.ACTIVITY }) {
                    Text(stringResource(R.string.activity_count, state.notifications.count { !it.read }))
                }
                TextButton(onClick = { model.closeChat(); page = if (page in mainPages) Page.SETTINGS else Page.PROFILE }) {
                    Text(stringResource(if (page in mainPages) R.string.settings else R.string.back))
                }
            }
            Text(stringResource(R.string.purpose), Modifier.padding(horizontal = CalafSpacing.medium), style = MaterialTheme.typography.bodySmall)
            if (state.busy || state.discovering) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                NoticeCard(stringResource(error.label()), error = true)
                TextButton(onClick = { model.observe(); if (page == Page.DISCOVER) model.discover() }) { Text(stringResource(R.string.retry_connection)) }
            }
            state.notice?.let { NoticeCard(stringResource(it.label())) }
            when {
                page == Page.PROFILE -> Box(Modifier.weight(1f)) { profileContent { model.act(CommunityCommand.Publish(it)) } }
                state.conversation != null && page == Page.MESSAGES -> ChatScreen(uid, state, model::send, model::closeChat,
                    { confirmation = CommunityCommand.Unmatch(it) }, { confirmation = CommunityCommand.Block(it) }, { reportUser = it },
                    model::loadEarlierMessages, model::markVisibleMessageRead)
                page == Page.PREFERENCES -> PreferencesScreen(state.preferences, state.busy) { model.act(CommunityCommand.Preferences(it)) }
                page == Page.PRIVACY -> PrivacyScreen(state.settings, state.busy) { model.act(CommunityCommand.Privacy(it)) }
                page == Page.GUARDIAN -> GuardianScreen(state) { model.act(it) }
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = CalafSpacing.medium).testTag("community_list"), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
                    item { Text(stringResource(page.label), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = CalafSpacing.medium)) }
                    when (page) {
                        Page.ACTIVITY -> {
                            item { Text(stringResource(R.string.activity_description)) }
                            if (state.notifications.isEmpty()) item { Text(stringResource(R.string.activity_empty)) }
                            items(state.notifications, key = MemberNotification::id) { notification ->
                                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(CalafSpacing.medium)) {
                                    Text(stringResource(if (notification.category == "MATCH") R.string.activity_match else R.string.activity_message),
                                        style = MaterialTheme.typography.titleMedium)
                                    if (notification.createdAt > 0) Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(notification.createdAt)))
                                    val match = state.matches.firstOrNull { it.id == notification.targetId && it.status == MatchStatus.ACTIVE }
                                    if (match != null) TextButton(onClick = {
                                        model.act(CommunityCommand.ReadNotification(notification.id, notification.createdAt))
                                        model.openChat(match); page = Page.MESSAGES
                                    }, enabled = !state.busy) { Text(stringResource(R.string.open_conversation)) }
                                    if (!notification.read) TextButton(onClick = {
                                        model.act(CommunityCommand.ReadNotification(notification.id, notification.createdAt))
                                    }, enabled = !state.busy) { Text(stringResource(R.string.activity_mark_read)) }
                                } }
                            }
                        }
                        Page.DISCOVER -> {
                            item { Row {
                                Button(onClick = { model.discover() }, enabled = !state.discovering) { Text(stringResource(R.string.refresh)) }
                                TextButton(onClick = { page = Page.PREFERENCES }) { Text(stringResource(R.string.preferences)) }
                            } }
                            if (state.profile == null) item { Text(stringResource(R.string.publish_first)) }
                            if (state.candidates.isEmpty()) item { Text(stringResource(R.string.empty_discovery)) }
                            items(state.candidates, key = CommunityProfile::id) { candidate ->
                                CandidateCard(candidate, state.busy, { model.act(CommunityCommand.Interest(candidate.id, it)) },
                                    { reportUser = candidate.id }, { confirmation = CommunityCommand.Block(candidate.id) })
                            }
                            if (state.cursor != null) item { TextButton(onClick = { model.discover(true) }, enabled = !state.discovering) { Text(stringResource(R.string.load_more)) } }
                        }
                        Page.MATCHES, Page.MESSAGES -> {
                            val matches = state.matches.filter { it.status == MatchStatus.ACTIVE }
                            if (matches.isEmpty()) item { Text(stringResource(if (page == Page.MATCHES) R.string.empty_matches else R.string.empty_messages)) }
                            items(matches, key = Match::id) { match ->
                                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(CalafSpacing.medium)) {
                                    Text(match.name.ifBlank { stringResource(R.string.member) }, style = MaterialTheme.typography.titleLarge)
                                    Button(onClick = { model.openChat(match); page = Page.MESSAGES }) { Text(stringResource(R.string.open_conversation)) }
                                    Row {
                                        TextButton(onClick = { reportUser = match.otherId }) { Text(stringResource(R.string.report)) }
                                        TextButton(onClick = { confirmation = CommunityCommand.Block(match.otherId) }) { Text(stringResource(R.string.block)) }
                                        TextButton(onClick = { confirmation = CommunityCommand.Unmatch(match.id) }) { Text(stringResource(R.string.unmatch)) }
                                    }
                                } }
                            }
                        }
                        Page.SETTINGS -> items(Page.entries.filter { it !in mainPages && it != Page.SETTINGS }) { destination ->
                            OutlinedButton(onClick = { page = destination }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(destination.label)) }
                        }
                        Page.BLOCKED -> {
                            val blocked = state.matches.filter(Match::blockedByMe)
                            if (blocked.isEmpty()) item { Text(stringResource(R.string.no_blocks)) }
                            items(blocked, key = Match::id) { Text(it.name.ifBlank { stringResource(R.string.member) }) }
                        }
                        Page.SAFETY -> item { Text(stringResource(R.string.safety_body)) }
                        Page.GUIDELINES -> item { Text(stringResource(R.string.guidelines_body)) }
                        Page.VERIFICATION -> item { Text(stringResource(R.string.verification_explanation)) }
                        Page.PREMIUM -> {
                            item { Text(stringResource(if (state.subscription.tier == SubscriptionTier.PREMIUM) R.string.premium_plan else R.string.free_plan)) }
                            item { Text(stringResource(R.string.premium_description)) }
                            item { NoticeCard(stringResource(R.string.premium_unavailable)) }
                        }
                        Page.ABOUT -> { item { Text(stringResource(R.string.purpose)) }; item { Text(stringResource(R.string.credits)) } }
                        Page.SECURITY -> {
                            item { OutlinedButton(onClick = { confirmation = CommunityCommand.RevokeSessions }, enabled = !state.busy) { Text(stringResource(R.string.logout_all)) } }
                            item { Text(stringResource(R.string.deletion_explanation)) }
                            item { OutlinedButton(onClick = { confirmation = CommunityCommand.RequestDeletion }, enabled = !state.busy) { Text(stringResource(R.string.deletion)) } }
                            item { Text(stringResource(R.string.export_scope)) }
                            item { OutlinedButton(onClick = { model.act(CommunityCommand.ExportData) }, enabled = !state.busy) { Text(stringResource(R.string.export)) } }
                            state.exportJson?.let { json -> item { SelectionContainer { Text(json) } } }
                        }
                        else -> Unit
                    }
                    item { Spacer(Modifier.height(CalafSpacing.large)) }
                }
            }
        }
    }
    reportUser?.let { userId -> ReportDialog(onDismiss = { reportUser = null }) { category, details ->
        model.act(CommunityCommand.Report(userId, category, details)) { reportUser = null }
    } }
    confirmation?.let { command -> AlertDialog(onDismissRequest = { confirmation = null },
        title = { Text(stringResource(R.string.confirm)) }, text = { Text(stringResource(R.string.confirm_security)) },
        confirmButton = { TextButton(onClick = { model.act(command); confirmation = null }, enabled = !state.busy) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable internal fun NoticeCard(text: String, error: Boolean = false) {
    Surface(Modifier.fillMaxWidth().padding(CalafSpacing.small), shape = MaterialTheme.shapes.small,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
        Text(text, Modifier.padding(CalafSpacing.medium), style = MaterialTheme.typography.bodyMedium)
    }
}
private fun CommunityError.label() = when (this) {
    CommunityError.OFFLINE -> R.string.offline; CommunityError.CONFIGURATION -> R.string.configuration
    CommunityError.PERMISSION -> R.string.permission; CommunityError.LIMIT -> R.string.limit
    CommunityError.INVALID -> R.string.invalid; CommunityError.SIGN_IN_AGAIN -> R.string.sign_in_again
    CommunityError.UNKNOWN -> R.string.unknown_error
}
private fun Notice.label() = when (this) {
    Notice.SAVED -> R.string.saved; Notice.MATCHED -> R.string.matched_notice; Notice.REPORTED -> R.string.reported_notice
    Notice.BLOCKED -> R.string.blocked_notice; Notice.UNMATCHED -> R.string.unmatched_notice; Notice.SENT -> R.string.sent_notice
    Notice.INVITATION_ACCEPTED -> R.string.accepted_notice; Notice.REVOKED -> R.string.revoked_notice; Notice.DELETION_REQUESTED -> R.string.deletion_notice
}
