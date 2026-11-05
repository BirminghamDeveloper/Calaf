package com.calaf.feature.onboarding

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.core.ui.*
import com.calaf.core.model.*

@Composable
fun ProfileScreen(state: ProfileUiState, onEdit: (ProfileDraft) -> Unit, onSave: () -> Unit,
    onEditAgain: () -> Unit, onRetry: () -> Unit, onLogout: () -> Unit, onPublish: (() -> Unit)? = null,
    photos: (@Composable () -> Unit)? = null, onStep: (Int) -> Unit = {}, onConsent: (Boolean) -> Unit = {}) {
    FormPage(stringResource(if (state.reviewing) R.string.your_profile else R.string.profile_title),
        stringResource(R.string.profile_private)) {
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@FormPage
        }
        val draft = state.draft
        if (state.reviewing) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.calaf.core.designsystem.CalafSpacing.large), verticalArrangement = Arrangement.spacedBy(com.calaf.core.designsystem.CalafSpacing.medium)) {
                    Text(draft.name, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.profile_location, draft.city, draft.country))
                    if (draft.languages.isNotBlank()) Text(stringResource(R.string.profile_languages, draft.languages))
                    if (draft.culture.isNotBlank()) Text(stringResource(R.string.profile_culture, draft.culture))
                    if (draft.biography.isNotBlank()) Text(draft.biography)
                }
            }
            StatusMessage(stringResource(R.string.profile_saved))
            Text(stringResource(R.string.onb_completion, state.completion), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onEditAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_profile)) }
            if (onPublish != null) Button(onClick = onPublish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.publish_profile)) }
        } else {
            OnboardingForm(state, onEdit, onSave, onStep, onConsent)
        }
        if (state.storageError) {
            StatusMessage(stringResource(R.string.error_storage), error = true)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.reload_profile)) }
        }
        photos?.invoke()
        TextButton(onClick = onLogout, enabled = !state.saving) { Text(stringResource(CoreR.string.logout)) }
    }
}

@Composable
internal fun ProfileField(label: Int, value: String, onChange: (String) -> Unit, enabled: Boolean,
    hint: Int? = null, keyboardType: KeyboardType = KeyboardType.Text, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, enabled = enabled, label = { Text(stringResource(label)) },
        supportingText = if (hint != null) { { Text(stringResource(hint)) } } else null,
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth())
}
