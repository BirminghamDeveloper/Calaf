package com.calaf.app

import androidx.compose.runtime.Composable
import com.calaf.core.model.AdminRepository
import com.calaf.feature.admin.AdminPanel
import com.calaf.feature.auth.AdminShortcut
import com.calaf.app.demo.DemoResetter
import com.calaf.app.demo.DemoSeed
import com.calaf.app.demo.DemoShowcase

/** Offline showcase behaviour. This file and its credentials exist only in the demo source set. */
class DemoAppMode(
    private val admin: AdminRepository,
    private val resetter: DemoResetter,
    override val walkthrough: WalkthroughState,
) : AppMode {
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
    override fun AdminSection() = AdminPanel(admin)

    /** The ready made guest member, so an investor can see a lived in app with one tap. */
    override val quickLogin = QuickLogin(DemoShowcase.GUEST_EMAIL, DemoSeed.MEMBER_PASSWORD)

    @Composable
    override fun SettingsExtras() = DemoResetSection(resetter::reset)
}
