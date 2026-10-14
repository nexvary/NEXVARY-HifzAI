package com.nexvary.hifzai.learning

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class QuranPartitionsTest {
    private fun corpus() = QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText())
    private val xml get() = File("src/main/assets/quran/metadata-v1.0.xml").readText()

    @Test fun pagesAndAjzaCoverExactlyTheSame6236CanonicalReferences() {
        val corpus = corpus()
        val index = QuranPartitions.parse(xml, corpus)
        assertEquals(604, index.ofKind(PartitionKind.PAGE).size)
        assertEquals(30, index.ofKind(PartitionKind.JUZ).size)
        for (kind in PartitionKind.entries) {
            val flattened = index.ofKind(kind).flatMap { index.verses(it, corpus) }
            assertEquals(corpus.verses.map { it.id }, flattened.map { it.id })
            flattened.forEachIndexed { i, verse -> assertSame(corpus.verses[i], verse) }
        }
    }
    @Test fun trustedKnownBoundaryAddressesAndSurahCrossings() {
        val corpus = corpus(); val index = QuranPartitions.parse(xml, corpus)
        val pages = index.ofKind(PartitionKind.PAGE)
        assertEquals(1001, pages.first().firstId); assertEquals(1007, pages.first().lastId)
        assertEquals(2001, pages[1].firstId); assertEquals(2005, pages[1].lastId)
        val juz = index.ofKind(PartitionKind.JUZ)
        assertEquals(1001, juz.first().firstId); assertEquals(2141, juz.first().lastId)
        assertEquals(78001, juz.last().firstId); assertEquals(114006, juz.last().lastId)
    }
    @Test fun corruptBoundaryAndXmlEntitiesRejected() {
        val corpus = corpus()
        assertTrue(runCatching { QuranPartitions.parse(xml.replace("<page index=\"2\" sura=\"2\" aya=\"1\"", "<page index=\"2\" sura=\"115\" aya=\"1\""), corpus) }.isFailure)
        assertTrue(runCatching { QuranPartitions.parse("<!DOCTYPE untrusted>"+xml, corpus) }.isFailure)
    }
    @Test fun rangeSessionPreservesAssistance() {
        val session = PracticeSession(1,1,PracticeKind.RECALL,1,true,DifficultyTag.NONE,1001,2141)
        assertEquals(2141, session.changeStage(PracticeKind.MASK_LIGHT).rangeEnd)
        assertFalse(session.changeStage(PracticeKind.RECALL).effectiveHighScoreAllowed)
    }
}
