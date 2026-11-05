package com.calaf.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.calaf.core.designsystem.CalafSpacing

/** The "Reset demo data" row at the end of the settings menu, with a confirmation first. */
@Composable
fun DemoResetSection(onReset: () -> Unit) {
    var asking by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.padding(top = CalafSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
    ) {
        OutlinedButton(onClick = { asking = true }, Modifier.fillMaxWidth().testTag("demo_reset")) {
            Text(stringResource(R.string.demo_reset))
        }
        Text(
            stringResource(R.string.demo_reset_note), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.demo_reset_title)) },
            text = { Text(stringResource(R.string.demo_reset_body)) },
            confirmButton = {
                TextButton(onClick = { asking = false; onReset() }, Modifier.testTag("demo_reset_confirm")) {
                    Text(stringResource(R.string.demo_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { asking = false }) { Text(stringResource(R.string.demo_reset_cancel)) }
            },
        )
    }
}
