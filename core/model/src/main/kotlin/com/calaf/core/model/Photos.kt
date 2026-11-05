package com.calaf.core.model

/** Moderation state of a photo. Other members only ever see [APPROVED] photos. */
enum class PhotoState { PENDING, APPROVED, REJECTED }

const val MAX_PHOTOS = 6

/**
 * One of a member's own photos. The photo with [isPrimary] is always first (order 0) and is the one shown on cards.
 * [blurForNonMatches] asks for the photo to be blurred for everyone the member has not matched with.
 */
data class Photo(
    val id: String,
    val order: Int,
    val isPrimary: Boolean,
    val state: PhotoState,
    val blurForNonMatches: Boolean = false,
)

/** What another member may see of a photo: its id and whether it must be shown blurred. Approved photos only. */
data class PublicPhoto(val id: String, val blurred: Boolean)

interface PhotoRepository {
    /** True when this backend can reorder photos, choose the primary photo and set blur. */
    val supportsManagement: Boolean

    /** True when moderation is simulated (the demo), so the screen must say so. */
    val simulatedModeration: Boolean

    /** The member's own photos in display order. */
    suspend fun photos(): List<Photo>

    /** The image bytes of one of the member's own photos, whatever its state. */
    suspend fun read(photoId: String): ByteArray

    /** Adds a processed JPEG at the end. Fails once [MAX_PHOTOS] are reached. */
    suspend fun add(jpeg: ByteArray): Photo

    suspend fun delete(photoId: String)

    /** Makes the photo the main one by moving it to the front. */
    suspend fun setPrimary(photoId: String)

    /** Puts the photos in the given order. [photoIds] must contain exactly the member's current photos. */
    suspend fun reorder(photoIds: List<String>)

    suspend fun setBlur(photoId: String, blur: Boolean)

    /** The image bytes of another member's photo. Fails unless the photo exists and is approved. */
    suspend fun readMemberPhoto(ownerId: String, photoId: String): ByteArray
}

/** Creates the photo repository for one signed in member. Bound per flavour. */
fun interface PhotoRepositoryFactory {
    fun create(uid: String): PhotoRepository
}

/** Pure list rules shared by every photo repository, so ordering and the single primary photo behave the same. */
object PhotoRules {
    /** Orders photos 0..n-1 with exactly the first one primary. */
    fun normalise(photos: List<Photo>): List<Photo> = renumber(photos.sortedBy { it.order })

    /** Numbers the photos by their position in [photos], ignoring their old order. */
    private fun renumber(photos: List<Photo>): List<Photo> =
        photos.mapIndexed { index, photo -> photo.copy(order = index, isPrimary = index == 0) }

    fun move(photos: List<Photo>, from: Int, to: Int): List<Photo> {
        val sorted = photos.sortedBy { it.order }.toMutableList()
        if (from !in sorted.indices || to !in sorted.indices || from == to) return renumber(sorted)
        sorted.add(to, sorted.removeAt(from))
        return renumber(sorted)
    }

    fun withPrimary(photos: List<Photo>, id: String): List<Photo> {
        val index = photos.sortedBy { it.order }.indexOfFirst { it.id == id }
        return if (index < 0) normalise(photos) else move(photos, index, 0)
    }

    fun without(photos: List<Photo>, id: String): List<Photo> = normalise(photos.filterNot { it.id == id })

    fun inOrder(photos: List<Photo>, ids: List<String>): List<Photo> {
        require(ids.toSet() == photos.map { it.id }.toSet() && ids.size == photos.size) { "Not the same photos" }
        val byId = photos.associateBy { it.id }
        return renumber(ids.map { byId.getValue(it) })
    }
}
