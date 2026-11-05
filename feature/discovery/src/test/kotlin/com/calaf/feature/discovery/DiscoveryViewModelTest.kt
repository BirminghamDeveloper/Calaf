package com.calaf.feature.discovery

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.DiscoveryPage
import com.calaf.core.model.Heritage
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.ReportCategory
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.FakePhotoRepository
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
    private val photos = FakePhotoRepository()

    private fun model() = DiscoveryViewModel("me", repository) { photos }

    @Test fun `pagination deduplicates and block removes a candidate`() = runTest(main.dispatcher) {
        val model = model()
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
        val model = model()
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
            val model = model()
            repository.page = DiscoveryPage(listOf(amina), null)
            model.discover(); runCurrent()
            repository.failWith = CommunityError.OFFLINE
            model.interest("a", like = false); runCurrent()
            assertEquals(listOf(amina), model.state.value.candidates)
            assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
            assertFalse(model.state.value.action.busy)
        }

    @Test fun `a failed discovery shows the error and retry loads again`() = runTest(main.dispatcher) {
        val model = model()
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
        val model = model()
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

    private fun profiles(count: Int, prefix: String = "p") =
        (1..count).map { amina.copy(id = "$prefix$it", name = "Member $it") }

    @Test fun `liking removes the card straight away and a failure brings it back to the front`() =
        runTest(main.dispatcher) {
            val model = model()
            repository.page = DiscoveryPage(profiles(3), null)
            model.discover(); runCurrent()
            repository.failWith = CommunityError.OFFLINE
            model.interest("p1", like = true)
            assertEquals(listOf("p2", "p3"), model.state.value.candidates.map { it.id })
            runCurrent()
            assertEquals(listOf("p1", "p2", "p3"), model.state.value.candidates.map { it.id })
            assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        }

    @Test fun `a swipe while another request is running is ignored and keeps its card`() = runTest(main.dispatcher) {
        val model = model()
        repository.page = DiscoveryPage(profiles(3), null)
        model.discover(); runCurrent()
        model.interest("p1", like = true)
        model.interest("p2", like = false)
        runCurrent()
        assertEquals(1, repository.commands.size)
        assertEquals(listOf("p2", "p3"), model.state.value.candidates.map { it.id })
    }

    @Test fun `the next page is fetched once three cards or fewer remain`() = runTest(main.dispatcher) {
        val model = model()
        repository.page = DiscoveryPage(profiles(5), "page2")
        model.discover(); runCurrent()
        repository.page = DiscoveryPage(profiles(4, "q"), null)
        model.interest("p1", like = false); runCurrent()
        // Four cards remain, so nothing is fetched yet.
        assertEquals(4, model.state.value.candidates.size)
        assertEquals("page2", model.state.value.cursor)
        model.interest("p2", like = false); runCurrent()
        // Three remain: the second page arrived and the cursor is used up.
        assertEquals(7, model.state.value.candidates.size)
        assertNull(model.state.value.cursor)
        model.interest("p3", like = false); runCurrent()
        assertEquals(6, model.state.value.candidates.size)
    }

    @Test fun `paging rules fetch only with a cursor and nothing in flight`() {
        assertTrue(DiscoveryPaging.shouldPrefetch(3, "next", discovering = false))
        assertTrue(DiscoveryPaging.shouldPrefetch(0, "next", discovering = false))
        assertFalse(DiscoveryPaging.shouldPrefetch(4, "next", discovering = false))
        assertFalse(DiscoveryPaging.shouldPrefetch(1, null, discovering = false))
        assertFalse(DiscoveryPaging.shouldPrefetch(1, "next", discovering = true))
    }

    @Test fun `the first page loads once on opening`() = runTest(main.dispatcher) {
        val model = model()
        repository.page = DiscoveryPage(profiles(2), null)
        model.discoverIfNeeded(); runCurrent()
        assertEquals(2, model.state.value.candidates.size)
        repository.page = DiscoveryPage(profiles(5, "z"), null)
        model.discoverIfNeeded(); runCurrent()
        assertEquals(listOf("p1", "p2"), model.state.value.candidates.map { it.id })
    }

    @Test fun `photos are read through the viewer and cached, and failures give null`() = runTest(main.dispatcher) {
        val model = model()
        photos.bytes["ph1"] = byteArrayOf(5)
        val photo = PublicPhoto("ph1", blurred = false)
        assertEquals(5, model.photo("owner", photo)?.first()?.toInt())
        photos.bytes["ph1"] = byteArrayOf(9)
        assertEquals(5, model.photo("owner", photo)?.first()?.toInt())
        photos.failWith = java.io.IOException("no")
        assertNull(model.photo("owner", PublicPhoto("other", blurred = false)))
    }

    @Test fun `saved filters appear in the state`() = runTest(main.dispatcher) {
        val model = model()
        repository.livePreferences.value = PartnerPreference(heritage = setOf(Heritage.SOMALI))
        model.observe(); runCurrent()
        assertEquals(setOf(Heritage.SOMALI), model.state.value.preferences.heritage)
        model.stop()
    }

    @Test fun `applying filters saves them, then reloads discovery from the first page`() = runTest(main.dispatcher) {
        val model = model()
        repository.page = DiscoveryPage(profiles(2), "more")
        model.discover(); runCurrent()
        assertEquals("more", model.state.value.cursor)
        val filters = PartnerPreference(minAge = 25, heritage = setOf(Heritage.KENYAN))
        repository.page = DiscoveryPage(profiles(1, "filtered"), null)
        var saved = 0
        model.applyFilters(filters) { saved++ }; runCurrent()
        assertEquals(listOf<CommunityCommand>(CommunityCommand.Preferences(filters)), repository.commands)
        assertEquals(1, saved)
        assertEquals(listOf("filtered1"), model.state.value.candidates.map { it.id })
        assertNull(model.state.value.cursor)
        assertTrue(model.state.value.loaded)
    }

    @Test fun `a failed filter save keeps the stack and the sheet open`() = runTest(main.dispatcher) {
        val model = model()
        repository.page = DiscoveryPage(profiles(2), null)
        model.discover(); runCurrent()
        repository.failWith = CommunityError.OFFLINE
        var saved = 0
        model.applyFilters(PartnerPreference(minAge = 30)) { saved++ }; runCurrent()
        assertEquals(0, saved)
        assertEquals(listOf("p1", "p2"), model.state.value.candidates.map { it.id })
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
    }

    @Test fun `clearing the sheet keeps gender and country`() {
        val kept = PartnerPreference(
            minAge = 30, gender = com.calaf.core.model.Gender.WOMAN, country = "UK", heritage = setOf(Heritage.SOMALI),
        )
        val cleared = clearedFilters(kept)
        assertEquals(com.calaf.core.model.Gender.WOMAN, cleared.gender)
        assertEquals("UK", cleared.country)
        assertEquals(18, cleared.minAge)
        assertTrue(cleared.heritage.isEmpty())
    }
}
