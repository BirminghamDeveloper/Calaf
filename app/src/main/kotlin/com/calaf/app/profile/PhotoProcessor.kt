package com.calaf.app.profile

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoProcessor {
    private const val MAX_INPUT = 15 * 1024 * 1024
    suspend fun prepare(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        require(uri.scheme == "content")
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) {
                require(out.size() + count <= MAX_INPUT)
                out.write(buffer, 0, count)
                count = input.read(buffer)
            }
            out.toByteArray()
        } ?: error("Image unavailable")
        encode(bytes)
    }
    /** Re-encoding corrects orientation and strips GPS/EXIF and embedded source metadata. */
    fun encode(bytes: ByteArray): ByteArray {
        require(bytes.size <= MAX_INPUT)
        val exif = bytes.inputStream().use(::ExifInterface)
        val source = decode(bytes, 1600)
        val matrix = Matrix().apply {
            if (exif.isFlipped) postScale(-1f, 1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val ratio = minOf(1f, 1600f / maxOf(rotated.width, rotated.height))
        val scaled = Bitmap.createScaledBitmap(rotated, maxOf(1, (rotated.width * ratio).toInt()), maxOf(1, (rotated.height * ratio).toInt()), true)
        try {
            return ByteArrayOutputStream().use { out ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, out))
                out.toByteArray().also { require(it.isNotEmpty() && it.size < 5 * 1024 * 1024) }
            }
        } finally {
            if (scaled !== rotated && scaled !== source) scaled.recycle()
            if (rotated !== source) rotated.recycle()
            source.recycle()
        }
    }
    fun decode(bytes: ByteArray, maximum: Int = 512): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000 && bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maximum * 2) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Image cannot be decoded")
    }
}
