package com.nexvary.hifzai.learning

/** Exercises hold references only: no rewritten, AI-generated or merged Quran text. */
object VerseExercises {
    fun continuation(verses: List<VerifiedVerse>, index: Int): VerifiedVerse? {
        val current = verses.getOrNull(index) ?: return null
        val next = verses.getOrNull(index + 1) ?: return null
        return next.takeIf { it.chapter == current.chapter && it.verse == current.verse + 1 }
    }
    fun orderChallenge(verses: List<VerifiedVerse>, start: Int): List<VerifiedVerse> {
        require(start in verses.indices)
        val segment = verses.drop(start).take(3)
        return if (segment.size >= 2) segment.drop(1) + segment.first() else segment
    }
    fun isCanonicalOrder(selected: List<VerifiedVerse>): Boolean = selected.isNotEmpty() &&
        selected.zipWithNext().all { (a, b) -> a.chapter == b.chapter && a.verse + 1 == b.verse }
}
