package com.calaf.feature.matches

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.Compliment
import com.calaf.core.model.LikedYou
import com.calaf.core.model.CommunityError
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MatchesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val match = Match("me_other", "other", "Member", MatchStatus.ACTIVE)
    private val photos = FakePhotoRepository(listOf(Photo("mine", 0, true, PhotoState.APPROVED)))
    private val admirer = CommunityProfile(
        "admirer", "Admirer", 31, "London", "UK", compliment = Compliment("Salaam, lovely profile"),
        photos = listOf(PublicPhoto("theirs", false)),
    )

    private fun model() = MatchesViewModel("me", repository) { photos }

    @Test fun `observing shows live matches`() = runTest(main.dispatcher) {
        val model = model()
        model.observe(); runCurrent()
        repository.liveMatches.value = listOf(match); runCurrent()
        assertEquals(listOf(match), model.state.value.matches)
        model.stop()
    }

    @Test fun `unmatch and block send commands and confirm`() = runTest(main.dispatcher) {
        val model = model()
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
        val model = model()
        var done = false
        repository.failWith = CommunityError.LIMIT
        model.report("other", ReportCategory.HARASSMENT, "details") { done = true }; runCurrent()
        assertFalse(done)
        assertEquals(CommunityError.LIMIT, model.state.value.action.error)
        assertFalse(model.state.value.action.busy)
    }

    @Test fun `a stream error is shown and observe clears it`() = runTest(main.dispatcher) {
        val model = model()
        repository.failWith = CommunityError.OFFLINE
        model.unmatch("x"); runCurrent()
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        model.observe(); runCurrent()
        assertEquals(null, model.state.value.action.error)
        model.stop()
    }

    @Test fun `observing also loads who liked you`() = runTest(main.dispatcher) {
        repository.liked = LikedYou(1, listOf(admirer), simulated = true)
        val model = model()
        assertNull(model.state.value.likedYou)
        model.observe(); runCurrent()
        assertEquals(listOf(admirer), model.state.value.likedYou?.profiles)
        assertTrue(model.state.value.likedYou?.simulated == true)
        assertFalse(model.state.value.loadingLikedYou)
        model.stop()
    }

    @Test fun `a locked list shows only the count`() = runTest(main.dispatcher) {
        repository.liked = LikedYou(3, emptyList(), locked = true)
        val model = model()
        model.refreshLikedYou(); runCurrent()
        val liked = model.state.value.likedYou!!
        assertTrue(liked.locked)
        assertEquals(3, liked.count)
        assertTrue(liked.profiles.isEmpty())
    }

    @Test fun `liking back a member who liked you shows the match moment and refreshes the list`() =
        runTest(main.dispatcher) {
            repository.liked = LikedYou(1, listOf(admirer))
            val model = model()
            model.refreshLikedYou(); runCurrent()
            repository.result = CommandResult(matched = true, matchId = "admirer_me")
            repository.liked = LikedYou(0, emptyList())
            model.likeBack(admirer); runCurrent()
            assertEquals(listOf<CommunityCommand>(CommunityCommand.Interest("admirer", true, "")), repository.commands)
            assertEquals("admirer_me", model.state.value.matchMoment?.matchId)
            assertEquals(admirer, model.state.value.matchMoment?.profile)
            assertEquals(0, model.state.value.likedYou?.count)
            val (mine, theirs) = model.matchPhotos(admirer)
            assertNotNull(mine)
            assertNotNull(theirs)
            model.dismissMatchMoment()
            assertNull(model.state.value.matchMoment)
        }

    @Test fun `passing sends a pass with no note and shows no match moment`() = runTest(main.dispatcher) {
        val model = model()
        model.pass(admirer); runCurrent()
        assertEquals(listOf<CommunityCommand>(CommunityCommand.Interest("admirer", false, "")), repository.commands)
        assertNull(model.state.value.matchMoment)
    }

    @Test fun `a failed answer keeps the list and shows the error`() = runTest(main.dispatcher) {
        repository.liked = LikedYou(1, listOf(admirer))
        val model = model()
        model.refreshLikedYou(); runCurrent()
        repository.failWith = CommunityError.OFFLINE
        model.likeBack(admirer); runCurrent()
        assertEquals(listOf(admirer), model.state.value.likedYou?.profiles)
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        assertNull(model.state.value.matchMoment)
    }
}
