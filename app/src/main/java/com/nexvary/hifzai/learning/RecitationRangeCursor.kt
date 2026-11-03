package com.nexvary.hifzai.learning

/** Bounded range repetition without duplicating recordings or bypassing per-open validation. */
class RecitationRangeCursor(val size: Int, extraRangeRepeats: Int = 0) {
    init { require(size in 1..2000); require(extraRangeRepeats in 0..20) }
    val passes = extraRangeRepeats + 1
    val totalItems = size * passes
    var index = 0
        private set
    var completedItems = 0
        private set
    val pass: Int get() = minOf(completedItems / size + 1, passes)
    val complete: Boolean get() = completedItems == totalItems

    /** Called only after the entire per-ayah repeat cycle finishes. Null means the range is done. */
    fun completeItem(): Int? {
        check(!complete) { "Range already completed" }
        completedItems++
        if (complete) return null
        index = completedItems % size
        return index
    }
}
