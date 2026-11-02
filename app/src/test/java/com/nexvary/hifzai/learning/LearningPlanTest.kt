package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class LearningPlanTest {
    private val verses = (1..5).map { VerifiedVerse(1, it, "نص تعليمي للاختبار") }
    private val cards = mapOf(1001 to ReviewCard(1001, repetitions = 1, dueEpochDay = 8),
        1002 to ReviewCard(1002, repetitions = 1, dueEpochDay = 12))
    private fun observation(day: Long, grade: RecallGrade = RecallGrade.GOOD, hints: Int = 0,
        exposed: Boolean = false, kind: PracticeKind? = PracticeKind.RECALL) =
        RecallObservation(day, grade, hints, exposed, DifficultyTag.NONE, kind)

    @Test fun modeBudgetAndFutureDueAreRespected() {
        assertEquals(listOf(1001), LearningGoal(2, 1).plan(verses, cards, 10).dueVerseIds)
        assertEquals(listOf(1003, 1004), LearningGoal(2, 1).plan(verses, cards, 10).newVerseIds)
        assertTrue(LearningGoal(2, 1, StudyMode.REVIEW_ONLY).plan(verses, cards, 10).newVerseIds.isEmpty())
        assertTrue(LearningGoal(2, 1, StudyMode.NEW_ONLY).plan(verses, cards, 10).dueVerseIds.isEmpty())
        assertTrue(LearningGoal(0, 1, StudyMode.NEW_ONLY).plan(verses, cards, 10).allVerseIds.isEmpty())
    }
    @Test fun invalidBudgetsCannotEnterPlanner() {
        for ((new, reviews) in listOf(-1 to 15, 21 to 15, 2 to 0, 2 to 101))
            assertTrue(runCatching { LearningGoal(new, reviews) }.isFailure)
    }
    @Test fun weekHasInclusiveBoundariesAndNoFutureOrOlderEvents() {
        val events = listOf(observation(3), observation(4), observation(10), observation(11),
            observation(10, hints = 1), observation(8, exposed = true), observation(9, RecallGrade.AGAIN))
        val result = LearningTrends.week(events, 10)
        assertEquals(5, result.attempts)
        assertEquals(2, result.independent)
        assertEquals(2, result.assisted)
        assertEquals(1, result.forgotten)
        assertEquals(4, result.activeDays)
        assertEquals(40, result.independentPercent)
        assertNull(LearningTrends.week(emptyList(), 10).independentPercent)
    }
    @Test fun oldUnknownModeIsNotInventedAndRecentFailuresGuideOnlyDrills() {
        val events = listOf(observation(10, kind = null), observation(10, kind = PracticeKind.BRIDGE))
        assertEquals(setOf(PracticeKind.BRIDGE), LearningTrends.byMode(events, 10).keys)
        assertNull(LearningTrends.recentRecommendation(listOf(observation(10)), 10))
        assertNull(LearningTrends.recentRecommendation(List(3) { observation(0, RecallGrade.AGAIN) }, 20))
        assertEquals(PracticeKind.MASK_LIGHT, LearningTrends.recentRecommendation(
            List(3) { observation(10, RecallGrade.AGAIN) }, 10)?.kind)
        assertEquals(PracticeKind.BRIDGE, LearningTrends.recentRecommendation(
            List(3) { observation(10, RecallGrade.AGAIN, kind = PracticeKind.BRIDGE) }, 10)?.kind)
    }
}
