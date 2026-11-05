package com.calaf.app.demo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoAdminRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private fun fixture() = DemoFixture(temp.newFolder("store"))

    @Test fun `only an administrator can use moderation`() = runBlocking {
        val demo = fixture()
        assertTrue(runCatching { demo.admin.members() }.isFailure)
        demo.auth.register("new@example.test", "DemoPass!2026")
        assertFalse(demo.admin.isAdmin)
        assertTrue(runCatching { demo.admin.members() }.isFailure)
        assertTrue(runCatching { demo.admin.suspend("demo-hassan", true) }.isFailure)
        assertTrue(runCatching { demo.admin.resolveReport(DemoSeed.SAMPLE_REPORT_ID, false) }.isFailure)
    }

    @Test fun `the admin role is a flag on the stored user, not a fixed id`() = runBlocking {
        val demo = fixture()
        demo.auth.register("promoted@example.test", "DemoPass!2026")
        val id = demo.session.requireUid()
        demo.store.user(id)!!.put("admin", true)
        assertTrue(demo.admin.isAdmin)
        assertEquals(6, demo.admin.members().size)
        demo.store.user(id)!!.put("admin", false)
        assertFalse(demo.admin.isAdmin)
        demo.loginAsAdmin()
        assertTrue(demo.session.isAdmin)
        assertTrue(demo.admin.members().single { it.id == DemoSeed.ADMIN_ID }.isAdmin)
    }

    @Test fun `suspend, restore and resolve are recorded in the audit log`() = runBlocking {
        val demo = fixture()
        demo.loginAsAdmin()
        demo.admin.suspend("demo-hassan", true)
        assertTrue(demo.admin.members().single { it.id == "demo-hassan" }.suspended)
        demo.admin.suspend("demo-hassan", false)
        demo.admin.resolveReport(DemoSeed.SAMPLE_REPORT_ID, false)
        assertEquals("RESOLVED", demo.admin.reports().single().status)
        assertEquals(3, demo.admin.auditCount())
        assertTrue(runCatching { demo.admin.resolveReport(DemoSeed.SAMPLE_REPORT_ID, true) }.isFailure)
    }

    @Test fun `administrators and unknown members cannot be suspended`() = runBlocking {
        val demo = fixture()
        demo.loginAsAdmin()
        assertTrue(runCatching { demo.admin.suspend(DemoSeed.ADMIN_ID, true) }.isFailure)
        assertTrue(runCatching { demo.admin.suspend("missing", true) }.isFailure)
        assertEquals(0, demo.admin.auditCount())
    }
}
