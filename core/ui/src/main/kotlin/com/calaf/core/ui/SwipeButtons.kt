package com.calaf.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing

/**
 * Pass and like buttons for people who cannot or would rather not drag. In a [SwipeCardStack] they call
 * [SwipeStackController.swipe], so they do exactly what a swipe does. The optional [onReport] and [onBlock] buttons
 * keep the safety actions one tap away.
 */
@Composable
fun SwipeButtons(
    onPass: () -> Unit,
    onLike: () -> Unit,
    passLabel: String,
    likeLabel: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    reportLabel: String? = null,
    blockLabel: String? = null,
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {},
) {
    Row(
        modifier, horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reportLabel != null) {
            IconButton(onClick = onReport, enabled = enabled) { CalafIcon(CalafIcons.Report, reportLabel) }
        }
        FilledTonalIconButton(
            onClick = onPass, enabled = enabled,
            modifier = Modifier.size(BUTTON_SIZE),
        ) { CalafIcon(CalafIcons.Close, passLabel) }
        FilledIconButton(
            onClick = onLike, enabled = enabled,
            modifier = Modifier.size(BUTTON_SIZE),
        ) { CalafIcon(CalafIcons.Matches, likeLabel, selected = true) }
        if (blockLabel != null) {
            IconButton(onClick = onBlock, enabled = enabled) { CalafIcon(CalafIcons.Block, blockLabel) }
        }
    }
}

private val BUTTON_SIZE = 64.dp
