package com.calaf.feature.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.*

@Composable internal fun <T> ChoiceField(label: String, value: T, values: List<T>, display: @Composable (T) -> String, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { open = true }) { Text(display(value)) }
            DropdownMenu(open, { open = false }) {
                values.forEach { item -> DropdownMenuItem(text = { Text(display(item)) }, onClick = { onChange(item); open = false }) }
            }
        }
    }
}

@Composable internal fun PreferencesScreen(current: PartnerPreference, busy: Boolean, onSave: (PartnerPreference) -> Unit) {
    var min by rememberSaveable(current) { mutableStateOf(current.minAge.toString()) }
    var max by rememberSaveable(current) { mutableStateOf(current.maxAge.toString()) }
    var country by rememberSaveable(current) { mutableStateOf(current.country) }
    var language by rememberSaveable(current) { mutableStateOf(current.language) }
    var gender by rememberSaveable(current) { mutableStateOf(current.gender) }
    var nonSmoker by rememberSaveable(current) { mutableStateOf(current.nonSmokerOnly) }
    var wantsChildren by rememberSaveable(current) { mutableStateOf(current.wantsChildren) }
    val valid = (min.toIntOrNull() ?: 0) >= 18 && (max.toIntOrNull() ?: 999) <= 120 && (min.toIntOrNull() ?: 999) <= (max.toIntOrNull() ?: 0)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
        Text(stringResource(R.string.preferences), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(min, { min = it.take(3) }, label = { Text(stringResource(R.string.min_age)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(max, { max = it.take(3) }, label = { Text(stringResource(R.string.max_age)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ChoiceField(stringResource(R.string.gender), gender, listOf(null, Gender.WOMAN, Gender.MAN), {
            stringResource(when (it) { Gender.WOMAN -> R.string.woman; Gender.MAN -> R.string.man; else -> R.string.any })
        }) { gender = it }
        OutlinedTextField(country, { country = it.take(100) }, label = { Text(stringResource(R.string.country)) })
        OutlinedTextField(language, { language = it.take(60) }, label = { Text(stringResource(R.string.language)) })
        Toggle(stringResource(R.string.non_smoker), nonSmoker, !busy) { nonSmoker = it }
        ChoiceField(stringResource(R.string.preference_children), wantsChildren, listOf(null, Answer.YES, Answer.NO, Answer.OPEN), {
            stringResource(when (it) { Answer.YES -> R.string.preference_yes; Answer.NO -> R.string.preference_no;
                Answer.OPEN -> R.string.preference_open; else -> R.string.any })
        }) { wantsChildren = it }
        if (!valid) Text(stringResource(R.string.invalid_age), color = MaterialTheme.colorScheme.error)
        Button(onClick = { onSave(current.copy(minAge = min.toInt(), maxAge = max.toInt(), gender = gender, country = country, language = language, nonSmokerOnly = nonSmoker, wantsChildren = wantsChildren)) },
            enabled = valid && !busy) { Text(stringResource(R.string.save)) }
    }
}

@Composable internal fun PrivacyScreen(current: PrivacySettings, busy: Boolean, onSave: (PrivacySettings) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium)) {
        Text(stringResource(R.string.privacy), style = MaterialTheme.typography.headlineMedium)
        Toggle(stringResource(R.string.visible), draft.visible, !busy) { draft = draft.copy(visible = it) }
        Toggle(stringResource(R.string.show_city), draft.showCity, !busy) { draft = draft.copy(showCity = it) }
        Toggle(stringResource(R.string.read_receipts), draft.readReceipts, !busy) { draft = draft.copy(readReceipts = it) }
        Toggle(stringResource(R.string.notifications), draft.notifications, !busy) { draft = draft.copy(notifications = it) }
        Toggle(stringResource(R.string.analytics), draft.analytics, !busy) { draft = draft.copy(analytics = it) }
        Toggle(stringResource(R.string.marketing), draft.marketing, !busy) { draft = draft.copy(marketing = it) }
        Button(onClick = { onSave(draft) }, enabled = !busy) { Text(stringResource(R.string.save)) }
    }
}

@Composable private fun Toggle(label: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value, enabled = enabled, role = Role.Switch, onValueChange = onChange), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(vertical = CalafSpacing.medium))
        Switch(value, null, enabled = enabled)
    }
}

@Composable internal fun GuardianScreen(state: CommunityUiState, onAction: (CommunityCommand) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
        Text(stringResource(R.string.guardian), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.guardian_explanation))
        Button(onClick = { onAction(CommunityCommand.InviteGuardian) }, enabled = !state.busy) { Text(stringResource(R.string.invite_guardian)) }
        state.invitationCode?.let { invitation ->
            Text(stringResource(R.string.invitation_help))
            SelectionContainer { Text(invitation) }
        }
        OutlinedTextField(code, { code = it.trim().take(48) }, label = { Text(stringResource(R.string.invitation_code)) })
        OutlinedButton(onClick = { onAction(CommunityCommand.AcceptGuardian(code)) }, enabled = code.length == 48 && !state.busy) { Text(stringResource(R.string.accept_invitation)) }
        if (state.guardians.isEmpty()) Text(stringResource(R.string.no_guardians))
        state.guardians.forEach { guardian ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(CalafSpacing.medium)) {
                Text(stringResource(when (guardian.status) { GuardianStatus.INVITED -> R.string.invited; GuardianStatus.ACCEPTED -> R.string.accepted; GuardianStatus.REVOKED -> R.string.revoked }))
                if (guardian.status != GuardianStatus.REVOKED) TextButton(onClick = { onAction(CommunityCommand.RevokeGuardian(guardian.id)) }, enabled = !state.busy) { Text(stringResource(R.string.revoke)) }
            } }
        }
    }
}
