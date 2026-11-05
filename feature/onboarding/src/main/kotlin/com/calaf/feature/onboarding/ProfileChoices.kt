package com.calaf.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.calaf.core.model.*

@Composable internal fun <T> ProfileChoice(label: Int, value: T, options: List<T>, display: @Composable (T) -> String, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(display(value)) }
            DropdownMenu(expanded, { expanded = false }) { options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = { onChange(option); expanded = false })
            } }
        }
    }
}
internal fun Gender.label() = when (this) { Gender.WOMAN -> R.string.choice_woman; Gender.MAN -> R.string.choice_man; else -> R.string.choice_unspecified }
internal fun Answer.label() = when (this) { Answer.YES -> R.string.choice_yes; Answer.NO -> R.string.choice_no; Answer.OPEN -> R.string.choice_open; else -> R.string.choice_unspecified }
internal fun MarriageTimeline.label() = when (this) { MarriageTimeline.WHEN_READY -> R.string.choice_when_ready; MarriageTimeline.WITHIN_YEAR -> R.string.choice_within_year; MarriageTimeline.ONE_TO_TWO_YEARS -> R.string.choice_two_years }

internal fun MaritalStatus.label() = when (this) {
    MaritalStatus.NEVER_MARRIED -> R.string.onb_marital_never
    MaritalStatus.DIVORCED -> R.string.onb_marital_divorced
    MaritalStatus.WIDOWED -> R.string.onb_marital_widowed
    MaritalStatus.UNSPECIFIED -> R.string.choice_unspecified
}

internal fun Religiosity.label() = when (this) {
    Religiosity.VERY_PRACTISING -> R.string.onb_relig_very
    Religiosity.PRACTISING -> R.string.onb_relig_practising
    Religiosity.MODERATELY -> R.string.onb_relig_moderately
    Religiosity.NOT_PRACTISING -> R.string.onb_relig_not
    Religiosity.UNSPECIFIED -> R.string.choice_unspecified
}

internal fun PrayerHabit.label() = when (this) {
    PrayerHabit.ALWAYS -> R.string.onb_prays_always
    PrayerHabit.USUALLY -> R.string.onb_prays_usually
    PrayerHabit.SOMETIMES -> R.string.onb_prays_sometimes
    PrayerHabit.NEVER -> R.string.onb_prays_never
    PrayerHabit.UNSPECIFIED -> R.string.choice_unspecified
}

internal fun Heritage.label() = when (this) {
    Heritage.SOMALI -> R.string.onb_heritage_somali
    Heritage.SOMALILAND -> R.string.onb_heritage_somaliland
    Heritage.ETHIOPIAN -> R.string.onb_heritage_ethiopian
    Heritage.ERITREAN -> R.string.onb_heritage_eritrean
    Heritage.DJIBOUTIAN -> R.string.onb_heritage_djiboutian
    Heritage.KENYAN -> R.string.onb_heritage_kenyan
    Heritage.SUDANESE -> R.string.onb_heritage_sudanese
    Heritage.OTHER -> R.string.onb_heritage_other
    Heritage.UNSPECIFIED -> R.string.choice_unspecified
}
