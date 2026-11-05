package com.calaf.app

import android.content.Context
import com.calaf.app.demo.AvatarSource

/** Reads the bundled avatar images from the demo flavour's drawable resources. */
class AndroidAvatarSource(private val context: Context) : AvatarSource {
    override fun bytes(key: String): ByteArray {
        val id = context.resources.getIdentifier(key, "drawable", context.packageName)
        require(id != 0) { "Unknown avatar $key" }
        return context.resources.openRawResource(id).use { it.readBytes() }
    }
}
