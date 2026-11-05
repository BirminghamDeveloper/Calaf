package com.calaf.feature.onboarding

import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoState
import com.calaf.core.testing.FakePhotoRepository
import com.calaf.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PhotoViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private fun photo(id: String, order: Int) = Photo(id, order, order == 0, PhotoState.APPROVED)
    private fun ids(model: PhotoViewModel) = model.state.value.items.map { it.photo.id }

    @Test fun `loading shows photos with their bytes and what the backend supports`() = runTest(main.dispatcher) {
        val repository = FakePhotoRepository(listOf(photo("a", 0), photo("b", 1)))
        repository.bytes["a"] = byteArrayOf(7)
        val model = PhotoViewModel(repository)
        assertTrue(model.state.value.supportsManagement)
        assertTrue(model.state.value.simulatedModeration)
        model.load(); runCurrent()
        assertEquals(listOf("a", "b"), ids(model))
        assertTrue(model.state.value.ready)
        assertNotNull(model.state.value.items.first().bytes)
        assertFalse(model.state.value.busy)
        model.stop()
        assertTrue(model.state.value.items.isEmpty())
    }

    @Test fun `adding stops at six photos`() = runTest(main.dispatcher) {
        val model = PhotoViewModel(FakePhotoRepository())
        model.load(); runCurrent()
        repeat(MAX_PHOTOS + 2) {
            model.add { byteArrayOf(it.toByte()) }
            runCurrent()
        }
        assertEquals(MAX_PHOTOS, ids(model).size)
        assertFalse(model.state.value.canAdd)
        assertFalse(model.state.value.error)
    }

    @Test fun `moving, choosing the main photo and blur update the list`() = runTest(main.dispatcher) {
        val model = PhotoViewModel(FakePhotoRepository(listOf(photo("a", 0), photo("b", 1), photo("c", 2))))
        model.load(); runCurrent()
        model.move(2, 0); runCurrent()
        assertEquals(listOf("c", "a", "b"), ids(model))
        assertTrue(model.state.value.items.first().photo.isPrimary)
        model.makePrimary("b"); runCurrent()
        assertEquals(listOf("b", "c", "a"), ids(model))
        model.setBlur("a", true); runCurrent()
        assertTrue(model.state.value.items.single { it.photo.id == "a" }.photo.blurForNonMatches)
        model.move(1, 1); model.move(9, 0); runCurrent()
        assertEquals(listOf("b", "c", "a"), ids(model))
    }

    @Test fun `removing the main photo promotes the next one`() = runTest(main.dispatcher) {
        val model = PhotoViewModel(FakePhotoRepository(listOf(photo("a", 0), photo("b", 1))))
        model.load(); runCurrent()
        model.remove("a"); runCurrent()
        assertEquals(listOf("b"), ids(model))
        assertTrue(model.state.value.items.single().photo.isPrimary)
    }

    @Test fun `a failure shows the error, frees the busy state and allows another try`() = runTest(main.dispatcher) {
        val repository = FakePhotoRepository(listOf(photo("a", 0)))
        val model = PhotoViewModel(repository)
        model.load(); runCurrent()
        repository.failWith = IOException("offline")
        model.remove("a"); runCurrent()
        assertTrue(model.state.value.error)
        assertFalse(model.state.value.busy)
        repository.failWith = null
        model.remove("a"); runCurrent()
        assertFalse(model.state.value.error)
        assertTrue(ids(model).isEmpty())
    }
}
