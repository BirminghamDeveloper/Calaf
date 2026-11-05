package com.calaf.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme

/**
 * The full screen "It's a match" moment with both main photos. Matched members see each other clear, so the photos
 * are not blurred. A null photo shows the first letter of the name instead.
 */
@Composable
fun MatchMomentScreen(
    yourName: String,
    theirName: String,
    yourPhoto: ByteArray?,
    theirPhoto: ByteArray?,
    onMessage: () -> Unit,
    onKeepBrowsing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier.fillMaxSize().padding(CalafSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.match_title), style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Assertive
                },
            )
            Text(
                stringResource(R.string.match_subtitle, theirName), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CalafSpacing.small, bottom = CalafSpacing.large),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CalafSpacing.medium)) {
                Portrait(yourPhoto, yourName, stringResource(R.string.match_photo_you), Modifier.weight(1f))
                val theirs = stringResource(R.string.match_photo_them, theirName)
                Portrait(theirPhoto, theirName, theirs, Modifier.weight(1f))
            }
            Column(
                Modifier.padding(top = CalafSpacing.large).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CalafSpacing.small),
            ) {
                Button(onClick = onMessage, Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget)) {
                    Text(stringResource(R.string.match_message))
                }
                OutlinedButton(
                    onClick = onKeepBrowsing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget),
                ) { Text(stringResource(R.string.match_keep_browsing)) }
            }
        }
    }
}

@Composable
private fun Portrait(photo: ByteArray?, name: String, description: String, modifier: Modifier) {
    Box(
        modifier.aspectRatio(PORTRAIT_RATIO).clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo, contentDescription = description, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), style = MaterialTheme.typography.displayLarge)
                }
            }
        }
    }
}

private const val PORTRAIT_RATIO = 0.75f

@Preview(showBackground = true)
@Composable
private fun MatchMomentPreview() {
    CalafTheme {
        MatchMomentScreen("Amina", "Hassan (Demo)", null, null, onMessage = {}, onKeepBrowsing = {})
    }
}
