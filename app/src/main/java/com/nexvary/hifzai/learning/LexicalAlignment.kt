package com.nexvary.hifzai.learning

enum class WordEditKind { MATCH, OMITTED, INSERTED, SUBSTITUTED }
data class WordEdit(val kind: WordEditKind, val referenceIndex: Int?, val heardIndex: Int?)
data class LexicalAlignment(val edits: List<WordEdit>, val referenceWords: Int) {
    val omissions get() = edits.count { it.kind == WordEditKind.OMITTED }
    val insertions get() = edits.count { it.kind == WordEditKind.INSERTED }
    val substitutions get() = edits.count { it.kind == WordEditKind.SUBSTITUTED }
    val distance get() = omissions + insertions + substitutions
}

/** Derived experimental word positions only; never changes, grades or renders Quran text. */
object LexicalAligner {
    fun align(reference: String, heard: String): LexicalAlignment {
        fun words(text: String) = QuranSearchIndex.normalize(text).trim()
            .split(Regex("\\s+")).filter { it.isNotEmpty() }
        require(reference.length <= 20_000 && heard.length <= 20_000)
        val a = words(reference); val b = words(heard)
        require(a.size <= 512 && b.size <= 512)
        val distance = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 0..a.size) distance[i][0] = i
        for (j in 0..b.size) distance[0][j] = j
        for (i in 1..a.size) for (j in 1..b.size) distance[i][j] = minOf(
            distance[i-1][j] + 1, distance[i][j-1] + 1,
            distance[i-1][j-1] + if (a[i-1] == b[j-1]) 0 else 1)
        val result = mutableListOf<WordEdit>()
        var i = a.size; var j = b.size
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && a[i-1] == b[j-1] && distance[i][j] == distance[i-1][j-1] -> {
                    result += WordEdit(WordEditKind.MATCH, --i, --j)
                }
                i > 0 && j > 0 && distance[i][j] == distance[i-1][j-1] + 1 -> {
                    result += WordEdit(WordEditKind.SUBSTITUTED, --i, --j)
                }
                i > 0 && distance[i][j] == distance[i-1][j] + 1 -> {
                    result += WordEdit(WordEditKind.OMITTED, --i, null)
                }
                else -> result.add(WordEdit(WordEditKind.INSERTED, null, --j))
            }
        }
        return LexicalAlignment(result.reversed(), a.size)
    }
}
