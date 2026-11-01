package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class LexicalAlignmentTest {
    @Test fun distinguishesOmissionInsertionAndSubstitution() {
        assertEquals(1, LexicalAligner.align("ألف باء جيم", "ألف جيم").omissions)
        assertEquals(1, LexicalAligner.align("ألف باء", "ألف جديد باء").insertions)
        assertEquals(1, LexicalAligner.align("ألف باء", "ألف جيم").substitutions)
        val omitted = LexicalAligner.align("ألف باء جيم", "ألف جيم").edits.single { it.kind == WordEditKind.OMITTED }
        assertEquals(1, omitted.referenceIndex)
        assertNull(omitted.heardIndex)
    }
    @Test fun repeatedWordsMarksAndEmptyStringsAreDeterministic() {
        assertEquals(1, LexicalAligner.align("ألف ألف باء", "ألف باء").distance)
        assertEquals(0, LexicalAligner.align("نَصّ تعليمي", "نص تعليمي").distance)
        assertEquals(2, LexicalAligner.align("نص تعليمي", "").omissions)
        assertEquals(2, LexicalAligner.align("", "نص تعليمي").insertions)
        assertEquals(0, LexicalAligner.align("", "").distance)
    }
    @Test fun rejectsUnboundedInput() {
        assertTrue(runCatching { LexicalAligner.align("x ".repeat(513), "x") }.isFailure)
        assertTrue(runCatching { LexicalAligner.align("x", "x".repeat(20_001)) }.isFailure)
    }
    @Test fun expirationRevocationSourceAndNarrationAreSeparateChecks() {
        val rights = RecitationRights("fixture", "fixture", "https://example.com/a", "a".repeat(64),
            "synthetic-document", "synthetic-review", "hafs-an-asim", true, true, expiresEpochDay = 10)
        assertTrue(rights.permitsUse(true, todayEpochDay = 10))
        assertFalse(rights.permitsUse(true, todayEpochDay = 11))
        assertFalse(rights.copy(revoked = true).permitsUse(true, todayEpochDay = 9))
        assertFalse(rights.copy(sourceUrl = "https://").permitsUse(true, todayEpochDay = 9))
        assertFalse(rights.copy(sourceUrl = "https://user:password@example.com").permitsUse(true, todayEpochDay = 9))
        assertFalse(rights.copy(riwayah = "warsh").permitsUse(true, todayEpochDay = 9))
    }
}
