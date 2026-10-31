package com.calaf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import com.calaf.app.R
import com.calaf.core.designsystem.*
import com.calaf.core.model.AccountError

@Composable
fun FormPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = CalafDimensions.contentWidth).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(CalafSpacing.large),
                verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.intro_tagline), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
            }
        }
    }
}

@Composable
fun StatusMessage(message: String, error: Boolean = false) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(message, Modifier.padding(CalafSpacing.medium))
    }
}

@Composable
fun AccountErrorMessage(error: AccountError?) {
    if (error == null) return
    StatusMessage(stringResource(when (error) {
        AccountError.InvalidInput -> R.string.error_input
        AccountError.Credentials -> R.string.error_credentials
        AccountError.Network -> R.string.error_network
        AccountError.LocalBackend -> R.string.error_local_backend
        AccountError.EmailInUse -> R.string.error_email_in_use
        AccountError.RateLimited -> R.string.error_rate_limit
        AccountError.WeakPassword -> R.string.error_password
        else -> R.string.error_service
    }), error = true)
}
