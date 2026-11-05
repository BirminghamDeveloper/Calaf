package com.calaf.feature.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.model.Match
import com.calaf.core.ui.R as CoreR

/**
 * Back, the member's name, Report and Block (always one tap away) and a chat menu with the less urgent actions:
 * sharing with a guardian and unmatching.
 */
@Composable
internal fun ChatHeader(
    match: Match,
    onBack: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onUnmatch: () -> Unit,
    onGuardian: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Text(match.name, style = MaterialTheme.typography.titleLarge)
    Row {
        IconButton(onClick = onBack) { CalafIcon(CalafIcons.Back, stringResource(CoreR.string.back)) }
        IconButton(onClick = onReport) { CalafIcon(CalafIcons.Report, stringResource(CoreR.string.report)) }
        IconButton(onClick = onBlock) { CalafIcon(CalafIcons.Block, stringResource(CoreR.string.block)) }
        IconButton(onClick = { menu = true }) { CalafIcon(CalafIcons.More, stringResource(R.string.chat_more)) }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_menu_guardian)) },
                onClick = {
                    menu = false
                    onGuardian()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(CoreR.string.unmatch)) },
                onClick = {
                    menu = false
                    onUnmatch()
                },
            )
        }
    }
}
