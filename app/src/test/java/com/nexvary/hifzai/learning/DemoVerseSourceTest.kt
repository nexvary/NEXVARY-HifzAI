package com.nexvary.hifzai.learning

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression-only pin for the 4-verse demo fixture, checked against
 * https://quran.com/112 on 2026-10-14.
 * This is NOT a qualified scholarly certification or production corpus approval.
 */
class DemoVerseSourceTest {
    @Test fun surahAlIkhlasMatchesTheFourRecordedDemoLines() {
        val original = listOf(
            "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            "ٱللَّهُ ٱلصَّمَدُ",
            "لَمْ يَلِدْ وَلَمْ يُولَدْ",
            "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ"
        )
        val actual = DemoVerses.alIkhlas
        assertEquals(4, actual.size)
        assertEquals(listOf(1,2,3,4), actual.map { it.verse })
        assertEquals(listOf(112,112,112,112), actual.map { it.chapter })
        assertEquals(original, actual.map { it.text })
    }
}
