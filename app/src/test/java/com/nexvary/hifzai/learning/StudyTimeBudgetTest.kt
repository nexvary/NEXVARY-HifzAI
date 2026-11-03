package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class StudyTimeBudgetTest {
    @Test fun balancedBudgetReservesReviewTimeWithoutExceedingAvailableMinutes() {
        for (minutes in StudyTimeBudget.choices) {
            val goal = StudyTimeBudget.suggest(minutes, StudyMode.BALANCED)
            assertTrue(goal.newVerses >= 1)
            assertTrue(goal.reviews >= 1)
            assertTrue(goal.newVerses * 3 + goal.reviews <= minutes)
        }
        assertEquals(LearningGoal(1, 12), StudyTimeBudget.suggest(15, StudyMode.BALANCED))
    }
    @Test fun modesAndBoundsAreRespectedAndUnsupportedTimesRejected() {
        for (minutes in StudyTimeBudget.choices) {
            val review = StudyTimeBudget.suggest(minutes, StudyMode.REVIEW_ONLY)
            assertEquals(0, review.newVerses)
            assertEquals(minutes, review.reviews)
            val new = StudyTimeBudget.suggest(minutes, StudyMode.NEW_ONLY)
            assertTrue(new.newVerses * 3 <= minutes)
            assertEquals(StudyMode.NEW_ONLY, new.mode)
        }
        for (minutes in listOf(-1, 0, 1, 7, 61, Int.MAX_VALUE))
            assertTrue(runCatching { StudyTimeBudget.suggest(minutes, StudyMode.BALANCED) }.isFailure)
    }
}
