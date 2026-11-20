package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class RecallLadderTest {
    private val verses = listOf(
        VerifiedVerse(112,1,"تجربة تعليمية"),
        VerifiedVerse(112,2,"تجربة ثانية"),
        VerifiedVerse(112,3,"تجربة ثالثة")
    )

    @Test fun ladderStartsWithReadingAndEndsWithBridge() {
        assertEquals(PracticeKind.MASK_LIGHT, RecallLadder.next(PracticeKind.READ))
        assertEquals(PracticeKind.MASK_HEAVY, RecallLadder.next(PracticeKind.MASK_LIGHT))
        assertEquals(PracticeKind.RECALL, RecallLadder.next(PracticeKind.MASK_HEAVY))
        assertEquals(PracticeKind.BRIDGE, RecallLadder.next(PracticeKind.RECALL))
        assertEquals(PracticeKind.BRIDGE, RecallLadder.next(PracticeKind.BRIDGE))
    }

    @Test fun concealmentAlwaysWithinRange() {
        for (step in RecallLadder.sequence) {
            for (hint in 0..3) {
                assertTrue(RecallLadder.concealment(step, hint) in 0f..1f)
            }
        }
        assertEquals(0f, RecallLadder.concealment(PracticeKind.RECALL, 0, true))
        assertEquals(1f, RecallLadder.concealment(PracticeKind.RECALL), 0f)
    }

    @Test fun assistedResultsCannotBePresentedAsUnassistedEasy() {
        assertEquals(RecallGrade.HARD, RecallLadder.boundedGrade(RecallGrade.EASY, 1, false))
        assertEquals(RecallGrade.HARD, RecallLadder.boundedGrade(RecallGrade.GOOD, 0, true))
        assertEquals(RecallGrade.EASY, RecallLadder.boundedGrade(RecallGrade.EASY, 0, false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hintsMustBeNonnegative() {
        RecallLadder.boundedGrade(RecallGrade.GOOD, -1, false)
    }

    @Test fun dailyPlanIncludesOnlyDueAndNew() {
        val c = mapOf(verses[0].id to ReviewCard(verses[0].id,1,dueEpochDay=5),
            verses[1].id to ReviewCard(verses[1].id,1,dueEpochDay=25))
        val plan = DailyMemorizationPlan.create(verses,c,10)
        assertEquals(listOf(verses[0].id), plan.dueVerseIds)
        assertEquals(listOf(verses[2].id), plan.newVerseIds)
        assertFalse(plan.allVerseIds.contains(verses[1].id))
    }

    @Test fun respectingNewBudget() {
        val plan = DailyMemorizationPlan.create(verses, emptyMap(), 10, maxNew=1)
        assertEquals(1,plan.newVerseIds.size)
    }

    @Test fun selfReportedDifficultyRecordedOnlyWhenStruggling() {
        val memory = MemoryFingerprint(112001)
        val harder = MemoryCoach.record(memory,RecallGrade.HARD,0,false,DifficultyTag.SIMILAR_VERSE)
        assertEquals(1, harder.similarityMisses)
        val easy = MemoryCoach.record(harder,RecallGrade.EASY,0,false,DifficultyTag.SIMILAR_VERSE)
        assertEquals(1, easy.similarityMisses)
    }

    @Test fun wordActivityNeverMutatesText() {
        val text = "نص عربي محفوظ أصلي"
        assertEquals("نص عربي", ReadOnlyVerseExercise.firstWords(text,2))
        assertEquals("محفوظ أصلي", ReadOnlyVerseExercise.lastWords(text,2))
        assertEquals("نص عربي محفوظ أصلي", text)
    }
}
