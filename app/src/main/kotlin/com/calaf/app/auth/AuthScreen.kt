package com.calaf.app.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.*
import com.calaf.app.R
import com.calaf.app.BuildConfig
import com.calaf.app.ui.*
import com.calaf.core.designsystem.CalafDimensions

@Composable
fun AuthScreen(state: AuthUiState, register: Boolean, onSubmit: (String, String, Boolean) -> Unit,
    onReset: (String) -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately never enter saved instance state or disk storage.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var adult by rememberSaveable { mutableStateOf(false) }
    FormPage(stringResource(if (register) R.string.create_account else R.string.login),
        stringResource(if (register) R.string.register_subtitle else R.string.login_subtitle)) {
        OutlinedTextField(email, { email = it.take(254) }, label = { Text(stringResource(R.string.email)) },
            singleLine = true, enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it.take(128) }, label = { Text(stringResource(R.string.password)) },
            supportingText = { if (register) Text(stringResource(R.string.password_hint)) },
            singleLine = true, enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) R.string.hide_password else R.string.show_password))
            } }, modifier = Modifier.fillMaxWidth())
        if (register) {
            OutlinedTextField(confirmation, { confirmation = it.take(128); mismatch = false },
                label = { Text(stringResource(R.string.confirm_password)) }, singleLine = true,
                enabled = !state.busy, isError = mismatch,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            if (mismatch) StatusMessage(stringResource(R.string.password_mismatch), error = true)
            Row(Modifier.fillMaxWidth().toggleable(value = adult, enabled = !state.busy,
                role = Role.Checkbox, onValueChange = { adult = it })) {
                Checkbox(adult, onCheckedChange = null, enabled = !state.busy)
                Text(stringResource(R.string.adult_confirmation), modifier = Modifier.weight(1f)) }
        }
        AccountErrorMessage(state.error)
        if (state.notice == AuthNotice.ResetSent) StatusMessage(stringResource(
            if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) R.string.local_reset_help else R.string.reset_sent))
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            mismatch = register && password != confirmation
            if (!mismatch) onSubmit(email, password, adult)
        }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)) {
            Text(stringResource(if (register) R.string.create_account else R.string.login))
        }
        if (!register) TextButton(onClick = { onReset(email) }, enabled = !state.busy) { Text(stringResource(R.string.forgot_password)) }
        TextButton(onClick = onBack, enabled = !state.busy) { Text(stringResource(R.string.back)) }
    }
}

@Composable
fun VerificationScreen(state: AuthUiState, onSend: () -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit) {
    val local = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS
    FormPage(stringResource(R.string.verify_title), stringResource(R.string.verify_body, state.account?.email.orEmpty())) {
        if (local) StatusMessage(stringResource(R.string.local_verification_help))
        AccountErrorMessage(state.error)
        when (state.notice) {
            AuthNotice.VerificationSent -> StatusMessage(stringResource(R.string.verification_sent))
            AuthNotice.NotVerified -> StatusMessage(stringResource(R.string.not_verified))
            else -> Unit
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = onSend, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (local) R.string.verify_local_email else R.string.send_verification)) }
        OutlinedButton(onClick = onRefresh, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.check_verification)) }
        TextButton(onClick = onLogout, enabled = !state.busy) { Text(stringResource(R.string.logout)) }
    }
}
