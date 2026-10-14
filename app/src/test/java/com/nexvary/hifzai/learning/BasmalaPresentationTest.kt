package com.nexvary.hifzai.learning

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class BasmalaPresentationTest {
    private fun corpus() = QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText())

    @Test fun everyDisplayedSliceReassemblesToEveryOriginalSourceRecord() {
        val corpus = corpus()
        corpus.verses.forEach { verse -> assertEquals(verse.text, corpus.presentation(verse).original()) }
        assertEquals(112, corpus.verses.count { corpus.presentation(it).opening != null })
        assertEquals(6236, corpus.verses.size)
    }
    @Test fun fatihahIsNumberedAndTawbahHasNoOpeningButOtherSurahsSeparateItOnce() {
        val corpus = corpus()
        assertNull(corpus.presentation(corpus.find(1,1)!!).opening)
        assertNull(corpus.presentation(corpus.find(9,1)!!).opening)
        val ikhlas = corpus.find(112,1)!!
        val parts = corpus.presentation(ikhlas)
        assertEquals(corpus.find(1,1)!!.text, parts.opening)
        assertFalse(parts.ayahText.startsWith(parts.opening!!))
        assertEquals(ikhlas.text, parts.original())
    }
    @Test fun sourceShaddaAtTinAndQadrIsPreservedRatherThanReplacedByFatihah() {
        val corpus = corpus()
        for (chapter in listOf(95,97)) {
            val verse = corpus.find(chapter,1)!!
            val parts = corpus.presentation(verse)
            assertTrue(parts.opening!!.startsWith("بِّ"))
            assertNotEquals(corpus.find(1,1)!!.text, parts.opening)
            assertEquals(verse.text, parts.original())
        }
        assertNull(corpus.presentation(corpus.find(27,30)!!).opening)
    }
    @Test fun bridgeAndSimilarityIndexDoNotTreatTheUnnumberedOpeningAsAyahWords() {
        val corpus = corpus()
        assertEquals(corpus.ayahText(corpus.find(2,1)!!),
            BridgeTrainer.tailOfPrevious(corpus.chapter(2),1,textForExercise=corpus::ayahText))
        val query = corpus.find(1,1)!!.text
        val results = corpus.searchIndex.search(query)
        assertTrue(results.any { it.chapter==1 && it.verse==1 })
        assertFalse(results.any { it.chapter==112 && it.verse==1 })
        assertEquals(6236, corpus.verses.size)
    }
}
