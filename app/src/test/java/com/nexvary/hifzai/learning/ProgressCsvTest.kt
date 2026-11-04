package com.nexvary.hifzai.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCsvTest {
    private val header =
        "surah,ayah,attempts,independent_recall,hinted,transition_misses,word_misses,similarity_misses\r\n"

    @Test fun exportsOnlyStudiedCanonicalAddressesAndCountersInCorpusOrder() {
        val verses = listOf(VerifiedVerse(1, 7, "first private fixture"),
            VerifiedVerse(2, 1, "second private fixture"), VerifiedVerse(2, 2, "unassessed fixture"))
        val memories = linkedMapOf(
            2001 to MemoryFingerprint(2001, attempts = 1, recalled = 1),
            1007 to MemoryFingerprint(1007, attempts = 5, recalled = 2, hinted = 1,
                transitionsMissed = 1, wordMisses = 2, similarityMisses = 1),
            2002 to MemoryFingerprint(2002),
            9999 to MemoryFingerprint(9999, attempts = 8)
        )
        assertEquals(header + "1,7,5,2,1,1,2,1\r\n2,1,1,1,0,0,0,0\r\n",
            ProgressCsv.encode(verses, memories))
    }

    @Test fun emptyProgressExportsAHeaderWithoutInventingData() {
        assertEquals(header, ProgressCsv.encode(listOf(VerifiedVerse(1, 1, "fixture")), emptyMap()))
    }
}
