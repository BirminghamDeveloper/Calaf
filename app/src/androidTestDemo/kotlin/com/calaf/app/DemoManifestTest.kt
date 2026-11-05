package com.calaf.app

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoManifestTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun demoApkCannotUseTheNetwork() {
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        // The demo flavour does not ship Firebase at all.
        assertThrows(ClassNotFoundException::class.java) { Class.forName("com.google.firebase.FirebaseApp") }
    }
}
