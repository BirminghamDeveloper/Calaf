package com.calaf.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.calaf.core.model.ReportCategory

@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (ReportCategory, String) -> Unit) {
    var category by rememberSaveable { mutableStateOf(ReportCategory.OTHER) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ChoiceField(
                    stringResource(R.string.report_category), category, ReportCategory.entries,
                    { stringResource(it.labelRes()) },
                ) { category = it }
                OutlinedTextField(
                    details, { details = it.take(MAX_DETAILS) },
                    label = { Text(stringResource(R.string.report_details)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(category, details) }) { Text(stringResource(R.string.report)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks before a risky action such as blocking, unmatching or deleting an account. */
@Composable
fun ConfirmDialog(enabled: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm)) },
        text = { Text(stringResource(R.string.confirm_security)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val MAX_DETAILS = 2000
