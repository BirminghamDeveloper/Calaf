package com.calaf.core.model

import kotlinx.coroutines.CancellationException

/**
 * Reads other members' photos as [viewer] and remembers them, so a card that moves from the back of the stack to the
 * front does not read its photo again. Every failure (not approved, blocked, offline) is just "no photo".
 */
class MemberPhotoCache(private val photos: PhotoRepositoryFactory, private val viewer: String) {
    private val cache = mutableMapOf<String, ByteArray>()

    suspend fun photo(ownerId: String, photo: PublicPhoto): ByteArray? {
        val key = "$ownerId/${photo.id}"
        cache[key]?.let { return it }
        return try {
            photos.create(viewer).readMemberPhoto(ownerId, photo.id).also { cache[key] = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The viewer's own main photo and the other member's main photo, for the match moment. Matched members see each
     * other's photos clear, so blur is deliberately not applied here.
     */
    suspend fun matchPhotos(profile: CommunityProfile): Pair<ByteArray?, ByteArray?> {
        val mine = try {
            val own = photos.create(viewer)
            own.photos().firstOrNull { it.state == PhotoState.APPROVED }?.let { own.read(it.id) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val theirs = profile.photos.firstOrNull()?.let { photo(profile.id, it) }
        return mine to theirs
    }
}
