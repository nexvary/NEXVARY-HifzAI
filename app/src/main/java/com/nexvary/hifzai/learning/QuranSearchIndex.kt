package com.nexvary.hifzai.learning

/** Derived lookup only. Results always point to the untouched original verse. */
class QuranSearchIndex(verses: List<VerifiedVerse>, textForIndex: (VerifiedVerse) -> String = { it.text }) {
    private val entries = verses.map { it to normalize(textForIndex(it)) }

    fun search(query: String, limit: Int = 40): List<VerifiedVerse> {
        require(limit in 1..200)
        val normalized = normalize(query).trim()
        if (normalized.length < 2) return emptyList()
        return entries.asSequence().filter { (_, text) -> text.contains(normalized) }
            .take(limit).map { it.first }.toList()
    }

    companion object {
        private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
        fun normalize(text: String): String = text.replace(marks, "")
            .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace(Regex("\\s+"), " ")

        fun address(text: String): Pair<Int, Int>? {
            val western = buildString {
                text.trim().forEach { ch ->
                    val number = Character.digit(ch, 10)
                    append(if (number >= 0) ('0'.code + number).toChar() else ch)
                }
            }
            val match = Regex("^(\\d{1,3})\\s*[:/ ]\\s*(\\d{1,3})$").matchEntire(western) ?: return null
            val chapter = match.groupValues[1].toInt()
            val ayah = match.groupValues[2].toInt()
            return if (chapter in 1..114 && ayah in 1..286) chapter to ayah else null
        }
    }
}
