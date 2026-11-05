package com.calaf.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.calaf.core.model.PhotoState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PhotoRepositoryTest {
    @Test fun privatePhotoUploadReadAndRemovalUseLocalStorage() = runBlocking {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        val app = InstrumentationRegistry.getInstrumentation().targetContext.appEntryPoint()
        app.auth().logout()
        app.auth().register("photo-${System.currentTimeMillis()}@calaf.test", "PhotoCheck!2026")
        val auth = FirebaseAuth.getInstance(FirebaseApp.getInstance("calaf-local"))
        try {
            app.auth().sendVerification()
            val account = app.auth().refresh()!!
            assertTrue(account.emailVerified)
            val repository = app.photoRepositories().create(account.id)
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
            bitmap.recycle()
            assertTrue(repository.photos().isEmpty())
            val added = repository.add(bytes)
            try {
                assertEquals(listOf(added.id), repository.photos().map { it.id })
                // Nothing reviews live photos yet, so they stay pending and private.
                assertEquals(PhotoState.PENDING, added.state)
                assertArrayEquals(bytes, repository.read(added.id))
                assertTrue(runCatching { repository.reorder(listOf(added.id)) }.isFailure)
                assertTrue(runCatching { repository.readMemberPhoto(account.id, added.id) }.isFailure)
            } finally { repository.delete(added.id) }
            assertTrue(repository.photos().isEmpty())
        } finally {
            auth.currentUser?.delete()?.await()
            app.auth().logout()
        }
    }
}
