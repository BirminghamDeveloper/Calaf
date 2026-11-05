package com.calaf.feature.settings

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.Guardian
import com.calaf.core.model.GuardianStatus
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PrivacySettings
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()

    @Test fun `observing combines settings, preferences and the blocked list`() = runTest(main.dispatcher) {
        val model = SettingsViewModel("me", repository)
        val active = Match("a", "x", "Active", MatchStatus.ACTIVE)
        val blocked = Match("b", "y", "Blocked", MatchStatus.BLOCKED, blockedByMe = true)
        repository.liveMatches.value = listOf(active, blocked)
        repository.livePrivacy.value = PrivacySettings(readReceipts = true)
        repository.livePreferences.value = PartnerPreference(minAge = 25)
        model.observe(); runCurrent()
        assertEquals(listOf(blocked), model.state.value.blocked)
        assertEquals(true, model.state.value.settings.readReceipts)
        assertEquals(25, model.state.value.preferences.minAge)
        model.stop()
    }

    @Test fun `saving sends the command and confirms`() = runTest(main.dispatcher) {
        val model = SettingsViewModel("me", repository)
        model.savePrivacy(PrivacySettings(visible = false)); runCurrent()
        val expected = CommunityCommand.Privacy(PrivacySettings(visible = false))
        assertEquals(listOf<CommunityCommand>(expected), repository.commands)
        assertEquals(Notice.SAVED, model.state.value.action.notice)
        model.savePreferences(PartnerPreference(maxAge = 40)); runCurrent()
        assertEquals(CommunityCommand.Preferences(PartnerPreference(maxAge = 40)), repository.commands.last())
    }

    @Test fun `export keeps the returned json and security actions confirm`() = runTest(main.dispatcher) {
        val model = SettingsViewModel("me", repository)
        repository.result = CommandResult(exportJson = "{\"email\":\"a\"}")
        model.exportData(); runCurrent()
        assertEquals("{\"email\":\"a\"}", model.state.value.exportJson)
        repository.result = CommandResult()
        model.requestDeletion(); runCurrent()
        assertEquals(Notice.DELETION_REQUESTED, model.state.value.action.notice)
        model.revokeSessions(); runCurrent()
        assertEquals(Notice.REVOKED, model.state.value.action.notice)
        assertEquals("{\"email\":\"a\"}", model.state.value.exportJson)
    }

    @Test fun `a failed save shows the error and allows another try`() = runTest(main.dispatcher) {
        val model = SettingsViewModel("me", repository)
        repository.failWith = CommunityError.INVALID
        model.savePrivacy(PrivacySettings()); runCurrent()
        assertEquals(CommunityError.INVALID, model.state.value.action.error)
        assertFalse(model.state.value.action.busy)
        repository.failWith = null
        model.savePrivacy(PrivacySettings()); runCurrent()
        assertEquals(null, model.state.value.action.error)
        assertEquals(Notice.SAVED, model.state.value.action.notice)
    }
}
