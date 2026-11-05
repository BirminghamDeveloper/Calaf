package com.calaf.app.demo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DemoStoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun `first start seeds the demo once and writes the schema version`() {
        val directory = temp.newFolder("store")
        val first = DemoStore(directory)
        assertEquals(5, first.rows("users").size)
        assertEquals(2, first.rows("pairs").size)
        val saved = JSONObject(File(directory, "demo.json").readText())
        assertEquals(DemoStore.SCHEMA_VERSION, saved.getInt("schemaVersion"))
        val second = DemoStore(directory)
        assertEquals(5, second.rows("users").size)
        assertEquals(4, second.rows("notifications").size)
    }

    @Test fun `data and the signed in member survive a restart`() {
        val directory = temp.newFolder("store")
        val first = DemoStore(directory)
        first.table("drafts").put("someone", JSONObject().put("name", "Kept"))
        first.sessionId = "demo-hassan"
        val second = DemoStore(directory)
        assertEquals("Kept", second.table("drafts").getJSONObject("someone").getString("name"))
        assertEquals("demo-hassan", second.sessionId)
        second.sessionId = null
        assertEquals(null, DemoStore(directory).sessionId)
    }

    @Test fun `an unreadable file is replaced with fresh seeded data`() {
        val directory = temp.newFolder("store")
        File(directory, "demo.json").writeText("{ not json")
        assertEquals(5, DemoStore(directory).rows("users").size)
    }

    @Test fun `a custom seed can leave the store empty`() {
        val store = DemoStore(temp.newFolder("store"), seed = {})
        assertTrue(store.rows("users").isEmpty())
    }

    @Test fun `createMatch makes one pair and one MATCH notification per member and is idempotent`() {
        val store = DemoStore(temp.newFolder("store"), seed = {})
        val id = store.createMatch("b", "a")
        assertEquals("a_b", id)
        store.createMatch("a", "b")
        assertEquals(1, store.rows("pairs").size)
        assertEquals(listOf("a", "b"), store.rows("notifications").map { it.second.getString("owner") }.sorted())
    }

    @Test fun `a block counts in both directions`() {
        val store = DemoStore(temp.newFolder("store"), seed = {})
        store.table("blocks").put("a_b", true)
        assertTrue(store.isBlockedEitherWay("a", "b"))
        assertTrue(store.isBlockedEitherWay("b", "a"))
        assertFalse(store.isBlockedEitherWay("a", "c"))
    }

    @Test fun `a version 1 store migrates so seeded members keep faith details and others lose them`() {
        val directory = temp.newFolder("legacy")
        val legacy = JSONObject()
            .put("schemaVersion", 1)
            .put("users", JSONObject()
                .put("demo-hassan", JSONObject().put("email", "hassan@calaf.test").put("seed", true))
                .put("member", JSONObject().put("email", "m@example.test")))
            .put("profiles", JSONObject()
                .put("demo-hassan", JSONObject().put("name", "Hassan").put("religion", "Islam"))
                .put("member", JSONObject().put("name", "Member").put("religion", "Islam")))
            .put("drafts", JSONObject().put("member", JSONObject().put("name", "Member").put("religion", "Islam")))
            .put("meta", JSONObject().put("seeded", true))
        File(directory, "demo.json").writeText(legacy.toString())
        val store = DemoStore(directory)
        val profiles = store.table("profiles")
        assertEquals("Islam", profiles.getJSONObject("demo-hassan").getString("religion"))
        assertTrue(profiles.getJSONObject("demo-hassan").getBoolean("sensitiveConsent"))
        assertEquals("", profiles.getJSONObject("member").getString("religion"))
        assertFalse(profiles.getJSONObject("member").getBoolean("sensitiveConsent"))
        assertEquals("", store.table("drafts").getJSONObject("member").getString("religion"))
        // Written back, so reopening does not migrate again and the version is current.
        val saved = JSONObject(File(directory, "demo.json").readText())
        assertEquals(DemoStore.SCHEMA_VERSION, saved.getInt("schemaVersion"))
        assertFalse(saved.getJSONObject("profiles").getJSONObject("member").getBoolean("sensitiveConsent"))
    }

    @Test fun `a version 2 store migrates old photo files to approved photo rows and seeds avatars`() {
        val directory = temp.newFolder("legacy-photos")
        val legacy = JSONObject()
            .put("schemaVersion", 2)
            .put("users", JSONObject()
                .put("demo-hassan", JSONObject().put("email", "hassan@calaf.test").put("seed", true))
                .put("member", JSONObject().put("email", "m@example.test")))
            .put("meta", JSONObject().put("seeded", true))
        File(directory, "demo.json").writeText(legacy.toString())
        val memberDir = File(directory, "photos/member").apply { mkdirs() }
        File(memberDir, "photo-4.jpg").writeBytes(byteArrayOf(4))
        File(memberDir, "photo-1.jpg").writeBytes(byteArrayOf(1))
        File(memberDir, "notes.txt").writeText("ignored")
        val store = DemoStore(directory)
        val photos = DemoPhotoRows.ownerPhotos(store, "member")
        assertEquals(listOf("photo-1", "photo-4"), photos.map { it.id })
        assertEquals(listOf(0, 1), photos.map { it.order })
        assertEquals(listOf(true, false), photos.map { it.isPrimary })
        assertTrue(photos.all { it.state == com.calaf.core.model.PhotoState.APPROVED })
        assertTrue(DemoPhotoRows.ownerPhotos(store, "demo-hassan").isNotEmpty())
        assertEquals(DemoStore.SCHEMA_VERSION, JSONObject(File(directory, "demo.json").readText()).getInt("schemaVersion"))
    }
}
