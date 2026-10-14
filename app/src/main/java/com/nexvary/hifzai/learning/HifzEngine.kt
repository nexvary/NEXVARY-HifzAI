package com.nexvary.hifzai.learning

import kotlin.math.roundToInt

/** A review record is independent of UI and canonical Quranic text. */
data class ReviewCard(
    val verseId: Int,
    val repetitions: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val dueEpochDay: Long = 0,
    val lapses: Int = 0
)

enum class RecallGrade { AGAIN, HARD, GOOD, EASY }

object HifzScheduler {
    /** A deterministic SM-2-inspired policy, not a validated learning outcome. */
    fun grade(card: ReviewCard, result: RecallGrade, todayEpochDay: Long): ReviewCard {
        if (result == RecallGrade.AGAIN) {
            return card.copy(
                repetitions = 0,
                ease = (card.ease - 0.2).coerceIn(1.3, 3.0),
                intervalDays = 0,
                dueEpochDay = todayEpochDay,
                lapses = card.lapses + 1
            )
        }
        val repetitions = card.repetitions + 1
        val baseInterval = when (repetitions) {
            1 -> 1
            2 -> 3
            else -> (card.intervalDays.coerceAtLeast(1) * card.ease).roundToInt().coerceAtLeast(4)
        }
        val interval = when (result) {
            RecallGrade.HARD -> (baseInterval * 0.75).roundToInt().coerceAtLeast(1)
            RecallGrade.EASY -> (baseInterval * 1.3).roundToInt().coerceAtLeast(1)
            else -> baseInterval
        }
        val easeAdjustment = when (result) {
            RecallGrade.HARD -> -0.15
            RecallGrade.GOOD -> 0.0
            RecallGrade.EASY -> 0.1
            RecallGrade.AGAIN -> 0.0
        }
        return card.copy(
            repetitions = repetitions,
            ease = (card.ease + easeAdjustment).coerceIn(1.3, 3.0),
            intervalDays = interval,
            dueEpochDay = todayEpochDay + interval
        )
    }
}

/** Visibility mask: the original text is NEVER mutated or persisted in masked form. */
object VerseMask {
    fun display(canonicalText: String, maskedFraction: Float, seed: Int = 0): String {
        val tokens = canonicalText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return ""
        val fraction = maskedFraction.coerceIn(0f, 1f)
        if (fraction <= 0f) return canonicalText
        if (fraction >= 1f) return tokens.joinToString(" ") { "●●●" }
        val hiddenCount = (tokens.size * fraction).roundToInt().coerceIn(1, tokens.size)
        val ordering = tokens.indices.sortedBy { (it * 37L + seed * 19L).floorMod(101L) }
        val hidden = ordering.take(hiddenCount).toSet()
        return tokens.mapIndexed { index, value -> if (index in hidden) "●●●" else value }
            .joinToString(" ")
    }

    private fun Long.floorMod(mod: Long): Long = ((this % mod) + mod) % mod
}
