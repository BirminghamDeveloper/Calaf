package com.calaf.feature.chat

import com.calaf.core.model.ChatMessage
import com.calaf.core.model.Match
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How a day separator should read relative to today. */
enum class DayKind { TODAY, YESTERDAY, OTHER }

/** One row of the conversation: a day separator or a message. */
sealed interface ChatItem {
    data class Day(val date: LocalDate, val kind: DayKind) : ChatItem

    data class Message(val message: ChatMessage) : ChatItem
}

/** Where one of my own messages is: still being sent, delivered, or read by the other member. */
enum class MessageStatus { SENDING, SENT, READ }

/**
 * The pure rules behind the conversation list, kept out of Compose so they can be unit tested. Times are in
 * milliseconds, and [zone] decides where midnight is.
 */
object ChatTimeline {
    /**
     * Messages oldest first with a [ChatItem.Day] before the first message of each day. A message with no time yet
     * (it is still being sent) belongs to today.
     */
    fun items(messages: List<ChatMessage>, nowMillis: Long, zone: ZoneId): List<ChatItem> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val result = mutableListOf<ChatItem>()
        var lastDay: LocalDate? = null
        for (message in messages) {
            val day = if (message.createdAt == 0L) {
                today
            } else {
                Instant.ofEpochMilli(message.createdAt).atZone(zone).toLocalDate()
            }
            if (day != lastDay) {
                result += ChatItem.Day(day, kind(day, today))
                lastDay = day
            }
            result += ChatItem.Message(message)
        }
        return result
    }

    fun kind(day: LocalDate, today: LocalDate): DayKind = when (day) {
        today -> DayKind.TODAY
        today.minusDays(1) -> DayKind.YESTERDAY
        else -> DayKind.OTHER
    }

    /** The status of [message], or null if it is not mine. Read only counts once the other member has opted in. */
    fun status(message: ChatMessage, myId: String, conversation: Match): MessageStatus? = when {
        message.senderId != myId -> null
        message.createdAt == 0L -> MessageStatus.SENDING
        conversation.otherReadAt >= message.createdAt -> MessageStatus.READ
        else -> MessageStatus.SENT
    }
}
