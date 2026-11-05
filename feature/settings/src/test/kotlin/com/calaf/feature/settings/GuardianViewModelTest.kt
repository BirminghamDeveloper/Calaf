package com.calaf.feature.settings

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.Guardian
import com.calaf.core.model.GuardianMatch
import com.calaf.core.model.GuardianStatus
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuardianViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()

    @Test fun `invite keeps the invitation code and guardians follow the repository`() = runTest(main.dispatcher) {
        val model = GuardianViewModel("me", repository)
        model.observe(); runCurrent()
        repository.result = CommandResult(invitationCode = "code-1")
        model.invite(); runCurrent()
        assertEquals("code-1", model.state.value.invitationCode)
        val guardian = Guardian("g", "me", "wali", GuardianStatus.ACCEPTED)
        repository.liveGuardians.value = listOf(guardian); runCurrent()
        assertEquals(listOf(guardian), model.state.value.guardians)
        model.stop()
    }

    @Test fun `accept and revoke send commands and confirm`() = runTest(main.dispatcher) {
        val model = GuardianViewModel("me", repository)
        model.accept("code"); runCurrent()
        assertEquals(Notice.INVITATION_ACCEPTED, model.state.value.action.notice)
        model.revoke("g"); runCurrent()
        assertEquals(Notice.REVOKED, model.state.value.action.notice)
        assertEquals(
            listOf(CommunityCommand.AcceptGuardian("code"), CommunityCommand.RevokeGuardian("g")),
            repository.commands,
        )
    }

    @Test fun `conversations shared with a guardian are listed and refreshed after accepting`() =
        runTest(main.dispatcher) {
            val model = GuardianViewModel("me", repository)
            model.observe(); runCurrent()
            assertTrue(model.state.value.shared.isEmpty())
            val shared = GuardianMatch("match", "owner", "Hassan", "Amina")
            repository.guarded = listOf(shared)
            model.accept("code"); runCurrent()
            assertEquals(listOf(shared), model.state.value.shared)
            repository.guarded = emptyList()
            model.revoke("g"); runCurrent()
            assertTrue(model.state.value.shared.isEmpty())
            model.stop()
        }
}
