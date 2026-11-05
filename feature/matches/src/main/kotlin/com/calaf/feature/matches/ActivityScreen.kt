package com.calaf.feature.matches

import com.calaf.core.ui.IllustrationKind
import com.calaf.core.ui.EmptyState
import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.MemberNotification
import com.calaf.core.ui.CalafListScreen
import java.text.DateFormat
import java.util.Date

/**
 * The Activity tab. The caller owns the [ActivityViewModel] because the navigation bar also shows its unread count.
 */
@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onMarkRead: (MemberNotification) -> Unit,
    onOpenConversation: (Match) -> Unit,
    onRetry: () -> Unit,
) {
    CalafListScreen(
        stringResource(CoreR.string.activity_title), state.action, "activity_list", onRetry = onRetry,
    ) {
        item { Text(stringResource(R.string.activity_description)) }
        if (state.notifications.isEmpty()) item { EmptyState(IllustrationKind.Calm, stringResource(R.string.activity_empty)) }
        items(state.notifications, key = MemberNotification::id) { notification ->
            val match = state.matches.firstOrNull { it.id == notification.targetId && it.status == MatchStatus.ACTIVE }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(CalafSpacing.medium)) {
                    Text(
                        stringResource(
                            if (notification.category == "MATCH") {
                                R.string.activity_match
                            } else {
                                R.string.activity_message
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (notification.createdAt > 0) {
                        Text(DateFormat.getDateTimeInstance().format(Date(notification.createdAt)))
                    }
                    if (match != null) {
                        TextButton(
                            onClick = { onMarkRead(notification); onOpenConversation(match) },
                            enabled = !state.action.busy,
                        ) { Text(stringResource(R.string.open_conversation)) }
                    }
                    if (!notification.read) {
                        TextButton(onClick = { onMarkRead(notification) }, enabled = !state.action.busy) {
                            Text(stringResource(R.string.activity_mark_read))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityScreenPreview() {
    CalafTheme {
        ActivityScreen(
            ActivityUiState(listOf(MemberNotification("n1", "MATCH", "a_b", 1_700_000_000_000, false))),
            onMarkRead = {}, onOpenConversation = {}, onRetry = {},
        )
    }
}
