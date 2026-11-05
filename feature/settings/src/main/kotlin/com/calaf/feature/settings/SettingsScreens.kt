package com.calaf.feature.settings

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.Match
import com.calaf.core.model.SubscriptionTier
import com.calaf.core.ui.ActionBanner
import com.calaf.core.ui.ActionState
import com.calaf.core.ui.CalafListScreen
import com.calaf.core.ui.ConfirmDialog
import com.calaf.core.ui.NoticeCard

/** The list of settings pages. */
@Composable
fun SettingsMenuScreen(onOpen: (SettingsPage) -> Unit) {
    CalafListScreen(stringResource(CoreR.string.settings), ActionState(), "settings_list") {
        items(SettingsPage.entries) { page ->
            OutlinedButton(onClick = { onOpen(page) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(page.label))
            }
        }
    }
}

/** One settings page. Gets its ViewModel from Hilt, so previews and tests use the page screens directly. */
@Composable
fun SettingsPageRoute(uid: String, page: SettingsPage, onOpenSharedChat: (String) -> Unit = {}) {
    when (page) {
        SettingsPage.GUARDIAN -> GuardianRoute(uid, onOpenSharedChat)
        SettingsPage.SAFETY -> StaticPage(R.string.safety, R.string.safety_body)
        SettingsPage.GUIDELINES -> StaticPage(R.string.guidelines, R.string.guidelines_body)
        SettingsPage.VERIFICATION -> StaticPage(R.string.verification, R.string.verification_explanation)
        SettingsPage.ABOUT -> AboutPage()
        else -> SettingsDataRoute(uid, page)
    }
}

@Composable
private fun GuardianRoute(uid: String, onOpenShared: (String) -> Unit) {
    val model = hiltViewModel<GuardianViewModel, GuardianViewModel.Factory>(
        key = "guardian_$uid",
        creationCallback = { it.create(uid) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    Column {
        ActionBanner(state.action)
        GuardianScreen(
            state, onInvite = model::invite, onAccept = model::accept, onRevoke = model::revoke,
            onOpenShared = onOpenShared,
        )
    }
}

@Composable
private fun SettingsDataRoute(uid: String, page: SettingsPage) {
    val model = hiltViewModel<SettingsViewModel, SettingsViewModel.Factory>(
        key = "settings_$uid",
        creationCallback = { it.create(uid) },
    )
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) {
        model.observe()
        onDispose { model.stop() }
    }
    val busy = state.action.busy
    when (page) {
        SettingsPage.PREFERENCES -> Column {
            ActionBanner(state.action)
            PreferencesScreen(state.preferences, busy, model::savePreferences)
        }
        SettingsPage.PRIVACY -> Column {
            ActionBanner(state.action)
            PrivacyScreen(state.settings, busy, model::savePrivacy)
        }
        SettingsPage.SECURITY -> SecurityPage(state, model::revokeSessions, model::requestDeletion, model::exportData)
        SettingsPage.PREMIUM -> PremiumPage(state)
        else -> BlockedPage(state)
    }
}

@Composable
private fun StaticPage(title: Int, body: Int) {
    CalafListScreen(stringResource(title), ActionState(), "settings_page") {
        item { Text(stringResource(body)) }
    }
}

@Composable
private fun AboutPage() {
    CalafListScreen(stringResource(R.string.about), ActionState(), "settings_page") {
        item { Text(stringResource(CoreR.string.purpose)) }
        item { Text(stringResource(R.string.credits)) }
    }
}

@Composable
private fun PremiumPage(state: SettingsUiState) {
    CalafListScreen(stringResource(R.string.premium), state.action, "settings_page") {
        item {
            Text(
                stringResource(
                    if (state.subscription.tier == SubscriptionTier.PREMIUM) {
                        R.string.premium_plan
                    } else {
                        R.string.free_plan
                    },
                ),
            )
        }
        item { Text(stringResource(R.string.premium_description)) }
        item { NoticeCard(stringResource(R.string.premium_unavailable)) }
    }
}

@Composable
private fun BlockedPage(state: SettingsUiState) {
    CalafListScreen(stringResource(R.string.blocked), state.action, "settings_page") {
        if (state.blocked.isEmpty()) item { Text(stringResource(R.string.no_blocks)) }
        items(state.blocked, key = Match::id) { Text(it.name.ifBlank { stringResource(CoreR.string.member) }) }
    }
}

private enum class SecurityAction { LOGOUT_ALL, DELETE }

@Composable
private fun SecurityPage(
    state: SettingsUiState,
    onLogoutAll: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    var confirming by remember { mutableStateOf<SecurityAction?>(null) }
    val busy = state.action.busy
    CalafListScreen(stringResource(R.string.security), state.action, "settings_page") {
        item {
            OutlinedButton(onClick = { confirming = SecurityAction.LOGOUT_ALL }, enabled = !busy) {
                Text(stringResource(R.string.logout_all))
            }
        }
        item { Text(stringResource(R.string.deletion_explanation)) }
        item {
            OutlinedButton(onClick = { confirming = SecurityAction.DELETE }, enabled = !busy) {
                Text(stringResource(R.string.deletion))
            }
        }
        item { Text(stringResource(R.string.export_scope)) }
        item { OutlinedButton(onClick = onExport, enabled = !busy) { Text(stringResource(R.string.export)) } }
        state.exportJson?.let { json -> item { SelectionContainer { Text(json) } } }
    }
    confirming?.let { action ->
        ConfirmDialog(
            enabled = !busy,
            onConfirm = {
                when (action) {
                    SecurityAction.LOGOUT_ALL -> onLogoutAll()
                    SecurityAction.DELETE -> onDelete()
                }
                confirming = null
            },
            onDismiss = { confirming = null },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuPreview() {
    CalafTheme { SettingsMenuScreen(onOpen = {}) }
}
