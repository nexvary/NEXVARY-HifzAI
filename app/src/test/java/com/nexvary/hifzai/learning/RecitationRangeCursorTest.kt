package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class RecitationRangeCursorTest {
    @Test fun defaultSelectionStopsAfterTheLastAyah() {
        val range = RecitationRangeCursor(3)
        assertEquals(1, range.passes)
        assertEquals(1, range.completeItem())
        assertEquals(2, range.completeItem())
        assertNull(range.completeItem())
        assertEquals(3, range.completedItems)
        assertEquals(2, range.index)
        assertTrue(range.complete)
        assertThrows(IllegalStateException::class.java) { range.completeItem() }
    }
    @Test fun repeatsTheWholeRangeInCanonicalOrderRatherThanEachItem() {
        val range = RecitationRangeCursor(3, 2)
        val indices = mutableListOf<Int>()
        val passes = mutableListOf<Int>()
        while (!range.complete) {
            indices += range.index; passes += range.pass; range.completeItem()
        }
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0, 1, 2), indices)
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3), passes)
        assertEquals(9, range.completedItems)
        assertEquals(3, range.pass)
    }
    @Test fun singleAyahRangeHasAnExactFiniteEnd() {
        val range = RecitationRangeCursor(1, 20)
        repeat(20) { assertEquals(0, range.completeItem()) }
        assertFalse(range.complete)
        assertNull(range.completeItem())
        assertEquals(21, range.completedItems)
    }
    @Test fun invalidAndOversizedWorkIsRejected() {
        for (size in listOf(-1, 0, 2001, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { RecitationRangeCursor(size) }
        }
        for (repeats in listOf(-1, 21, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { RecitationRangeCursor(1, repeats) }
        }
    }
    @Test fun maximumSelectionRemainsBoundedAndStopsExactly() {
        val range = RecitationRangeCursor(2000, 20)
        assertEquals(42000, range.totalItems)
        repeat(41999) { assertNotNull(range.completeItem()) }
        assertNull(range.completeItem())
        assertEquals(1999, range.index)
        assertEquals(21, range.pass)
    }
}
