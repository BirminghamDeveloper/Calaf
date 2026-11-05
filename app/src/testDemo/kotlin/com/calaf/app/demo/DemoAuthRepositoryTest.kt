package com.calaf.app.demo

import com.calaf.core.model.AccountError
import com.calaf.core.model.AccountException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoAuthRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private fun fixture() = DemoFixture(temp.newFolder("store"))

    private fun failure(block: suspend () -> Unit): AccountError? =
        runCatching { runBlocking { block() } }.exceptionOrNull().let { (it as? AccountException)?.reason }

    @Test fun `registration signs in, persists and can log in again after a restart`() = runBlocking {
        val first = fixture()
        first.auth.register("New@Example.test", "DemoPass!2026")
        val id = first.auth.refresh()!!.id
        assertEquals("new@example.test", first.auth.accounts.first()!!.email)
        val reopened = first.reopen()
        assertEquals(id, reopened.auth.accounts.first()!!.id)
        reopened.auth.logout()
        assertNull(reopened.auth.accounts.first())
        reopened.auth.login("new@example.test", "DemoPass!2026")
        assertEquals(id, reopened.auth.refresh()!!.id)
        assertFalse(reopened.session.isAdmin)
    }

    @Test fun `wrong password, unknown email and duplicate email are refused`() {
        val demo = fixture()
        assertEquals(AccountError.Credentials, failure { demo.auth.login(DemoSeed.ADMIN_EMAIL, "wrong") })
        assertEquals(AccountError.Credentials, failure { demo.auth.login("nobody@example.test", "DemoPass!2026") })
        runBlocking { demo.auth.register("a@example.test", "DemoPass!2026") }
        assertEquals(AccountError.EmailInUse, failure { demo.auth.register("A@example.test", "DemoPass!2026") })
        assertEquals(AccountError.InvalidInput, failure { demo.auth.register("not an email", "DemoPass!2026") })
    }

    @Test fun `seeded members log in with the public demo credentials`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hassan")
        assertEquals("demo-hassan", demo.auth.refresh()!!.id)
        assertFalse(demo.session.isAdmin)
        demo.loginAsAdmin()
        assertTrue(demo.session.isAdmin)
        assertEquals(DemoSeed.ADMIN_EMAIL, demo.auth.refresh()!!.email)
    }

    @Test fun `a suspended member cannot log in`() = runBlocking {
        val demo = fixture()
        demo.loginAsAdmin()
        demo.admin.suspend("demo-hassan", true)
        demo.auth.logout()
        assertEquals(AccountError.Credentials, failure { demo.loginAsMember("hassan") })
    }

    @Test fun `password reset is unavailable offline`() {
        assertEquals(AccountError.Unavailable, failure { fixture().auth.resetPassword("a@example.test") })
    }
}
