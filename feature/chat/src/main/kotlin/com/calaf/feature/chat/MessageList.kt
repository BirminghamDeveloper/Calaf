package com.calaf.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.Match
import com.calaf.core.model.SafetyWarning
import com.calaf.core.ui.R as CoreR
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * The conversation, newest at the bottom: day separators, bubbles (yours on the right), your message status, a warning
 * banner on flagged messages and a label on simulated demo replies.
 */
@Composable
internal fun MessageList(
    uid: String,
    conversation: Match,
    state: ChatUiState,
    list: LazyListState,
    onEarlier: () -> Unit,
    modifier: Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    // Recomputed whenever messages change, so a message sent just after midnight moves under a new day heading.
    val items = remember(state.messages) { ChatTimeline.items(state.messages, System.currentTimeMillis(), zone) }
    LazyColumn(
        modifier.fillMaxWidth().testTag("chat_history"), state = list, reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
    ) {
        items(items.asReversed(), key = ::itemKey) { item ->
            when (item) {
                is ChatItem.Day -> DaySeparator(item)
                is ChatItem.Message -> Bubble(item.message, uid, conversation)
            }
        }
        if (state.hasEarlierMessages) {
            item {
                TextButton(onClick = onEarlier, enabled = !state.loadingHistory) {
                    Text(stringResource(if (state.loadingHistory) R.string.please_wait else R.string.earlier_messages))
                }
            }
        }
    }
}

internal fun itemKey(item: ChatItem): Any = when (item) {
    is ChatItem.Day -> "day-${item.date}"
    is ChatItem.Message -> item.message.id
}

@Composable
internal fun DaySeparator(day: ChatItem.Day) {
    val label = when (day.kind) {
        DayKind.TODAY -> stringResource(R.string.chat_day_today)
        DayKind.YESTERDAY -> stringResource(R.string.chat_day_yesterday)
        DayKind.OTHER -> dayText(day.date)
    }
    Box(Modifier.fillMaxWidth().padding(vertical = CalafSpacing.small), contentAlignment = Alignment.Center) {
        Text(
            label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
    }
}

private fun dayText(date: LocalDate): String {
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(start))
}

@Composable
internal fun Bubble(message: ChatMessage, uid: String, conversation: Match, showStatus: Boolean = true) {
    val mine = message.senderId == uid
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = bubbleShape(mine), modifier = Modifier.widthIn(max = MAX_BUBBLE_WIDTH),
        ) {
            Column(Modifier.padding(CalafSpacing.medium), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (message.isDemo) {
                    Text(
                        stringResource(R.string.chat_demo_reply), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(message.text)
                if (message.warnings.isNotEmpty() && !mine) WarningBanner(message.warnings)
                Meta(message, uid, mine, conversation, showStatus)
            }
        }
    }
}

private fun bubbleShape(mine: Boolean) = RoundedCornerShape(
    topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp,
)

@Composable
private fun Meta(message: ChatMessage, uid: String, mine: Boolean, conversation: Match, showStatus: Boolean) {
    val time = if (message.createdAt == 0L) {
        stringResource(R.string.please_wait)
    } else {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAt))
    }
    val sender = stringResource(if (mine) R.string.you else CoreR.string.member)
    val status = if (showStatus) ChatTimeline.status(message, uid, conversation) else null
    Text(stringResource(R.string.sent_at, sender, time), style = MaterialTheme.typography.labelSmall)
    if (status != null) {
        Text(
            stringResource(
                when (status) {
                    MessageStatus.SENDING -> R.string.chat_status_sending
                    MessageStatus.SENT -> R.string.chat_status_sent
                    MessageStatus.READ -> R.string.chat_status_read
                },
            ),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** A clear warning on a message from the other member with a link, contact details or a money request. */
@Composable
private fun WarningBanner(warnings: List<SafetyWarning>) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(CalafSpacing.small)) {
            warnings.forEach { warning ->
                Text(
                    stringResource(warning.receivedMessage()), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

private fun SafetyWarning.receivedMessage() = when (this) {
    SafetyWarning.LINK -> R.string.chat_warn_link
    SafetyWarning.PERSONAL_INFORMATION -> R.string.chat_warn_personal
    SafetyWarning.MONEY_REQUEST -> R.string.chat_warn_money
}

private val MAX_BUBBLE_WIDTH = 300.dp
