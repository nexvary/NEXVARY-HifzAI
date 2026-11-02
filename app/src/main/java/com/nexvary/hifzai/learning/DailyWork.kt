package com.nexvary.hifzai.learning

/** Distinct self-assessed verses for one local calendar day, not a mastery/efficacy claim. */
data class DailyWork(val epochDay: Long, val newVerseIds: Set<Int> = emptySet(),
    val reviewVerseIds: Set<Int> = emptySet()) {
    init {
        require(epochDay >= 0 && newVerseIds.size + reviewVerseIds.size <= 6236)
        require(newVerseIds.intersect(reviewVerseIds).isEmpty())
        require((newVerseIds + reviewVerseIds).all { it / 1000 in 1..114 && it % 1000 in 1..286 })
    }
    val allVerseIds: Set<Int> get() = newVerseIds + reviewVerseIds
    fun record(verseId: Int, firstAttempt: Boolean): DailyWork = when {
        verseId in allVerseIds -> this
        firstAttempt -> copy(newVerseIds = newVerseIds + verseId)
        else -> copy(reviewVerseIds = reviewVerseIds + verseId)
    }
}
