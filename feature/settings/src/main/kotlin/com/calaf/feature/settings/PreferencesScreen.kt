package com.calaf.feature.settings

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.Answer
import com.calaf.core.model.Gender
import com.calaf.core.model.PartnerPreference
import com.calaf.core.ui.ChoiceField

@Composable
fun PreferencesScreen(current: PartnerPreference, busy: Boolean, onSave: (PartnerPreference) -> Unit) {
    var min by rememberSaveable(current) { mutableStateOf(current.minAge.toString()) }
    var max by rememberSaveable(current) { mutableStateOf(current.maxAge.toString()) }
    var country by rememberSaveable(current) { mutableStateOf(current.country) }
    var language by rememberSaveable(current) { mutableStateOf(current.language) }
    var gender by rememberSaveable(current) { mutableStateOf(current.gender) }
    var nonSmoker by rememberSaveable(current) { mutableStateOf(current.nonSmokerOnly) }
    var wantsChildren by rememberSaveable(current) { mutableStateOf(current.wantsChildren) }
    val minAge = min.toIntOrNull()
    val maxAge = max.toIntOrNull()
    val valid = (minAge ?: 0) >= MIN_ADULT_AGE && (maxAge ?: Int.MAX_VALUE) <= MAX_AGE &&
        (minAge ?: Int.MAX_VALUE) <= (maxAge ?: 0)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
    ) {
        Text(stringResource(CoreR.string.preferences), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            min, { min = it.take(3) }, label = { Text(stringResource(R.string.min_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            max, { max = it.take(3) }, label = { Text(stringResource(R.string.max_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        ChoiceField(
            stringResource(R.string.gender), gender, listOf(null, Gender.WOMAN, Gender.MAN),
            {
                stringResource(
                    when (it) {
                        Gender.WOMAN -> R.string.woman
                        Gender.MAN -> R.string.man
                        else -> R.string.any
                    },
                )
            },
        ) { gender = it }
        OutlinedTextField(country, { country = it.take(100) }, label = { Text(stringResource(R.string.country)) })
        OutlinedTextField(language, { language = it.take(60) }, label = { Text(stringResource(R.string.language)) })
        Toggle(stringResource(R.string.non_smoker), nonSmoker, !busy) { nonSmoker = it }
        ChoiceField(
            stringResource(R.string.preference_children), wantsChildren,
            listOf(null, Answer.YES, Answer.NO, Answer.OPEN),
            {
                stringResource(
                    when (it) {
                        Answer.YES -> R.string.preference_yes
                        Answer.NO -> R.string.preference_no
                        Answer.OPEN -> R.string.preference_open
                        else -> R.string.any
                    },
                )
            },
        ) { wantsChildren = it }
        if (!valid) Text(stringResource(R.string.invalid_age), color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                onSave(
                    current.copy(
                        minAge = min.toInt(), maxAge = max.toInt(), gender = gender, country = country,
                        language = language, nonSmokerOnly = nonSmoker, wantsChildren = wantsChildren,
                    ),
                )
            },
            enabled = valid && !busy,
        ) { Text(stringResource(R.string.save)) }
    }
}

private const val MIN_ADULT_AGE = 18
private const val MAX_AGE = 120
