package com.nexvary.hifzai.learning

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class QuranCorpusJvmTest {
    private fun corpus() = QuranCorpus.parse(
        File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText())

    @Test fun entireCorpusHasContiguousOrderAndImmutableViews() {
        val corpus = corpus()
        assertEquals(6236, corpus.verses.size)
        assertEquals((1..114).toList(), corpus.surahs.map { it.number })
        assertTrue(runCatching { (corpus.verses as MutableList<*>).clear() }.exceptionOrNull() is UnsupportedOperationException)
        assertTrue(runCatching { (corpus.chapter(2) as MutableList<*>).clear() }.exceptionOrNull() is UnsupportedOperationException)
        assertTrue(runCatching { (corpus.surahs as MutableList<*>).clear() }.exceptionOrNull() is UnsupportedOperationException)
        assertEquals(286, corpus.chapter(2).size)
        for (surah in corpus.surahs) {
            assertEquals((1..surah.ayahCount).toList(), corpus.chapter(surah.number).map { it.verse })
        }
    }

    @Test fun wholeSurahReorderingCannotPassParser() {
        val text = File("src/main/assets/quran/uthmani-v1.1.txt").readText()
        val lines = text.lines()
        val records = lines.filter { it.isNotBlank() && !it.startsWith("#") }
        val swapped = records.filter { it.startsWith("2|") } + records.filter { it.startsWith("1|") } +
            records.filter { !it.startsWith("1|") && !it.startsWith("2|") }
        val fixture = (swapped + lines.filter { it.startsWith("#") }).joinToString("\n")
        assertTrue(runCatching { QuranCorpus.parse(fixture,
            File("src/main/assets/quran/surah-metadata.txt").readText()) }.isFailure)
    }
    @Test fun basmalaIsVerbatimReferenceOnlyWhereApplicable() {
        val corpus = corpus()
        assertNull(corpus.openingBasmala(1)); assertNull(corpus.openingBasmala(9))
        assertEquals(corpus.find(1,1)!!.text, corpus.openingBasmala(114))
        assertEquals(6236, corpus.verses.size)
    }
}
