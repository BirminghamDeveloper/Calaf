package com.calaf.app.demo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoPhotoRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private val jpeg = byteArrayOf(1, 2, 3)

    @Test fun `upload, read and delete a private photo`() = runBlocking {
        val demo = DemoFixture(temp.newFolder("store"))
        demo.loginAsMember("hassan")
        val photos = demo.photos("demo-hassan")
        assertTrue(photos.slots().isEmpty())
        photos.upload(2, jpeg)
        assertEquals(listOf(2), photos.slots())
        assertArrayEquals(jpeg, demo.reopen().photos("demo-hassan").read(2))
        photos.delete(2)
        assertTrue(photos.slots().isEmpty())
    }

    @Test fun `bad uploads and other members are refused`() = runBlocking {
        val demo = DemoFixture(temp.newFolder("store"))
        demo.loginAsMember("hassan")
        val photos = demo.photos("demo-hassan")
        photos.upload(0, jpeg)
        assertTrue(runCatching { photos.upload(0, jpeg) }.isFailure)
        assertTrue(runCatching { photos.upload(1, ByteArray(0)) }.isFailure)
        assertTrue(runCatching { photos.upload(6, jpeg) }.isFailure)
        assertTrue(runCatching { demo.photos("demo-amina").slots() }.isFailure)
    }
}
