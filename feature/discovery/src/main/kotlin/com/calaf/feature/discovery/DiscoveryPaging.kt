package com.calaf.feature.discovery

/** When to fetch the next page of discovery. Pure, so it is unit tested without a ViewModel. */
object DiscoveryPaging {
    /** Fetch more once this many cards (or fewer) remain. */
    const val PREFETCH_THRESHOLD = 3

    fun shouldPrefetch(remaining: Int, cursor: String?, discovering: Boolean): Boolean =
        cursor != null && !discovering && remaining <= PREFETCH_THRESHOLD
}
