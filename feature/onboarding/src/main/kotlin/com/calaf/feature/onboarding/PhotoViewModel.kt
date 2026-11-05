package com.calaf.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoRepository
import com.calaf.core.model.PhotoRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A photo with its image bytes for display, or null if the image could not be read. */
data class PhotoItem(val photo: Photo, val bytes: ByteArray?)

data class PhotoUiState(
    val items: List<PhotoItem> = emptyList(),
    val busy: Boolean = false,
    val ready: Boolean = false,
    val error: Boolean = false,
    /** Reordering, choosing the main photo and blur are only offered when the backend supports them. */
    val supportsManagement: Boolean = false,
    /** True in the demo, where moderation is simulated and the screen must say so. */
    val simulatedModeration: Boolean = false,
) {
    val canAdd: Boolean get() = ready && !busy && items.size < MAX_PHOTOS
}

class PhotoViewModel(private val repository: PhotoRepository) : ViewModel() {
    private val mutable = MutableStateFlow(
        PhotoUiState(
            supportsManagement = repository.supportsManagement,
            simulatedModeration = repository.simulatedModeration,
        ),
    )
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var revision = 0
    private val previews = mutableMapOf<String, ByteArray?>()

    fun stop() {
        revision++
        job?.cancel()
        mutable.update { it.copy(items = emptyList(), busy = false, ready = false, error = false) }
    }

    fun load() = perform { reload() }

    private suspend fun reload() {
        val photos = repository.photos()
        previews.keys.retainAll(photos.map { it.id }.toSet())
        val items = photos.map { photo -> PhotoItem(photo, preview(photo)) }
        mutable.update { it.copy(items = items, ready = true) }
    }

    private suspend fun preview(photo: Photo): ByteArray? = previews.getOrPut(photo.id) {
        try {
            repository.read(photo.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    /** Adds a photo. [prepare] runs off the main thread to strip metadata and resize the picked image. */
    fun add(prepare: suspend () -> ByteArray) {
        if (!state.value.canAdd) return
        perform {
            repository.add(prepare())
            reload()
        }
    }

    fun remove(photoId: String) = perform {
        repository.delete(photoId)
        reload()
    }

    fun makePrimary(photoId: String) = perform {
        repository.setPrimary(photoId)
        reload()
    }

    /** Moves the photo at position [from] to position [to], as dragging or the move buttons ask. */
    fun move(from: Int, to: Int) {
        val photos = state.value.items.map { it.photo }
        if (from == to || from !in photos.indices || to !in photos.indices) return
        perform {
            repository.reorder(PhotoRules.move(photos, from, to).map { it.id })
            reload()
        }
    }

    fun setBlur(photoId: String, blur: Boolean) = perform {
        repository.setBlur(photoId, blur)
        reload()
    }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.busy) return
        val current = ++revision
        mutable.update { it.copy(busy = true, error = false) }
        job = viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(error = true) }
            } finally {
                if (current == revision) mutable.update { it.copy(busy = false) }
            }
        }
    }
}
