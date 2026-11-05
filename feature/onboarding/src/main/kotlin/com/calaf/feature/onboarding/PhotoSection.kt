package com.calaf.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calaf.core.model.PhotoRepository

/** The photo manager: up to six photos, reorder by drag, main photo, blur, moderation state, add and remove. */
@Composable
fun PhotoSection(uid: String, repository: PhotoRepository) {
    val model: PhotoViewModel = viewModel(
        key = "photos_$uid",
        factory = viewModelFactory { initializer { PhotoViewModel(repository) } },
    )
    val state by model.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    var remove by remember { mutableStateOf<String?>(null) }
    DisposableEffect(model) {
        model.load()
        onDispose { model.stop() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) model.add { PhotoProcessor.prepare(resolver, uri) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.photos_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.photos_private), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.onb_photo_limit), style = MaterialTheme.typography.bodySmall)
        if (state.simulatedModeration) {
            Text(
                stringResource(R.string.onb_photo_demo), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (state.supportsManagement) {
            Text(stringResource(R.string.onb_photo_drag), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.onb_photo_unmanaged), style = MaterialTheme.typography.bodySmall)
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error) Text(stringResource(R.string.photos_error), color = MaterialTheme.colorScheme.error)
        PhotoList(state, model::makePrimary, model::move, model::setBlur) { remove = it }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = state.canAdd,
        ) { Text(stringResource(R.string.photo_add)) }
        TextButton(onClick = model::load, enabled = !state.busy) { Text(stringResource(R.string.photos_reload)) }
    }
    remove?.let { photoId ->
        AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text(stringResource(R.string.photo_remove)) },
            text = { Text(stringResource(R.string.photo_remove_confirm)) },
            confirmButton = {
                TextButton(onClick = { model.remove(photoId); remove = null }, enabled = !state.busy) {
                    Text(stringResource(R.string.photo_remove))
                }
            },
            dismissButton = { TextButton(onClick = { remove = null }) { Text(stringResource(R.string.photo_cancel)) } },
        )
    }
}
