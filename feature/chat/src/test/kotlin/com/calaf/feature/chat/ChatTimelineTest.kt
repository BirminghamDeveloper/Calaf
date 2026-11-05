package com.calaf.feature.chat

import com.calaf.core.model.ChatMessage
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ChatTimelineTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val now = LocalDate.of(2026, 11, 5).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun message(id: String, time: Long, sender: String = "them") =
        ChatMessage(id, sender, "text $id", time, emptyList())

    @Test fun `a day separator comes before the first message of each day`() {
        val today = LocalDate.of(2026, 11, 5)
        val messages = listOf(
            message("a", at(today.minusDays(3), 9)), message("b", at(today.minusDays(3), 10)),
            message("c", at(today.minusDays(1), 23)), message("d", at(today, 0)), message("e", at(today, 11)),
        )
        val items = ChatTimeline.items(messages, now, utc)
        assertEquals(
            listOf(
                ChatItem.Day(today.minusDays(3), DayKind.OTHER), ChatItem.Message(messages[0]),
                ChatItem.Message(messages[1]),
                ChatItem.Day(today.minusDays(1), DayKind.YESTERDAY), ChatItem.Message(messages[2]),
                ChatItem.Day(today, DayKind.TODAY), ChatItem.Message(messages[3]), ChatItem.Message(messages[4]),
            ),
            items,
        )
    }

    @Test fun `midnight depends on the time zone`() {
        // 23:30 UTC on the 4th is already the 5th in Mogadishu (UTC+3).
        val late = LocalDate.of(2026, 11, 4).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val items = ChatTimeline.items(listOf(message("a", late)), now, ZoneId.of("Africa/Mogadishu"))
        assertEquals(DayKind.TODAY, (items.first() as ChatItem.Day).kind)
        val inUtc = ChatTimeline.items(listOf(message("a", late)), now, utc)
        assertEquals(DayKind.YESTERDAY, (inUtc.first() as ChatItem.Day).kind)
    }

    @Test fun `a message still being sent belongs to today and an empty chat has no items`() {
        val items = ChatTimeline.items(listOf(message("pending", 0L, "me")), now, utc)
        assertEquals(DayKind.TODAY, (items.first() as ChatItem.Day).kind)
        assertEquals(emptyList<ChatItem>(), ChatTimeline.items(emptyList(), now, utc))
    }

    @Test fun `status is sending, sent or read for my messages only`() {
        val conversation = Match("m", "them", "Them", MatchStatus.ACTIVE, otherReadAt = 2_000)
        assertNull(ChatTimeline.status(message("x", 1_000, "them"), "me", conversation))
        assertEquals(MessageStatus.SENDING, ChatTimeline.status(message("x", 0, "me"), "me", conversation))
        assertEquals(MessageStatus.READ, ChatTimeline.status(message("x", 2_000, "me"), "me", conversation))
        assertEquals(MessageStatus.READ, ChatTimeline.status(message("x", 1_000, "me"), "me", conversation))
        assertEquals(MessageStatus.SENT, ChatTimeline.status(message("x", 3_000, "me"), "me", conversation))
        val noReceipts = conversation.copy(otherReadAt = 0)
        assertEquals(MessageStatus.SENT, ChatTimeline.status(message("x", 1_000, "me"), "me", noReceipts))
    }
}
