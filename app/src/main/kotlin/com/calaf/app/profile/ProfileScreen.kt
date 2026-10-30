package com.calaf.app.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.app.R
import com.calaf.app.ui.*
import com.calaf.core.model.*

@Composable
fun ProfileScreen(state: ProfileUiState, onEdit: (ProfileDraft) -> Unit, onSave: () -> Unit,
    onEditAgain: () -> Unit, onRetry: () -> Unit, onLogout: () -> Unit, onPublish: (() -> Unit)? = null,
    photos: (@Composable () -> Unit)? = null) {
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
            OutlinedButton(onClick = onEditAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_profile)) }
            if (onPublish != null) Button(onClick = onPublish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.publish_profile)) }
        } else {
            ProfileField(R.string.display_name, draft.name, { onEdit(draft.copy(name = it.take(60))) }, !state.saving)
            ProfileField(R.string.date_of_birth, draft.birthDate, { onEdit(draft.copy(birthDate = it.take(10))) }, !state.saving,
                hint = R.string.birth_hint, keyboardType = KeyboardType.Ascii)
            ProfileField(R.string.city, draft.city, { onEdit(draft.copy(city = it.take(100))) }, !state.saving)
            ProfileField(R.string.country, draft.country, { onEdit(draft.copy(country = it.take(100))) }, !state.saving)
            ProfileField(R.string.languages, draft.languages, { onEdit(draft.copy(languages = it.take(200))) }, !state.saving)
            ProfileField(R.string.culture, draft.culture, { onEdit(draft.copy(culture = it.take(200))) }, !state.saving)
            ProfileField(R.string.biography, draft.biography, { onEdit(draft.copy(biography = it.take(500))) }, !state.saving, multiline = true)
            ProfileChoice(R.string.profile_gender, draft.gender, Gender.entries, { stringResource(it.label()) }) { onEdit(draft.copy(gender = it)) }
            ProfileField(R.string.profile_religion, draft.religion, { onEdit(draft.copy(religion = it.take(60))) }, !state.saving)
            ProfileField(R.string.profile_occupation, draft.occupation, { onEdit(draft.copy(occupation = it.take(100))) }, !state.saving)
            ProfileField(R.string.profile_education, draft.education, { onEdit(draft.copy(education = it.take(100))) }, !state.saving)
            ProfileField(R.string.profile_interests, draft.interests, { onEdit(draft.copy(interests = it.take(200))) }, !state.saving)
            ProfileField(R.string.profile_values, draft.values, { onEdit(draft.copy(values = it.take(200))) }, !state.saving)
            ProfileChoice(R.string.profile_children, draft.children, listOf(Answer.UNSPECIFIED, Answer.YES, Answer.NO), { stringResource(it.label()) }) { onEdit(draft.copy(children = it)) }
            ProfileChoice(R.string.profile_wants_children, draft.wantsChildren, Answer.entries, { stringResource(it.label()) }) { onEdit(draft.copy(wantsChildren = it)) }
            ProfileChoice(R.string.profile_smoking, draft.smoking, listOf(Answer.UNSPECIFIED, Answer.YES, Answer.NO), { stringResource(it.label()) }) { onEdit(draft.copy(smoking = it)) }
            ProfileChoice(R.string.profile_relocation, draft.relocation, Answer.entries, { stringResource(it.label()) }) { onEdit(draft.copy(relocation = it)) }
            ProfileChoice(R.string.profile_timeline, draft.timeline, MarriageTimeline.entries, { stringResource(it.label()) }) { onEdit(draft.copy(timeline = it)) }
            state.issue?.let { issue ->
                StatusMessage(stringResource(when (issue) {
                    ProfileIssue.Name -> R.string.error_name
                    ProfileIssue.BirthDate -> R.string.error_birth
                    ProfileIssue.Underage -> R.string.error_underage
                    ProfileIssue.City -> R.string.error_city
                    ProfileIssue.Country -> R.string.error_country
                    else -> R.string.error_profile_length
                }), error = true)
            }
            if (state.saved) Text(stringResource(R.string.draft_saved), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_profile)) }
        }
        if (state.storageError) {
            StatusMessage(stringResource(R.string.error_storage), error = true)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.reload_profile)) }
        }
        photos?.invoke()
        TextButton(onClick = onLogout, enabled = !state.saving) { Text(stringResource(R.string.logout)) }
    }
}

@Composable
private fun ProfileField(label: Int, value: String, onChange: (String) -> Unit, enabled: Boolean,
    hint: Int? = null, keyboardType: KeyboardType = KeyboardType.Text, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, enabled = enabled, label = { Text(stringResource(label)) },
        supportingText = if (hint != null) { { Text(stringResource(hint)) } } else null,
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth())
}
