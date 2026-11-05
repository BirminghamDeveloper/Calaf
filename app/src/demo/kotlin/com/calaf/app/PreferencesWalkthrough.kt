package com.calaf.app

import android.content.Context

/** Remembers on the device that the first launch tour was shown. */
class PreferencesWalkthrough(context: Context) : WalkthroughState {
    private val preferences = context.getSharedPreferences("calaf_demo_tour", Context.MODE_PRIVATE)

    override var seen: Boolean
        get() = preferences.getBoolean(KEY, false)
        set(value) = preferences.edit().putBoolean(KEY, value).apply()

    private companion object {
        const val KEY = "seen"
    }
}
