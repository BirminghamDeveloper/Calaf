package com.calaf.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoRulesTest {
    private fun photo(id: String, order: Int) = Photo(id, order, isPrimary = false, state = PhotoState.APPROVED)
    private val three = listOf(photo("a", 5), photo("b", 9), photo("c", 2))

    private fun ids(photos: List<Photo>) = photos.map { it.id }

    @Test fun `normalise orders from zero and makes only the first primary`() {
        val result = PhotoRules.normalise(three)
        assertEquals(listOf("c", "a", "b"), ids(result))
        assertEquals(listOf(0, 1, 2), result.map { it.order })
        assertEquals(listOf(true, false, false), result.map { it.isPrimary })
    }

    @Test fun `move reorders and keeps a single primary at the front`() {
        val start = PhotoRules.normalise(three)
        val moved = PhotoRules.move(start, 2, 0)
        assertEquals(listOf("b", "c", "a"), ids(moved))
        assertEquals(listOf(true, false, false), moved.map { it.isPrimary })
        assertEquals(ids(start), ids(PhotoRules.move(start, 1, 1)))
        assertEquals(ids(start), ids(PhotoRules.move(start, 7, 0)))
    }

    @Test fun `choosing a primary moves that photo to the front`() {
        val result = PhotoRules.withPrimary(PhotoRules.normalise(three), "b")
        assertEquals(listOf("b", "c", "a"), ids(result))
        assertEquals("b", result.single { it.isPrimary }.id)
    }

    @Test fun `deleting the primary promotes the next photo`() {
        val result = PhotoRules.without(PhotoRules.normalise(three), "c")
        assertEquals(listOf("a", "b"), ids(result))
        assertEquals("a", result.single { it.isPrimary }.id)
        assertEquals(emptyList<Photo>(), PhotoRules.without(listOf(photo("x", 0)), "x"))
    }

    @Test fun `reorder needs exactly the current photos`() {
        val start = PhotoRules.normalise(three)
        assertEquals(listOf("b", "a", "c"), ids(PhotoRules.inOrder(start, listOf("b", "a", "c"))))
        assertThrows(IllegalArgumentException::class.java) { PhotoRules.inOrder(start, listOf("a", "b")) }
        assertThrows(IllegalArgumentException::class.java) { PhotoRules.inOrder(start, listOf("a", "b", "z")) }
    }
}
