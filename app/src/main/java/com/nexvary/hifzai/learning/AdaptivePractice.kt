package com.nexvary.hifzai.learning

/**
 * Learning evidence is self-reported in the first release.
 * Never confuse these estimates with verified recitation or tajweed accuracy.
 */
data class MemoryFingerprint(
    val verseId: Int,
    val attempts: Int = 0,
    val recalled: Int = 0,
    val hinted: Int = 0,
    val transitionsMissed: Int = 0
) {
    val independentRecallRate: Double
        get() = if (attempts == 0) 0.0 else recalled.toDouble() / attempts
}

enum class PracticeKind { READ, MASK_LIGHT, MASK_HEAVY, RECALL, BRIDGE }

data class TrainingRecommendation(
    val kind: PracticeKind,
    val explanation: String,
    val maskFraction: Float
)

object MemoryCoach {
    fun record(
        before: MemoryFingerprint,
        grade: RecallGrade,
        hintsUsed: Int,
        isBridgePractice: Boolean
    ): MemoryFingerprint {
        require(hintsUsed >= 0)
        val remembered = grade == RecallGrade.GOOD || grade == RecallGrade.EASY
        val independent = remembered && hintsUsed == 0
        return before.copy(
            attempts = before.attempts + 1,
            recalled = before.recalled + if (independent) 1 else 0,
            hinted = before.hinted + if (hintsUsed > 0) 1 else 0,
            transitionsMissed = before.transitionsMissed +
                if (isBridgePractice && !remembered) 1 else 0
        )
    }

    fun recommend(card: ReviewCard, memory: MemoryFingerprint): TrainingRecommendation =
        when {
            memory.attempts == 0 ->
                TrainingRecommendation(PracticeKind.READ, "ابدأ بقراءة الآية ثم إخفاء كلماتها", 0f)
            memory.transitionsMissed > 0 && memory.transitionsMissed * 3 >= memory.attempts ->
                TrainingRecommendation(PracticeKind.BRIDGE, "تحتاج إلى تدريب ربط الآيات", 1f)
            memory.hinted * 2 >= memory.attempts ->
                TrainingRecommendation(PracticeKind.MASK_LIGHT, "ابدأ بتلميحات أقل تدريجيًا", 0.35f)
            card.lapses >= 2 ->
                TrainingRecommendation(PracticeKind.MASK_LIGHT, "نعيد تثبيت موضع يتكرر نسيانه", 0.35f)
            memory.independentRecallRate >= 0.8 && memory.attempts >= 3 ->
                TrainingRecommendation(PracticeKind.RECALL, "جرّب التسميع دون رؤية الآية", 1f)
            else ->
                TrainingRecommendation(PracticeKind.MASK_HEAVY, "استرجع معظم الكلمات دون النظر", 0.7f)
        }

    /** Prioritise overdue reviews, then learning cards, then non-due reviewed cards. */
    fun studyOrder(
        verses: List<VerifiedVerse>,
        cards: Map<Int, ReviewCard>,
        todayEpochDay: Long
    ): List<VerifiedVerse> {
        return verses.sortedWith(
            compareBy<VerifiedVerse> { verse ->
                val card = cards[verse.id]
                when {
                    card == null || card.repetitions == 0 -> 1
                    card.dueEpochDay <= todayEpochDay -> 0
                    else -> 2
                }
            }.thenBy { verse ->
                val card = cards[verse.id]
                if (card != null && card.repetitions > 0) card.dueEpochDay else Long.MAX_VALUE
            }.thenBy { it.chapter }.thenBy { it.verse }
        )
    }

    fun dueCount(cards: Collection<ReviewCard>, todayEpochDay: Long) =
        cards.count { it.repetitions > 0 && it.dueEpochDay <= todayEpochDay }

    fun independentRecallPercent(memories: Collection<MemoryFingerprint>): Int {
        val attempts = memories.sumOf { it.attempts }
        if (attempts == 0) return 0
        return (100L * memories.sumOf { it.recalled } / attempts).toInt()
    }
}

object BridgeTrainer {
    /** Practice hint, not Quran text: displayed separately and clearly labelled. */
    fun tailOfPrevious(verses: List<VerifiedVerse>, index: Int, words: Int = 2): String? {
        if (index !in 1..verses.lastIndex) return null
        require(words > 0)
        val previous = verses[index - 1]
        val current = verses[index]
        if (previous.chapter != current.chapter || previous.verse + 1 != current.verse) return null
        return previous.text.trim().split(Regex("\\s+")).takeLast(words).joinToString(" ")
    }
}

/**
 * Similar-verse suggestions only, never used to render an authoritative mushaf.
 * Removing tashkeel is confined to the comparison index.
 */
object SimilarityIndex {
    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private fun tokens(text: String): Set<String> =
        text.replace(marks, "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .toSet()

    fun similarTo(target: VerifiedVerse, all: List<VerifiedVerse>, threshold: Double = 0.5): List<VerifiedVerse> {
        require(threshold in 0.0..1.0)
        val words = tokens(target.text)
        if (words.size < 2) return emptyList()
        return all.filter { it.id != target.id }
            .map { it to tokens(it.text) }
            .filter { (_, other) ->
                val union = (words + other).size
                union > 0 && words.intersect(other).size.toDouble() / union >= threshold
            }.map { it.first }
    }
}
