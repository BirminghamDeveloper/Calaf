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
}
