package com.calaf.feature.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CompatibilityArea
import com.calaf.core.model.CompatibilityResult
import com.calaf.core.model.Gender
import com.calaf.core.model.Heritage
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.Religiosity
import com.calaf.core.model.VerificationStatus
import com.calaf.core.ui.SwipeButtons
import com.calaf.core.ui.labelRes
import com.calaf.core.ui.R as CoreR

/**
 * A member's whole profile: every approved photo, about, the basics, values, a faith section when they chose to share
 * it, and how your answers compare. Pass, like, report and block stay at the bottom.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfileDetailScreen(
    profile: CommunityProfile,
    photoLoader: suspend (PublicPhoto) -> ByteArray?,
    enabled: Boolean,
    onBack: () -> Unit,
    onInterest: (Boolean) -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = CalafSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { CalafIcon(CalafIcons.Back, stringResource(R.string.disc_detail_back)) }
            Text(profile.name, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Gallery(profile, photoLoader)
            Column(
                Modifier.padding(CalafSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
            ) {
                Header(profile)
                About(profile)
                Basics(profile)
                Values(profile)
                Faith(profile)
                Compatibility(profile.compatibility)
            }
        }
        val like = stringResource(R.string.interest)
        val pass = stringResource(R.string.pass)
        Surface(tonalElevation = 3.dp) {
            Box(Modifier.fillMaxWidth().padding(CalafSpacing.small), contentAlignment = Alignment.Center) {
                SwipeButtons(
                    onPass = { onInterest(false) }, onLike = { onInterest(true) },
                    passLabel = pass, likeLabel = like, enabled = enabled,
                    reportLabel = stringResource(CoreR.string.report), blockLabel = stringResource(CoreR.string.block),
                    onReport = onReport, onBlock = onBlock,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Gallery(profile: CommunityProfile, loader: suspend (PublicPhoto) -> ByteArray?) {
    val count = profile.photos.size
    val pager = rememberPagerState { count.coerceAtLeast(1) }
    Box(Modifier.fillMaxWidth().height(GALLERY_HEIGHT)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            MemberPhoto(profile, profile.photos.getOrNull(page), page, count, loader)
        }
        if (count > 1) {
            Surface(
                Modifier.align(Alignment.BottomEnd).padding(CalafSpacing.small),
                shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
            ) {
                Text(
                    stringResource(R.string.disc_detail_photo_count, pager.currentPage + 1, count),
                    Modifier.padding(horizontal = CalafSpacing.small, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun Header(profile: CommunityProfile) {
    Column {
        Text(
            stringResource(R.string.candidate_title, profile.name, profile.age),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.location, profile.city, profile.country),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (profile.verification == VerificationStatus.EMAIL_ONLY) {
            Text(stringResource(R.string.disc_detail_verification), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        Text(
            stringResource(title), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Fact(label: Int, value: String?) {
    if (value.isNullOrBlank()) return
    Text(stringResource(R.string.disc_fact, stringResource(label), value))
}

@Composable
private fun Fact(label: Int, valueRes: Int?) {
    if (valueRes != null) Fact(label, stringResource(valueRes))
}

@Composable
private fun About(profile: CommunityProfile) {
    if (profile.biography.isBlank() && profile.culture.isBlank() && profile.languages.isEmpty()) return
    Section(R.string.disc_detail_about) {
        if (profile.biography.isNotBlank()) Text(profile.biography)
        Fact(R.string.disc_f_culture, profile.culture)
        Fact(R.string.disc_f_languages, profile.languages.joinToString())
    }
}

@Composable
private fun Basics(profile: CommunityProfile) {
    Section(R.string.disc_detail_basics) {
        Fact(R.string.disc_f_marital, profile.maritalStatus.labelRes())
        Fact(R.string.disc_f_children, profile.children.labelRes())
        Fact(R.string.disc_f_wants, profile.wantsChildren.labelRes())
        if (profile.heightCm > 0) {
            Fact(R.string.disc_f_height, stringResource(CoreR.string.lbl_height, profile.heightCm))
        }
        Fact(R.string.disc_f_work, profile.occupation)
        Fact(R.string.disc_f_education, profile.education)
        Fact(R.string.disc_f_smoking, profile.smoking.labelRes())
        Fact(R.string.disc_f_relocate, profile.relocation.labelRes())
        Fact(R.string.disc_f_timeline, profile.timeline.labelRes())
        Fact(R.string.disc_f_born, profile.countryOfBirth)
    }
}

@Composable
private fun Values(profile: CommunityProfile) {
    if (profile.values.isEmpty() && profile.interests.isEmpty()) return
    Section(R.string.disc_detail_values) {
        Fact(R.string.disc_f_values, profile.values.joinToString())
        Fact(R.string.disc_f_interests, profile.interests.joinToString())
    }
}

/** Only shown when the member chose to share faith details. Otherwise these fields are empty and so is the section. */
@Composable
private fun Faith(profile: CommunityProfile) {
    val shared = profile.religion.isNotBlank() || profile.religiosity != Religiosity.UNSPECIFIED ||
        profile.prays != PrayerHabit.UNSPECIFIED || profile.heritage != Heritage.UNSPECIFIED
    if (!shared) return
    Section(R.string.disc_detail_faith) {
        Fact(R.string.disc_f_religion, profile.religion)
        Fact(R.string.disc_f_religiosity, profile.religiosity.labelRes())
        Fact(R.string.disc_f_prays, profile.prays.labelRes())
        Fact(R.string.disc_f_heritage, profile.heritage.labelRes())
    }
}

@Composable
private fun Compatibility(result: CompatibilityResult) {
    Section(R.string.disc_detail_compat) {
        val score = result.score
        Text(
            if (score == null) {
                stringResource(R.string.compatibility_unknown)
            } else {
                stringResource(R.string.compatibility, score)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        if (result.strong.isNotEmpty()) {
            val areas = result.strong.map { stringResource(it.labelRes()) }.joinToString()
            Text(stringResource(R.string.disc_detail_shared, areas))
        }
        if (result.differences.isNotEmpty()) {
            val areas = result.differences.map { stringResource(it.labelRes()) }.joinToString()
            Text(stringResource(R.string.disc_detail_differ, areas))
        }
        Text(stringResource(R.string.compatibility_explanation), style = MaterialTheme.typography.bodySmall)
    }
}

internal fun CompatibilityArea.labelRes() = when (this) {
    CompatibilityArea.TIMELINE -> R.string.area_timeline
    CompatibilityArea.WANTS_CHILDREN -> R.string.area_children
    CompatibilityArea.SMOKING -> R.string.area_smoking
    CompatibilityArea.RELOCATION -> R.string.area_relocation
    CompatibilityArea.RELIGION -> R.string.area_religion
    CompatibilityArea.LANGUAGES -> R.string.area_languages
    CompatibilityArea.VALUES -> R.string.area_values
    CompatibilityArea.INTERESTS -> R.string.area_interests
}

private val GALLERY_HEIGHT = 380.dp

@Preview(showBackground = true)
@Composable
private fun ProfileDetailPreview() {
    val demo = CommunityProfile(
        "a", "Amina (Demo)", 30, "London", "United Kingdom", biography = "Fictional preview profile.",
        languages = listOf("Somali", "English"), values = listOf("Family", "Honesty"), occupation = "Teacher",
        gender = Gender.WOMAN, heritage = Heritage.SOMALILAND, religiosity = Religiosity.PRACTISING,
        compatibility = CompatibilityResult(75, listOf(CompatibilityArea.TIMELINE), listOf(CompatibilityArea.SMOKING)),
        photos = listOf(PublicPhoto("p1", false), PublicPhoto("p2", true)),
    )
    CalafTheme {
        ProfileDetailScreen(demo, { null }, true, onBack = {}, onInterest = {}, onReport = {}, onBlock = {})
    }
}
