package com.calaf.feature.settings

import com.calaf.core.ui.R as CoreR
import androidx.annotation.StringRes

/** The pages reachable from the settings menu. */
enum class SettingsPage(@StringRes val label: Int) {
    PREFERENCES(CoreR.string.preferences),
    PRIVACY(R.string.privacy),
    GUARDIAN(R.string.guardian),
    VERIFICATION(R.string.verification),
    PREMIUM(R.string.premium),
    SAFETY(R.string.safety),
    GUIDELINES(R.string.guidelines),
    BLOCKED(R.string.blocked),
    SECURITY(R.string.security),
    ABOUT(R.string.about),
}
