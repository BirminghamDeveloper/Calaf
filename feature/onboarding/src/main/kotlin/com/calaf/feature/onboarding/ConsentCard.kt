package com.calaf.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme

/**
 * Explicit consent for UK GDPR special category data (religion and heritage). Off by default. Turning it off
 * deletes the details, which the ViewModel does when [onChange] reports false.
 */
@Composable
internal fun ConsentCard(granted: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
            Text(stringResource(R.string.onb_consent_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onb_consent_body), style = MaterialTheme.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)
                    .toggleable(granted, enabled = enabled, role = Role.Switch, onValueChange = onChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
            ) {
                Text(stringResource(R.string.onb_consent_switch), Modifier.weight(1f))
                Switch(granted, null, enabled = enabled)
            }
            Text(
                stringResource(if (granted) R.string.onb_consent_withdraw else R.string.onb_consent_off),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentCardPreview() {
    CalafTheme { ConsentCard(granted = false, enabled = true, onChange = {}) }
}
