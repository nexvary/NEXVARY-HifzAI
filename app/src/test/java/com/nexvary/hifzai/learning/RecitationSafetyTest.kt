package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class RecitationSafetyTest {
    private val reference = VerifiedVerse(1, 1, "نص تعليمي للاختبار")
    @Test fun noEvaluationMeansNoAssessmentEvenWhenAsrConfident() {
        assertTrue(RecitationComparator.compare(reference, RecognitionResult(reference.text, 1.0, "hafs-an-asim"))
            is RecitationAssessment.UnableToConfirm)
    }
    @Test fun lowOrInvalidConfidenceAndOtherRiwayatFailClosed() {
        for (confidence in listOf(0.3, Double.NaN, Double.POSITIVE_INFINITY, -1.0, 2.0))
            assertTrue(RecitationComparator.compare(reference, RecognitionResult(reference.text, confidence, "hafs-an-asim"), 0.9)
                is RecitationAssessment.UnableToConfirm)
        assertTrue(RecitationComparator.compare(reference, RecognitionResult(reference.text, 1.0, "warsh"), 0.9)
            is RecitationAssessment.UnableToConfirm)
    }
    @Test fun lexicalDistanceDoesNotJudgeTajweed() {
        assertEquals(RecitationAssessment.LexicalComparison(0, 3), RecitationComparator.compare(reference,
            RecognitionResult("نَصّ تعليمي للاختبار", 0.99, "hafs-an-asim"), 0.9))
        assertEquals(RecitationAssessment.LexicalComparison(1, 3), RecitationComparator.compare(reference,
            RecognitionResult("نص للاختبار", 0.99, "hafs-an-asim"), 0.9))
        assertEquals("نص تعليمي للاختبار", reference.text)
    }
    @Test fun publicUrlIsInsufficientForRecordingRights() {
        val record = RecitationRights("1", "1", "https://example.com/audio", "a".repeat(64), "", "", "hafs-an-asim", true, true)
        assertFalse(record.permitsUse(true))
    }
    @Test fun playbackOfflineAndTimingsRequireSeparatePermissions() {
        val record = RecitationRights("1", "1", "https://example.com/audio", "a".repeat(64), "document", "internal-review", "hafs-an-asim", true, false)
        assertTrue(record.permitsUse(false))
        assertFalse(record.permitsUse(true))
        assertFalse(record.permitsUse(false, true))
        assertTrue(record.copy(permitsOffline = true, timingVerified = true).permitsUse(true, true))
    }
}
