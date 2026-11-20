package com.nexvary.hifzai.learning

/** A deliberate sequence of active-recall practice activities.
 * This layer never rewrites a Quran source or claims to assess pronunciation.
 */
object RecallLadder {
    val sequence = listOf(
        PracticeKind.READ,
        PracticeKind.MASK_LIGHT,
        PracticeKind.MASK_HEAVY,
        PracticeKind.RECALL,
        PracticeKind.BRIDGE
    )

    fun next(step: PracticeKind): PracticeKind =
        sequence.getOrElse(sequence.indexOf(step) + 1) { PracticeKind.BRIDGE }

    fun concealment(step: PracticeKind, hints: Int = 0, answerRevealed: Boolean = false): Float {
        if (answerRevealed) return 0f
        return when (step) {
            PracticeKind.READ -> 0f
            PracticeKind.MASK_LIGHT -> 0.35f
            PracticeKind.MASK_HEAVY -> 0.70f
            PracticeKind.RECALL, PracticeKind.BRIDGE -> when {
                hints <= 0 -> 1f
                hints == 1 -> 0.8f
                else -> 0.55f
            }
        }
    }

    /**
     * Self-reported grading only. Assistance can never produce an unassisted high score.
     * An exposed answer during an active test is treated as assistance.
     */
    fun boundedGrade(reported: RecallGrade, hints: Int, exposedAnswer: Boolean): RecallGrade {
        require(hints >= 0)
        val assisted = hints > 0 || exposedAnswer
        return when {
            assisted && (reported == RecallGrade.EASY || reported == RecallGrade.GOOD) -> RecallGrade.HARD
            else -> reported
        }
    }
}

data class DailyPlan(
    val dueVerseIds: List<Int>,
    val newVerseIds: List<Int>
) {
    val allVerseIds: List<Int> get() = dueVerseIds + newVerseIds
}

object DailyMemorizationPlan {
    /**
     * A study budget in number of new verses, not a claim about learning efficacy.
     * Future-due cards are never pushed into a new-verse session.
     */
    fun create(
        verses: List<VerifiedVerse>,
        cards: Map<Int, ReviewCard>,
        today: Long,
        maxDue: Int = 15,
        maxNew: Int = 2
    ): DailyPlan {
        require(maxDue >= 0 && maxNew >= 0)
        val unique = verses.distinctBy { it.id }
        val due = unique.filter { v ->
            val c = cards[v.id]
            c != null && (c.repetitions > 0 || c.lapses > 0) && c.dueEpochDay <= today
        }.sortedWith(compareBy({ cards[it.id]?.dueEpochDay ?: Long.MAX_VALUE }, { it.chapter }, { it.verse }))
            .take(maxDue)
        val new = unique.filter { v ->
            val c = cards[v.id]
            c == null || (c.repetitions == 0 && c.lapses == 0)
        }
            .sortedWith(compareBy({ it.chapter }, { it.verse }))
            .take(maxNew)
        return DailyPlan(due.map { it.id }, new.map { it.id })
    }
}

/** Formatting helpers never modify the immutable supplied Quranic verse record. */
object ReadOnlyVerseExercise {
    fun firstWords(text: String, n: Int): String {
        require(n > 0)
        return text.trim().split(Regex("\\s+")).take(n).joinToString(" ")
    }
    fun lastWords(text: String, n: Int): String {
        require(n > 0)
        return text.trim().split(Regex("\\s+")).takeLast(n).joinToString(" ")
    }
}
