package com.calaf.app.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.PhotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PrivatePhoto(val slot: Int, val preview: Bitmap?)
data class PhotoState(val photos: List<PrivatePhoto> = emptyList(), val busy: Boolean = false,
    val ready: Boolean = false, val error: Boolean = false)
class PhotoViewModel(private val repository: PhotoRepository) : ViewModel() {
    private val mutable = MutableStateFlow(PhotoState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var revision = 0
    fun stop() { revision++; job?.cancel(); mutable.value = PhotoState() }
    fun load() = perform { reload() }
    private suspend fun reload() {
        val photos = repository.slots().map { slot ->
            val preview = try {
                val bytes = repository.read(slot)
                withContext(Dispatchers.Default) { PhotoProcessor.decode(bytes) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            PrivatePhoto(slot, preview)
        }
        mutable.update { it.copy(photos = photos, ready = true) }
    }
    fun add(prepare: suspend () -> ByteArray) {
        if (!state.value.ready) return
        val slot = (0..5).firstOrNull { id -> state.value.photos.none { it.slot == id } } ?: return
        perform { repository.upload(slot, prepare()); reload() }
    }
    fun remove(slot: Int) = perform { repository.delete(slot); reload() }
    private fun perform(action: suspend () -> Unit) {
        if (state.value.busy) return
        val current = ++revision
        mutable.update { it.copy(busy = true, error = false) }
        job = viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(error = true) } }
            finally { if (current == revision) mutable.update { it.copy(busy = false) } }
        }
    }
}
