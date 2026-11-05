package com.calaf.app.demo

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DemoShowcaseTest {
    @get:Rule val temp = TemporaryFolder()

    private fun showcaseStore(directory: File) = DemoStore(directory, seed = DemoSeed::seedEverything)

    @Test fun `the showcase adds twenty fictional members, the guest and the core five`() {
        val store = showcaseStore(temp.newFolder("store"))
        assertEquals(5 + 20 + 1, store.rows("users").size)
        val members = store.rows("profiles").count { (id, _) ->
            id != DemoShowcase.GUEST_ID && store.user(id)?.optBoolean("demo") == true
        }
        assertEquals(20, members)
    }

    @Test fun `every showcase member is labelled as a demo and has a photo`() {
        val store = showcaseStore(temp.newFolder("store"))
        val showcase = store.rows("profiles").filter { (id, _) -> store.user(id)?.optBoolean("demo") == true }
        assertEquals(21, showcase.size)
        showcase.forEach { (id, profile) ->
            val labelled = profile.getString("name").endsWith("(Demo)") || id == DemoShowcase.GUEST_ID
            assertTrue("$id is not labelled", labelled)
            assertTrue("$id has no photo", store.rows("photos").any { (_, row) -> row.optString("owner") == id })
        }
    }

    @Test fun `seeding twice changes nothing`() {
        val directory = temp.newFolder("store")
        val first = showcaseStore(directory)
        val users = first.rows("users").size
        val photos = first.rows("photos").size
        DemoShowcase.seedShowcase(first)
        val second = showcaseStore(directory)
        assertEquals(users, second.rows("users").size)
        assertEquals(photos, second.rows("photos").size)
    }

    @Test fun `the guest arrives with matches, a chat and people waiting`() = runBlocking<Unit> {
        val fixture = DemoFixture(temp.newFolder("store"), showcase = true)
        fixture.auth.login(DemoShowcase.GUEST_EMAIL, DemoSeed.MEMBER_PASSWORD)
        val matches = fixture.community.matches(DemoShowcase.GUEST_ID).first()
        assertEquals(2, matches.size)
        assertTrue(matches.any { it.messageCount >= 3 })
        val liked = fixture.community.likedYou()
        assertTrue(liked.profiles.isNotEmpty())
        assertTrue(fixture.community.notifications(DemoShowcase.GUEST_ID).first().isNotEmpty())
    }

    @Test fun `reset removes everything a person did and restores the showcase`() = runBlocking<Unit> {
        val directory = temp.newFolder("store")
        val fixture = DemoFixture(directory, showcase = true)
        fixture.auth.register("someone.new@example.com", "Password123!")
        fixture.store.table("drafts").put("junk", org.json.JSONObject().put("x", 1))
        File(fixture.store.photoRoot, "stray.jpg").apply { parentFile.mkdirs(); writeText("x") }
        var shownTourAgain = false
        DemoResetter(fixture.store, fixture.session) { shownTourAgain = true }.reset()
        assertTrue(shownTourAgain)
        assertNull(fixture.store.sessionId)
        assertEquals(26, fixture.store.rows("users").size)
        val emails = fixture.store.rows("users").map { (_, user) -> user.optString("email") }
        assertFalse("someone.new@example.com" in emails)
        assertEquals(0, fixture.store.table("drafts").length())
        assertFalse(File(fixture.store.photoRoot, "stray.jpg").exists())
        assertNotNull(fixture.store.user(DemoShowcase.GUEST_ID))
        assertEquals(26, DemoStore(directory, seed = DemoSeed::seedEverything).rows("users").size)
    }
}
