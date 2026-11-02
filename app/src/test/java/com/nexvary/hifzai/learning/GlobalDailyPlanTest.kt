package com.nexvary.hifzai.learning

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class GlobalDailyPlanTest {
    private val corpus by lazy { QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText()) }
    private val cards = mapOf(114006 to ReviewCard(114006, repetitions = 1, dueEpochDay = 8),
        2001 to ReviewCard(2001, repetitions = 1, dueEpochDay = 9),
        1001 to ReviewCard(1001, repetitions = 1, dueEpochDay = 11))

    @Test fun globalReviewsPrecedeNewStudyAndRespectDueOrderAndTheChosenChapter() {
        val plan = LearningGoal(2, 2).planFor(corpus, cards, 10, 1)
        assertEquals(listOf(114006, 2001), plan.dueVerseIds)
        assertEquals(listOf(1002, 1003), plan.newVerseIds)
        assertEquals(listOf(114006, 2001, 1002, 1003), plan.allVerseIds)
        assertEquals(listOf(114006), LearningGoal(2, 1).planFor(corpus, cards, 10, 1).dueVerseIds)
    }
    @Test fun modesApplyToBothScopesWithoutPullingFutureReviewsIntoNewStudy() {
        val new = LearningGoal(2, 2, StudyMode.NEW_ONLY).planFor(corpus, cards, 10, 1)
        assertTrue(new.dueVerseIds.isEmpty()); assertEquals(listOf(1002, 1003), new.newVerseIds)
        val reviews = LearningGoal(2, 2, StudyMode.REVIEW_ONLY).planFor(corpus, cards, 10, 1)
        assertEquals(listOf(114006, 2001), reviews.allVerseIds)
        assertTrue(reviews.newVerseIds.isEmpty()); assertFalse(1001 in reviews.allVerseIds)
    }
    @Test fun dayWorkConsumesGlobalBudgetsButDoesNotChangeTheNewStudyAnchor() {
        val goal = LearningGoal(2, 2)
        val work = DailyWork(10).record(114006, false).record(2003, true)
        val plan = goal.planFor(corpus, cards, 10, 1, work)
        assertEquals(listOf(2001), plan.dueVerseIds)
        assertEquals(listOf(1002), plan.newVerseIds)
        val exhausted = work.record(2001, false).record(1002, true)
        assertTrue(goal.planFor(corpus, cards, 10, 1, exhausted).allVerseIds.isEmpty())
    }
    @Test fun invalidAnchorsAndStaleWorkAreRejected() {
        for (chapter in listOf(0, 115))
            assertTrue(runCatching { LearningGoal().planFor(corpus, cards, 10, chapter) }.isFailure)
        assertTrue(runCatching { LearningGoal().planFor(corpus, cards, 10, 1, DailyWork(9)) }.isFailure)
    }
    @Test fun guidedAnchorSurvivesCrossChapterAndStageChangesButIsNotValidForManualSessions() {
        val session = PracticeSession(114, 6, PracticeKind.RECALL, 0, false, DifficultyTag.NONE,
            followsDailyPlan = true, planChapter = 1)
        assertEquals(1, session.copy(chapter = 2, ayah = 1).changeStage(PracticeKind.BRIDGE).planChapter)
        assertEquals(114, PracticeSession(114, 6, PracticeKind.READ, 0, false, DifficultyTag.NONE,
            followsDailyPlan = true).planChapter)
        assertTrue(runCatching { session.copy(planChapter = 0) }.isFailure)
        assertTrue(runCatching { session.copy(planChapter = 115) }.isFailure)
        assertTrue(runCatching { session.copy(followsDailyPlan = false) }.isFailure)
    }
}
