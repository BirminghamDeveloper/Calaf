package com.calaf.app

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.calaf.feature.auth.AdminShortcut

/** What differs between flavours in the shared UI. Each flavour binds its own implementation through Hilt. */
interface AppMode {
    /** Banner shown above every screen, or null for none. */
    @get:StringRes val bannerText: Int? get() = null

    /** Extra notice on the login and register screens, or null for none. */
    @get:StringRes val authNotice: Int? get() = null

    val supportsPasswordReset: Boolean get() = true

    /** True only for debug builds of the live flavour that talk to the local Firebase emulators. */
    val usesLocalEmulators: Boolean get() = false

    val adminShortcut: AdminShortcut? get() = null

    /** Extra controls on the profile screen, such as the demo moderation panel. */
    @Composable fun AdminSection() = Unit

    /** Remembers whether the first launch tour was shown, or null when the flavour has no tour. */
    val walkthrough: WalkthroughState? get() = null

    /** One tap sign in for the welcome screen, or null for none. */
    val quickLogin: QuickLogin? get() = null

    /** Extra rows at the end of the settings menu, such as resetting the demo. */
    @Composable fun SettingsExtras() = Unit
}

/** Whether the first launch tour has been shown. */
interface WalkthroughState {
    var seen: Boolean
}

/** Credentials behind a "Try the demo" button. Public and fictional. */
data class QuickLogin(val email: String, val password: String)

/** Used by previews and tests that do not care about flavour specific behaviour. */
object PlainAppMode : AppMode
