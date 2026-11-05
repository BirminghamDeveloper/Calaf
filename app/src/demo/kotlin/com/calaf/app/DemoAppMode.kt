package com.calaf.app

import androidx.compose.runtime.Composable
import com.calaf.app.offline.OfflineAdminSection
import com.calaf.app.demo.DemoAdminRepository
import com.calaf.app.demo.DemoSeed

/** Offline showcase behaviour. This file and its credentials exist only in the demo source set. */
class DemoAppMode(private val admin: DemoAdminRepository) : AppMode {
    override val bannerText = R.string.demo_banner
    override val authNotice = R.string.demo_auth_notice
    override val supportsPasswordReset = false

    /** Public demo credentials for the seeded administrator. Fictional, on device only. */
    override val adminShortcut = AdminShortcut(
        typedEmail = "admin",
        typedPassword = "admin",
        loginEmail = DemoSeed.ADMIN_EMAIL,
        loginPassword = DemoSeed.ADMIN_PASSWORD,
    )

    @Composable
    override fun AdminSection() = OfflineAdminSection(admin)
}
