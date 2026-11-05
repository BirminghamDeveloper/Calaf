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
}

/** Used by previews and tests that do not care about flavour specific behaviour. */
object PlainAppMode : AppMode
