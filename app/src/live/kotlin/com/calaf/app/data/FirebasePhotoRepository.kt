package com.calaf.app.data

import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoRepository
import com.calaf.core.model.PhotoState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Private owner only objects in Storage. Never create or expose public download URLs.
 *
 * The live photo pipeline (thumbnails, review, ordering, blur, public copies) is deferred, so this only stores and
 * lists the member's own photos. They all show as [PhotoState.PENDING] because nothing reviews them yet, and
 * other members can see none of them. Ordering is by slot and the first slot is the main photo.
 */
class FirebasePhotoRepository(
    private val uid: String,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
) : PhotoRepository {
    override val supportsManagement = false
    override val simulatedModeration = false

    private fun reference(slot: Int) = run {
        require(slot in 0 until MAX_PHOTOS)
        check(auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true)
        storage.reference.child("privateUploads/$uid/photo-$slot.jpg")
    }

    private fun slot(photoId: String): Int = photoId.removePrefix(PREFIX).toIntOrNull()?.takeIf { photoId.startsWith(PREFIX) }
        ?: throw IllegalArgumentException("Unknown photo")

    private suspend fun usedSlots(): List<Int> = buildList {
        for (slot in 0 until MAX_PHOTOS) {
            try {
                reference(slot).metadata.await()
                add(slot)
            } catch (error: StorageException) {
                if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error
            }
        }
    }

    override suspend fun photos(): List<Photo> =
        usedSlots().mapIndexed { index, slot -> Photo("$PREFIX$slot", index, index == 0, PhotoState.PENDING) }

    override suspend fun read(photoId: String): ByteArray = reference(slot(photoId)).getBytes(MAX_BYTES).await()

    override suspend fun add(jpeg: ByteArray): Photo {
        require(jpeg.isNotEmpty() && jpeg.size < MAX_BYTES)
        val used = usedSlots()
        val slot = (0 until MAX_PHOTOS).firstOrNull { it !in used } ?: throw IllegalArgumentException("Photo limit reached")
        val task = reference(slot).putBytes(
            jpeg,
            StorageMetadata.Builder().setContentType("image/jpeg").setCacheControl("private, no-store").build(),
        )
        try {
            task.await()
        } catch (cancelled: CancellationException) {
            task.cancel()
            throw cancelled
        }
        return photos().single { it.id == "$PREFIX$slot" }
    }

    override suspend fun delete(photoId: String) {
        try {
            reference(slot(photoId)).delete().await()
        } catch (error: StorageException) {
            if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error
        }
    }

    override suspend fun setPrimary(photoId: String) = unsupported()

    override suspend fun reorder(photoIds: List<String>) = unsupported()

    override suspend fun setBlur(photoId: String, blur: Boolean) = unsupported()

    /** Other members cannot see live photos yet: nothing is reviewed or copied to a public location. */
    override suspend fun readMemberPhoto(ownerId: String, photoId: String): ByteArray = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Not available until the live photo pipeline")

    private companion object {
        const val PREFIX = "photo-"
        const val MAX_BYTES = 5L * 1024 * 1024
    }
}
