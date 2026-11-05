package com.calaf.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.ui.Illustration
import com.calaf.core.ui.IllustrationKind
import kotlinx.coroutines.launch

private class TourPage(val kind: IllustrationKind, val title: Int, val body: Int)

private val pages = listOf(
    TourPage(IllustrationKind.Discover, R.string.tour_discover_title, R.string.tour_discover_body),
    TourPage(IllustrationKind.Chat, R.string.tour_chat_title, R.string.tour_chat_body),
    TourPage(IllustrationKind.Privacy, R.string.tour_privacy_title, R.string.tour_privacy_body),
)

/** Three short screens shown once on the first launch of the demo. Skip is always available. */
@Composable
internal fun WalkthroughScreen(onFinished: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(CalafSpacing.large)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier.heightIn(min = CalafDimensions.touchTarget).testTag("tour_skip"),
                ) { Text(stringResource(R.string.tour_skip)) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CalafSpacing.large, Alignment.CenterVertically),
                ) {
                    Illustration(page.kind)
                    Text(
                        stringResource(page.title), style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(page.body), style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.tour_progress, pager.currentPage + 1, pages.size),
                Modifier.fillMaxWidth().padding(bottom = CalafSpacing.small),
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    if (last) onFinished() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget).testTag("tour_next"),
            ) { Text(stringResource(if (last) R.string.tour_start else R.string.tour_next)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WalkthroughPreview() = CalafTheme { WalkthroughScreen {} }
