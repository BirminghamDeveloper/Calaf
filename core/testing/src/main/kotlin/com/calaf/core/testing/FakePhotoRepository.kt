package com.calaf.core.testing

import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoRepository
import com.calaf.core.model.PhotoRules
import com.calaf.core.model.PhotoState

/** An in memory [PhotoRepository]. New photos are approved at once, like the demo. Set [failWith] to make calls throw. */
class FakePhotoRepository(initial: List<Photo> = emptyList()) : PhotoRepository {
    override var supportsManagement = true
    override var simulatedModeration = true
    var failWith: Exception? = null
    private var list = PhotoRules.normalise(initial)
    private var counter = 0
    val bytes = mutableMapOf<String, ByteArray>()

    private fun check() {
        failWith?.let { throw it }
    }

    override suspend fun photos(): List<Photo> {
        check()
        return list
    }

    override suspend fun read(photoId: String): ByteArray {
        check()
        return bytes[photoId] ?: byteArrayOf(1)
    }

    override suspend fun add(jpeg: ByteArray): Photo {
        check()
        require(list.size < MAX_PHOTOS)
        val photo = Photo("p${counter++}", list.size, false, PhotoState.APPROVED)
        bytes[photo.id] = jpeg
        list = PhotoRules.normalise(list + photo)
        return list.last()
    }

    override suspend fun delete(photoId: String) {
        check()
        list = PhotoRules.without(list, photoId)
    }

    override suspend fun setPrimary(photoId: String) {
        check()
        list = PhotoRules.withPrimary(list, photoId)
    }

    override suspend fun reorder(photoIds: List<String>) {
        check()
        list = PhotoRules.inOrder(list, photoIds)
    }

    override suspend fun setBlur(photoId: String, blur: Boolean) {
        check()
        list = list.map { if (it.id == photoId) it.copy(blurForNonMatches = blur) else it }
    }

    override suspend fun readMemberPhoto(ownerId: String, photoId: String): ByteArray {
        check()
        return bytes[photoId] ?: byteArrayOf(1)
    }
}
