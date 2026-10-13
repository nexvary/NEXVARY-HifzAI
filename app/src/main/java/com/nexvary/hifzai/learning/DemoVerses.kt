package com.nexvary.hifzai.learning

data class VerifiedVerse(val chapter: Int, val verse: Int, val text: String) {
    val id: Int get() = chapter * 1000 + verse
}

/**
 * Small prototype fixture, manually cross-checked with https://quran.com/112 .
 * This is NOT a substitute for importing and verifying an authoritative corpus.
 * Never use language models to generate canonical Quran verses.
 */
object DemoVerses {
    val alIkhlas: List<VerifiedVerse> = listOf(
        VerifiedVerse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ"),
        VerifiedVerse(112, 2, "ٱللَّهُ ٱلصَّمَدُ"),
        VerifiedVerse(112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ"),
        VerifiedVerse(112, 4, "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ")
    )
}
