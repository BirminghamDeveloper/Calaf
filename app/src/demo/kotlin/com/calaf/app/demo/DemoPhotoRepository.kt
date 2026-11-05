package com.calaf.app.demo

import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoRepository
import com.calaf.core.model.PhotoRules
import com.calaf.core.model.PhotoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * One member's photos in the demo. Uploads are approved at once, which the screen labels as demo moderation. The demo
 * administrator can reject (or approve) any photo, and other members only ever read approved ones.
 */
class DemoPhotoRepository(
    private val store: DemoStore,
    private val session: DemoSession,
    private val avatars: AvatarSource,
    private val owner: String,
) : PhotoRepository {
    override val supportsManagement = true
    override val simulatedModeration = true

    private fun mine(): List<Photo> {
        session.requireOwner(owner)
        return DemoPhotoRows.ownerPhotos(store, owner)
    }

    private fun requireMine(photoId: String): List<Photo> {
        val photos = mine()
        require(photos.any { it.id == photoId }) { "Not your photo" }
        return photos
    }

    private fun commit(photos: List<Photo>, newRows: Map<String, org.json.JSONObject> = emptyMap()) {
        DemoPhotoRows.write(store, photos, newRows)
        store.save()
    }

    override suspend fun photos(): List<Photo> = withContext(Dispatchers.IO) { mine() }

    override suspend fun read(photoId: String): ByteArray = withContext(Dispatchers.IO) {
        requireMine(photoId)
        DemoPhotoRows.bytes(store, avatars, photoId)
    }

    override suspend fun add(jpeg: ByteArray): Photo = withContext(Dispatchers.IO) {
        val photos = mine()
        require(photos.size < MAX_PHOTOS) { "Photo limit reached" }
        require(jpeg.isNotEmpty() && jpeg.size < MAX_BYTES) { "Invalid photo" }
        val id = "p-${UUID.randomUUID()}"
        val target = DemoPhotoRows.file(store, owner, id)
        target.parentFile!!.mkdirs()
        target.writeBytes(jpeg)
        val added = Photo(id, photos.size, isPrimary = false, state = PhotoState.APPROVED)
        commit(PhotoRules.normalise(photos + added), mapOf(id to DemoPhotoRows.newRow(owner)))
        mine().single { it.id == id }
    }

    override suspend fun delete(photoId: String) {
        withContext(Dispatchers.IO) {
            val remaining = PhotoRules.without(requireMine(photoId), photoId)
            DemoPhotoRows.remove(store, photoId)
            DemoPhotoRows.file(store, owner, photoId).delete()
            commit(remaining)
        }
    }

    override suspend fun setPrimary(photoId: String) {
        withContext(Dispatchers.IO) { commit(PhotoRules.withPrimary(requireMine(photoId), photoId)) }
    }

    override suspend fun reorder(photoIds: List<String>) {
        withContext(Dispatchers.IO) { commit(PhotoRules.inOrder(mine(), photoIds)) }
    }

    override suspend fun setBlur(photoId: String, blur: Boolean) {
        withContext(Dispatchers.IO) {
            commit(requireMine(photoId).map { if (it.id == photoId) it.copy(blurForNonMatches = blur) else it })
        }
    }

    override suspend fun readMemberPhoto(ownerId: String, photoId: String): ByteArray = withContext(Dispatchers.IO) {
        val viewer = session.requireUid()
        val row = DemoPhotoRows.row(store, photoId)
        val visible = row != null && row.optString("owner") == ownerId && !store.isSuspended(ownerId) &&
            (ownerId == viewer || !store.isBlockedEitherWay(viewer, ownerId)) &&
            DemoPhotoRows.photo(photoId, row).state == PhotoState.APPROVED
        // Unapproved, rejected, blocked and unknown photos all look the same to the caller: not found.
        if (!visible) throw CommunityException(CommunityError.PERMISSION)
        DemoPhotoRows.bytes(store, avatars, photoId)
    }

    private companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}
