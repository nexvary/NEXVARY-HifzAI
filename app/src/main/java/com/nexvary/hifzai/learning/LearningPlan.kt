package com.nexvary.hifzai.learning

enum class StudyMode(val arabic: String) {
    BALANCED("حفظ ومراجعة"), REVIEW_ONLY("مراجعة فقط"), NEW_ONLY("حفظ جديد فقط")
}

/** A learner-controlled workload, not an efficacy or retention prediction. */
data class LearningGoal(val newVerses: Int = 2, val reviews: Int = 15,
    val mode: StudyMode = StudyMode.BALANCED) {
    init { require(newVerses in 0..20 && reviews in 1..100) }
    fun plan(verses: List<VerifiedVerse>, cards: Map<Int, ReviewCard>, today: Long): DailyPlan =
        DailyMemorizationPlan.create(verses, cards, today,
            maxDue = if (mode == StudyMode.NEW_ONLY) 0 else reviews,
            maxNew = if (mode == StudyMode.REVIEW_ONLY) 0 else newVerses)
}

data class WeeklyLearningSummary(val attempts: Int, val independent: Int, val assisted: Int,
    val forgotten: Int, val activeDays: Int) {
    val independentPercent: Int? get() = if (attempts == 0) null else (100L * independent / attempts).toInt()
}

object LearningTrends {
    /** Only retained observations are counted; this is not a complete lifetime cohort. */
    fun week(observations: List<RecallObservation>, endDay: Long): WeeklyLearningSummary {
        val events = observations.filter { it.epochDay in (endDay - 6)..endDay }
        return WeeklyLearningSummary(events.size, events.count {
            it.hints == 0 && !it.exposed && it.grade in setOf(RecallGrade.GOOD, RecallGrade.EASY)
        }, events.count { it.hints > 0 || it.exposed }, events.count { it.grade == RecallGrade.AGAIN },
            events.map { it.epochDay }.distinct().size)
    }
    fun byMode(observations: List<RecallObservation>, endDay: Long): Map<PracticeKind, WeeklyLearningSummary> =
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
