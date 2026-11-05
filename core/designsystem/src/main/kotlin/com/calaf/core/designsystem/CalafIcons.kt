package com.calaf.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/** One icon with an outlined drawable and, for navigation icons, a filled one for the selected state. */
class CalafIcon(@DrawableRes val outlined: Int, @DrawableRes val filled: Int = outlined)

/**
 * Material Symbols Rounded (Apache 2.0), copied as vector drawables from
 * github.com/google/material-design-icons `symbols/android`. Outlined when not selected, filled when selected.
 */
object CalafIcons {
    val Discover = CalafIcon(R.drawable.ic_calaf_discover, R.drawable.ic_calaf_discover_filled)
    val Matches = CalafIcon(R.drawable.ic_calaf_matches, R.drawable.ic_calaf_matches_filled)
    val Messages = CalafIcon(R.drawable.ic_calaf_messages, R.drawable.ic_calaf_messages_filled)
    val Activity = CalafIcon(R.drawable.ic_calaf_activity, R.drawable.ic_calaf_activity_filled)
    val Profile = CalafIcon(R.drawable.ic_calaf_profile, R.drawable.ic_calaf_profile_filled)
    val Back = CalafIcon(R.drawable.ic_calaf_back)
    val More = CalafIcon(R.drawable.ic_calaf_more)
    val Report = CalafIcon(R.drawable.ic_calaf_report)
    val Block = CalafIcon(R.drawable.ic_calaf_block)
    val Send = CalafIcon(R.drawable.ic_calaf_send)
    val Settings = CalafIcon(R.drawable.ic_calaf_settings)
    val Close = CalafIcon(R.drawable.ic_calaf_close)
}

/**
 * Draws [icon]. Pass a [contentDescription] for any icon that stands alone as a button, and null only when
 * a visible text label next to it already says the same thing.
 */
@Composable
fun CalafIcon(
    icon: CalafIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Icon(
        painterResource(if (selected) icon.filled else icon.outlined),
        contentDescription,
        modifier.size(CalafDimensions.icon),
    )
}
