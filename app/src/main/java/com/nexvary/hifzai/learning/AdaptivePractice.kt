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
    val transitionsMissed: Int = 0,
    val wordMisses: Int = 0,
    val similarityMisses: Int = 0
) {
    val independentRecallRate: Double
        get() = if (attempts == 0) 0.0 else recalled.toDouble() / attempts
}

enum class PracticeKind { READ, MASK_LIGHT, MASK_HEAVY, RECALL, BRIDGE }

/** Learner-observed difficulty. This is NOT an automated Quran error judgment. */
enum class DifficultyTag { NONE, WORDS, TRANSITION, SIMILAR_VERSE }

data class TrainingRecommendation(
    val kind: PracticeKind,
    val explanation: String,
    val maskFraction: Float,
    val evidence: RecommendationEvidence
)

enum class RecommendationWindow { ACCUMULATED, RECENT }

data class RecommendationEvidence(val attempts: Int, val window: RecommendationWindow) {
    /** Sample size describes the available self-assessments, without predicting future recall. */
    val description: String get() = when {
        attempts == 0 -> "اقتراح بداية • لا توجد محاولات مسجلة لهذه الآية"
        window == RecommendationWindow.RECENT -> "أساس الاقتراح: $attempts من محاولات التقييم الذاتي خلال آخر ١٤ يومًا"
        else -> "أساس الاقتراح: $attempts من محاولات التقييم الذاتي المتراكمة لهذه الآية"
    }
}

object MemoryCoach {
    fun record(
        before: MemoryFingerprint,
        grade: RecallGrade,
        hintsUsed: Int,
        isBridgePractice: Boolean,
        difficulty: DifficultyTag = DifficultyTag.NONE
    ): MemoryFingerprint {
        require(hintsUsed >= 0)
        val remembered = grade == RecallGrade.GOOD || grade == RecallGrade.EASY
        val independent = remembered && hintsUsed == 0
        return before.copy(
            attempts = before.attempts + 1,
            recalled = before.recalled + if (independent) 1 else 0,
            hinted = before.hinted + if (hintsUsed > 0) 1 else 0,
            transitionsMissed = before.transitionsMissed +
                if (!remembered && (isBridgePractice || difficulty == DifficultyTag.TRANSITION)) 1 else 0,
            wordMisses = before.wordMisses +
                if (!remembered && difficulty == DifficultyTag.WORDS) 1 else 0,
            similarityMisses = before.similarityMisses +
                if (!remembered && difficulty == DifficultyTag.SIMILAR_VERSE) 1 else 0
        )
    }

    fun recommend(card: ReviewCard, memory: MemoryFingerprint): TrainingRecommendation {
        val evidence = RecommendationEvidence(memory.attempts, RecommendationWindow.ACCUMULATED)
        fun recommendation(kind: PracticeKind, explanation: String, mask: Float) =
            TrainingRecommendation(kind, explanation, mask, evidence)
        return when {
            memory.attempts == 0 ->
                recommendation(PracticeKind.READ, "ابدأ بقراءة الآية ثم إخفاء كلماتها", 0f)
            memory.transitionsMissed > 0 && memory.transitionsMissed * 3 >= memory.attempts ->
                recommendation(PracticeKind.BRIDGE, "تحتاج إلى تدريب ربط الآيات", 1f)
            memory.similarityMisses > 0 && memory.similarityMisses * 3 >= memory.attempts ->
                recommendation(PracticeKind.MASK_LIGHT, "راجع الكلمات التي تختلط عليك؛ تصنيف التشابه يحتاج مراجعة معلّم", 0.35f)
            memory.wordMisses > 0 && memory.wordMisses * 3 >= memory.attempts ->
                recommendation(PracticeKind.MASK_LIGHT, "تدرّب على الكلمات المتعثرة بالتدرج", 0.35f)
            memory.hinted * 2 >= memory.attempts ->
                recommendation(PracticeKind.MASK_LIGHT, "ابدأ بتلميحات أقل تدريجيًا", 0.35f)
            card.lapses >= 2 ->
                recommendation(PracticeKind.MASK_LIGHT, "نعيد تثبيت موضع يتكرر نسيانه", 0.35f)
            memory.independentRecallRate >= 0.8 && memory.attempts >= 3 ->
                recommendation(PracticeKind.RECALL, "جرّب التسميع دون رؤية الآية", 1f)
            else ->
                recommendation(PracticeKind.MASK_HEAVY, "استرجع معظم الكلمات دون النظر", 0.7f)
        }
    }

    /**
     * Five-tier verse ordering. Within each tier, oldest due date comes first.
     *
     *  Tier 0 — current-session failures: graded AGAIN this session (in [sessionLapses]),
     *            repetitions reset to 0 by SM-2. Re-encounter the verse immediately.
     *  Tier 1 — previously lapsed and due: repetitions=0, lapses>0, dueEpochDay<=today,
     *            but NOT graded this session. Failed on an earlier day, now overdue again.
     *  Tier 2 — ordinary overdue: repetitions>0, dueEpochDay<=today. Never lapsed, or
     *            lapsed then successfully reviewed (repetitions advanced back above 0).
     *  Tier 3 — new: no card, or repetitions=0 and lapses=0. Never studied before.
     *  Tier 4 — future: dueEpochDay>today. Not yet due.
     *
     * [sessionLapses] is an in-memory set of verse IDs graded AGAIN in the current practice
     * session. It is NOT persisted; callers supply it from ViewModel state. When empty (the
     * default), tiers 0 and 1 collapse into a single lapsed-due group, which still precedes
     * ordinary overdue cards.
     */
    fun studyOrder(
        verses: List<VerifiedVerse>,
        cards: Map<Int, ReviewCard>,
        todayEpochDay: Long,
        sessionLapses: Set<Int> = emptySet()
    ): List<VerifiedVerse> {
        return verses.sortedWith(
            compareBy<VerifiedVerse> { verse ->
                val card = cards[verse.id]
                val isLapsed = card != null && card.repetitions == 0 && card.lapses > 0
                    && card.dueEpochDay <= todayEpochDay
                when {
                    isLapsed && verse.id in sessionLapses -> 0
                    isLapsed -> 1
                    card != null && card.repetitions > 0 && card.dueEpochDay <= todayEpochDay -> 2
                    card == null || (card.repetitions == 0 && card.lapses == 0) -> 3
                    else -> 4
                }
            }.thenBy { verse ->
                val card = cards[verse.id]
                if (card != null && (card.repetitions > 0 || card.lapses > 0)) card.dueEpochDay else Long.MAX_VALUE
            }.thenBy { it.chapter }.thenBy { it.verse }
        )
    }
    fun dueCount(cards: Collection<ReviewCard>, todayEpochDay: Long) =
        cards.count { (it.repetitions > 0 || it.lapses > 0) && it.dueEpochDay <= todayEpochDay }

    fun independentRecallPercent(memories: Collection<MemoryFingerprint>): Int {
        val attempts = memories.sumOf { it.attempts }
        if (attempts == 0) return 0
        return (100L * memories.sumOf { it.recalled } / attempts).toInt()
    }
}

object BridgeTrainer {
    /** Practice hint, not Quran text: displayed separately and clearly labelled. */
    fun tailOfPrevious(verses: List<VerifiedVerse>, index: Int, words: Int = 2,
        textForExercise: (VerifiedVerse) -> String = { it.text }): String? {
        if (index !in 1..verses.lastIndex) return null
        require(words > 0)
        val previous = verses[index - 1]
        val current = verses[index]
        if (previous.chapter != current.chapter || previous.verse + 1 != current.verse) return null
        return textForExercise(previous).trim().split(Regex("\\s+")).takeLast(words).joinToString(" ")
    }

    /**
     * Reverse bridge: first [words] of the verse at [index]+1.
     * Prompts "what verse follows?" — Phase 030 reverse-transition practice.
     * Returns null when there is no immediately consecutive next verse in the same chapter.
     */
    fun headOfNext(verses: List<VerifiedVerse>, index: Int, words: Int = 2,
        textForExercise: (VerifiedVerse) -> String = { it.text }): String? {
        if (index !in 0 until verses.lastIndex) return null
        require(words > 0)
        val current = verses[index]
        val next = verses[index + 1]
        if (current.chapter != next.chapter || current.verse + 1 != next.verse) return null
        return textForExercise(next).trim().split(Regex("\\s+")).take(words).joinToString(" ")
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

    fun similarTo(target: VerifiedVerse, all: List<VerifiedVerse>, threshold: Double = 0.5,
        textForComparison: (VerifiedVerse) -> String = { it.text }): List<VerifiedVerse> {
        require(threshold in 0.0..1.0)
        val words = tokens(textForComparison(target))
        if (words.size < 2) return emptyList()
        return all.filter { it.id != target.id }
            .map { it to tokens(textForComparison(it)) }
            .filter { (_, other) ->
                val union = (words + other).size
                union > 0 && words.intersect(other).size.toDouble() / union >= threshold
            }.map { it.first }
    }
}
