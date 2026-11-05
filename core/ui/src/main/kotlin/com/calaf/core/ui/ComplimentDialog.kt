package com.calaf.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.Compliment
import com.calaf.core.model.Compliments
import com.calaf.core.model.MessageSafety
import com.calaf.core.model.SafetyWarning

/** Write a compliment to send with a like. Shows the safety warnings the receiver would see, as you type. */
@Composable
fun ComplimentDialog(recipientName: String, enabled: Boolean, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    val warnings = MessageSafety.warnings(note)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cmp_title, recipientName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
                OutlinedTextField(
                    note, { note = it.take(Compliments.MAX_LENGTH) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cmp_hint)) },
                    supportingText = {
                        Text(stringResource(R.string.cmp_counter, note.length, Compliments.MAX_LENGTH))
                    },
                    minLines = 3,
                )
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    warnings.forEach { warning ->
                        Text(stringResource(warning.sendingMessage()), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { Compliments.clean(note)?.let(onSend) },
                enabled = enabled && Compliments.clean(note) != null,
            ) {
                Text(stringResource(R.string.cmp_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cmp_cancel)) } },
    )
}

private fun SafetyWarning.sendingMessage() = when (this) {
    SafetyWarning.LINK -> R.string.cmp_warning_link
    SafetyWarning.PERSONAL_INFORMATION -> R.string.cmp_warning_personal
    SafetyWarning.MONEY_REQUEST -> R.string.cmp_warning_money
}

/** A compliment on the receiver's card, with a warning when the note is flagged. */
@Composable
fun ComplimentBanner(fromName: String, compliment: Compliment, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(CalafSpacing.small)) {
            Text(stringResource(R.string.cmp_received, fromName), style = MaterialTheme.typography.labelLarge)
            Text(compliment.note, style = MaterialTheme.typography.bodyMedium)
            if (compliment.warnings.isNotEmpty()) {
                Text(
                    stringResource(R.string.cmp_note_with_warning), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
