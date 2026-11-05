package com.calaf.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.Guardian
import com.calaf.core.model.GuardianStatus

@Composable
fun GuardianScreen(
    state: GuardianUiState,
    onInvite: () -> Unit,
    onAccept: (String) -> Unit,
    onRevoke: (String) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val busy = state.action.busy
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CalafSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
    ) {
        Text(stringResource(R.string.guardian), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.guardian_explanation))
        Button(onClick = onInvite, enabled = !busy) { Text(stringResource(R.string.invite_guardian)) }
        state.invitationCode?.let { invitation ->
            Text(stringResource(R.string.invitation_help))
            SelectionContainer { Text(invitation) }
        }
        OutlinedTextField(
            code, { code = it.trim().take(CODE_LENGTH) },
            label = { Text(stringResource(R.string.invitation_code)) },
        )
        OutlinedButton(onClick = { onAccept(code) }, enabled = code.length == CODE_LENGTH && !busy) {
            Text(stringResource(R.string.accept_invitation))
        }
        if (state.guardians.isEmpty()) Text(stringResource(R.string.no_guardians))
        state.guardians.forEach { guardian ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(CalafSpacing.medium)) {
                    Text(
                        stringResource(
                            when (guardian.status) {
                                GuardianStatus.INVITED -> R.string.invited
                                GuardianStatus.ACCEPTED -> R.string.accepted
                                GuardianStatus.REVOKED -> R.string.revoked
                            },
                        ),
                    )
                    if (guardian.status != GuardianStatus.REVOKED) {
                        TextButton(onClick = { onRevoke(guardian.id) }, enabled = !busy) {
                            Text(stringResource(R.string.revoke))
                        }
                    }
                }
            }
        }
    }
}

private const val CODE_LENGTH = 48

@Preview(showBackground = true)
@Composable
private fun GuardianScreenPreview() {
    CalafTheme {
        GuardianScreen(
            GuardianUiState(listOf(Guardian("g1", "me", "wali", GuardianStatus.ACCEPTED))),
            onInvite = {}, onAccept = {}, onRevoke = {},
        )
    }
}
