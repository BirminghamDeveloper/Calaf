package com.calaf.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.MatchMoment

/**
 * Loads both main photos and shows [MatchMomentScreen] for a new match. Back means keep browsing. [onMessage] gets
 * the match id so the caller can open that chat.
 */
@Composable
fun MatchMomentOverlay(
    moment: MatchMoment,
    yourName: String,
    loadPhotos: suspend (CommunityProfile) -> Pair<ByteArray?, ByteArray?>,
    onMessage: (String) -> Unit,
    onKeepBrowsing: () -> Unit,
) {
    val photos by produceState<Pair<ByteArray?, ByteArray?>>(null to null, moment.matchId) {
        value = loadPhotos(moment.profile)
    }
    BackHandler(onBack = onKeepBrowsing)
    MatchMomentScreen(
        yourName = yourName, theirName = moment.profile.name, yourPhoto = photos.first, theirPhoto = photos.second,
        onMessage = { onMessage(moment.matchId) }, onKeepBrowsing = onKeepBrowsing,
    )
}
