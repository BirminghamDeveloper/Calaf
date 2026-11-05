package com.calaf.feature.discovery

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CompatibilityArea
import com.calaf.core.model.VerificationStatus

@Composable
internal fun CandidateCard(
    profile: CommunityProfile,
    busy: Boolean,
    onInterest: (Boolean) -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CalafSpacing.large), verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
            Text(
                stringResource(R.string.candidate_title, profile.name, profile.age),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(stringResource(R.string.location, profile.city, profile.country))
            if (profile.verification == VerificationStatus.EMAIL_ONLY) {
                Text(stringResource(R.string.email_verified), style = MaterialTheme.typography.labelMedium)
            }
            Text(profile.biography)
            if (profile.culture.isNotBlank()) Text(profile.culture)
            if (profile.languages.isNotEmpty()) Text(profile.languages.joinToString())
            val score = profile.compatibility
            val percentage = score.score
            Text(
                if (percentage == null) {
                    stringResource(R.string.compatibility_unknown)
                } else {
                    stringResource(R.string.compatibility, percentage)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.compatibility_explanation), style = MaterialTheme.typography.bodySmall)
            if (score.strong.isNotEmpty()) {
                val areas = score.strong.map { stringResource(it.labelRes()) }.joinToString()
                Text(stringResource(R.string.strong_areas, areas))
            }
            if (score.differences.isNotEmpty()) {
                val areas = score.differences.map { stringResource(it.labelRes()) }.joinToString()
                Text(stringResource(R.string.differences, areas))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
                OutlinedButton(onClick = { onInterest(false) }, enabled = !busy) { Text(stringResource(R.string.pass)) }
                Button(onClick = { onInterest(true) }, enabled = !busy) { Text(stringResource(R.string.interest)) }
            }
            Row {
                TextButton(onClick = onReport, enabled = !busy) { Text(stringResource(CoreR.string.report)) }
                TextButton(onClick = onBlock, enabled = !busy) { Text(stringResource(CoreR.string.block)) }
            }
        }
    }
}

private fun CompatibilityArea.labelRes() = when (this) {
    CompatibilityArea.TIMELINE -> R.string.area_timeline
    CompatibilityArea.WANTS_CHILDREN -> R.string.area_children
    CompatibilityArea.SMOKING -> R.string.area_smoking
    CompatibilityArea.RELOCATION -> R.string.area_relocation
    CompatibilityArea.RELIGION -> R.string.area_religion
    CompatibilityArea.LANGUAGES -> R.string.area_languages
    CompatibilityArea.VALUES -> R.string.area_values
    CompatibilityArea.INTERESTS -> R.string.area_interests
}
