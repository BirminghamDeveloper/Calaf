package com.calaf.core.ui

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.ReportCategory
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActionRunnerTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val state = MutableStateFlow(ActionState())

    private fun runner(scope: CoroutineScope) =
        ActionRunner(repository, scope, { state.value }) { change -> state.update(change) }

    @Test fun `success confirms, calls back and frees the busy state`() = runTest(main.dispatcher) {
        var results = 0
        var successes = 0
        repository.result = CommandResult(matched = true)
        runner(this).run(CommunityCommand.Block("x"), onResult = { results++ }, onSuccess = { successes++ })
        assertTrue(state.value.busy)
        runCurrent()
        assertEquals(Notice.MATCHED, state.value.notice)
        assertFalse(state.value.busy)
        assertEquals(1, results)
        assertEquals(1, successes)
    }

    @Test fun `a command is ignored while another is running`() = runTest(main.dispatcher) {
        val runner = runner(this)
        runner.run(CommunityCommand.Block("a"))
        runner.run(CommunityCommand.Block("b"))
        runCurrent()
        assertEquals(listOf<CommunityCommand>(CommunityCommand.Block("a")), repository.commands)
    }

    @Test fun `failure shows the error, skips callbacks and allows another try`() = runTest(main.dispatcher) {
        var calledBack = false
        val runner = runner(this)
        repository.failWith = CommunityError.LIMIT
        runner.run(
            CommunityCommand.RequestDeletion,
            onResult = { calledBack = true },
            onSuccess = { calledBack = true },
        )
        runCurrent()
        assertEquals(CommunityError.LIMIT, state.value.error)
        assertNull(state.value.notice)
        assertFalse(state.value.busy)
        assertFalse(calledBack)
        repository.failWith = null
        runner.run(CommunityCommand.RequestDeletion)
        runCurrent()
        assertNull(state.value.error)
        assertEquals(Notice.DELETION_REQUESTED, state.value.notice)
    }

    @Test fun `an unexpected exception becomes the unknown error`() = runTest(main.dispatcher) {
        val runner = runner(this)
        runner.fail(IllegalStateException("boom"))
        assertEquals(CommunityError.UNKNOWN, state.value.error)
        runner.clearError()
        assertNull(state.value.error)
    }

    @Test fun `each command maps to its notice`() {
        val none = CommandResult()
        val report = CommunityCommand.Report("x", ReportCategory.OTHER, "")
        assertEquals(Notice.REPORTED, noticeFor(report, none))
        assertEquals(Notice.UNMATCHED, noticeFor(CommunityCommand.Unmatch("m"), none))
        assertEquals(Notice.SENT, noticeFor(CommunityCommand.Send("m", "id", "hi"), none))
        assertEquals(Notice.REVOKED, noticeFor(CommunityCommand.RevokeSessions, none))
        assertEquals(Notice.SAVED, noticeFor(CommunityCommand.ExportData, none))
    }
}
