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
    // ----- Tier distinction: session vs previously lapsed -----

    /** Tier 0 (session-lapsed) is ordered before Tier 1 (previously-lapsed-due). */
    @Test fun sessionLapsedPrecedesPreviouslyLapsedInStudyOrder() {
        val today = 10L
        val sessionCard = ReviewCard(a.id, repetitions = 0, lapses = 1, dueEpochDay = today)
        val prevCard    = ReviewCard(b.id, repetitions = 0, lapses = 1, dueEpochDay = today - 1)
        val cards = mapOf(a.id to sessionCard, b.id to prevCard)
        val ordered = MemoryCoach.studyOrder(listOf(b, a, c), cards, today, sessionLapses = setOf(a.id))
        assertTrue("session-lapsed must precede previously-lapsed", ordered.indexOf(a) < ordered.indexOf(b))
    }

    /** Without sessionLapses all lapsed-due cards still precede ordinary overdue. */
    @Test fun lapsedCardsPreceedOverdueInStudyOrder() {
        val today = 10L
        val lapsedA = ReviewCard(a.id, repetitions = 0, lapses = 1, dueEpochDay = today)
        val overdueB = ReviewCard(b.id, repetitions = 1, lapses = 0, dueEpochDay = 5)
        val cards = mapOf(a.id to lapsedA, b.id to overdueB)
        val ordered = MemoryCoach.studyOrder(listOf(b, c, a), cards, today)
        assertTrue("lapsed card must come before older overdue card", ordered.indexOf(a) < ordered.indexOf(b))
    }

    /** Previously lapsed card from an earlier day, now re-due, stays Tier 1. */
    @Test fun previouslyLapsedCardIsStillHighPriority() {
        val today = 20L
        val oldLapse = ReviewCard(a.id, repetitions = 0, lapses = 2, dueEpochDay = 13L)
        val normal   = ReviewCard(b.id, repetitions = 3, lapses = 0, dueEpochDay = 13L)
        val ordered  = MemoryCoach.studyOrder(listOf(b, a), mapOf(a.id to oldLapse, b.id to normal), today)
        assertTrue("previously lapsed must beat ordinary overdue", ordered.indexOf(a) < ordered.indexOf(b))
    }

    /** Multiple lapsed cards are sub-sorted by ascending dueEpochDay (oldest failure first). */
    @Test fun multipleLapsedCardsAreOrderedByOldestDueFirst() {
        val today = 10L
        val lapseEarlier = ReviewCard(a.id, repetitions = 0, lapses = 1, dueEpochDay = 7L)
        val lapseLater   = ReviewCard(b.id, repetitions = 0, lapses = 1, dueEpochDay = today)
        val ordered = MemoryCoach.studyOrder(listOf(b, a), mapOf(a.id to lapseEarlier, b.id to lapseLater), today)
        assertEquals("older-lapsed verse must come first", a, ordered[0])
    }

    /** Ordinary overdue (Tier 2) precedes new verses (Tier 3). */
    @Test fun overdueCardsComeBeforeNewOnes() {
        val cards = mapOf(a.id to ReviewCard(a.id, 1, dueEpochDay = 5),
            b.id to ReviewCard(b.id, 1, dueEpochDay = 100))
        assertEquals(listOf(a, c, b), MemoryCoach.studyOrder(listOf(b, c, a), cards, 6))
        assertEquals(1, MemoryCoach.dueCount(cards.values, 6))
    }

    /** An empty verse list returns an empty list. */
    @Test fun emptyVerseListReturnsEmpty() {
        assertEquals(emptyList<VerifiedVerse>(), MemoryCoach.studyOrder(emptyList(), emptyMap(), 1L))
    }

    /** Future cards (Tier 4) land after new verses (Tier 3). */
    @Test fun futureCardIsLastTier() {
        val today = 5L
        val future = ReviewCard(a.id, repetitions = 1, lapses = 0, dueEpochDay = 100L)
        val ordered = MemoryCoach.studyOrder(listOf(a, b), mapOf(a.id to future), today)
        assertEquals("new verse before future card", b, ordered[0])
        assertEquals("future card is last", a, ordered[1])
    }

    /** Grading AGAIN three times accumulates lapses; card stays in lapsed tier. */
    @Test fun repeatedAgainGradesAccumulateLapses() {
        val today = 10L
        var card = ReviewCard(a.id)
        repeat(3) { card = HifzScheduler.grade(card, RecallGrade.AGAIN, today) }
        assertEquals(3, card.lapses)
        assertEquals(0, card.repetitions)
        assertEquals(today, card.dueEpochDay)
        val ordered = MemoryCoach.studyOrder(listOf(a), mapOf(a.id to card), today)
        assertEquals(a, ordered[0])
    }

    /** dueEpochDay == today is due; dueEpochDay == today+1 is future. */
    @Test fun dateBoundaryExactlyToday() {
        val today = 7L
        val dueToday    = ReviewCard(a.id, repetitions = 1, dueEpochDay = today)
        val dueTomorrow = ReviewCard(b.id, repetitions = 1, dueEpochDay = today + 1)
        assertEquals(1, MemoryCoach.dueCount(listOf(dueToday, dueTomorrow), today))
        val ordered = MemoryCoach.studyOrder(listOf(a, b), mapOf(a.id to dueToday, b.id to dueTomorrow), today)
        assertTrue("due-today card must precede future card", ordered.indexOf(a) < ordered.indexOf(b))
    }

    // ----- BridgeTrainer.headOfNext (Phase 030) -----

    @Test fun headOfNextReturnsFirstWordsOfNextVerse() {
        assertEquals("نص ثان", BridgeTrainer.headOfNext(listOf(a, b), 0))
    }

    @Test fun headOfNextNullWhenNotConsecutive() {
        assertNull(BridgeTrainer.headOfNext(listOf(a, c), 0))
    }

    @Test fun headOfNextNullAtEndOfList() {
        assertNull(BridgeTrainer.headOfNext(listOf(a, b), 1))
    }

    @Test fun headOfNextWordCountRespected() {
        assertEquals("نص", BridgeTrainer.headOfNext(listOf(a, b), 0, words = 1))
    }
}