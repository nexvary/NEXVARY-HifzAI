package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class DailyWorkTest {
    private val verses = (1..5).map { VerifiedVerse(1, it, "نص اختبار") }
    @Test fun retryDoesNotChargeAnotherVerseOrChangeItsFirstClassification() {
        val work = DailyWork(10).record(1001, true).record(1001, false).record(1002, false)
        assertEquals(setOf(1001), work.newVerseIds)
        assertEquals(setOf(1002), work.reviewVerseIds)
    }
    @Test fun workloadIsSharedAcrossSurahsAndSessions() {
        val work = DailyWork(10).record(2001, true)
        val goal = LearningGoal(2, 2)
        assertEquals(listOf(1001), goal.plan(verses, emptyMap(), 10, work).newVerseIds)
        assertEquals(listOf(1001), goal.plan(verses, emptyMap(), 10, work).newVerseIds)
    }
    @Test fun alreadyAssessedFailuresAreNotAutomaticallyRescheduledInsideTheSameDayPlan() {
        val cards = mapOf(1001 to ReviewCard(1001, lapses = 1, dueEpochDay = 10),
            1002 to ReviewCard(1002, repetitions = 1, dueEpochDay = 9))
        val plan = LearningGoal(1, 2).plan(verses, cards, 10, DailyWork(10).record(1001, false))
        assertEquals(listOf(1002), plan.dueVerseIds)
        assertEquals(listOf(1003), plan.newVerseIds)
    }
    @Test fun manualOverrideCanExceedBudgetWithoutNegativeRemainingCounts() {
        val work = DailyWork(10).record(1001, true).record(1002, true).record(1003, false)
        val plan = LearningGoal(1, 1).plan(verses, emptyMap(), 10, work)
        assertTrue(plan.allVerseIds.isEmpty())
        assertEquals(3, work.allVerseIds.size)
    }
    @Test fun theNextDayStartsAFreshPlanAndStaleDayInputsAreRejected() {
        assertEquals(2, LearningGoal().plan(verses, emptyMap(), 11).newVerseIds.size)
        assertTrue(runCatching { LearningGoal().plan(verses, emptyMap(), 11, DailyWork(10)) }.isFailure)
    }
    @Test fun invalidReferencesAndOverlappingClassificationAreRejected() {
        assertTrue(runCatching { DailyWork(-1) }.isFailure)
        assertTrue(runCatching { DailyWork(10).record(0, true) }.isFailure)
        assertTrue(runCatching { DailyWork(10, setOf(1001), setOf(1001)) }.isFailure)
    }
    @Test fun guidedSessionKeepsItsModeAcrossStageChangesAndCannotAlsoBeARange() {
        val session = PracticeSession(1, 1, PracticeKind.READ, 0, false, DifficultyTag.NONE, followsDailyPlan = true)
        assertTrue(session.changeStage(PracticeKind.RECALL).followsDailyPlan)
        assertTrue(runCatching { session.copy(rangeStart = 1001, rangeEnd = 1007) }.isFailure)
    }
}
