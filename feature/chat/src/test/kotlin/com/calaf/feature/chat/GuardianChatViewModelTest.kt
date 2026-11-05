package com.calaf.feature.chat

import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.GuardianMatch
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuardianChatViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val shared = GuardianMatch("match", "owner", "Hassan (Demo)", "Amina (Demo)")

    private fun message(id: Int) = ChatMessage("m$id", "owner", "Hello $id", id.toLong(), emptyList())

    @Test fun `a guardian sees the shared conversation and who is talking`() = runTest(main.dispatcher) {
        repository.guarded = listOf(shared)
        repository.liveGuardianMessages.value = listOf(message(1))
        val model = GuardianChatViewModel("match", repository)
        model.observe(); runCurrent()
        assertEquals(shared, model.state.value.shared)
        assertEquals(listOf("m1"), model.state.value.messages.map { it.id })
        assertTrue(model.state.value.loaded)
        assertFalse(model.state.value.unavailable)
        repository.liveGuardianMessages.value = listOf(message(1), message(2)); runCurrent()
        assertEquals(2, model.state.value.messages.size)
        model.stop()
    }

    @Test fun `when the member stops sharing the conversation becomes unavailable and is cleared`() =
        runTest(main.dispatcher) {
            val denied = object : CommunityRepository by repository {
                override fun guardianMessages(matchId: String): Flow<List<ChatMessage>> =
                    flow { throw CommunityException(CommunityError.PERMISSION) }
            }
            val model = GuardianChatViewModel("match", denied)
            model.observe(); runCurrent()
            assertTrue(model.state.value.unavailable)
            assertTrue(model.state.value.messages.isEmpty())
            assertNull(model.state.value.error)
            model.stop()
        }

    @Test fun `another failure shows an error and keeps the conversation available`() = runTest(main.dispatcher) {
        val offline = object : CommunityRepository by repository {
            override fun guardianMessages(matchId: String): Flow<List<ChatMessage>> =
                flow { throw CommunityException(CommunityError.OFFLINE) }
        }
        val model = GuardianChatViewModel("match", offline)
        model.observe(); runCurrent()
        assertEquals(CommunityError.OFFLINE, model.state.value.error)
        assertFalse(model.state.value.unavailable)
        model.stop()
    }
}
