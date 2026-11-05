package com.calaf.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.Answer
import com.calaf.core.model.Gender
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileIssue
import com.calaf.core.model.Religiosity
import com.calaf.core.ui.StatusMessage
import com.calaf.core.ui.R as CoreR

/** The five step profile form: basics, about you, your life, faith and heritage (optional), review. */
@Composable
internal fun OnboardingForm(
    state: ProfileUiState,
    onEdit: (ProfileDraft) -> Unit,
    onSave: () -> Unit,
    onStep: (Int) -> Unit,
    onConsent: (Boolean) -> Unit,
) {
    val enabled = !state.saving
    LinearProgressIndicator(progress = { (state.step + 1f) / ONBOARDING_STEPS }, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            stringResource(R.string.onb_step, state.step + 1, ONBOARDING_STEPS),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(stringResource(R.string.onb_completion, state.completion), style = MaterialTheme.typography.labelLarge)
    }
    Text(
        stringResource(stepTitle(state.step)), style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    val draft = state.draft
    when (state.step) {
        0 -> BasicsStep(draft, enabled, onEdit)
        1 -> AboutStep(draft, enabled, onEdit)
        2 -> LifeStep(draft, enabled, onEdit)
        3 -> FaithStep(draft, enabled, onEdit, onConsent)
        else -> Text(stringResource(R.string.onb_review_intro))
    }
    state.issue?.let { issue -> StatusMessage(stringResource(issueMessage(issue)), error = true) }
    if (state.saved) Text(stringResource(R.string.draft_saved), style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        if (state.step > FIRST_STEP) {
            OutlinedButton(
                onClick = { onStep(state.step - 1) }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = CalafDimensions.touchTarget),
            ) { Text(stringResource(CoreR.string.back)) }
        }
        val last = state.step == LAST_STEP
        Button(
            onClick = { if (last) onSave() else onStep(state.step + 1) }, enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = CalafDimensions.touchTarget),
        ) { Text(stringResource(if (last) R.string.save_profile else R.string.onb_next)) }
    }
}

private fun stepTitle(step: Int) = when (step) {
    0 -> R.string.onb_title_basics
    1 -> R.string.onb_title_about
    2 -> R.string.onb_title_life
    3 -> R.string.onb_title_faith
    else -> R.string.onb_title_review
}

private fun issueMessage(issue: ProfileIssue) = when (issue) {
    ProfileIssue.Name -> R.string.error_name
    ProfileIssue.BirthDate -> R.string.error_birth
    ProfileIssue.Underage -> R.string.error_underage
    ProfileIssue.City -> R.string.error_city
    ProfileIssue.Country -> R.string.error_country
    ProfileIssue.Height -> R.string.error_height
    ProfileIssue.ConsentRequired -> R.string.error_consent
    ProfileIssue.Biography, ProfileIssue.Length -> R.string.error_profile_length
}

@Composable
private fun BasicsStep(draft: ProfileDraft, enabled: Boolean, onEdit: (ProfileDraft) -> Unit) {
    ProfileField(R.string.display_name, draft.name, { onEdit(draft.copy(name = it.take(60))) }, enabled)
    ProfileField(
        R.string.date_of_birth, draft.birthDate, { onEdit(draft.copy(birthDate = it.take(10))) }, enabled,
        hint = R.string.birth_hint, keyboardType = KeyboardType.Ascii,
    )
    ProfileChoice(R.string.profile_gender, draft.gender, Gender.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(gender = it))
    }
    ProfileField(R.string.city, draft.city, { onEdit(draft.copy(city = it.take(100))) }, enabled)
    ProfileField(R.string.country, draft.country, { onEdit(draft.copy(country = it.take(100))) }, enabled)
    ProfileField(
        R.string.onb_country_of_birth, draft.countryOfBirth, { onEdit(draft.copy(countryOfBirth = it.take(100))) },
        enabled,
    )
}

@Composable
private fun AboutStep(draft: ProfileDraft, enabled: Boolean, onEdit: (ProfileDraft) -> Unit) {
    ProfileField(
        R.string.biography, draft.biography, { onEdit(draft.copy(biography = it.take(500))) }, enabled,
        multiline = true,
    )
    ProfileField(R.string.languages, draft.languages, { onEdit(draft.copy(languages = it.take(200))) }, enabled)
    ProfileField(R.string.culture, draft.culture, { onEdit(draft.copy(culture = it.take(200))) }, enabled)
    ProfileField(
        R.string.profile_interests, draft.interests, { onEdit(draft.copy(interests = it.take(200))) }, enabled,
    )
    ProfileField(R.string.profile_values, draft.values, { onEdit(draft.copy(values = it.take(200))) }, enabled)
}

@Composable
private fun LifeStep(draft: ProfileDraft, enabled: Boolean, onEdit: (ProfileDraft) -> Unit) {
    ProfileChoice(R.string.onb_marital, draft.maritalStatus, MaritalStatus.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(maritalStatus = it))
    }
    ProfileChoice(
        R.string.profile_children, draft.children, listOf(Answer.UNSPECIFIED, Answer.YES, Answer.NO),
        { stringResource(it.label()) },
    ) { onEdit(draft.copy(children = it)) }
    ProfileChoice(R.string.profile_wants_children, draft.wantsChildren, Answer.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(wantsChildren = it))
    }
    OutlinedTextField(
        value = if (draft.heightCm == 0) "" else draft.heightCm.toString(),
        onValueChange = { text -> onEdit(draft.copy(heightCm = text.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
        enabled = enabled, label = { Text(stringResource(R.string.onb_height)) },
        supportingText = { Text(stringResource(R.string.onb_height_hint)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
    )
    ProfileField(
        R.string.profile_occupation, draft.occupation, { onEdit(draft.copy(occupation = it.take(100))) }, enabled,
    )
    ProfileField(
        R.string.profile_education, draft.education, { onEdit(draft.copy(education = it.take(100))) }, enabled,
    )
    ProfileChoice(
        R.string.profile_smoking, draft.smoking, listOf(Answer.UNSPECIFIED, Answer.YES, Answer.NO),
        { stringResource(it.label()) },
    ) { onEdit(draft.copy(smoking = it)) }
    ProfileChoice(R.string.profile_relocation, draft.relocation, Answer.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(relocation = it))
    }
    ProfileChoice(R.string.profile_timeline, draft.timeline, MarriageTimeline.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(timeline = it))
    }
}

@Composable
private fun FaithStep(
    draft: ProfileDraft,
    enabled: Boolean,
    onEdit: (ProfileDraft) -> Unit,
    onConsent: (Boolean) -> Unit,
) {
    ConsentCard(draft.sensitiveConsent, enabled, onConsent)
    if (!draft.sensitiveConsent) return
    ProfileField(R.string.profile_religion, draft.religion, { onEdit(draft.copy(religion = it.take(60))) }, enabled)
    ProfileChoice(R.string.onb_religiosity, draft.religiosity, Religiosity.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(religiosity = it))
    }
    ProfileChoice(R.string.onb_prays, draft.prays, PrayerHabit.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(prays = it))
    }
    ProfileChoice(R.string.onb_heritage, draft.heritage, Heritage.entries, { stringResource(it.label()) }) {
        onEdit(draft.copy(heritage = it))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun OnboardingFormPreview() {
    com.calaf.core.designsystem.CalafTheme {
        androidx.compose.foundation.layout.Column(
            Modifier.padding(CalafSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            OnboardingForm(
                ProfileUiState(loading = false, step = 3, draft = ProfileDraft(name = "Amina (Demo)")),
                onEdit = {}, onSave = {}, onStep = {}, onConsent = {},
            )
        }
    }
}
