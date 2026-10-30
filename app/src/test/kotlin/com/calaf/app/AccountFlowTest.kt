package com.calaf.app

import com.calaf.app.auth.*
import com.calaf.app.profile.ProfileViewModel
import com.calaf.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountFlowTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `invalid registration never calls backend`() = runTest(dispatcher) {
        val repository = FakeAuth()
        val model = AuthViewModel(repository)
        runCurrent()
        model.submit("invalid", "short", true, false)
        runCurrent()
        assertEquals(0, repository.calls)
        assertEquals(AccountError.InvalidInput, model.state.value.error)
    }
    @Test fun `network failure releases busy state and allows retry`() = runTest(dispatcher) {
        val repository = FakeAuth().apply { fail = true }
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
        val repository = FakeAuth()
        repository.accounts.value = Account("user", "a@example.org", false)
        val model = AuthViewModel(repository)
        runCurrent()
        assertFalse(model.state.value.account!!.emailVerified)
        model.refresh()
        runCurrent()
        assertTrue(model.state.value.account!!.emailVerified)
    }
    @Test fun `draft saves and restores without sharing across accounts`() = runTest(dispatcher) {
        val repository = FakeProfiles()
        val model = ProfileViewModel("user-a", repository)
        runCurrent()
        val draft = ProfileDraft("Amina", "1995-02-03", "London", "UK")
        model.edit(draft)
        model.save()
        runCurrent()
        assertTrue(model.state.value.reviewing)
        val restored = ProfileViewModel("user-a", repository)
        val other = ProfileViewModel("user-b", repository)
        runCurrent()
        assertEquals(draft, restored.state.value.draft)
        assertEquals(ProfileDraft(), other.state.value.draft)
    }
    @Test fun `failed save never shows success`() = runTest(dispatcher) {
        val repository = FakeProfiles().apply { fail = true }
        val model = ProfileViewModel("user", repository)
        runCurrent()
        model.edit(ProfileDraft("Amina", "1995-02-03", "London", "UK"))
        model.save()
        runCurrent()
        assertTrue(model.state.value.storageError)
        assertFalse(model.state.value.reviewing)
        assertFalse(model.state.value.saved)
    }

    @Test fun `admin shortcut only works when local mode is explicitly enabled`() = runTest(dispatcher) {
        val repository = FakeAuth()
        val live = AuthViewModel(repository)
        live.submit("admin", "admin", false, false)
        runCurrent()
        assertEquals(0, repository.calls)
        val local = AuthViewModel(repository, allowLocalAdmin = true)
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
        val repository = FakeAuth()
        val local = AuthViewModel(repository, allowLocalAdmin = true)
        local.submit("admin", "admin", true, true)
        runCurrent()
        assertEquals(0, repository.calls)
    }
    @Test fun `verification send refreshes verified session`() = runTest(dispatcher) {
        val repository = FakeAuth()
        repository.accounts.value = Account("user", "a@example.org", false)
        val model = AuthViewModel(repository)
        runCurrent()
        model.sendVerification()
        runCurrent()
        assertTrue(model.state.value.account!!.emailVerified)
        assertFalse(model.state.value.busy)
    }

    private class FakeAuth : AuthRepository {
        override val accounts = MutableStateFlow<Account?>(null)
        var calls = 0
        var lastPassword = ""
        var fail = false
        override suspend fun register(email: String, password: String) = login(email, password)
        override suspend fun login(email: String, password: String) {
            calls++
            lastPassword = password
            if (fail) throw AccountException(AccountError.Network)
            accounts.value = Account("user", email, false)
        }
        override suspend fun resetPassword(email: String) = Unit
        override suspend fun sendVerification() = Unit
        override suspend fun refresh() = accounts.value?.copy(emailVerified = true)
        override fun logout() { accounts.value = null }
    }
    private class FakeProfiles : ProfileRepository {
        private val values = mutableMapOf<String, ProfileDraft>()
        var fail = false
        override suspend fun load(userId: String) = values[userId] ?: ProfileDraft()
        override suspend fun save(userId: String, profile: ProfileDraft) {
            if (fail) throw java.io.IOException()
            values[userId] = profile
        }
    }
}
