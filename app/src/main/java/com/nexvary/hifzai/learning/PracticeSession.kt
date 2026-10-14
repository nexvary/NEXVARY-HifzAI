package com.nexvary.hifzai.learning

/** No audio or typed recitation is persisted with a session. */
data class PracticeSession(
    val chapter: Int,
    val ayah: Int,
    val stage: PracticeKind,
    val hints: Int,
    val exposed: Boolean,
    val difficulty: DifficultyTag
) {
    init {
        require(chapter in 1..114 && ayah in 1..286)
        require(hints in 0..100)
    }
    /** Assistance belongs to the entire attempt, not a selected screen/stage. */
    fun changeStage(next: PracticeKind) = copy(
        stage = next,
        exposed = exposed || (next == PracticeKind.READ && stage != PracticeKind.READ)
    )
    val effectiveHighScoreAllowed: Boolean get() = hints == 0 && !exposed &&
        (stage == PracticeKind.RECALL || stage == PracticeKind.BRIDGE)
}

data class RecallObservation(
    val epochDay: Long,
    val grade: RecallGrade,
    val hints: Int,
    val exposed: Boolean,
    val difficulty: DifficultyTag
)
