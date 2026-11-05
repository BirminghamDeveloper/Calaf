package com.calaf.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.PublicPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One photo of another member. A photo flagged blurred is blurred before it is drawn, so the sharp image never
 * reaches the screen. With no photo, or while it loads, a calm initial is shown instead of an empty box.
 */
@Composable
fun MemberPhoto(
    profile: CommunityProfile,
    photo: PublicPhoto?,
    description: String,
    loader: suspend (PublicPhoto) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val bytes by produceState<ByteArray?>(null, profile.id, photo?.id, photo?.blurred) {
        value = null
        val loaded = photo?.let { loader(it) }
        value = if (loaded != null && photo.blurred) {
            withContext(Dispatchers.Default) { PhotoBlur.blur(loaded) }
        } else {
            loaded
        }
    }
    val shown = bytes
    if (shown != null) {
        AsyncImage(
            model = shown,
            contentDescription = description,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.name.take(1).uppercase(), style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
