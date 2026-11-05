package com.calaf.feature.onboarding

import com.calaf.core.model.ProfileDraft
import com.calaf.core.testing.FakeProfileRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val dispatcher get() = main.dispatcher

    @Test fun `draft saves and restores without sharing across accounts`() = runTest(dispatcher) {
        val repository = FakeProfileRepository()
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
        val repository = FakeProfileRepository().apply { fail = true }
        val model = ProfileViewModel("user", repository)
        runCurrent()
        model.edit(ProfileDraft("Amina", "1995-02-03", "London", "UK"))
        model.save()
        runCurrent()
        assertTrue(model.state.value.storageError)
        assertFalse(model.state.value.reviewing)
        assertFalse(model.state.value.saved)
    }
}
