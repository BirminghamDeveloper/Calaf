package com.calaf.app

/** Real Firebase behaviour. [local] is true only for debug builds pointed at the local emulators. */
class LiveAppMode(private val local: Boolean) : AppMode {
    override val bannerText = if (local) R.string.local_development else null
    override val usesLocalEmulators = local
}
