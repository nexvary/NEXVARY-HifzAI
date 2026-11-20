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
    /** A verse graded AGAIN (lapsed) must head the daily due list so it is re-reviewed
     *  before older overdue verses that have simply been waiting for their next interval. */
    @Test fun lapsedVersePrecedesOlderOverdueInDailyPlan() {
        val today = 10L
        val lapsed = ReviewCard(verses[0].id, repetitions = 0, lapses = 1, dueEpochDay = today)
        val overdue = ReviewCard(verses[1].id, repetitions = 1, lapses = 0, dueEpochDay = 5)
        val cards = mapOf(verses[0].id to lapsed, verses[1].id to overdue)
        val plan = DailyMemorizationPlan.create(verses, cards, today, maxDue = 5, maxNew = 0)
        assertEquals("lapsed verse must be first in the due list", verses[0].id, plan.dueVerseIds.first())
    }

    // ----- Anti-starvation: lapsed cards must not consume the entire maxDue budget -----

    /** When all due cards are lapsed, normal overdue cards still get at least half the budget. */
    @Test fun antiStarvationCapLapsedCards() {
        val today = 10L
        // 10 lapsed cards + 5 normal overdue, maxDue = 10
        val manyLapsed = (1..10).map { i ->
            val v = VerifiedVerse(100 + i, 1, "a")
            v to ReviewCard(v.id, repetitions = 0, lapses = 1, dueEpochDay = today)
        }
        val normalOverdue = (1..5).map { i ->
            val v = VerifiedVerse(200 + i, 1, "b")
            v to ReviewCard(v.id, repetitions = 1, lapses = 0, dueEpochDay = 5)
        }
        val allVerses = manyLapsed.map { it.first } + normalOverdue.map { it.first }
        val cards = (manyLapsed + normalOverdue).toMap().mapKeys { it.key.id }
        val plan = DailyMemorizationPlan.create(allVerses, cards, today, maxDue = 10, maxNew = 0)
        // lapsedCap = ceil(10/2) = 5, so at most 5 lapsed, at least 5 normal
        val lapsedIds = manyLapsed.map { it.first.id }.toSet()
        val normalIds = normalOverdue.map { it.first.id }.toSet()
        val lapsedInPlan = plan.dueVerseIds.count { it in lapsedIds }
        val normalInPlan = plan.dueVerseIds.count { it in normalIds }
        assertTrue("lapsed capped at half maxDue", lapsedInPlan <= 5)
        assertTrue("normal overdue fills remaining slots", normalInPlan >= 5)
        assertEquals(10, plan.dueVerseIds.size)
    }

    /** Empty review queue returns empty due list. */
    @Test fun emptyReviewQueueReturnsNewOnly() {
        val plan = DailyMemorizationPlan.create(verses, emptyMap(), 10, maxDue = 5, maxNew = 2)
        assertTrue("due list must be empty when no cards exist", plan.dueVerseIds.isEmpty())
        assertEquals(2, plan.newVerseIds.size)
    }

    /** Daily plan respects the maxDue budget (does not overflow). */
    @Test fun dailyPlanRespectsDueBudget() {
        val today = 10L
        val manyDue = (1..20).map { i ->
            val v = VerifiedVerse(10 + i, 1, "x")
            v to ReviewCard(v.id, repetitions = 1, dueEpochDay = today - i)
        }
        val plan = DailyMemorizationPlan.create(manyDue.map { it.first }, manyDue.toMap().mapKeys { it.key.id }, today, maxDue = 7, maxNew = 0)
        assertEquals(7, plan.dueVerseIds.size)
    }

    /** dueEpochDay == today is included; dueEpochDay == today+1 is excluded. */
    @Test fun dateBoundaryExactlyToday() {
        val today = 10L
        val dueToday    = ReviewCard(verses[0].id, repetitions = 1, dueEpochDay = today)
        val dueTomorrow = ReviewCard(verses[1].id, repetitions = 1, dueEpochDay = today + 1)
        val cards = mapOf(verses[0].id to dueToday, verses[1].id to dueTomorrow)
        val plan = DailyMemorizationPlan.create(verses, cards, today, maxDue = 5, maxNew = 0)
        assertTrue("today card must be in plan", verses[0].id in plan.dueVerseIds)
        assertFalse("tomorrow card must not be in plan", verses[1].id in plan.dueVerseIds)
    }

    /** sessionLapses in create() puts those cards at the front of the lapsed group. */
    @Test fun sessionLapsesPrecededPreviouslyLapsedInCreate() {
        val today = 10L
        val sessionLapse = ReviewCard(verses[0].id, repetitions = 0, lapses = 1, dueEpochDay = today)
        val prevLapse    = ReviewCard(verses[1].id, repetitions = 0, lapses = 1, dueEpochDay = today - 2)
        val cards = mapOf(verses[0].id to sessionLapse, verses[1].id to prevLapse)
        val plan = DailyMemorizationPlan.create(verses, cards, today, maxDue = 5, maxNew = 0, sessionLapses = setOf(verses[0].id))
        assertEquals("session-lapsed verse must be first in plan", verses[0].id, plan.dueVerseIds.first())
    }
}