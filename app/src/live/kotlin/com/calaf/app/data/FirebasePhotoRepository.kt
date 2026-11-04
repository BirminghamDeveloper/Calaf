package com.calaf.app.data

import com.calaf.core.model.PhotoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Private owner-only objects; never create or expose public download URLs. */
class FirebasePhotoRepository(private val uid: String, private val auth: FirebaseAuth,
    private val storage: FirebaseStorage) : PhotoRepository {
    private fun reference(slot: Int) = run {
        require(slot in 0..5)
        check(auth.currentUser?.uid == uid && auth.currentUser?.isEmailVerified == true)
        storage.reference.child("privateUploads/$uid/photo-$slot.jpg")
    }
    override suspend fun slots(): List<Int> = buildList {
        for (slot in 0..5) {
            try { reference(slot).metadata.await(); add(slot) }
            catch (error: StorageException) { if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error }
        }
    }
    override suspend fun read(slot: Int) = reference(slot).getBytes(5L * 1024 * 1024).await()
    override suspend fun upload(slot: Int, jpeg: ByteArray) {
        require(jpeg.isNotEmpty() && jpeg.size < 5 * 1024 * 1024)
        val task = reference(slot).putBytes(jpeg, StorageMetadata.Builder().setContentType("image/jpeg")
            .setCacheControl("private, no-store").build())
        try { task.await() } catch (cancelled: CancellationException) { task.cancel(); throw cancelled }
    }
    override suspend fun delete(slot: Int) {
        try { reference(slot).delete().await() }
        catch (error: StorageException) { if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error }
    }
}
