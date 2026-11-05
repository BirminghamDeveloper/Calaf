package com.calaf.feature.onboarding

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.calaf.core.designsystem.CalafDimensions
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.PhotoState
import kotlin.math.roundToInt

private val ThumbnailSize = 96.dp

@Composable
internal fun PhotoList(
    state: PhotoUiState,
    onMakePrimary: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onBlur: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalafSpacing.small)) {
        state.items.forEachIndexed { index, item ->
            PhotoRow(
                item, index, state.items.size, state.supportsManagement, !state.busy,
                onMakePrimary, onMove, onBlur, onRemove,
            )
        }
    }
}

@Composable
private fun PhotoRow(
    item: PhotoItem,
    index: Int,
    count: Int,
    manage: Boolean,
    enabled: Boolean,
    onMakePrimary: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onBlur: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    val photo = item.photo
    var rowHeight by remember { mutableFloatStateOf(1f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f).offset { IntOffset(0, dragOffset.roundToInt()) }
            .onSizeChanged { rowHeight = it.height.toFloat().coerceAtLeast(1f) },
    ) {
        Row(
            Modifier.padding(CalafSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small),
            verticalAlignment = Alignment.Top,
        ) {
            if (manage) {
                val handle = stringResource(R.string.onb_photo_handle, index + 1)
                Box(
                    Modifier.size(CalafDimensions.touchTarget).semantics { contentDescription = handle }
                        .pointerInput(index, count, enabled) {
                            if (!enabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = true },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                },
                                onDragEnd = {
                                    val target = (index + (dragOffset / rowHeight).roundToInt()).coerceIn(0, count - 1)
                                    dragOffset = 0f
                                    dragging = false
                                    if (target != index) onMove(index, target)
                                },
                                onDragCancel = {
                                    dragOffset = 0f
                                    dragging = false
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(HANDLE_GLYPH, style = MaterialTheme.typography.titleLarge) }
            }
            Thumbnail(item, index)
            Details(item, index, count, manage, enabled, onMakePrimary, onMove, onBlur, onRemove)
        }
    }
}

@Composable
private fun Thumbnail(item: PhotoItem, index: Int) {
    val shape = Modifier.size(ThumbnailSize).clip(MaterialTheme.shapes.medium)
    if (item.bytes != null) {
        AsyncImage(
            model = item.bytes, contentDescription = stringResource(R.string.photo_description, index + 1),
            modifier = shape, contentScale = ContentScale.Crop,
        )
    } else {
        Box(shape, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.photo_preview_failed), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Details(
    item: PhotoItem,
    index: Int,
    count: Int,
    manage: Boolean,
    enabled: Boolean,
    onMakePrimary: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onBlur: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    val photo = item.photo
    Column(Modifier.padding(start = CalafSpacing.small)) {
        if (photo.isPrimary) {
            Text(stringResource(R.string.onb_photo_main), style = MaterialTheme.typography.labelLarge)
        }
        Text(stringResource(stateLabel(photo.state)), style = MaterialTheme.typography.bodySmall)
        if (manage) {
            if (!photo.isPrimary) {
                TextButton(onClick = { onMakePrimary(photo.id) }, enabled = enabled) {
                    Text(stringResource(R.string.onb_photo_make_main))
                }
            }
            Row {
                TextButton(onClick = { onMove(index, index - 1) }, enabled = enabled && index > 0) {
                    Text(stringResource(R.string.onb_photo_up))
                }
                TextButton(onClick = { onMove(index, index + 1) }, enabled = enabled && index < count - 1) {
                    Text(stringResource(R.string.onb_photo_down))
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = CalafDimensions.touchTarget).toggleable(
                    photo.blurForNonMatches, enabled = enabled, role = Role.Switch,
                    onValueChange = { onBlur(photo.id, it) },
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CalafSpacing.small),
            ) {
                Text(
                    stringResource(R.string.onb_photo_blur), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Switch(photo.blurForNonMatches, null, enabled = enabled)
            }
        }
        TextButton(onClick = { onRemove(photo.id) }, enabled = enabled) { Text(stringResource(R.string.photo_remove)) }
    }
}

private fun stateLabel(state: PhotoState) = when (state) {
    PhotoState.PENDING -> R.string.onb_photo_pending
    PhotoState.APPROVED -> R.string.onb_photo_approved
    PhotoState.REJECTED -> R.string.onb_photo_rejected
}

private const val HANDLE_GLYPH = "☰"
