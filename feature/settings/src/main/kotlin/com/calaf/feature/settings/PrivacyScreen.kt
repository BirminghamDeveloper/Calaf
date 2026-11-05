package com.calaf.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.PrivacySettings

@Composable
fun PrivacyScreen(current: PrivacySettings, busy: Boolean, onSave: (PrivacySettings) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium)) {
        Text(stringResource(R.string.privacy), style = MaterialTheme.typography.headlineMedium)
        Toggle(stringResource(R.string.visible), draft.visible, !busy) { draft = draft.copy(visible = it) }
        Toggle(stringResource(R.string.show_city), draft.showCity, !busy) { draft = draft.copy(showCity = it) }
        Toggle(stringResource(R.string.read_receipts), draft.readReceipts, !busy) {
            draft = draft.copy(readReceipts = it)
        }
        Toggle(stringResource(R.string.notifications), draft.notifications, !busy) {
            draft = draft.copy(notifications = it)
        }
        Toggle(stringResource(R.string.analytics), draft.analytics, !busy) { draft = draft.copy(analytics = it) }
        Toggle(stringResource(R.string.marketing), draft.marketing, !busy) { draft = draft.copy(marketing = it) }
        Button(onClick = { onSave(draft) }, enabled = !busy) { Text(stringResource(R.string.save)) }
    }
}

@Composable
internal fun Toggle(label: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, Modifier.weight(1f).padding(vertical = CalafSpacing.medium))
        Switch(value, null, enabled = enabled)
    }
}
