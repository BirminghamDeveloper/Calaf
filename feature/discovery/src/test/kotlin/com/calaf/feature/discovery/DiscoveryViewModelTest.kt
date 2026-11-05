package com.calaf.feature.discovery

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.DiscoveryPage
import com.calaf.core.model.ReportCategory
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoveryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val amina = CommunityProfile("a", "Amina", 30, "London", "UK")
    private val hodan = amina.copy(id = "b", name = "Hodan")

    @Test fun `pagination deduplicates and block removes a candidate`() = runTest(main.dispatcher) {
        val model = DiscoveryViewModel("me", repository)
        repository.page = DiscoveryPage(listOf(amina), "next")
        model.discover(); runCurrent()
        repository.page = DiscoveryPage(listOf(amina, hodan), null)
        model.discover(true); runCurrent()
        assertEquals(listOf(amina, hodan), model.state.value.candidates)
        assertNull(model.state.value.cursor)
        model.block("a"); runCurrent()
        assertEquals(listOf(hodan), model.state.value.candidates)
        assertEquals(Notice.BLOCKED, model.state.value.action.notice)
    }

    @Test fun `liking removes the candidate and reports a match`() = runTest(main.dispatcher) {
        val model = DiscoveryViewModel("me", repository)
        repository.page = DiscoveryPage(listOf(amina, hodan), null)
        model.discover(); runCurrent()
        repository.result = CommandResult(matched = true)
        model.interest("a", like = true); runCurrent()
        assertEquals(listOf(CommunityCommand.Interest("a", true)), repository.commands)
        assertEquals(listOf(hodan), model.state.value.candidates)
        assertEquals(Notice.MATCHED, model.state.value.action.notice)
    }

    @Test fun `a failed action keeps the candidate, shows the error and frees the busy state`() =
        runTest(main.dispatcher) {
            val model = DiscoveryViewModel("me", repository)
            repository.page = DiscoveryPage(listOf(amina), null)
            model.discover(); runCurrent()
            repository.failWith = CommunityError.OFFLINE
            model.interest("a", like = false); runCurrent()
            assertEquals(listOf(amina), model.state.value.candidates)
            assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
            assertFalse(model.state.value.action.busy)
        }

    @Test fun `a failed discovery shows the error and retry loads again`() = runTest(main.dispatcher) {
        val model = DiscoveryViewModel("me", repository)
        repository.page = DiscoveryPage(listOf(amina), null)
        repository.liveProfile.value = amina
        model.observe(); runCurrent()
        assertEquals(amina, model.state.value.profile)
        model.retry(); runCurrent()
        assertEquals(listOf(amina), model.state.value.candidates)
        assertTrue(model.state.value.loaded)
        assertFalse(model.state.value.discovering)
        model.stop()
    }

    @Test fun `report only calls back on success`() = runTest(main.dispatcher) {
        val model = DiscoveryViewModel("me", repository)
        var closed = 0
        repository.failWith = CommunityError.OFFLINE
        model.report("a", ReportCategory.OTHER, "x") { closed++ }; runCurrent()
        assertEquals(0, closed)
        repository.failWith = null
        model.report("a", ReportCategory.OTHER, "x") { closed++ }; runCurrent()
        assertEquals(1, closed)
        assertEquals(Notice.REPORTED, model.state.value.action.notice)
        model.dismissNotice()
        assertNull(model.state.value.action.notice)
    }
}
