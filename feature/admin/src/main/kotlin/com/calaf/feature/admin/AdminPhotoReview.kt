package com.calaf.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.calaf.core.model.AdminPhoto
import com.calaf.core.model.AdminRepository
import com.calaf.core.model.PhotoState

/** The demo photo queue: every photo with who owns it and Approve or Reject. [confirm] asks before acting. */
@Composable
internal fun AdminPhotoReview(
    admin: AdminRepository,
    revision: Int,
    confirm: (message: String, action: () -> Unit) -> Unit,
) {
    val photos = remember(revision) { admin.photos() }
    val approveText = stringResource(R.string.admin_photo_approve_confirm)
    val rejectText = stringResource(R.string.admin_photo_reject_confirm)
    Text(stringResource(R.string.admin_photos_title, photos.size), style = MaterialTheme.typography.titleMedium)
    photos.forEach { photo ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val bytes = remember(revision, photo.id) { runCatching { admin.photoBytes(photo.id) }.getOrNull() }
                AsyncImage(
                    model = bytes,
                    contentDescription = stringResource(R.string.admin_photo_description, photo.ownerEmail),
                    modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Column {
                    Text(stringResource(R.string.admin_photo_owner, photo.ownerEmail))
                    Text(stringResource(stateLabel(photo)), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(
                            onClick = { confirm(approveText) { admin.reviewPhoto(photo.id, approve = true) } },
                            enabled = photo.state != PhotoState.APPROVED,
                        ) { Text(stringResource(R.string.admin_photo_approve)) }
                        TextButton(
                            onClick = { confirm(rejectText) { admin.reviewPhoto(photo.id, approve = false) } },
                            enabled = photo.state != PhotoState.REJECTED,
                        ) { Text(stringResource(R.string.admin_photo_reject)) }
                    }
                }
            }
        }
    }
}

private fun stateLabel(photo: AdminPhoto) = when (photo.state) {
    PhotoState.PENDING -> R.string.admin_photo_state_pending
    PhotoState.APPROVED -> R.string.admin_photo_state_approved
    PhotoState.REJECTED -> R.string.admin_photo_state_rejected
}
