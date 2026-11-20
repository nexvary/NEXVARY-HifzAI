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
     *
     * Due cards are split into two groups ordered by priority, then by oldest due date:
     *   1. Lapsed (repetitions=0, lapses>0, dueEpochDay<=today): capped at ceil(maxDue/2)
     *      so ordinary overdue cards are never starved of session slots.
     *   2. Ordinary overdue (repetitions>0, dueEpochDay<=today): fills remaining slots.
     *
     * To distinguish current-session failures from previously-lapsed-due cards, callers may
     * pass [sessionLapses] — an in-memory set of verse IDs graded AGAIN this session.
     * Lapsed cards in [sessionLapses] are sorted to the front of the lapsed group.
     */
    fun create(
        verses: List<VerifiedVerse>,
        cards: Map<Int, ReviewCard>,
        today: Long,
        maxDue: Int = 15,
        maxNew: Int = 2,
        sessionLapses: Set<Int> = emptySet()
    ): DailyPlan {
        require(maxDue >= 0 && maxNew >= 0)
        val unique = verses.distinctBy { it.id }

        // Anti-starvation: lapsed cards get at most ceil(maxDue/2) slots.
        val lapsedCap = if (maxDue == 0) 0 else ((maxDue + 1) / 2).coerceAtLeast(1)
        val lapsed = unique.filter { v ->
            val c = cards[v.id]
            c != null && c.repetitions == 0 && c.lapses > 0 && c.dueEpochDay <= today
        }.sortedWith(compareBy(
            // Current-session failures (Tier 0) precede previously-lapsed-due (Tier 1).
            { v -> if (v.id in sessionLapses) 0 else 1 },
            { cards[it.id]?.dueEpochDay ?: Long.MAX_VALUE },
            { it.chapter },
            { it.verse }
        )).take(lapsedCap)

        val lapsedIds = lapsed.map { it.id }.toHashSet()
        val normal = unique.filter { v ->
            val c = cards[v.id]
            c != null && c.repetitions > 0 && c.dueEpochDay <= today && v.id !in lapsedIds
        }.sortedWith(compareBy(
            { cards[it.id]?.dueEpochDay ?: Long.MAX_VALUE },
            { it.chapter },
            { it.verse }
        )).take(maxDue - lapsed.size)

        val due = lapsed + normal

        val newCards = unique.filter { v ->
            val c = cards[v.id]
            c == null || (c.repetitions == 0 && c.lapses == 0)
        }.sortedWith(compareBy({ it.chapter }, { it.verse })).take(maxNew)
        return DailyPlan(due.map { it.id }, newCards.map { it.id })
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
