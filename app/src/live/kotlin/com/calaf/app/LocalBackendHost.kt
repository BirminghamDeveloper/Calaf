package com.calaf.app

/** Android emulators expose the host at 10.0.2.2. USB phones use adb reverse. */
internal val localBackendHost: String
    get() = if (android.os.Build.HARDWARE in setOf("ranchu", "goldfish") ||
        android.os.Build.FINGERPRINT.startsWith("generic")) "10.0.2.2" else "127.0.0.1"
