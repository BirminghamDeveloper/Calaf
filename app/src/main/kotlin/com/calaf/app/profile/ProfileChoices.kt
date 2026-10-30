package com.calaf.app.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.calaf.app.R
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
