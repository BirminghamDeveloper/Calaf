package com.calaf.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.calaf.feature.onboarding.PhotoSection
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoState
import com.calaf.core.testing.FakePhotoRepository
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class PhotoSectionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun photoPreviewAndConfirmedRemovalUpdateTheProfile() {
        val image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { out -> image.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
        image.recycle()
        val repository = FakePhotoRepository(listOf(Photo("p0", 0, true, PhotoState.APPROVED)))
        repository.bytes["p0"] = bytes
        compose.setContent { CalafTheme { PhotoSection("photo-ui", repository) } }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Private photo 1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Remove photo").performClick()
        compose.onAllNodesWithText("Remove photo").onLast().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Private photo 1").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Choose a photo").assertIsEnabled()
    }
}
