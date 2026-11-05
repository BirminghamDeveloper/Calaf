package com.calaf.app.demo

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityException
import com.calaf.core.model.MAX_PHOTOS
import com.calaf.core.model.PhotoState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoPhotoRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private fun fixture() = DemoFixture(temp.newFolder("store"))
    private fun jpeg(n: Int) = byteArrayOf(n.toByte(), 9, 9)

    @Test fun `seeded members start with two or three approved avatar photos`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val photos = demo.photos("demo-hassan").photos()
        assertTrue(photos.size in 2..3)
        assertTrue(photos.all { it.state == PhotoState.APPROVED })
        assertEquals(listOf(true, false), photos.take(2).map { it.isPrimary })
        assertArrayEquals(DemoAvatars.key(2).toByteArray(), demo.photos("demo-hassan").read(photos.first().id))
    }

    @Test fun `a new member can add photos which are approved at once, then read and delete them`() = runBlocking<Unit> {
        val demo = fixture()
        demo.auth.register("new@example.test", "DemoPass!2026")
        val uid = demo.session.requireUid()
        val photos = demo.photos(uid)
        assertTrue(photos.photos().isEmpty())
        val first = photos.add(jpeg(1))
        val second = photos.add(jpeg(2))
        assertEquals(PhotoState.APPROVED, first.state)
        assertTrue(photos.photos().first().isPrimary)
        assertArrayEquals(jpeg(2), demo.reopen().photos(uid).read(second.id))
        photos.delete(first.id)
        assertEquals(listOf(second.id), photos.photos().map { it.id })
        assertTrue(photos.photos().single().isPrimary)
        assertFalse(demo.store.rows("photos").any { it.first == first.id })
    }

    @Test fun `at most six photos and only sensible uploads are accepted`() = runBlocking<Unit> {
        val demo = fixture()
        demo.auth.register("new@example.test", "DemoPass!2026")
        val photos = demo.photos(demo.session.requireUid())
        repeat(MAX_PHOTOS) { photos.add(jpeg(it)) }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { photos.add(jpeg(7)) } }
        photos.delete(photos.photos().last().id)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { photos.add(ByteArray(0)) } }
        photos.add(jpeg(8))
        assertEquals(MAX_PHOTOS, photos.photos().size)
    }

    @Test fun `reordering, choosing the main photo and blur persist`() = runBlocking<Unit> {
        val demo = fixture()
        demo.auth.register("new@example.test", "DemoPass!2026")
        val photos = demo.photos(demo.session.requireUid())
        val a = photos.add(jpeg(1)).id
        val b = photos.add(jpeg(2)).id
        val c = photos.add(jpeg(3)).id
        photos.reorder(listOf(c, a, b))
        assertEquals(listOf(c, a, b), demo.reopen().photos(demo.session.requireUid()).photos().map { it.id })
        photos.setPrimary(b)
        val afterPrimary = photos.photos()
        assertEquals(listOf(b, c, a), afterPrimary.map { it.id })
        assertEquals(listOf(true, false, false), afterPrimary.map { it.isPrimary })
        photos.setBlur(a, true)
        assertTrue(demo.reopen().photos(demo.session.requireUid()).photos().single { it.id == a }.blurForNonMatches)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { photos.reorder(listOf(a, b)) } }
    }

    @Test fun `members cannot touch each others photos`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val mine = demo.photos("demo-hassan").photos().first().id
        assertTrue(runCatching { demo.photos("demo-amina").photos() }.isFailure)
        assertTrue(runCatching { demo.photos("demo-amina").add(jpeg(1)) }.isFailure)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { demo.photos("demo-hassan").read("nope") } }
        demo.auth.logout()
        demo.loginAsMember("amina")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { demo.photos("demo-amina").delete(mine) } }
    }

    @Test fun `other members only read approved photos of members who are not blocked`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsAdmin()
        val hassanPhoto = DemoPhotoRows.ownerPhotos(demo.store, "demo-hassan").first().id
        demo.admin.reviewPhoto(hassanPhoto, approve = false)
        demo.auth.logout()
        demo.loginAsMember("hodan")
        val viewer = demo.photos("demo-hodan")
        assertTrue(runCatching { viewer.readMemberPhoto("demo-hassan", hassanPhoto) }.exceptionOrNull() is CommunityException)
        val second = DemoPhotoRows.ownerPhotos(demo.store, "demo-hassan")[1].id
        assertArrayEquals(DemoAvatars.key(3).toByteArray(), viewer.readMemberPhoto("demo-hassan", second))
        // The photo id must belong to the stated owner.
        assertTrue(runCatching { viewer.readMemberPhoto("demo-amina", second) }.isFailure)
        demo.community.execute(CommunityCommand.Block("demo-hassan"))
        assertTrue(runCatching { viewer.readMemberPhoto("demo-hassan", second) }.isFailure)
    }

    @Test fun `a signed out caller reads nothing`() {
        val demo = fixture()
        assertTrue(runCatching { runBlocking { demo.photos("demo-hassan").readMemberPhoto("demo-hassan", "x") } }.isFailure)
    }
}
