package com.calaf.app.demo

import com.calaf.core.model.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Private photos for one member, kept as files next to the demo store. Never shown to anyone else. */
class DemoPhotoRepository(
    private val store: DemoStore,
    private val session: DemoSession,
    private val owner: String,
) : PhotoRepository {
    private fun file(slot: Int): File {
        session.requireOwner(owner)
        require(slot in SLOTS)
        return File(File(store.photoRoot, owner), "photo-$slot.jpg")
    }

    override suspend fun slots(): List<Int> = withContext(Dispatchers.IO) { SLOTS.filter { file(it).exists() } }

    override suspend fun read(slot: Int): ByteArray = withContext(Dispatchers.IO) { file(slot).readBytes() }

    override suspend fun upload(slot: Int, jpeg: ByteArray) = withContext(Dispatchers.IO) {
        require(jpeg.isNotEmpty() && jpeg.size < MAX_BYTES)
        val target = file(slot)
        require(!target.exists())
        target.parentFile!!.mkdirs()
        target.writeBytes(jpeg)
    }

    override suspend fun delete(slot: Int) {
        withContext(Dispatchers.IO) {
            val target = file(slot)
            check(!target.exists() || target.delete())
        }
    }

    private companion object {
        val SLOTS = 0..5
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}
