package com.nexvary.hifzai.learning

enum class StudyMode(val arabic: String) {
    BALANCED("حفظ ومراجعة"), REVIEW_ONLY("مراجعة فقط"), NEW_ONLY("حفظ جديد فقط")
}

/** A learner-controlled workload, not an efficacy or retention prediction. */
data class LearningGoal(val newVerses: Int = 2, val reviews: Int = 15,
    val mode: StudyMode = StudyMode.BALANCED) {
    init { require(newVerses in 0..20 && reviews in 1..100) }
    fun plan(verses: List<VerifiedVerse>, cards: Map<Int, ReviewCard>, today: Long,
        work: DailyWork = DailyWork(today)): DailyPlan {
        require(work.epochDay == today)
        val completed = work.allVerseIds
        return DailyMemorizationPlan.create(verses.filter { it.id !in completed }, cards, today,
            maxDue = if (mode == StudyMode.NEW_ONLY) 0 else (reviews - work.reviewVerseIds.size).coerceAtLeast(0),
            maxNew = if (mode == StudyMode.REVIEW_ONLY) 0 else (newVerses - work.newVerseIds.size).coerceAtLeast(0))
    }
    /** Reviews come from the whole corpus; new study stays in the learner's chosen chapter. */
    fun planFor(corpus: QuranCorpus, cards: Map<Int, ReviewCard>, today: Long, newChapter: Int,
        work: DailyWork = DailyWork(today)): DailyPlan {
        require(newChapter in 1..114 && work.epochDay == today)
        val covered = work.allVerseIds
        val due = DailyMemorizationPlan.create(corpus.verses.filter { it.id !in covered }, cards, today,
            maxDue = if (mode == StudyMode.NEW_ONLY) 0 else (reviews - work.reviewVerseIds.size).coerceAtLeast(0),
            maxNew = 0)
        val new = DailyMemorizationPlan.create(corpus.chapter(newChapter).filter { it.id !in covered }, cards, today,
            maxDue = 0,
            maxNew = if (mode == StudyMode.REVIEW_ONLY) 0 else (newVerses - work.newVerseIds.size).coerceAtLeast(0))
        return DailyPlan(due.dueVerseIds, new.newVerseIds)
    }
}

/** Explicit planning assumption, not a measured reading speed or learning prediction. */
object StudyTimeBudget {
    val choices = listOf(5, 10, 15, 20, 30, 45, 60)

    /** Reserve roughly two thirds for review in balanced mode; never change the mode. */
    fun suggest(minutes: Int, mode: StudyMode): LearningGoal {
        require(minutes in choices)
        val newVerses = when (mode) {
            StudyMode.REVIEW_ONLY -> 0
            StudyMode.NEW_ONLY -> (minutes / 3).coerceAtMost(20)
            StudyMode.BALANCED -> (minutes / 9).coerceAtLeast(1).coerceAtMost(20)
        }
        val reviewMinutes = if (mode == StudyMode.REVIEW_ONLY) minutes else minutes - newVerses * 3
        return LearningGoal(newVerses, reviewMinutes.coerceIn(1, 100), mode)
    }
}

data class LearningPeriodSummary(val attempts: Int, val independent: Int, val assisted: Int,
    val forgotten: Int, val activeDays: Int) {
    val independentPercent: Int? get() = if (attempts == 0) null else (100L * independent / attempts).toInt()
}

object LearningTrends {
    fun week(observations: List<RecallObservation>, endDay: Long): LearningPeriodSummary =
        summarize(observations, endDay - 6, endDay)

    fun last30Days(observations: List<RecallObservation>, endDay: Long): LearningPeriodSummary =
        summarize(observations, endDay - 29, endDay)

    /** Only retained observations are counted; this is not a complete lifetime cohort. */
    private fun summarize(observations: List<RecallObservation>, startDay: Long,
        endDay: Long): LearningPeriodSummary {
        val events = observations.filter { it.epochDay in startDay..endDay }
        return LearningPeriodSummary(events.size, events.count {
            it.hints == 0 && !it.exposed && it.grade in setOf(RecallGrade.GOOD, RecallGrade.EASY)
        }, events.count { it.hints > 0 || it.exposed }, events.count { it.grade == RecallGrade.AGAIN },
            events.map { it.epochDay }.distinct().size)
    }
    fun byMode(observations: List<RecallObservation>, endDay: Long): Map<PracticeKind, LearningPeriodSummary> =
        observations.filter { it.kind != null }.groupBy { it.kind!! }.mapValues { week(it.value, endDay) }

    /** More recent observations guide a drill without changing the review schedule. */
    fun recentRecommendation(history: List<RecallObservation>, today: Long): TrainingRecommendation? {
        val recent = history.filter { it.epochDay in (today - 13)..today }.takeLast(10)
        if (recent.size < 3) return null
        fun weight(o: RecallObservation) = 1.0 / (1 + today - o.epochDay).toDouble()
        val total = recent.sumOf(::weight)
        val struggling = recent.filter { it.grade == RecallGrade.AGAIN || it.grade == RecallGrade.HARD }
        if (struggling.sumOf(::weight) / total < 0.5) return null
        val transition = struggling.filter { it.difficulty == DifficultyTag.TRANSITION || it.kind == PracticeKind.BRIDGE }
        return if (transition.sumOf(::weight) / total >= 0.5)
            TrainingRecommendation(PracticeKind.BRIDGE, "تكررت صعوبة الانتقال في آخر محاولاتك؛ جرّب الربط. اقتراح من تقييمك الذاتي", 1f)
        else TrainingRecommendation(PracticeKind.MASK_LIGHT, "آخر محاولاتك تحتاج تثبيتًا؛ جرّب الإخفاء الخفيف. يمكنك اختيار تدريب آخر", 0.35f)
    }
}
