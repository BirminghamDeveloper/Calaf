package com.calaf.app.demo

/** Where the bundled illustrated avatars come from. The Android build reads drawables, JVM tests use a fake. */
fun interface AvatarSource {
    fun bytes(key: String): ByteArray
}

/** The ten generated avatars bundled in `res/drawable-nodpi`. They are abstract art, never real people. */
object DemoAvatars {
    const val COUNT = 10

    fun key(number: Int): String = "avatar_%02d".format(((number % COUNT) + COUNT) % COUNT + 1)
}
