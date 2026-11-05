package com.calaf.feature.matches

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.MemberNotification
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActivityViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()

    @Test fun `unread count follows the notifications`() = runTest(main.dispatcher) {
        val model = ActivityViewModel("me", repository)
        model.observe(); runCurrent()
        assertEquals(0, model.state.value.unreadCount)
        repository.liveNotifications.value = listOf(
            MemberNotification("n1", "MATCH", "m", 1, read = false),
            MemberNotification("n2", "MESSAGE", "m", 2, read = true),
        )
        repository.liveMatches.value = listOf(Match("m", "other", "Member", MatchStatus.ACTIVE))
        runCurrent()
        assertEquals(1, model.state.value.unreadCount)
        assertEquals(1, model.state.value.matches.size)
        model.stop()
    }

    @Test fun `marking a notification read sends the command`() = runTest(main.dispatcher) {
        val model = ActivityViewModel("me", repository)
        model.markRead(MemberNotification("n1", "MATCH", "m", 5, read = false)); runCurrent()
        assertEquals(listOf(CommunityCommand.ReadNotification("n1", 5)), repository.commands)
        assertEquals(Notice.SAVED, model.state.value.action.notice)
    }
}
