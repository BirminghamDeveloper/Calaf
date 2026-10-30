package com.calaf.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.calaf.app.profile.PhotoProcessor
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PhotoProcessorTest {
    @Test fun resizingCorrectsOrientationAndRemovesLocationMetadata() {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File.createTempFile("photo-check", ".jpg", directory)
        try {
            val image = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
            image.eraseColor(Color.GREEN)
            file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            image.recycle()
            ExifInterface(file).apply {
                setLatLong(51.5, -0.1)
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val result = PhotoProcessor.encode(file.readBytes())
            val metadata = result.inputStream().use(::ExifInterface)
            assertNull(metadata.latLong)
            val decoded = PhotoProcessor.decode(result, 1600)
            assertEquals(800, decoded.width)
            assertEquals(1600, decoded.height)
            assertTrue(result.size < 5 * 1024 * 1024)
            decoded.recycle()
        } finally { file.delete() }
    }
    @Test fun unreadableImagesAreRejected() {
        assertThrows(Exception::class.java) { PhotoProcessor.encode("not an image".toByteArray()) }
    }
}
