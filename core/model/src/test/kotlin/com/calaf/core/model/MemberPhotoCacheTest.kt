package com.calaf.core.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemberPhotoCacheTest {
    private class Photos(var reads: Int = 0, var fail: Boolean = false) : PhotoRepository {
        override val supportsManagement = true
        override val simulatedModeration = false
        var own = listOf(Photo("mine", 0, true, PhotoState.APPROVED), Photo("later", 1, false, PhotoState.PENDING))
        override suspend fun photos() = own
        override suspend fun read(photoId: String) = byteArrayOf(1, photoId.length.toByte())
        override suspend fun add(jpeg: ByteArray): Photo = error("unused")
        override suspend fun delete(photoId: String) = error("unused")
        override suspend fun setPrimary(photoId: String) = error("unused")
        override suspend fun reorder(photoIds: List<String>) = error("unused")
        override suspend fun setBlur(photoId: String, blur: Boolean) = error("unused")
        override suspend fun readMemberPhoto(ownerId: String, photoId: String): ByteArray {
            reads++
            if (fail) throw IllegalStateException("not approved")
            return "$ownerId/$photoId".toByteArray()
        }
    }

    private val member = CommunityProfile("owner", "Owner", 30, "A", "B", photos = listOf(PublicPhoto("p1", true)))

    @Test fun `a photo is read once and then served from memory`() = runBlocking {
        val repository = Photos()
        val cache = MemberPhotoCache({ repository }, "me")
        assertArrayEquals("owner/p1".toByteArray(), cache.photo("owner", PublicPhoto("p1", false)))
        cache.photo("owner", PublicPhoto("p1", false))
        assertEquals(1, repository.reads)
    }

    @Test fun `any failure is simply no photo and is not remembered`() = runBlocking {
        val repository = Photos(fail = true)
        val cache = MemberPhotoCache({ repository }, "me")
        assertNull(cache.photo("owner", PublicPhoto("p1", false)))
        repository.fail = false
        assertArrayEquals("owner/p1".toByteArray(), cache.photo("owner", PublicPhoto("p1", false)))
    }

    @Test fun `match photos are the viewers approved main photo and the other members main photo`() = runBlocking {
        val cache = MemberPhotoCache({ Photos() }, "me")
        val (mine, theirs) = cache.matchPhotos(member)
        assertArrayEquals(byteArrayOf(1, 4), mine)
        assertArrayEquals("owner/p1".toByteArray(), theirs)
        val none = cache.matchPhotos(member.copy(photos = emptyList()))
        assertNull(none.second)
        val pendingOnly = MemberPhotoCache({ Photos().apply { own = own.drop(1) } }, "me").matchPhotos(member)
        assertNull(pendingOnly.first)
    }
}
