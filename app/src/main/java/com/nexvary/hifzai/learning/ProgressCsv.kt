package com.nexvary.hifzai.learning

/** A portable snapshot of counters. Free text and notebook data never enter the export. */
internal object ProgressCsv {
    fun encode(verses: List<VerifiedVerse>, memories: Map<Int, MemoryFingerprint>): String = buildString {
        append("surah,ayah,attempts,independent_recall,hinted,transition_misses,word_misses,similarity_misses\r\n")
        for (verse in verses) {
            val memory = memories[verse.id] ?: continue
            if (memory.attempts == 0) continue
            append(verse.chapter).append(',').append(verse.verse).append(',')
            append(memory.attempts).append(',').append(memory.recalled).append(',')
            append(memory.hinted).append(',').append(memory.transitionsMissed).append(',')
            append(memory.wordMisses).append(',').append(memory.similarityMisses).append("\r\n")
        }
    }
}
