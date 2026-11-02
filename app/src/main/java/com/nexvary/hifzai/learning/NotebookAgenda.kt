package com.nexvary.hifzai.learning

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Collections

enum class AgendaFilter(val arabic: String) {
    TODAY("اليوم"), UPCOMING("القادمة"), OVERDUE("السابقة غير المكتملة"),
    COMPLETED("المكتملة"), ALL("الكل")
}

sealed class AgendaEntry {
    abstract val id: String
    abstract val studentId: String
    abstract val order: LocalDateTime
    abstract val completed: Boolean
    val day: LocalDate get() = order.toLocalDate()
    data class Lesson(val record: NotebookLesson, override val order: LocalDateTime) : AgendaEntry() {
        override val id get() = record.id
        override val studentId get() = record.studentId
        override val completed get() = record.completed
    }
    data class Assignment(val record: NotebookAssignment, override val order: LocalDateTime) : AgendaEntry() {
        override val id get() = record.id
        override val studentId get() = record.studentId
        override val completed get() = record.completed
    }
}

data class AgendaSnapshot(val entries: List<AgendaEntry>, val unreadable: Int)
data class AgendaPage(val entries: List<AgendaEntry>, val index: Int, val pageCount: Int, val total: Int) {
    val hasPrevious get() = index > 0
    val hasNext get() = index + 1 < pageCount
}

/** Personal day-based organisation, not a reservation, notification or verified learning result. */
object NotebookAgenda {
    fun snapshot(students: List<NotebookStudent>, lessons: List<NotebookLesson>,
        assignments: List<NotebookAssignment>, corpus: QuranCorpus): AgendaSnapshot {
        val permitted = students.filter { it.guardianConsent }.map { it.id }.toSet()
        var unreadable = 0
        val entries = mutableListOf<AgendaEntry>()
        val lessonIds = lessons.groupingBy { it.id }.eachCount()
        lessons.forEach { record ->
            val item = runCatching {
                require(record.id.isNotBlank() && lessonIds[record.id] == 1 && record.studentId in permitted)
                AgendaEntry.Lesson(record, LocalDateTime.parse(record.dateTime))
            }.getOrNull()
            if (item == null) unreadable++ else entries += item
        }
        val assignmentIds = assignments.groupingBy { it.id }.eachCount()
        assignments.forEach { record ->
            val item = runCatching {
                require(record.id.isNotBlank() && assignmentIds[record.id] == 1 && record.studentId in permitted)
                require(record.firstAyah <= record.lastAyah && corpus.find(record.chapter, record.firstAyah) != null &&
                    corpus.find(record.chapter, record.lastAyah) != null)
                AgendaEntry.Assignment(record, LocalDate.parse(record.dueDate).atTime(LocalTime.MAX))
            }.getOrNull()
            if (item == null) unreadable++ else entries += item
        }
        return AgendaSnapshot(Collections.unmodifiableList(entries.sortedWith(order)), unreadable)
    }

    private val order = compareBy<AgendaEntry>({ it.order }, { if (it is AgendaEntry.Lesson) 0 else 1 }, { it.id })
    fun query(snapshot: AgendaSnapshot, today: LocalDate, filter: AgendaFilter,
        studentId: String? = null): List<AgendaEntry> = snapshot.entries.filter {
        (studentId == null || it.studentId == studentId) && when (filter) {
            AgendaFilter.ALL -> true
            AgendaFilter.COMPLETED -> it.completed
            AgendaFilter.TODAY -> !it.completed && it.day == today
            AgendaFilter.OVERDUE -> !it.completed && it.day < today
            AgendaFilter.UPCOMING -> !it.completed && it.day > today
        }
    }.let { if (filter == AgendaFilter.COMPLETED) it.sortedWith(order.reversed()) else it }

    fun page(entries: List<AgendaEntry>, requested: Int, pageSize: Int = 5): AgendaPage {
        require(requested >= 0 && pageSize in 1..20)
        val count = (entries.size + pageSize - 1) / pageSize
        val index = requested.coerceAtMost((count - 1).coerceAtLeast(0))
        return AgendaPage(Collections.unmodifiableList(entries.drop(index * pageSize).take(pageSize)),
            index, count, entries.size)
    }
}
