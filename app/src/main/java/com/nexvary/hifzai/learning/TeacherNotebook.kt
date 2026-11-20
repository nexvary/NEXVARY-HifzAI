package com.nexvary.hifzai.learning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Personal offline organisation, not a verified academy or remote identity service. */
data class NotebookStudent(val id: String, val alias: String, val guardianConsent: Boolean)
data class NotebookLesson(
    val id: String, val studentId: String, val teacherLabel: String,
    val dateTime: String, val note: String, val completed: Boolean = false
)
data class NotebookComplaint(val lessonId: String, val note: String)

object NotebookRules {
    fun displayDateTime(value: String): String = runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("yyyy/MM/dd • HH:mm"))
    }.getOrDefault("تعذر قراءة الموعد")
    fun validateStudent(alias: String, guardianConsent: Boolean) {
        require(alias.trim().length in 1..40) { "استخدم اسمًا مستعارًا من 1 إلى 40 حرفًا" }
        require(guardianConsent) { "يلزم إقرار إشراف ولي الأمر أو كون الطالب بالغًا" }
    }
    fun validateLesson(student: NotebookStudent, teacher: String, dateTime: String, note: String) {
        require(student.guardianConsent)
        require(teacher.trim().length in 1..60) { "اكتب اسم المعلم الذي تعرفه؛ لا تحقق آليًا من مؤهلاته" }
        LocalDateTime.parse(dateTime)
        require(note.length <= 500)
    }
}

class TeacherNotebook(context: Context) {
    private val prefs = context.getSharedPreferences("teacher_notebook_v1", Context.MODE_PRIVATE)
    fun students(): List<NotebookStudent> = runCatching {
        val data = JSONArray(prefs.getString("students", "[]"))
        (0 until data.length()).map { i -> data.getJSONObject(i).let {
            NotebookStudent(it.getString("id"), it.getString("alias"), it.getBoolean("consent"))
        } }
    }.getOrDefault(emptyList())
    fun lessons(): List<NotebookLesson> = runCatching {
        val data = JSONArray(prefs.getString("lessons", "[]"))
        (0 until data.length()).map { i -> data.getJSONObject(i).let {
            NotebookLesson(it.getString("id"), it.getString("student"), it.getString("teacher"),
                it.getString("time"), it.getString("note"), it.optBoolean("completed"))
        } }.sortedBy { it.dateTime }
    }.getOrDefault(emptyList())
    fun addStudent(alias: String, consent: Boolean) {
        NotebookRules.validateStudent(alias, consent)
        val students = students()
        require(students.size < 50) { "الحد المحلي 50 طالبًا" }
        val data = JSONArray()
        (students + NotebookStudent(UUID.randomUUID().toString(), alias.trim(), consent)).forEach {
            data.put(JSONObject().put("id", it.id).put("alias", it.alias).put("consent", it.guardianConsent))
        }
        check(prefs.edit().putString("students", data.toString()).commit())
    }
    fun addLesson(studentId: String, teacher: String, dateTime: String, note: String) {
        val student = students().first { it.id == studentId }
        NotebookRules.validateLesson(student, teacher, dateTime, note)
        val existing = lessons()
        require(existing.size < 500) { "الحد المحلي 500 جلسة" }
        saveLessons(existing + NotebookLesson(UUID.randomUUID().toString(), studentId, teacher.trim(), dateTime, note))
    }
    fun updateLesson(id: String, note: String, completed: Boolean) {
        require(note.length <= 500)
        saveLessons(lessons().map { if (it.id == id) it.copy(note = note, completed = completed) else it })
    }
    private fun saveLessons(lessons: List<NotebookLesson>) {
        val data = JSONArray()
        lessons.forEach {
            data.put(JSONObject().put("id", it.id).put("student", it.studentId).put("teacher", it.teacherLabel)
                .put("time", it.dateTime).put("note", it.note).put("completed", it.completed))
        }
        check(prefs.edit().putString("lessons", data.toString()).commit())
    }
    fun removeStudent(id: String) {
        val existingStudents = students()
        require(existingStudents.any { it.id == id }) { "ملف الطالب غير موجود" }
        val existingLessons = lessons()
        val removedLessonIds = existingLessons.filter { it.studentId == id }.map { it.id }.toSet()
        val studentData = JSONArray()
        existingStudents.filter { it.id != id }.forEach {
            studentData.put(JSONObject().put("id", it.id).put("alias", it.alias).put("consent", it.guardianConsent))
        }
        val lessonData = JSONArray()
        existingLessons.filter { it.studentId != id }.forEach {
            lessonData.put(JSONObject().put("id", it.id).put("student", it.studentId).put("teacher", it.teacherLabel)
                .put("time", it.dateTime).put("note", it.note).put("completed", it.completed))
        }
        val complaintData = JSONArray()
        complaints().filter { it.lessonId !in removedLessonIds }.forEach {
            complaintData.put(JSONObject().put("lesson", it.lessonId).put("note", it.note))
        }
        // One editor commits all related changes atomically; unrelated students retain their records.
        check(prefs.edit().putString("students", studentData.toString())
            .putString("lessons", lessonData.toString()).putString("complaints", complaintData.toString()).commit())
    }
    fun complaints(): List<NotebookComplaint> = runCatching {
        val data = JSONArray(prefs.getString("complaints", "[]"))
        (0 until data.length()).map { i -> data.getJSONObject(i).let {
            NotebookComplaint(it.getString("lesson"), it.getString("note"))
        } }
    }.getOrDefault(emptyList())
    fun recordComplaint(lessonId: String, note: String) {
        require(lessons().any { it.id == lessonId })
        require(note.trim().length in 1..500)
        val data = JSONArray()
        (complaints().takeLast(99) + NotebookComplaint(lessonId, note.trim())).forEach {
            data.put(JSONObject().put("lesson", it.lessonId).put("note", it.note))
        }
        check(prefs.edit().putString("complaints", data.toString()).commit())
    }
    fun clear() = prefs.edit().clear().commit()
}
