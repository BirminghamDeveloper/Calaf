package com.calaf.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.calaf.app.profile.PhotoSection
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.PhotoRepository
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class PhotoSectionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun photoPreviewAndConfirmedRemovalUpdateTheProfile() {
        val image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { out -> image.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
        image.recycle()
        val repository = object : PhotoRepository {
            var present = true
            override suspend fun slots() = if (present) listOf(0) else emptyList()
            override suspend fun read(slot: Int) = bytes
            override suspend fun upload(slot: Int, jpeg: ByteArray) { present = true }
            override suspend fun delete(slot: Int) { present = false }
        }
        compose.setContent { CalafTheme { PhotoSection("photo-ui", repository) } }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Private photo 1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Remove photo").performClick()
        compose.onAllNodesWithText("Remove photo").onLast().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Private photo 1").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Choose a photo").assertIsEnabled()
    }
}
