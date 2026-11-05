package com.calaf.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePhysicsTest {
    private val width = 1_000f

    @Test fun `a slow drag past a third of the width sends the card that way`() {
        assertEquals(SwipeDirection.LIKE, SwipePhysics.decide(400f, 0f, width))
        assertEquals(SwipeDirection.PASS, SwipePhysics.decide(-400f, 0f, width))
        assertEquals(SwipeDirection.LIKE, SwipePhysics.decide(350f, 0f, width))
    }

    @Test fun `a short slow drag springs back`() {
        assertNull(SwipePhysics.decide(0f, 0f, width))
        assertNull(SwipePhysics.decide(300f, 200f, width))
        assertNull(SwipePhysics.decide(-349f, -999f, width))
    }

    @Test fun `a firm flick in the drag direction sends the card even after a short drag`() {
        assertEquals(SwipeDirection.LIKE, SwipePhysics.decide(120f, 1_500f, width))
        assertEquals(SwipeDirection.PASS, SwipePhysics.decide(-120f, -1_500f, width))
        assertEquals(SwipeDirection.LIKE, SwipePhysics.decide(40f, 1_000f, width))
    }

    @Test fun `a flick against the drag never sends the card`() {
        assertNull(SwipePhysics.decide(120f, -1_500f, width))
        assertNull(SwipePhysics.decide(-120f, 1_500f, width))
        // Pulled far, then flicked firmly back: the member changed their mind.
        assertNull(SwipePhysics.decide(500f, -1_800f, width))
        assertEquals(SwipeDirection.LIKE, SwipePhysics.decide(500f, -500f, width))
    }

    @Test fun `a tiny touch with a fast velocity is not a swipe`() {
        assertNull(SwipePhysics.decide(5f, 5_000f, width))
        assertNull(SwipePhysics.decide(0f, 5_000f, width))
    }

    @Test fun `an unmeasured area never swipes`() {
        assertNull(SwipePhysics.decide(500f, 5_000f, 0f))
        assertEquals(0f, SwipePhysics.rotation(100f, 0f), 0f)
        assertEquals(0f, SwipePhysics.stampAlpha(100f, 0f), 0f)
    }

    @Test fun `rotation follows the drag and is capped`() {
        assertEquals(0f, SwipePhysics.rotation(0f, width), 0.001f)
        assertTrue(SwipePhysics.rotation(200f, width) > 0f)
        assertTrue(SwipePhysics.rotation(-200f, width) < 0f)
        assertEquals(SwipePhysics.MAX_ROTATION_DEGREES, SwipePhysics.rotation(5_000f, width), 0.001f)
        assertEquals(-SwipePhysics.MAX_ROTATION_DEGREES, SwipePhysics.rotation(-5_000f, width), 0.001f)
    }

    @Test fun `the stamp fades in until the card would be sent`() {
        assertEquals(0f, SwipePhysics.stampAlpha(0f, width), 0f)
        assertEquals(0.5f, SwipePhysics.stampAlpha(175f, width), 0.001f)
        assertEquals(1f, SwipePhysics.stampAlpha(350f, width), 0f)
        assertEquals(1f, SwipePhysics.stampAlpha(-900f, width), 0f)
        assertEquals(SwipeDirection.LIKE, SwipePhysics.stampDirection(10f))
        assertEquals(SwipeDirection.PASS, SwipePhysics.stampDirection(-10f))
        assertNull(SwipePhysics.stampDirection(0f))
    }

    @Test fun `a card exits beyond the edge on its own side`() {
        assertTrue(SwipePhysics.exitOffset(SwipeDirection.LIKE, width) > width)
        assertTrue(SwipePhysics.exitOffset(SwipeDirection.PASS, width) < -width)
    }
}
