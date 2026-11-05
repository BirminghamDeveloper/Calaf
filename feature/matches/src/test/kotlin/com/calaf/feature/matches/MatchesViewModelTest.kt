package com.calaf.feature.matches

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.ReportCategory
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MatchesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val match = Match("me_other", "other", "Member", MatchStatus.ACTIVE)

    @Test fun `observing shows live matches`() = runTest(main.dispatcher) {
        val model = MatchesViewModel("me", repository)
        model.observe(); runCurrent()
        repository.liveMatches.value = listOf(match); runCurrent()
        assertEquals(listOf(match), model.state.value.matches)
        model.stop()
    }

    @Test fun `unmatch and block send commands and confirm`() = runTest(main.dispatcher) {
        val model = MatchesViewModel("me", repository)
        model.unmatch("me_other"); runCurrent()
        assertEquals(Notice.UNMATCHED, model.state.value.action.notice)
        model.block("other"); runCurrent()
        assertEquals(Notice.BLOCKED, model.state.value.action.notice)
        assertEquals(
            listOf(CommunityCommand.Unmatch("me_other"), CommunityCommand.Block("other")),
            repository.commands,
        )
    }

    @Test fun `a failed report shows the error, frees busy and does not call back`() = runTest(main.dispatcher) {
        val model = MatchesViewModel("me", repository)
        var done = false
        repository.failWith = CommunityError.LIMIT
        model.report("other", ReportCategory.HARASSMENT, "details") { done = true }; runCurrent()
        assertFalse(done)
        assertEquals(CommunityError.LIMIT, model.state.value.action.error)
        assertFalse(model.state.value.action.busy)
    }

    @Test fun `a stream error is shown and observe clears it`() = runTest(main.dispatcher) {
        val model = MatchesViewModel("me", repository)
        repository.failWith = CommunityError.OFFLINE
        model.unmatch("x"); runCurrent()
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        model.observe(); runCurrent()
        assertEquals(null, model.state.value.action.error)
        model.stop()
    }
}
