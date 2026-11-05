package com.calaf.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Blurs an image for members who are not matched with its owner. It shrinks the picture to a few pixels and scales
 * it back up, which hides every detail on all Android versions (unlike a render time blur, which needs Android 12).
 * The sharp bytes are never handed to the screen when a photo is blurred.
 */
object PhotoBlur {
    private const val SMALL_SIDE = 12
    private const val OUTPUT_SIDE = 256
    private const val QUALITY = 80

    /** Returns blurred JPEG bytes, or [bytes] unchanged if they cannot be decoded as an image. */
    fun blur(bytes: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val small = Bitmap.createScaledBitmap(source, SMALL_SIDE, SMALL_SIDE, true)
        val large = Bitmap.createScaledBitmap(small, OUTPUT_SIDE, OUTPUT_SIDE, true)
        return ByteArrayOutputStream().use { out ->
            large.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }.also {
            if (source !== small) source.recycle()
            small.recycle()
            large.recycle()
        }
    }
}
