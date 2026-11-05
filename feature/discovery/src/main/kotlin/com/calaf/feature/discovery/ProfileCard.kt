package com.calaf.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CompatibilityArea
import com.calaf.core.model.PublicPhoto
import com.calaf.core.ui.PhotoBlur
import com.calaf.core.ui.ComplimentBanner
import com.calaf.core.ui.MemberPhoto
import com.calaf.core.ui.labelRes
import com.calaf.core.ui.R as CoreR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One profile as a full height card: a photo with tap zones (right side next, left side previous), segments showing
 * which photo is open, and the key facts over a scrim. Photos flagged blurred are blurred before they are drawn.
 */
@Composable
internal fun ProfileCard(
    profile: CommunityProfile,
    photoLoader: suspend (PublicPhoto) -> ByteArray?,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(profile.id) { mutableIntStateOf(0) }
    val photoCount = profile.photos.size
    val index = selected.coerceIn(0, (photoCount - 1).coerceAtLeast(0))
    val summary = stringResource(R.string.disc_card_summary, profile.name, profile.age, profile.city)
    Card(modifier.fillMaxSize().semantics { contentDescription = summary }) {
        Box(Modifier.fillMaxSize()) {
            val description = stringResource(R.string.disc_photo_description, profile.name, index + 1, photoCount)
            MemberPhoto(profile, profile.photos.getOrNull(index), description, photoLoader)
            profile.compliment?.let { compliment ->
                ComplimentBanner(
                    profile.name, compliment,
                    Modifier.align(Alignment.TopCenter).padding(top = BANNER_TOP_PADDING, start = 8.dp, end = 8.dp),
                )
            }
            Scrim(Modifier.align(Alignment.BottomStart))
            if (photoCount > 1) {
                PhotoSegments(photoCount, index, Modifier.align(Alignment.TopStart))
                TapZones(
                    onPrevious = { selected = (index - 1).coerceAtLeast(0) },
                    onNext = { selected = (index + 1).coerceAtMost(photoCount - 1) },
                )
            }
            Facts(profile, onOpenDetails, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun Scrim(modifier: Modifier) {
    Box(
        modifier.fillMaxWidth().height(SCRIM_HEIGHT)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(SCRIM_COLOR)))),
    )
}

@Composable
private fun PhotoSegments(count: Int, selected: Int, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().padding(CalafSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small),
    ) {
        repeat(count) { position ->
            Box(
                Modifier.weight(1f).height(4.dp).background(
                    if (position == selected) Color.White else Color.White.copy(alpha = INACTIVE_SEGMENT_ALPHA),
                    RoundedCornerShape(2.dp),
                ),
            )
        }
    }
}

@Composable
private fun TapZones(onPrevious: () -> Unit, onNext: () -> Unit) {
    val previous = stringResource(R.string.disc_prev_photo)
    val next = stringResource(R.string.disc_next_photo)
    Row(Modifier.fillMaxSize().padding(bottom = FACTS_CLEARANCE)) {
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .semantics { contentDescription = previous }
                .clickable(role = Role.Button, onClick = onPrevious),
        )
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .semantics { contentDescription = next }
                .clickable(role = Role.Button, onClick = onNext),
        )
    }
}

@Composable
private fun Facts(profile: CommunityProfile, onOpenDetails: () -> Unit, modifier: Modifier) {
    val open = stringResource(R.string.disc_detail_open)
    Row(
        modifier.fillMaxWidth().clickable(onClickLabel = open, role = Role.Button, onClick = onOpenDetails)
            .padding(CalafSpacing.medium),
        verticalAlignment = Alignment.Bottom,
    ) {
        FactLines(profile, Modifier.weight(1f))
        IconButton(onClick = onOpenDetails) { CalafIcon(CalafIcons.Info, open, tint = Color.White) }
    }
}

@Composable
private fun FactLines(profile: CommunityProfile, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.candidate_title, profile.name, profile.age),
            style = MaterialTheme.typography.headlineMedium, color = Color.White,
        )
        Text(
            stringResource(R.string.location, profile.city, profile.country),
            style = MaterialTheme.typography.bodyLarge, color = Color.White,
        )
        val details = buildList {
            profile.heritage.labelRes()?.let { add(stringResource(it)) }
            if (profile.occupation.isNotBlank()) add(profile.occupation)
            add(stringResource(profile.timeline.labelRes()))
        }
        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Color.White)
        val reasons = profile.compatibility.strong.take(MAX_REASONS)
        if (reasons.isNotEmpty()) {
            val areas = reasons.map { stringResource(it.labelRes()) }.joinToString()
            Text(
                stringResource(R.string.strong_areas, areas),
                style = MaterialTheme.typography.labelLarge, color = Color.White,
            )
        }
    }
}

private val BANNER_TOP_PADDING = 20.dp
private val SCRIM_HEIGHT = 220.dp
private val FACTS_CLEARANCE = 160.dp
private const val SCRIM_COLOR = 0xCC000000
private const val INACTIVE_SEGMENT_ALPHA = 0.4f
private const val MAX_REASONS = 3
