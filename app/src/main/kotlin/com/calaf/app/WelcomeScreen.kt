package com.calaf.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing

@Composable
internal fun WelcomeScreen(onRegister: () -> Unit, onLogin: () -> Unit) {
    var showSafety by rememberSaveable { mutableStateOf(false) }
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = CalafDimensions.contentWidth).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(CalafSpacing.large),
                verticalArrangement = Arrangement.spacedBy(CalafSpacing.large),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(CalafSpacing.medium))
                Text(stringResource(R.string.brand_caption), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)) {
                    Text(stringResource(R.string.create_account))
                }
                OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)) {
                    Text(stringResource(R.string.login))
                }
                ElevatedCard(colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )) {
                    Column(Modifier.padding(CalafSpacing.large), verticalArrangement = Arrangement.spacedBy(CalafSpacing.large)) {
                        ValueStatement(R.string.intention_title, R.string.intention_body)
                        ValueStatement(R.string.culture_title, R.string.culture_body)
                        ValueStatement(R.string.privacy_title, R.string.privacy_body)
                    }
                }
                OutlinedButton(onClick = { showSafety = true }, modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)) {
                    Text(stringResource(R.string.safety_action))
                }
                Text(stringResource(R.string.adult_notice), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (showSafety) {
        AlertDialog(
            onDismissRequest = { showSafety = false },
            title = { Text(stringResource(R.string.safety_title)) },
            text = { Text(stringResource(R.string.safety_body), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showSafety = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun ValueStatement(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}
