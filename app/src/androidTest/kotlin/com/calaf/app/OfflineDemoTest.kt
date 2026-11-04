package com.calaf.app

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.calaf.app.offline.OfflineDemo
import com.calaf.core.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class OfflineDemoTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun registrationPersistsAndOnlyAdministratorCanModerate() = runBlocking<Unit> {
        assumeTrue(BuildConfig.OFFLINE_DEMO)
        val store = "test-offline-${UUID.randomUUID()}"
        val first = OfflineDemo(context, store)
        first.register("new@example.test", "DemoPass!2026")
        val uid = first.refresh()!!.id
        assertTrue(runCatching { first.adminMembers() }.isFailure)
        assertTrue(runCatching { first.suspendMember("demo-admin", true) }.isFailure)
        first.logout()
        val reopened = OfflineDemo(context, store)
        reopened.login("new@example.test", "DemoPass!2026")
        assertEquals(uid, reopened.refresh()!!.id)
        reopened.logout()
        reopened.login("admin@calaf.test", "CalafAdmin!2026")
        assertTrue(reopened.isAdmin)
        assertEquals(6, reopened.adminMembers().size)
        reopened.suspendMember(uid, true)
        reopened.resolveReport("sample-report", false)
        assertEquals("RESOLVED", reopened.adminReports().single().status)
        assertEquals(2, reopened.auditCount())
        assertTrue(runCatching { reopened.suspendMember("demo-admin", true) }.isFailure)
        reopened.logout()
        assertTrue(runCatching { reopened.login("new@example.test", "DemoPass!2026") }.isFailure)
        reopened.login("admin@calaf.test", "CalafAdmin!2026")
        reopened.suspendMember(uid, false)
        reopened.logout()
        reopened.login("new@example.test", "DemoPass!2026")
        assertFalse(reopened.isAdmin)
        context.getSharedPreferences(store, 0).edit().clear().commit()
    }
    @Test fun messagesPersistAndBlockingRevokesConversationAccess() = runBlocking<Unit> {
        assumeTrue(BuildConfig.OFFLINE_DEMO)
        val store = "test-offline-${UUID.randomUUID()}"
        val demo = OfflineDemo(context, store)
        demo.login("hassan@calaf.test", "CalafDemo!2026")
        val match = demo.matches("demo-hassan").first().single()
        demo.execute(CommunityCommand.Send(match.id, "test-message", "Hello from the offline demo"))
        demo.execute(CommunityCommand.Send(match.id, "test-message", "Duplicate"))
        assertEquals(1, demo.messages(match.id).first().size)
        demo.logout()
        val reopened = OfflineDemo(context, store)
        reopened.login("amina@calaf.test", "CalafDemo!2026")
        assertEquals("Hello from the offline demo", reopened.messages(match.id).first().single().text)
        reopened.execute(CommunityCommand.Block("demo-hassan"))
        assertTrue(runCatching { reopened.execute(CommunityCommand.Send(match.id, "blocked-message", "Denied")) }.isFailure)
        context.getSharedPreferences(store, 0).edit().clear().commit()
    }
    @Test fun offlineApkCannotUseTheNetwork() {
        assumeTrue(BuildConfig.OFFLINE_DEMO)
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertTrue(com.google.firebase.FirebaseApp.getApps(context).isEmpty())
    }
}
