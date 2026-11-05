package com.calaf.feature.auth

import com.calaf.core.model.Account
import com.calaf.core.model.AccountError
import com.calaf.core.testing.FakeAuthRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val dispatcher get() = main.dispatcher
    private val shortcut = AdminShortcut("admin", "admin", "admin@calaf.test", "CalafAdmin!2026")

    @Test fun `invalid registration never calls backend`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val model = AuthViewModel(repository)
        runCurrent()
        model.submit("invalid", "short", true, false)
        runCurrent()
        assertEquals(0, repository.calls)
        assertEquals(AccountError.InvalidInput, model.state.value.error)
    }
    @Test fun `network failure releases busy state and allows retry`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply { fail = true }
        val model = AuthViewModel(repository)
        runCurrent()
        model.submit("a@example.org", "longpassword", false, false)
        runCurrent()
        assertEquals(AccountError.Network, model.state.value.error)
        assertFalse(model.state.value.busy)
        repository.fail = false
        model.submit("a@example.org", "longpassword", false, false)
        runCurrent()
        assertEquals("user", model.state.value.account?.id)
    }
    @Test fun `verification refresh updates routing state`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        repository.accounts.value = Account("user", "a@example.org", false)
        val model = AuthViewModel(repository)
        runCurrent()
        assertFalse(model.state.value.account!!.emailVerified)
        model.refresh()
        runCurrent()
        assertTrue(model.state.value.account!!.emailVerified)
    }
    @Test fun `admin shortcut only works when one is configured`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val live = AuthViewModel(repository)
        live.submit("admin", "admin", false, false)
        runCurrent()
        assertEquals(0, repository.calls)
        val local = AuthViewModel(repository, adminShortcut = shortcut)
        local.submit("admin", "wrong", false, false)
        runCurrent()
        assertEquals(0, repository.calls)
        local.submit("admin", "admin", false, false)
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals("admin@calaf.test", local.state.value.account?.email)
        assertEquals("CalafAdmin!2026", repository.lastPassword)
    }
    @Test fun `admin shortcut cannot register an account`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val local = AuthViewModel(repository, adminShortcut = shortcut)
        local.submit("admin", "admin", true, true)
        runCurrent()
        assertEquals(0, repository.calls)
    }
    @Test fun `verification send refreshes verified session`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        repository.accounts.value = Account("user", "a@example.org", false)
        val model = AuthViewModel(repository)
        runCurrent()
        model.sendVerification()
        runCurrent()
        assertTrue(model.state.value.account!!.emailVerified)
        assertFalse(model.state.value.busy)
    }
}
