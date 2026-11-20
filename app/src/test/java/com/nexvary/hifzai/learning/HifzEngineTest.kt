package com.nexvary.hifzai.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzEngineTest {
    @Test fun untouchedTextWhenNoMask() {
        val s = "قُلْ هُوَ ٱللَّهُ أَحَدٌ"
        assertEquals(s, VerseMask.display(s, 0f))
    }

    @Test fun maskDoesNotModifySource() {
        val s = "قُلْ هُوَ ٱللَّهُ أَحَدٌ"
        val masked = VerseMask.display(s, 0.5f, 12)
        assertTrue(masked.contains("●●●"))
        assertEquals(s, "قُلْ هُوَ ٱللَّهُ أَحَدٌ")
        assertFalse(masked == s)
    }

    @Test fun masksAreDeterministic() {
        val text = "one two three four five"
        assertEquals(VerseMask.display(text, 0.4f, 14), VerseMask.display(text, 0.4f, 14))
    }

    @Test fun failureIsDueNow() {
        val card = ReviewCard(112001, repetitions = 3, intervalDays = 7)
        val result = HifzScheduler.grade(card, RecallGrade.AGAIN, 100L)
        assertEquals(0, result.repetitions)
        assertEquals(100L, result.dueEpochDay)
        assertEquals(1, result.lapses)
    }

    @Test fun successSchedulesNextReview() {
        val first = HifzScheduler.grade(ReviewCard(112001), RecallGrade.GOOD, 100L)
        assertEquals(1, first.intervalDays)
        assertEquals(101L, first.dueEpochDay)
        val second = HifzScheduler.grade(first, RecallGrade.GOOD, 101L)
        assertEquals(3, second.intervalDays)
        assertEquals(104L, second.dueEpochDay)
    }

    @Test fun easeBounds() {
        var card = ReviewCard(112001)
        repeat(50) { card = HifzScheduler.grade(card, RecallGrade.HARD, it.toLong()) }
        assertTrue(card.ease >= 1.3)
        repeat(50) { card = HifzScheduler.grade(card, RecallGrade.EASY, it.toLong()) }
        assertTrue(card.ease <= 3.0)
    }
}
