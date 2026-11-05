package com.calaf.feature.onboarding

import com.calaf.core.model.Heritage
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileIssue
import com.calaf.core.model.Religiosity
import com.calaf.core.testing.FakeProfileRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
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

    private val valid = ProfileDraft("Amina", "1995-02-03", "London", "UK")

    @Test fun `steps move forward and back and stay inside the range`() = runTest(dispatcher) {
        val model = ProfileViewModel("user", FakeProfileRepository())
        runCurrent()
        assertEquals(FIRST_STEP, model.state.value.step)
        model.previousStep()
        assertEquals(FIRST_STEP, model.state.value.step)
        repeat(10) { model.nextStep() }
        assertEquals(LAST_STEP, model.state.value.step)
        model.goToStep(2)
        assertEquals(2, model.state.value.step)
        model.goToStep(-4)
        assertEquals(FIRST_STEP, model.state.value.step)
    }

    @Test fun `faith and heritage are dropped until consent is given and withdrawing deletes them`() =
        runTest(dispatcher) {
            val repository = FakeProfileRepository()
            val model = ProfileViewModel("user", repository)
            runCurrent()
            model.edit(valid.copy(religion = "Islam", heritage = Heritage.SOMALI))
            assertEquals("", model.state.value.draft.religion)
            assertEquals(Heritage.UNSPECIFIED, model.state.value.draft.heritage)
            model.setConsent(true)
            model.edit(model.state.value.draft.copy(heritage = Heritage.KENYAN, religiosity = Religiosity.PRACTISING))
            runCurrent()
            assertEquals(Heritage.KENYAN, model.state.value.draft.heritage)
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(Heritage.KENYAN, repository.load("user").heritage)
            model.setConsent(false)
            runCurrent()
            assertEquals(Heritage.UNSPECIFIED, model.state.value.draft.heritage)
            assertEquals(Religiosity.UNSPECIFIED, model.state.value.draft.religiosity)
            assertFalse(model.state.value.draft.sensitiveConsent)
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(Heritage.UNSPECIFIED, repository.load("user").heritage)
        }

    @Test fun `saving an invalid profile jumps to the step that needs fixing`() = runTest(dispatcher) {
        val model = ProfileViewModel("user", FakeProfileRepository())
        runCurrent()
        model.goToStep(LAST_STEP)
        model.edit(valid.copy(heightCm = 90))
        model.save(); runCurrent()
        assertEquals(ProfileIssue.Height, model.state.value.issue)
        assertEquals(2, model.state.value.step)
        model.edit(valid.copy(city = ""))
        model.save(); runCurrent()
        assertEquals(0, model.state.value.step)
    }

    @Test fun `completion follows the draft and old drafts without the new fields still load`() =
        runTest(dispatcher) {
            val repository = FakeProfileRepository()
            repository.save("user", valid)
            val model = ProfileViewModel("user", repository)
            runCurrent()
            assertTrue(model.state.value.completion in 1..99)
            assertTrue(model.state.value.reviewing)
            assertFalse(model.state.value.draft.sensitiveConsent)
        }
}
