package com.calaf.feature.community

import com.calaf.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val match = Match("match", "other", "Member", MatchStatus.ACTIVE)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `failed sends retry same id and success clears busy state`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = CommunityViewModel("me", repository)
        model.openChat(match); runCurrent()
        repository.fail = true
        model.send("Hello"); runCurrent()
        val first = repository.commands.last() as CommunityCommand.Send
        assertEquals(CommunityError.OFFLINE, model.state.value.error)
        assertFalse(model.state.value.busy)
        repository.fail = false
        model.send("Hello"); runCurrent()
        assertEquals(first.messageId, (repository.commands.last() as CommunityCommand.Send).messageId)
        assertEquals(Notice.SENT, model.state.value.notice)
        model.stop()
    }
    @Test fun `chat history merges without duplicates and stops at last page`() = runTest(dispatcher) {
        val repository = FakeRepository()
        repository.liveMessages.value = (51..100).map(::message)
        repository.older = listOf(message(49), message(50), message(51))
        val model = CommunityViewModel("me", repository)
        model.openChat(match); runCurrent()
        assertTrue(model.state.value.hasEarlierMessages)
        model.loadEarlierMessages(); runCurrent()
        assertEquals(52, model.state.value.messages.size)
        assertEquals("49", model.state.value.messages.first().id)
        assertFalse(model.state.value.hasEarlierMessages)
        model.closeChat(); runCurrent()
        assertTrue(model.state.value.messages.isEmpty())
        assertEquals(0, repository.liveMessages.subscriptionCount.value)
    }
    @Test fun `read receipts respect opt-in and skip outgoing messages`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = CommunityViewModel("me", repository)
        model.observe(); runCurrent(); model.openChat(match); runCurrent()
        model.markVisibleMessageRead(message(1)); runCurrent()
        assertTrue(repository.commands.isEmpty())
        repository.privacy.value = PrivacySettings(readReceipts = true); runCurrent()
        model.markVisibleMessageRead(message(2).copy(senderId = "me")); runCurrent()
        assertTrue(repository.commands.isEmpty())
        model.markVisibleMessageRead(message(3)); runCurrent()
        model.markVisibleMessageRead(message(3)); runCurrent()
        assertEquals(listOf(CommunityCommand.Read("match", "3")), repository.commands)
        model.stop()
    }
    @Test fun `discovery pagination deduplicates and block removes a candidate`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = CommunityViewModel("me", repository)
        val a = CommunityProfile("a", "Amina", 30, "London", "UK")
        val b = a.copy(id = "b", name = "Hodan")
        repository.page = DiscoveryPage(listOf(a), "next")
        model.discover(); runCurrent()
        repository.page = DiscoveryPage(listOf(a, b), null)
        model.discover(true); runCurrent()
        assertEquals(listOf(a, b), model.state.value.candidates)
        model.act(CommunityCommand.Block("a")); runCurrent()
        assertEquals(listOf(b), model.state.value.candidates)
        assertEquals(Notice.BLOCKED, model.state.value.notice)
        model.stop()
    }
    @Test fun `blocked match clears conversation and private messages`() = runTest(dispatcher) {
        val repository = FakeRepository()
        repository.liveMatches.value = listOf(match)
        repository.liveMessages.value = listOf(message(1))
        val model = CommunityViewModel("me", repository)
        model.observe(); runCurrent(); model.openChat(match); runCurrent()
        assertEquals(1, model.state.value.messages.size)
        repository.liveMatches.value = listOf(match.copy(status = MatchStatus.BLOCKED)); runCurrent()
        assertNull(model.state.value.conversation)
        assertTrue(model.state.value.messages.isEmpty())
        model.stop()
    }
    private fun message(id: Int) = ChatMessage(id.toString(), "other", "Message $id", id.toLong(), emptyList())
    private class FakeRepository : CommunityRepository {
        val liveMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val liveMatches = MutableStateFlow<List<Match>>(emptyList())
        val privacy = MutableStateFlow(PrivacySettings())
        var older = emptyList<ChatMessage>()
        var page = DiscoveryPage(emptyList(), null)
        var fail = false
        val commands = mutableListOf<CommunityCommand>()
        override fun profile(userId: String) = flowOf<CommunityProfile?>(null)
        override fun matches(userId: String) = liveMatches
        override fun settings(userId: String) = privacy
        override fun preferences(userId: String) = flowOf(PartnerPreference())
        override fun guardians(userId: String) = flowOf(emptyList<Guardian>())
        override fun subscription(userId: String) = flowOf(Subscription())
        override fun notifications(userId: String) = flowOf(emptyList<MemberNotification>())
        override fun messages(matchId: String) = liveMessages
        override suspend fun earlierMessages(matchId: String, beforeId: String) = older
        override suspend fun discover(cursor: String?) = page
        override suspend fun execute(command: CommunityCommand): CommandResult {
            commands += command
            if (fail) throw CommunityException(CommunityError.OFFLINE)
            return CommandResult()
        }
    }
}
