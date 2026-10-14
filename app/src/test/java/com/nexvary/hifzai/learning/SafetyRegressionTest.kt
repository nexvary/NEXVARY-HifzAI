package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class SafetyRegressionTest {
    @Test fun lapsedVersesRemainReviewsNotNew() {
        val verse = VerifiedVerse(2, 1, "نص اختبار")
        val lapsed = HifzScheduler.grade(ReviewCard(verse.id, repetitions = 3), RecallGrade.AGAIN, 100)
        val plan = DailyMemorizationPlan.create(listOf(verse), mapOf(verse.id to lapsed), 100)
        assertEquals(listOf(verse.id), plan.dueVerseIds)
        assertTrue(plan.newVerseIds.isEmpty())
        assertEquals(1, MemoryCoach.dueCount(listOf(lapsed), 100))
    }
    @Test fun changingStagesPreservesAssistance() {
        val initial = PracticeSession(1, 1, PracticeKind.RECALL, 1, true, DifficultyTag.WORDS)
        val changed = initial.changeStage(PracticeKind.MASK_LIGHT).changeStage(PracticeKind.RECALL)
        assertEquals(1, changed.hints)
        assertTrue(changed.exposed)
        assertFalse(changed.effectiveHighScoreAllowed)
    }
    @Test fun returningToReadingCountsAsExposure() {
        val initial = PracticeSession(1, 1, PracticeKind.RECALL, 0, false, DifficultyTag.NONE)
        assertTrue(initial.changeStage(PracticeKind.READ).exposed)
    }
    @Test fun zeroMaskReturnsEveryOriginalCharacter() {
        val original = "  نَصٌّ  لِلاختبار\nمع فواصل  "
        assertEquals(original, VerseMask.display(original, 0f))
    }
    @Test fun searchNeverReturnsNormalizedQuranAsCanonical() {
        val original = VerifiedVerse(2, 3, "نَصٌّ ٱخْتِبَارِيٌّ")
        val index = QuranSearchIndex(listOf(original))
        assertSame(original, index.search("نص اختباري").single())
        assertEquals("نَصٌّ ٱخْتِبَارِيٌّ", original.text)
        assertTrue(index.search(" ").isEmpty())
    }
    @Test fun addressesAcceptArabicAndRejectInvalidBounds() {
        assertEquals(2 to 255, QuranSearchIndex.address("٢:٢٥٥"))
        assertEquals(2 to 255, QuranSearchIndex.address("۲:۲۵۵"))
        assertNull(QuranSearchIndex.address("115:1"))
        assertNull(QuranSearchIndex.address("1:0"))
        assertNull(QuranSearchIndex.address("invalid"))
    }
    @Test fun orderingAndContinuationKeepCanonicalReferences() {
        val original = (1..4).map { VerifiedVerse(3, it, "نص اختبار $it") }
        val challenge = VerseExercises.orderChallenge(original, 0)
        assertFalse(VerseExercises.isCanonicalOrder(challenge))
        assertTrue(VerseExercises.isCanonicalOrder(challenge.sortedBy { it.verse }))
        assertSame(original[1], VerseExercises.continuation(original, 0))
        assertNull(VerseExercises.continuation(original, 3))
        assertNull(VerseExercises.continuation(listOf(original[0], VerifiedVerse(4, 1, "اختبار")), 0))
    }
    @Test(expected = IllegalArgumentException::class)
    fun minorsCannotBeAddedWithoutConsent() = NotebookRules.validateStudent("مستعار", false)
    @Test fun validLessonDoesNotCertifyTeacher() {
        NotebookRules.validateLesson(NotebookStudent("1", "مستعار", true), "المعلم المحلي", "2026-10-20T18:30", "مراجعة")
    }
    @Test(expected = java.time.format.DateTimeParseException::class)
    fun invalidAppointmentRejected() = NotebookRules.validateLesson(
        NotebookStudent("1", "مستعار", true), "المعلم", "2026-13-20T18:30", "")
}
