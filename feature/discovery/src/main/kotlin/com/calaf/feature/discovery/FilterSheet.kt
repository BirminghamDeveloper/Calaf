package com.calaf.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.Answer
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.Religiosity
import com.calaf.core.ui.labelRes
import kotlin.math.roundToInt

/**
 * The filter sheet: age, marital status, children, height, faith, heritage, birthplace, work, education, marriage
 * timeline and relocation. Nothing is saved until [onApply]. Gender and country live in Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    current: PartnerPreference,
    busy: Boolean,
    onApply: (PartnerPreference) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CalafSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            Text(stringResource(R.string.disc_filters_title), style = MaterialTheme.typography.titleLarge)
            BasicFilters(draft) { draft = it }
            LifeFilters(draft) { draft = it }
            FaithFilters(draft) { draft = it }
            Text(stringResource(R.string.disc_filter_more), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
                OutlinedButton(
                    onClick = { draft = clearedFilters(draft) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.disc_filter_clear)) }
                Button(onClick = { onApply(draft) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.disc_filter_apply))
                }
            }
        }
    }
}

/** The preference with every filter in the sheet switched off. Gender and country stay: they are not in the sheet. */
internal fun clearedFilters(value: PartnerPreference) =
    PartnerPreference(gender = value.gender, country = value.country)

@Composable
private fun BasicFilters(draft: PartnerPreference, onChange: (PartnerPreference) -> Unit) {
    FilterGroup(R.string.disc_filters_title) {
        Text(stringResource(R.string.disc_filter_age, draft.minAge, draft.maxAge))
        RangeSlider(
            value = draft.minAge.toFloat()..draft.maxAge.toFloat(),
            onValueChange = { range ->
                onChange(draft.copy(minAge = range.start.roundToInt(), maxAge = range.endInclusive.roundToInt()))
            },
            valueRange = MIN_AGE.toFloat()..MAX_AGE.toFloat(),
        )
        OutlinedTextField(
            draft.language, { onChange(draft.copy(language = it.take(MAX_TEXT))) },
            label = { Text(stringResource(R.string.disc_filter_language)) }, modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().toggleable(
                draft.nonSmokerOnly, role = Role.Switch, onValueChange = { onChange(draft.copy(nonSmokerOnly = it)) },
            ).padding(vertical = CalafSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            Text(stringResource(R.string.disc_filter_nonsmoker), Modifier.weight(1f))
            Switch(draft.nonSmokerOnly, null)
        }
    }
}

@Composable
private fun LifeFilters(draft: PartnerPreference, onChange: (PartnerPreference) -> Unit) {
    FilterGroup(R.string.disc_filter_marital) {
        MultiChoice(
            MaritalStatus.entries.filter { it != MaritalStatus.UNSPECIFIED }, draft.maritalStatuses,
            { it.labelRes() ?: R.string.disc_filter_any },
        ) { onChange(draft.copy(maritalStatuses = it)) }
    }
    FilterGroup(R.string.disc_filter_has_children) {
        AnswerChoice(draft.hasChildren) { onChange(draft.copy(hasChildren = it)) }
    }
    FilterGroup(R.string.disc_filter_wants_children) {
        AnswerChoice(draft.wantsChildren, listOf(Answer.YES, Answer.NO, Answer.OPEN)) {
            onChange(draft.copy(wantsChildren = it))
        }
    }
    FilterGroup(R.string.disc_filter_height) {
        HeightRange(
            draft.minHeightCm, draft.maxHeightCm,
            { onChange(draft.copy(minHeightCm = it)) }, { onChange(draft.copy(maxHeightCm = it)) },
        )
    }
    FilterGroup(R.string.disc_filter_timeline) {
        MultiChoice(MarriageTimeline.entries, draft.timelines, { it.labelRes() }) {
            onChange(draft.copy(timelines = it))
        }
    }
    FilterGroup(R.string.disc_filter_relocate) {
        AnswerChoice(draft.willingToRelocate) { onChange(draft.copy(willingToRelocate = it)) }
    }
    FilterGroup(R.string.disc_filter_born) {
        OutlinedTextField(
            draft.countryOfBirth, { onChange(draft.copy(countryOfBirth = it.take(MAX_TEXT))) },
            label = { Text(stringResource(R.string.disc_filter_born)) }, modifier = Modifier.fillMaxWidth(),
        )
    }
    FilterGroup(R.string.disc_filter_profession) {
        OutlinedTextField(
            draft.profession, { onChange(draft.copy(profession = it.take(MAX_TEXT))) },
            label = { Text(stringResource(R.string.disc_filter_profession)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            draft.education, { onChange(draft.copy(education = it.take(MAX_TEXT))) },
            label = { Text(stringResource(R.string.disc_filter_education)) }, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FaithFilters(draft: PartnerPreference, onChange: (PartnerPreference) -> Unit) {
    FilterGroup(R.string.disc_filter_religiosity, note = R.string.disc_filter_faith_note) {
        MultiChoice(
            Religiosity.entries.filter { it != Religiosity.UNSPECIFIED }, draft.religiosity,
            { it.labelRes() ?: R.string.disc_filter_any },
        ) { onChange(draft.copy(religiosity = it)) }
    }
    FilterGroup(R.string.disc_filter_prays, note = R.string.disc_filter_faith_note) {
        MultiChoice(
            PrayerHabit.entries.filter { it != PrayerHabit.UNSPECIFIED }, draft.prays,
            { it.labelRes() ?: R.string.disc_filter_any },
        ) { onChange(draft.copy(prays = it)) }
    }
    FilterGroup(R.string.disc_filter_heritage, note = R.string.disc_filter_faith_note) {
        MultiChoice(
            Heritage.entries.filter { it != Heritage.UNSPECIFIED }, draft.heritage,
            { it.labelRes() ?: R.string.disc_filter_any },
        ) { onChange(draft.copy(heritage = it)) }
    }
}

private const val MIN_AGE = 18
private const val MAX_AGE = 80
private const val MAX_TEXT = 100

@Preview(showBackground = true)
@Composable
private fun FilterSheetPreview() {
    CalafTheme {
        Column(Modifier.padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
            BasicFilters(PartnerPreference(minAge = 25, maxAge = 40)) {}
            LifeFilters(PartnerPreference(maritalStatuses = setOf(MaritalStatus.NEVER_MARRIED))) {}
        }
    }
}
