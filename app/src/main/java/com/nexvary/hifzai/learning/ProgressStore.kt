package com.nexvary.hifzai.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
        require(card.verseId == memory.verseId)
        val id = card.verseId
        prefs.edit()
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
            .apply()
    }

    private fun intValue(key: String, default: Int): Int =
        runCatching { prefs.getInt(key, default).coerceAtLeast(0) }.getOrDefault(default)
    private fun longValue(key: String, default: Long): Long =
        runCatching { prefs.getLong(key, default) }.getOrDefault(default)

    var fontSize: Int
        get() = intValue("font_size", 30).coerceIn(24, 44)
        set(value) { prefs.edit().putInt("font_size", value.coerceIn(24, 44)).apply() }
    var advanced: Boolean
        get() = runCatching { prefs.getBoolean("advanced", false) }.getOrDefault(false)
        set(value) { prefs.edit().putBoolean("advanced", value).apply() }

    fun bookmark(chapter: Int, ayah: Int) {
        require(chapter in 1..114 && ayah in 1..286)
        prefs.edit().putInt("bookmark_chapter", chapter).putInt("bookmark_ayah", ayah).apply()
    }
    fun bookmark(): Pair<Int, Int>? {
        val c = intValue("bookmark_chapter", 0)
        val a = intValue("bookmark_ayah", 0)
        return if (c in 1..114 && a in 1..286) c to a else null
    }

    fun saveSession(session: PracticeSession) {
        val json = JSONObject().put("chapter", session.chapter).put("ayah", session.ayah)
            .put("stage", session.stage.name).put("hints", session.hints)
            .put("exposed", session.exposed).put("difficulty", session.difficulty.name)
            .put("rangeStart", session.rangeStart).put("rangeEnd", session.rangeEnd)
        prefs.edit().putString("session", json.toString()).apply()
    }
    fun session(): PracticeSession? = runCatching {
        val j = JSONObject(prefs.getString("session", null) ?: return null)
        PracticeSession(j.getInt("chapter"), j.getInt("ayah"),
            PracticeKind.valueOf(j.getString("stage")), j.getInt("hints"),
            j.getBoolean("exposed"), DifficultyTag.valueOf(j.getString("difficulty")),
            j.optInt("rangeStart", 0), j.optInt("rangeEnd", 0))
    }.getOrNull()
    fun clearSession() = prefs.edit().remove("session").apply()

    fun history(id: Int): List<RecallObservation> = runCatching {
        val a = JSONArray(prefs.getString("history_$id", "[]"))
        (0 until a.length()).map { i ->
            val j = a.getJSONObject(i)
            RecallObservation(j.getLong("day"), RecallGrade.valueOf(j.getString("grade")),
                j.getInt("hints"), j.getBoolean("exposed"),
                DifficultyTag.valueOf(j.getString("difficulty")))
        }
    }.getOrDefault(emptyList())
    fun observe(id: Int, observation: RecallObservation) {
        val a = JSONArray()
        (history(id).takeLast(29) + observation).forEach { o ->
            a.put(JSONObject().put("day", o.epochDay).put("grade", o.grade.name)
                .put("hints", o.hints).put("exposed", o.exposed)
                .put("difficulty", o.difficulty.name))
        }
        prefs.edit().putString("history_$id", a.toString()).apply()
    }

    fun clear() = prefs.edit().clear().commit()
}
