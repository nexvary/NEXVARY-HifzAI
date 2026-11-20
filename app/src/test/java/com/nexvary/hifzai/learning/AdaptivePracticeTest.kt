package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class AdaptivePracticeTest {
    private val a = VerifiedVerse(1, 1, "نَصٌّ تَجْرِيبِيٌّ مُشَكَّلٌ")
    private val b = VerifiedVerse(1, 2, "نص ثان محفوظ")
    private val c = VerifiedVerse(2, 1, "نص ثالث تجريبي")

    @Test fun recordsHintedRecallWithoutClaimingIndependent() {
        val start = MemoryFingerprint(1001)
        val result = MemoryCoach.record(start, RecallGrade.EASY, 1, false)
        assertEquals(1, result.attempts)
        assertEquals(0, result.recalled)
        assertEquals(1, result.hinted)
        assertEquals(0.0, result.independentRecallRate, 0.0001)
    }

    @Test fun tracksTransitionDifficulties() {
        val result = MemoryCoach.record(MemoryFingerprint(1001), RecallGrade.AGAIN, 0, true)
        assertEquals(1, result.transitionsMissed)
        val recommendation = MemoryCoach.recommend(ReviewCard(1001), result)
        assertEquals(PracticeKind.BRIDGE, recommendation.kind)
        assertEquals(RecommendationEvidence(1, RecommendationWindow.ACCUMULATED), recommendation.evidence)
    }

    @Test fun overdueCardsComeBeforeNewOnes() {
        val cards = mapOf(a.id to ReviewCard(a.id, 1, dueEpochDay = 5),
            b.id to ReviewCard(b.id, 1, dueEpochDay = 100))
        assertEquals(listOf(a, c, b), MemoryCoach.studyOrder(listOf(b, c, a), cards, 6))
        assertEquals(1, MemoryCoach.dueCount(cards.values, 6))
    }

    /** A lapsed card (graded AGAIN, repetitions reset) must be presented before older overdue
     *  cards so the learner revisits the failed verse immediately rather than after all backlog. */
    @Test fun lapsedCardsPreceedOverdueInStudyOrder() {
        val today = 10L
        // verse `a` was just failed: repetitions=0, lapses=1, due today (SM-2 AGAIN path)
        val lapsedA = ReviewCard(a.id, repetitions = 0, lapses = 1, dueEpochDay = today)
        // verse `b` has been overdue since day 5 (repetitions=1, never lapsed)
        val overdueB = ReviewCard(b.id, repetitions = 1, lapses = 0, dueEpochDay = 5)
        val cards = mapOf(a.id to lapsedA, b.id to overdueB)
        val ordered = MemoryCoach.studyOrder(listOf(b, c, a), cards, today)
        val lapsedIdx = ordered.indexOf(a)
        val overdueIdx = ordered.indexOf(b)
        assertTrue("lapsed card must come before older overdue card", lapsedIdx < overdueIdx)
    }

    @Test fun bridgeDoesNotCrossSurah() {
        assertEquals("تَجْرِيبِيٌّ مُشَكَّلٌ", BridgeTrainer.tailOfPrevious(listOf(a, b), 1))
        assertNull(BridgeTrainer.tailOfPrevious(listOf(a, c), 1))
    }

    @Test fun similarityIgnoresDiacriticsOnlyForSearch() {
        val near = VerifiedVerse(3, 1, "نص تجريبي مشكل")
        assertTrue(SimilarityIndex.similarTo(a, listOf(a, near)).contains(near))
        assertEquals("نَصٌّ تَجْرِيبِيٌّ مُشَكَّلٌ", a.text)
    }

    @Test fun coldStartHasNoFictitiousProgress() {
        val a = MemoryFingerprint(42)
        assertEquals(0, MemoryCoach.independentRecallPercent(listOf(a)))
        val recommendation = MemoryCoach.recommend(ReviewCard(42), a)
        assertEquals(PracticeKind.READ, recommendation.kind)
        assertEquals(RecommendationEvidence(0, RecommendationWindow.ACCUMULATED), recommendation.evidence)
    }
}
