package com.calaf.app.demo

import java.io.File

/** A demo backend over a temp directory. [reopen] simulates the app being closed and started again. */
internal class DemoFixture(val directory: File) {
    val store = DemoStore(directory)
    val session = DemoSession(store)
    val auth = DemoAuthRepository(store, session)
    val profiles = DemoProfileRepository(store, session)
    val community = DemoCommunityRepository(store, session)
    val avatars = FakeAvatars()
    val admin = DemoAdminRepository(store, session, avatars)

    fun photos(owner: String) = DemoPhotoRepository(store, session, avatars, owner)

    fun reopen() = DemoFixture(directory)

    suspend fun loginAsMember(name: String) = auth.login(DemoSeed.memberEmail(name), DemoSeed.MEMBER_PASSWORD)

    suspend fun loginAsAdmin() = auth.login(DemoSeed.ADMIN_EMAIL, DemoSeed.ADMIN_PASSWORD)
}

/** Returns a few bytes that depend on the avatar key, so tests can tell avatars apart. */
internal class FakeAvatars : AvatarSource {
    override fun bytes(key: String): ByteArray = key.toByteArray()
}
