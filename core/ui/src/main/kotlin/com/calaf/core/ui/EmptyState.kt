package com.calaf.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.calaf.core.designsystem.CalafSpacing

/** A friendly picture and a sentence for a list or screen that has nothing to show yet. */
@Composable
fun EmptyState(kind: IllustrationKind, message: String, modifier: Modifier = Modifier, title: String? = null) {
    Column(
        modifier.fillMaxWidth().padding(CalafSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
    ) {
        Illustration(kind)
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        Text(
            message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
