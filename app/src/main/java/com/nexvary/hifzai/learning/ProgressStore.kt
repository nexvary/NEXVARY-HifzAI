package com.nexvary.hifzai.learning

import android.content.Context

/** Local-only data: no microphone or personal information is collected. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("hifz_progress_v1", Context.MODE_PRIVATE)

    fun card(id: Int): ReviewCard = ReviewCard(
        verseId = id,
        repetitions = prefs.getInt("repetitions_" + id, 0),
        intervalDays = prefs.getInt("interval_" + id, 0),
        dueEpochDay = prefs.getLong("due_" + id, 0L),
        lapses = prefs.getInt("lapses_" + id, 0),
        ease = java.lang.Double.longBitsToDouble(
            prefs.getLong("ease_" + id, java.lang.Double.doubleToRawLongBits(2.5))
        )
    )

    fun memory(id: Int): MemoryFingerprint = MemoryFingerprint(
        verseId = id,
        attempts = prefs.getInt("attempts_" + id, 0),
        recalled = prefs.getInt("recalled_" + id, 0),
        hinted = prefs.getInt("hinted_" + id, 0),
        transitionsMissed = prefs.getInt("transitions_" + id, 0),
        wordMisses = prefs.getInt("wordMisses_" + id, 0),
        similarityMisses = prefs.getInt("similarityMisses_" + id, 0)
    )

    fun save(card: ReviewCard, memory: MemoryFingerprint) {
        require(card.verseId == memory.verseId)
        val id = card.verseId
        prefs.edit()
            .putInt("repetitions_" + id, card.repetitions)
            .putInt("interval_" + id, card.intervalDays)
            .putLong("due_" + id, card.dueEpochDay)
            .putInt("lapses_" + id, card.lapses)
            .putLong("ease_" + id, java.lang.Double.doubleToRawLongBits(card.ease))
            .putInt("attempts_" + id, memory.attempts)
            .putInt("recalled_" + id, memory.recalled)
            .putInt("hinted_" + id, memory.hinted)
            .putInt("transitions_" + id, memory.transitionsMissed)
            .putInt("wordMisses_" + id, memory.wordMisses)
            .putInt("similarityMisses_" + id, memory.similarityMisses)
            .apply()
    }

    fun clear() = prefs.edit().clear().commit()
}
