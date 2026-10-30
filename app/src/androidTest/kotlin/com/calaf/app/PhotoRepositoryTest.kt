package com.calaf.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
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
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CalafApplication
        app.auth.logout()
        app.auth.register("photo-${System.currentTimeMillis()}@calaf.test", "PhotoCheck!2026")
        val auth = FirebaseAuth.getInstance(FirebaseApp.getInstance("calaf-local"))
        try {
            app.auth.sendVerification()
            val account = app.auth.refresh()!!
            assertTrue(account.emailVerified)
            val repository = app.photos(account.id)
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
            bitmap.recycle()
            assertTrue(repository.slots().isEmpty())
            repository.upload(0, bytes)
            try {
                assertEquals(listOf(0), repository.slots())
                assertArrayEquals(bytes, repository.read(0))
                assertTrue(runCatching { repository.upload(0, bytes) }.isFailure)
                assertTrue(runCatching { repository.upload(6, bytes) }.isFailure)
            } finally { repository.delete(0) }
            assertTrue(repository.slots().isEmpty())
        } finally {
            auth.currentUser?.delete()?.await()
            app.auth.logout()
        }
    }
}
