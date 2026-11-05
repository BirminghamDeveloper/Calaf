package com.calaf.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.calaf.core.designsystem.CalafSpacing

/**
 * Progress, the last error (with a retry button when [onRetry] is given) and the last confirmation.
 * Shown at the top of every screen that runs actions.
 */
@Composable
fun ActionBanner(action: ActionState, working: Boolean = false, onRetry: (() -> Unit)? = null) {
    if (action.busy || working) LinearProgressIndicator(Modifier.fillMaxWidth())
    action.error?.let { error ->
        NoticeCard(stringResource(error.labelRes()), error = true)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_connection)) }
    }
    action.notice?.let { NoticeCard(stringResource(it.labelRes())) }
}

@Composable
fun NoticeCard(text: String, error: Boolean = false) {
    Surface(
        Modifier.fillMaxWidth().padding(CalafSpacing.small),
        shape = MaterialTheme.shapes.small,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(text, Modifier.padding(CalafSpacing.medium), style = MaterialTheme.typography.bodyMedium)
    }
}
