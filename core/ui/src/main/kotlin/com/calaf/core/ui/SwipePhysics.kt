package com.calaf.core.ui

import kotlin.math.abs

/** Which way a card was sent. PASS is a swipe left, LIKE a swipe right. */
enum class SwipeDirection { PASS, LIKE }

/**
 * The pure rules behind [SwipeCardStack], kept free of Compose so they can be unit tested. Distances are in pixels,
 * velocities in pixels per second, and [widthPx] is the width of the card area.
 */
object SwipePhysics {
    /** A card dragged this fraction of the width is sent even if released slowly. */
    const val DISTANCE_FRACTION = 0.35f

    /** A flick faster than this sends the card even when it has not travelled far. */
    const val FLING_VELOCITY = 1_000f

    /** A card must have moved at least this fraction of the width for a fling to count, so a stray touch cannot. */
    const val MIN_FLING_DISTANCE_FRACTION = 0.04f

    const val MAX_ROTATION_DEGREES = 15f

    /**
     * Decides what a released drag means: send the card [SwipeDirection.PASS] or [SwipeDirection.LIKE], or null to
     * spring it back. A fling in the opposite direction to the drag cancels it.
     */
    fun decide(offsetX: Float, velocityX: Float, widthPx: Float): SwipeDirection? {
        if (widthPx <= 0f) return null
        val direction = if (offsetX >= 0f) SwipeDirection.LIKE else SwipeDirection.PASS
        if (abs(offsetX) >= widthPx * DISTANCE_FRACTION) {
            // Pulled far enough, unless they flicked it firmly back the other way.
            val flickedBack = abs(velocityX) >= FLING_VELOCITY && (velocityX > 0f) != (offsetX > 0f)
            return if (flickedBack) null else direction
        }
        val movedEnough = abs(offsetX) >= widthPx * MIN_FLING_DISTANCE_FRACTION
        val sameWay = (velocityX > 0f) == (offsetX > 0f)
        return if (movedEnough && abs(velocityX) >= FLING_VELOCITY && sameWay) direction else null
    }

    /** Tilt in degrees for a card at [offsetX]: proportional to the drag, never more than the maximum. */
    fun rotation(offsetX: Float, widthPx: Float): Float {
        if (widthPx <= 0f) return 0f
        return (offsetX / widthPx * MAX_ROTATION_DEGREES * 2f).coerceIn(-MAX_ROTATION_DEGREES, MAX_ROTATION_DEGREES)
    }

    /** How strongly the LIKE or PASS stamp shows, from 0 to 1, reaching 1 where the card would be sent. */
    fun stampAlpha(offsetX: Float, widthPx: Float): Float {
        if (widthPx <= 0f) return 0f
        return (abs(offsetX) / (widthPx * DISTANCE_FRACTION)).coerceIn(0f, 1f)
    }

    /** The side a stamp belongs to, or null while the card is centred. */
    fun stampDirection(offsetX: Float): SwipeDirection? = when {
        offsetX > 0f -> SwipeDirection.LIKE
        offsetX < 0f -> SwipeDirection.PASS
        else -> null
    }

    /** Where a card finishes when it flies off: beyond the edge on its side. */
    fun exitOffset(direction: SwipeDirection, widthPx: Float): Float =
        if (direction == SwipeDirection.LIKE) widthPx * EXIT_WIDTHS else -widthPx * EXIT_WIDTHS

    private const val EXIT_WIDTHS = 1.5f
}
