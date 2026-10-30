package com.calaf.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AuthNotice { ResetSent, VerificationSent, NotVerified }
data class AuthUiState(
    val ready: Boolean = false,
    val account: Account? = null,
    val busy: Boolean = false,
    val error: AccountError? = null,
    val notice: AuthNotice? = null,
)

class AuthViewModel(private val repository: AuthRepository, private val allowLocalAdmin: Boolean = false) : ViewModel() {
    private val mutable = MutableStateFlow(AuthUiState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            repository.accounts.collect { account ->
                mutable.update { it.copy(ready = true, account = account, error = null, notice = null) }
            }
        }
    }
    fun clearMessage() { mutable.update { it.copy(error = null, notice = null) } }
    fun submit(email: String, password: String, register: Boolean, adultConsent: Boolean) {
        if (allowLocalAdmin && !register && email.trim() == "admin" && password == "admin") {
            perform { repository.login("admin@calaf.test", "CalafAdmin!2026") }
            return
        }
        if (!AccountValidation.email(email) || password.isEmpty() ||
            (register && (!AccountValidation.password(password) || !adultConsent))) {
            mutable.update { it.copy(error = AccountError.InvalidInput, notice = null) }
            return
        }
        perform {
            if (register) repository.register(email, password) else repository.login(email, password)
        }
    }
    fun reset(email: String) {
        if (!AccountValidation.email(email)) {
            mutable.update { it.copy(error = AccountError.InvalidInput, notice = null) }
            return
        }
        perform { repository.resetPassword(email); mutable.update { it.copy(notice = AuthNotice.ResetSent) } }
    }
    fun sendVerification() = perform {
        repository.sendVerification()
        val account = repository.refresh()
        mutable.update { it.copy(account = account, notice = AuthNotice.VerificationSent) }
    }
    fun refresh() = perform {
        val account = repository.refresh()
        mutable.update { it.copy(account = account,
            notice = if (account?.emailVerified == false) AuthNotice.NotVerified else null) }
    }
    fun logout() {
        if (!state.value.busy) repository.logout()
    }
    private fun perform(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountException) { mutable.update { it.copy(error = error.reason) } }
            catch (_: Exception) { mutable.update { it.copy(error = AccountError.Unknown) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
