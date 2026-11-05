package com.calaf.feature.chat

import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityError
import com.calaf.core.model.Guardian
import com.calaf.core.model.GuardianStatus
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.PrivacySettings
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val match = Match("match", "other", "Member", MatchStatus.ACTIVE)

    private fun model() = ChatViewModel("me", "match", repository)

    private fun message(id: Int) = ChatMessage(id.toString(), "other", "Message $id", id.toLong(), emptyList())

    @Test fun `failed sends retry the same id and success clears the busy state`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        val model = model()
        model.observe(); runCurrent()
        repository.failWith = CommunityError.OFFLINE
        model.send("Hello"); runCurrent()
        val first = repository.commands.last() as CommunityCommand.Send
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        assertFalse(model.state.value.action.busy)
        repository.failWith = null
        model.send("Hello"); runCurrent()
        assertEquals(first.messageId, (repository.commands.last() as CommunityCommand.Send).messageId)
        assertEquals(Notice.SENT, model.state.value.action.notice)
        model.stop()
    }

    @Test fun `history merges without duplicates and stops at the last page`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        repository.liveMessages.value = (51..100).map(::message)
        repository.older = listOf(message(49), message(50), message(51))
        val model = model()
        model.observe(); runCurrent()
        assertTrue(model.state.value.hasEarlierMessages)
        model.loadEarlierMessages(); runCurrent()
        assertEquals(52, model.state.value.messages.size)
        assertEquals("49", model.state.value.messages.first().id)
        assertFalse(model.state.value.hasEarlierMessages)
        model.stop(); runCurrent()
        assertEquals(0, repository.liveMessages.subscriptionCount.value)
    }

    @Test fun `read receipts respect opt in and skip outgoing messages`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        val model = model()
        model.observe(); runCurrent()
        model.markVisibleMessageRead(message(1)); runCurrent()
        assertTrue(repository.commands.isEmpty())
        repository.livePrivacy.value = PrivacySettings(readReceipts = true); runCurrent()
        model.markVisibleMessageRead(message(2).copy(senderId = "me")); runCurrent()
        assertTrue(repository.commands.isEmpty())
        model.markVisibleMessageRead(message(3)); runCurrent()
        model.markVisibleMessageRead(message(3)); runCurrent()
        assertEquals(listOf(CommunityCommand.Read("match", "3")), repository.commands)
        model.stop()
    }

    @Test fun `a blocked match clears the private messages and sends one Closed event`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        repository.liveMessages.value = listOf(message(1))
        val model = model()
        val events = mutableListOf<ChatEvent>()
        val collector = launch { model.events.collect { events += it } }
        model.observe(); runCurrent()
        assertEquals(1, model.state.value.messages.size)
        assertEquals(match, model.state.value.conversation)
        repository.liveMatches.value = listOf(match.copy(status = MatchStatus.BLOCKED)); runCurrent()
        assertNull(model.state.value.conversation)
        assertTrue(model.state.value.messages.isEmpty())
        repository.liveMatches.value = emptyList(); runCurrent()
        assertEquals(listOf<ChatEvent>(ChatEvent.Closed), events)
        collector.cancel()
        model.stop()
    }

    @Test fun `unmatching closes the chat once the command succeeds`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        val model = model()
        model.observe(); runCurrent()
        model.unmatch(); runCurrent()
        assertEquals(listOf<CommunityCommand>(CommunityCommand.Unmatch("match")), repository.commands)
        assertEquals(ChatEvent.Closed, model.events.first())
        assertNull(model.state.value.conversation)
        model.stop()
    }

    @Test fun `a failed block keeps the chat open`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        val model = model()
        model.observe(); runCurrent()
        repository.failWith = CommunityError.OFFLINE
        model.block("other"); runCurrent()
        assertEquals(match, model.state.value.conversation)
        assertEquals(CommunityError.OFFLINE, model.state.value.action.error)
        model.stop()
    }

    @Test fun `a missing match closes the chat and sending does nothing`() = runTest(main.dispatcher) {
        val model = model()
        model.observe(); runCurrent()
        model.send("Hello"); runCurrent()
        assertTrue(repository.commands.isEmpty())
        assertNull(model.state.value.conversation)
        model.stop()
    }

    @Test fun `a member with an accepted guardian can share, and the match shows the sharing flags`() =
        runTest(main.dispatcher) {
            repository.liveMatches.value = listOf(match)
            val model = model()
            model.observe(); runCurrent()
            assertFalse(model.state.value.hasGuardian)
            repository.liveGuardians.value = listOf(
                Guardian("g1", "me", "wali", GuardianStatus.INVITED),
                Guardian("g2", "someone", "me", GuardianStatus.ACCEPTED),
            )
            runCurrent()
            // An invitation that is not accepted, or a guardian role for someone else, does not count.
            assertFalse(model.state.value.hasGuardian)
            repository.liveGuardians.value = listOf(Guardian("g1", "me", "wali", GuardianStatus.ACCEPTED))
            runCurrent()
            assertTrue(model.state.value.hasGuardian)
            model.shareWithGuardian(true); runCurrent()
            assertEquals(listOf<CommunityCommand>(CommunityCommand.ShareWithGuardian("match", true)), repository.commands)
            repository.liveMatches.value = listOf(match.copy(sharedWithGuardian = true, otherSharesWithGuardian = true))
            runCurrent()
            assertTrue(model.state.value.conversation!!.sharedWithGuardian)
            assertTrue(model.state.value.conversation!!.otherSharesWithGuardian)
            model.stop()
        }

    @Test fun `a failed share keeps the error and a guardian invitation keeps its code`() = runTest(main.dispatcher) {
        repository.liveMatches.value = listOf(match)
        val model = model()
        model.observe(); runCurrent()
        repository.failWith = CommunityError.INVALID
        model.shareWithGuardian(true); runCurrent()
        assertEquals(CommunityError.INVALID, model.state.value.action.error)
        repository.failWith = null
        repository.result = CommandResult(invitationCode = "code-9")
        model.inviteGuardian(); runCurrent()
        assertEquals("code-9", model.state.value.guardianCode)
        assertEquals(CommunityCommand.InviteGuardian, repository.commands.last())
        model.stop()
    }
}
