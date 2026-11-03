package com.nexvary.hifzai.learning

/** A persisted deletion intent must precede every destructive write. */
interface ResetJournal {
    val pending: Boolean
    fun begin(): Boolean
    fun complete(): Boolean
}

/** Recoverable deletion across the two independent local preference files. */
class LocalDataReset(
    private val journal: ResetJournal,
    private val clearProgress: () -> Boolean,
    private val clearNotebook: () -> Boolean,
) {
    enum class Result { READY, NOT_STARTED, PENDING }

    fun request(): Result = if (journal.begin()) recover() else Result.NOT_STARTED

    fun recover(): Result {
        if (!journal.pending) return Result.READY
        // Attempt both stores, even if one fails. Never expose a partial dataset.
        val progress = clearProgress()
        val notebook = clearNotebook()
        return if (progress && notebook && journal.complete()) Result.READY else Result.PENDING
    }
}
