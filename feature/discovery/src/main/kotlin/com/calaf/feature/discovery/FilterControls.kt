package com.calaf.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.Answer
import com.calaf.core.ui.labelRes

/** A titled filter group. The title is a heading for screen readers. */
@Composable
internal fun FilterGroup(title: Int, note: Int? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        Text(
            stringResource(title), style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (note != null) Text(stringResource(note), style = MaterialTheme.typography.bodySmall)
        content()
    }
}

/** Pick any number of [options]. Nothing picked means no filter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> MultiChoice(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> Int,
    onChange: (Set<T>) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onChange(if (option in selected) selected - option else selected + option) },
                label = { Text(stringResource(label(option))) },
            )
        }
    }
}

/** Any, Yes or No for an [Answer] filter. Null is Any. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnswerChoice(
    selected: Answer?,
    options: List<Answer> = listOf(Answer.YES, Answer.NO),
    onChange: (Answer?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        FilterChip(
            selected = selected == null, onClick = { onChange(null) },
            label = { Text(stringResource(R.string.disc_filter_any)) },
        )
        options.forEach { option ->
            val label = option.labelRes() ?: return@forEach
            FilterChip(
                selected = selected == option, onClick = { onChange(option) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

/** A whole number of centimetres, or empty for any. Stored as 0 when empty. */
@Composable
internal fun HeightField(label: Int, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(MAX_HEIGHT_DIGITS).toIntOrNull() ?: 0) },
        label = { Text(stringResource(label)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}

@Composable
internal fun HeightRange(min: Int, max: Int, onMin: (Int) -> Unit, onMax: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        HeightField(R.string.disc_filter_min, min, onMin, Modifier.weight(1f))
        HeightField(R.string.disc_filter_max, max, onMax, Modifier.weight(1f))
    }
}

private const val MAX_HEIGHT_DIGITS = 3
