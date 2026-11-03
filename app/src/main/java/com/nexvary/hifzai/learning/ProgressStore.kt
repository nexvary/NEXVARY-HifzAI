package com.nexvary.hifzai.learning

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Private local learning data; audio is never stored in these preferences. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("hifz_progress_v1", Context.MODE_PRIVATE)

    fun card(id: Int): ReviewCard = ReviewCard(
        verseId = id,
        repetitions = intValue("repetitions_" + id, 0),
        intervalDays = intValue("interval_" + id, 0),
        dueEpochDay = longValue("due_" + id, 0L),
        lapses = intValue("lapses_" + id, 0),
        ease = java.lang.Double.longBitsToDouble(
            longValue("ease_" + id, java.lang.Double.doubleToRawLongBits(2.5))
        ).let { if (it.isFinite()) it.coerceIn(1.3, 3.0) else 2.5 }
    )

    fun memory(id: Int): MemoryFingerprint = MemoryFingerprint(
        verseId = id,
        attempts = intValue("attempts_" + id, 0),
        recalled = intValue("recalled_" + id, 0),
        hinted = intValue("hinted_" + id, 0),
        transitionsMissed = intValue("transitions_" + id, 0),
        wordMisses = intValue("wordMisses_" + id, 0),
        similarityMisses = intValue("similarityMisses_" + id, 0)
    )

    fun save(card: ReviewCard, memory: MemoryFingerprint) {
        val editor = prefs.edit()
        writeCard(editor, card, memory)
        editor.apply()
    }

    private fun writeCard(editor: SharedPreferences.Editor, card: ReviewCard, memory: MemoryFingerprint) {
        require(card.verseId == memory.verseId)
        val id = card.verseId
        editor
            .putInt("repetitions_" + id, card.repetitions)
            .putInt("interval_" + id, card.intervalDays)
            .putLong("due_" + id, card.dueEpochDay)
            .putInt("lapses_" + id, card.lapses)
            .putLong("ease_" + id, java.lang.Double.doubleToRawLongBits(card.ease))
            .putInt("attempts_" + id, memory.attempts)
            .putInt("recalled_" + id, memory.recalled)
            .putInt("hinted_" + id, memory.hinted)
            .putInt("transitions_" + id, memory.transitionsMissed)
            .putInt("wordMisses_" + id, memory.wordMisses)
            .putInt("similarityMisses_" + id, memory.similarityMisses)
    }

    private fun intValue(key: String, default: Int): Int =
        runCatching { prefs.getInt(key, default).coerceAtLeast(0) }.getOrDefault(default)
    private fun longValue(key: String, default: Long): Long =
        runCatching { prefs.getLong(key, default) }.getOrDefault(default)

    var appearance: AppAppearance
        get() = AppAppearance.fromStored(runCatching { prefs.getString("appearance", null) }.getOrNull())
        set(value) { prefs.edit().putString("appearance", value.name).apply() }

    var fontSize: Int
        get() = intValue("font_size", 30).coerceIn(24, 44)
        set(value) { prefs.edit().putInt("font_size", value.coerceIn(24, 44)).apply() }
    var advanced: Boolean
        get() = runCatching { prefs.getBoolean("advanced", false) }.getOrDefault(false)
        set(value) { prefs.edit().putBoolean("advanced", value).apply() }

    var learningGoal: LearningGoal
        get() = runCatching {
            LearningGoal(intValue("goal_new", 2).coerceIn(0, 20), intValue("goal_reviews", 15).coerceIn(1, 100),
                StudyMode.valueOf(prefs.getString("goal_mode", StudyMode.BALANCED.name)!!))
        }.getOrDefault(LearningGoal())
        set(value) { prefs.edit().putInt("goal_new", value.newVerses).putInt("goal_reviews", value.reviews)
            .putString("goal_mode", value.mode.name).apply() }

    var favoriteReciter: String?
        get() = runCatching { prefs.getString("favorite_reciter", null) }.getOrNull()
            ?.takeIf { slug -> ReciterCatalog.all.any { it.slug == slug } }
        set(value) {
            require(value == null || ReciterCatalog.all.any { it.slug == value })
            prefs.edit().putString("favorite_reciter", value).apply()
        }

    val gentleReturnEnabled: Boolean
        get() = runCatching { prefs.getBoolean("gentle_return_enabled", false) }.getOrDefault(false)
    val gentleReturnAnchorDay: Long?
        get() = if (!gentleReturnEnabled) null else runCatching {
            prefs.getLong("gentle_return_anchor", -1).takeIf { it >= 0 }
        }.getOrNull()

    fun setGentleReturn(enabled: Boolean, today: Long = LocalDate.now().toEpochDay()) {
        require(today >= 0)
        val editor = prefs.edit().putBoolean("gentle_return_enabled", enabled)
        if (enabled) editor.putLong("gentle_return_anchor", today)
        else editor.remove("gentle_return_anchor")
        editor.apply()
    }
    fun postponeGentleReturn(today: Long = LocalDate.now().toEpochDay()) {
        require(today >= 0)
        if (gentleReturnEnabled) prefs.edit().putLong("gentle_return_anchor", today).apply()
    }

    fun bookmark(chapter: Int, ayah: Int) {
        require(chapter in 1..114 && ayah in 1..286)
        prefs.edit().putInt("bookmark_chapter", chapter).putInt("bookmark_ayah", ayah).apply()
    }
    fun bookmark(): Pair<Int, Int>? {
        val c = intValue("bookmark_chapter", 0)
        val a = intValue("bookmark_ayah", 0)
        return if (c in 1..114 && a in 1..286) c to a else null
    }

    /** Stored addresses are resolved against the immutable corpus; no Quran text is persisted. */
    fun savedPlaces(corpus: QuranCorpus): List<VerifiedVerse> = runCatching {
        val raw = prefs.getString("saved_places_v1", "[]") ?: "[]"
        require(raw.length <= 8192)
        val list = JSONArray(raw)
        require(list.length() <= 50)
        (0 until list.length()).mapNotNull { index -> runCatching {
            val place = list.getJSONObject(index)
            val chapter = place.get("chapter")
            val ayah = place.get("ayah")
            if (chapter is Int && ayah is Int) corpus.find(chapter, ayah) else null
        }.getOrNull() }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    /** Duplicate saves succeed without consuming quota. The existing reading stop is untouched. */
    fun addSavedPlace(chapter: Int, ayah: Int, corpus: QuranCorpus): Boolean {
        val place = requireNotNull(corpus.find(chapter, ayah)) { "مرجع الآية غير موجود" }
        val existing = savedPlaces(corpus)
        if (existing.any { it.id == place.id }) return true
        if (existing.size >= 50) return false
        writeSavedPlaces(existing + place)
        return true
    }

    fun removeSavedPlace(chapter: Int, ayah: Int, corpus: QuranCorpus) {
        requireNotNull(corpus.find(chapter, ayah)) { "مرجع الآية غير موجود" }
        writeSavedPlaces(savedPlaces(corpus).filterNot { it.chapter == chapter && it.verse == ayah })
    }

    private fun writeSavedPlaces(places: List<VerifiedVerse>) {
        val array = JSONArray()
        places.forEach { array.put(JSONObject().put("chapter", it.chapter).put("ayah", it.verse)) }
        prefs.edit().putString("saved_places_v1", array.toString()).apply()
    }

    fun saveSession(session: PracticeSession) {
        val json = JSONObject().put("chapter", session.chapter).put("ayah", session.ayah)
            .put("stage", session.stage.name).put("hints", session.hints)
            .put("exposed", session.exposed).put("difficulty", session.difficulty.name)
            .put("rangeStart", session.rangeStart).put("rangeEnd", session.rangeEnd)
            .put("followsDailyPlan", session.followsDailyPlan)
            .put("planChapter", session.planChapter)
        prefs.edit().putString("session", json.toString()).apply()
    }
    fun session(): PracticeSession? = runCatching {
        val j = JSONObject(prefs.getString("session", null) ?: return null)
        val followsPlan = j.optBoolean("followsDailyPlan", false)
        val planChapter = if (j.has("planChapter")) j.getInt("planChapter")
            else if (followsPlan) j.getInt("chapter") else 0
        PracticeSession(j.getInt("chapter"), j.getInt("ayah"),
            PracticeKind.valueOf(j.getString("stage")), j.getInt("hints"),
            j.getBoolean("exposed"), DifficultyTag.valueOf(j.getString("difficulty")),
            j.optInt("rangeStart", 0), j.optInt("rangeEnd", 0), followsPlan,
            planChapter)
    }.getOrNull()
    fun clearSession() = prefs.edit().remove("session").apply()

    /** Only the current stored day is retained. Legacy histories do not invent first-study dates. */
    fun workload(today: Long): DailyWork = runCatching {
        val raw = prefs.getString("daily_work", null) ?: return DailyWork(today)
        require(raw.length <= 128_000)
        val data = JSONObject(raw)
        if (data.getLong("day") != today) return DailyWork(today)
        fun ids(key: String): Set<Int> {
            val array = data.getJSONArray(key)
            require(array.length() <= 6236)
            return (0 until array.length()).map { array.getInt(it) }.toSet()
        }
        DailyWork(today, ids("new"), ids("reviews"))
    }.getOrElse { DailyWork(today) }

    private fun workloadJson(work: DailyWork): String = JSONObject().put("day", work.epochDay)
        .put("new", JSONArray(work.newVerseIds.sorted())).put("reviews", JSONArray(work.reviewVerseIds.sorted())).toString()

    fun history(id: Int): List<RecallObservation> = runCatching {
        val a = JSONArray(prefs.getString("history_$id", "[]"))
        (0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            RecallObservation(j.getLong("day"), RecallGrade.valueOf(j.getString("grade")),
                j.getInt("hints"), j.getBoolean("exposed"),
                DifficultyTag.valueOf(j.getString("difficulty")),
                j.optString("kind").takeIf { it.isNotEmpty() }?.let { PracticeKind.valueOf(it) })
        }
    }.getOrDefault(emptyList())
    fun observe(id: Int, observation: RecallObservation, firstAttempt: Boolean? = null) {
        val editor = prefs.edit()
        writeObservation(editor, id, observation, firstAttempt)
        editor.apply()
    }

    /** One preferences transaction for progress, retained history, day counts and consented anchor. */
    fun recordAssessment(card: ReviewCard, memory: MemoryFingerprint, observation: RecallObservation,
        firstAttempt: Boolean) {
        val editor = prefs.edit()
        writeCard(editor, card, memory)
        writeObservation(editor, card.verseId, observation, firstAttempt)
        editor.apply()
    }

    private fun writeObservation(editor: SharedPreferences.Editor, id: Int,
        observation: RecallObservation, firstAttempt: Boolean?) {
        val a = JSONArray()
        (history(id).takeLast(29) + observation).forEach { o ->
            a.put(JSONObject().put("day", o.epochDay).put("grade", o.grade.name)
                .put("hints", o.hints).put("exposed", o.exposed)
                .put("difficulty", o.difficulty.name).put("kind", o.kind?.name ?: ""))
        }
        editor.putString("history_$id", a.toString())
        if (firstAttempt != null) {
            val work = workload(observation.epochDay).record(id, firstAttempt)
            editor.putString("daily_work", workloadJson(work))
        }
        if (gentleReturnEnabled && observation.epochDay >= 0) {
            editor.putLong("gentle_return_anchor", maxOf(gentleReturnAnchorDay ?: 0, observation.epochDay))
        }
    }

    fun clear() = prefs.edit().clear().commit()
}
