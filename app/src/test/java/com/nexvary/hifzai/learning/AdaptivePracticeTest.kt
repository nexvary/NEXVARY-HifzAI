package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class AdaptivePracticeTest {
    private val a = VerifiedVerse(1, 1, "نَصٌّ تَجْرِيبِيٌّ مُشَكَّلٌ")
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
        assertEquals(PracticeKind.BRIDGE, MemoryCoach.recommend(ReviewCard(1001), result).kind)
    }

    @Test fun overdueCardsComeBeforeNewOnes() {
        val cards = mapOf(a.id to ReviewCard(a.id, 1, dueEpochDay = 5),
            b.id to ReviewCard(b.id, 1, dueEpochDay = 100))
        assertEquals(listOf(a, c, b), MemoryCoach.studyOrder(listOf(b, c, a), cards, 6))
        assertEquals(1, MemoryCoach.dueCount(cards.values, 6))
    }

    @Test fun bridgeDoesNotCrossSurah() {
        assertEquals("تَجْرِيبِيٌّ مُشَكَّلٌ", BridgeTrainer.tailOfPrevious(listOf(a, b), 1))
        assertNull(BridgeTrainer.tailOfPrevious(listOf(a, c), 1))
    }

    @Test fun similarityIgnoresDiacriticsOnlyForSearch() {
        val near = VerifiedVerse(3, 1, "نص تجريبي مشكل")
        assertTrue(SimilarityIndex.similarTo(a, listOf(a, near)).contains(near))
        assertEquals("نَصٌّ تَجْرِيبِيٌّ مُشَكَّلٌ", a.text)
    }

    @Test fun coldStartHasNoFictitiousProgress() {
        val a = MemoryFingerprint(42)
        assertEquals(0, MemoryCoach.independentRecallPercent(listOf(a)))
        assertEquals(PracticeKind.READ, MemoryCoach.recommend(ReviewCard(42), a).kind)
    }
}
