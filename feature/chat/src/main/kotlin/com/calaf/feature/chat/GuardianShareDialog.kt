package com.calaf.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing

/**
 * Opt in, for this one conversation, to let the member's guardian read it (read only). Off unless the member turns it
 * on. A member with no guardian yet can invite one from here.
 */
@Composable
internal fun GuardianShareDialog(
    shared: Boolean,
    hasGuardian: Boolean,
    code: String?,
    enabled: Boolean,
    onShare: (Boolean) -> Unit,
    onInvite: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_guardian_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
                Text(stringResource(R.string.chat_guardian_body))
                if (hasGuardian) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget).toggleable(
                            shared, enabled = enabled, role = Role.Switch, onValueChange = onShare,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
                    ) {
                        Text(stringResource(R.string.chat_guardian_switch), Modifier.weight(1f))
                        Switch(shared, null, enabled = enabled)
                    }
                } else {
                    Text(stringResource(R.string.chat_guardian_none))
                    OutlinedButton(onClick = onInvite, enabled = enabled) {
                        Text(stringResource(R.string.chat_guardian_invite))
                    }
                    if (code != null) {
                        Text(stringResource(R.string.chat_guardian_code))
                        SelectionContainer { Text(code) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_guardian_close)) } },
    )
}
