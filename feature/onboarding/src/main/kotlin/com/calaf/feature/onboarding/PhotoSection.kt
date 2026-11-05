package com.calaf.app.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calaf.app.R
import com.calaf.core.model.PhotoRepository

@Composable fun PhotoSection(uid: String, repository: PhotoRepository) {
    val model: PhotoViewModel = viewModel(key = "photos_$uid", factory = viewModelFactory { initializer { PhotoViewModel(repository) } })
    val state by model.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    var remove by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(model) { model.load(); onDispose { model.stop() } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) model.add { PhotoProcessor.prepare(resolver, uri) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.photos_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.photos_private), style = MaterialTheme.typography.bodySmall)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error) Text(stringResource(R.string.photos_error), color = MaterialTheme.colorScheme.error)
        state.photos.forEach { photo ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    val preview = photo.preview
                    if (preview != null) Image(preview.asImageBitmap(), stringResource(R.string.photo_description, photo.slot + 1),
                        modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
                    else Text(stringResource(R.string.photo_preview_failed))
                    TextButton(onClick = { remove = photo.slot }, enabled = !state.busy) { Text(stringResource(R.string.photo_remove)) }
                }
            }
        }
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = state.ready && !state.busy && state.photos.size < 6) { Text(stringResource(R.string.photo_add)) }
        TextButton(onClick = model::load, enabled = !state.busy) { Text(stringResource(R.string.photos_reload)) }
    }
    remove?.let { slot -> AlertDialog(onDismissRequest = { remove = null },
        title = { Text(stringResource(R.string.photo_remove)) }, text = { Text(stringResource(R.string.photo_remove_confirm)) },
        confirmButton = { TextButton(onClick = { model.remove(slot); remove = null }, enabled = !state.busy) { Text(stringResource(R.string.photo_remove)) } },
        dismissButton = { TextButton(onClick = { remove = null }) { Text(stringResource(R.string.photo_cancel)) } }) }
}
