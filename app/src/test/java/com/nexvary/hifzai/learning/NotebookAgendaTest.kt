package com.nexvary.hifzai.learning

import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NotebookAgendaTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val students = listOf(NotebookStudent("a", "اختبار أ", true), NotebookStudent("b", "اختبار ب", true))
    private val corpus by lazy { QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText()) }
    private fun lesson(id: String, day: LocalDate = today, time: String = "10:00", student: String = "a",
        completed: Boolean = false) = NotebookLesson(id, student, "اختبار", "${day}T$time", "ملاحظة", completed)
    private fun task(id: String, day: LocalDate = today, student: String = "a", completed: Boolean = false) =
        NotebookAssignment(id, student, 1, 1, 7, day.toString(), completed)
    private fun snapshot(lessons: List<NotebookLesson>, tasks: List<NotebookAssignment> = emptyList()) =
        NotebookAgenda.snapshot(students, lessons, tasks, corpus)
    private fun ids(source: AgendaSnapshot, filter: AgendaFilter, day: LocalDate = today, student: String? = null) =
        NotebookAgenda.query(source, day, filter, student).map { it.id }

    @Test fun dayBucketsAreExclusiveAndCompletionTakesPriorityOverDates() {
        val data = snapshot(listOf(lesson("early", time = "00:00"), lesson("late", time = "23:59"),
            lesson("old", today.minusDays(1)), lesson("next", today.plusDays(1)),
            lesson("complete", today.minusDays(2), completed = true)), listOf(task("task")))
        assertEquals(listOf("early", "late", "task"), ids(data, AgendaFilter.TODAY))
        assertEquals(listOf("old"), ids(data, AgendaFilter.OVERDUE))
        assertEquals(listOf("next"), ids(data, AgendaFilter.UPCOMING))
        assertEquals(listOf("complete"), ids(data, AgendaFilter.COMPLETED))
        assertEquals(6, ids(data, AgendaFilter.ALL).size)
        val buckets = AgendaFilter.entries.filter { it != AgendaFilter.ALL }.flatMap { ids(data, it) }
        assertEquals(6, buckets.distinct().size); assertEquals(6, buckets.size)
    }
    @Test fun timedLessonsPrecedeDateOnlyTasksAndCompletedRecordsUseReverseScheduleOrder() {
        val data = snapshot(listOf(lesson("later", time = "19:00"), lesson("earlier", time = "08:00"),
            lesson("old-done", today.minusDays(1), completed = true), lesson("new-done", completed = true)),
            listOf(task("task")))
        assertEquals(listOf("earlier", "later", "task"), ids(data, AgendaFilter.TODAY))
        assertEquals(listOf("new-done", "old-done"), ids(data, AgendaFilter.COMPLETED))
    }
    @Test fun studentSelectionDoesNotMixIdentitiesOrChangeSourceCompletion() {
        val lessons = listOf(lesson("a"), lesson("b", student = "b"))
        val tasks = listOf(task("a-task"), task("b-task", student = "b", completed = true))
        val data = snapshot(lessons, tasks)
        assertEquals(listOf("a", "a-task"), ids(data, AgendaFilter.TODAY, student = "a"))
        assertEquals(listOf("b-task"), ids(data, AgendaFilter.COMPLETED, student = "b"))
        assertTrue(ids(data, AgendaFilter.ALL, student = "missing").isEmpty())
        assertFalse(tasks.first().completed); assertTrue(tasks.last().completed)
    }
    @Test fun malformedOrOrphanedOrDuplicateRecordsAreCountedWithoutInventingDatesOrDeletingInput() {
        val lessons = listOf(lesson("ok"), lesson("dup"), lesson("dup"), lesson("missing", student = "missing"),
            lesson("bad").copy(dateTime = "broken"), lesson("consent", student = "c"))
        val tasks = listOf(task("valid", student = "b"), task("bad-date").copy(dueDate = "2026-02-30"),
            task("bad-ayah").copy(lastAyah = 8), task("orphan", student = "missing"))
        val data = NotebookAgenda.snapshot(students + NotebookStudent("c", "اختبار", false), lessons, tasks, corpus)
        assertEquals(listOf("ok", "valid"), ids(data, AgendaFilter.ALL))
        assertEquals(8, data.unreadable)
        assertEquals(6, lessons.size); assertEquals(4, tasks.size)
    }
    @Test fun rollingIntoANewDayMovesYesterdayAndTomorrowWithoutMinuteBasedOverdueClaims() {
        val data = snapshot(listOf(lesson("today", time = "00:01"), lesson("tomorrow", today.plusDays(1))))
        assertTrue(ids(data, AgendaFilter.OVERDUE).isEmpty())
        assertEquals(listOf("today"), ids(data, AgendaFilter.OVERDUE, today.plusDays(1)))
        assertEquals(listOf("tomorrow"), ids(data, AgendaFilter.TODAY, today.plusDays(1)))
        val leap = LocalDate.of(2024, 2, 29)
        assertEquals(listOf("leap"), ids(snapshot(listOf(lesson("leap", leap))), AgendaFilter.OVERDUE, leap.plusDays(1)))
    }
    @Test fun pagingIsBoundedClampsAfterDeletionAndHandlesEmptyResults() {
        val items = NotebookAgenda.query(snapshot((1..7).map { lesson("$it", time = "0$it:00") }), today, AgendaFilter.TODAY)
        val first = NotebookAgenda.page(items, 0)
        assertEquals(5, first.entries.size); assertEquals(2, first.pageCount)
        assertFalse(first.hasPrevious); assertTrue(first.hasNext)
        val last = NotebookAgenda.page(items, 99)
        assertEquals(listOf("6", "7"), last.entries.map { it.id }); assertEquals(1, last.index)
        assertFalse(last.hasNext); assertTrue(last.hasPrevious)
        assertEquals(0, NotebookAgenda.page(items.take(5), last.index).index)
        val empty = NotebookAgenda.page(emptyList(), 9)
        assertEquals(0, empty.pageCount); assertFalse(empty.hasNext); assertFalse(empty.hasPrevious)
        for ((page, size) in listOf(-1 to 5, 0 to 0, 0 to 21))
            assertTrue(runCatching { NotebookAgenda.page(items, page, size) }.isFailure)
    }
    @Test fun snapshotAndPageListsAreFrozenCopies() {
        val lessons = mutableListOf(lesson("first"))
        val data = snapshot(lessons)
        lessons += lesson("later")
        assertEquals(1, data.entries.size)
        assertTrue(runCatching { (data.entries as MutableList<AgendaEntry>).clear() }.isFailure)
        val page = NotebookAgenda.page(data.entries, 0)
        assertTrue(runCatching { (page.entries as MutableList<AgendaEntry>).clear() }.isFailure)
    }
}
