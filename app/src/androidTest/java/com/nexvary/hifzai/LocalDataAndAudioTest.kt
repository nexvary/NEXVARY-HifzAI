package com.nexvary.hifzai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.rule.GrantPermissionRule
import com.nexvary.hifzai.learning.*
import android.Manifest
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDataAndAudioTest {
    @get:Rule val microphone: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @get:Rule val ui = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun corpusOrderAddressBasmalaAndSearchOnDevice() {
        val corpus = QuranCorpus.load(context)
        assertEquals(6236, corpus.verses.size)
        assertEquals(114, corpus.surahs.size)
        assertNull(corpus.openingBasmala(1))
        assertNull(corpus.openingBasmala(9))
        assertEquals(corpus.find(1, 1)!!.text, corpus.openingBasmala(2))
        assertNull(corpus.find(1, 8))
        assertEquals(corpus.find(2, 255), corpus.searchIndex.search(corpus.find(2, 255)!!.text).single())
    }

    @Test fun progressSessionHistoryAndCorruptedPreferences() {
        val store = ProgressStore(context)
        store.clear()
        val session = PracticeSession(2, 255, PracticeKind.RECALL, 1, true, DifficultyTag.WORDS, 2001, 2286)
        store.saveSession(session)
        assertEquals(session, ProgressStore(context).session())
        store.bookmark(2, 255)
        assertEquals(2 to 255, store.bookmark())
        repeat(40) { store.observe(2255, RecallObservation(it.toLong(), RecallGrade.HARD, 1, true, DifficultyTag.WORDS)) }
        assertEquals(30, store.history(2255).size)
        val prefs = context.getSharedPreferences("hifz_progress_v1", 0)
        prefs.edit().putString("repetitions_2255", "broken").putString("session", "broken")
            .putLong("ease_2255", java.lang.Double.doubleToRawLongBits(Double.NaN)).commit()
        assertEquals(0, store.card(2255).repetitions)
        assertEquals(2.5, store.card(2255).ease, 0.0)
        assertNull(store.session())
        assertTrue(store.clear())
        assertNull(store.bookmark())
        assertTrue(store.history(2255).isEmpty())
    }

    @Test fun goalsFavoriteAndLegacyHistorySurviveReloadWithoutInventingModes() {
        val store = ProgressStore(context)
        store.clear()
        try {
            store.learningGoal = LearningGoal(5, 20, StudyMode.REVIEW_ONLY)
            store.favoriteReciter = ReciterCatalog.all.first().slug
            store.observe(1001, RecallObservation(10, RecallGrade.GOOD, 0, false, DifficultyTag.NONE))
            store.observe(1001, RecallObservation(11, RecallGrade.HARD, 1, false, DifficultyTag.TRANSITION, PracticeKind.BRIDGE))
            val reloaded = ProgressStore(context)
            assertEquals(LearningGoal(5, 20, StudyMode.REVIEW_ONLY), reloaded.learningGoal)
            assertEquals(ReciterCatalog.all.first().slug, reloaded.favoriteReciter)
            assertNull(reloaded.history(1001).first().kind)
            assertEquals(PracticeKind.BRIDGE, reloaded.history(1001).last().kind)
            context.getSharedPreferences("hifz_progress_v1", 0).edit()
                .putString("goal_mode", "corrupted").putString("favorite_reciter", "unknown").commit()
            assertEquals(LearningGoal(), reloaded.learningGoal)
            assertNull(reloaded.favoriteReciter)
        } finally { store.clear() }
    }

    @Test fun returnSuggestionRequiresConsentAndDisablingOrDeletionErasesItsDate() {
        val store = ProgressStore(context)
        store.clear()
        try {
            val event = RecallObservation(12, RecallGrade.GOOD, 0, false, DifficultyTag.NONE)
            store.observe(1001, event)
            assertFalse(store.gentleReturnEnabled)
            assertNull(store.gentleReturnAnchorDay)
            store.setGentleReturn(true, 10)
            assertTrue(ProgressStore(context).gentleReturnEnabled)
            store.observe(1001, event)
            assertEquals(12L, store.gentleReturnAnchorDay)
            assertTrue(GentleReturnPolicy.shouldSuggest(true, store.gentleReturnAnchorDay, 19))
            store.postponeGentleReturn(19)
            assertFalse(GentleReturnPolicy.shouldSuggest(true, store.gentleReturnAnchorDay, 19))
            store.observe(1001, event) // An older event cannot undo postponement.
            assertEquals(19L, store.gentleReturnAnchorDay)
            store.setGentleReturn(false, 19)
            assertNull(ProgressStore(context).gentleReturnAnchorDay)
            assertFalse(context.getSharedPreferences("hifz_progress_v1", 0).contains("gentle_return_anchor"))
            store.setGentleReturn(true, 19)
            assertTrue(store.clear())
            assertFalse(store.gentleReturnEnabled)
            assertNull(store.gentleReturnAnchorDay)
        } finally { store.clear() }
    }

    @Test fun dailyWorkSurvivesReloadDeduplicatesRetriesAndRollsOverWithoutInventingLegacyHistory() {
        val store = ProgressStore(context)
        store.clear()
        try {
            val event = RecallObservation(10, RecallGrade.AGAIN, 0, false, DifficultyTag.NONE)
            store.observe(1001, event) // Historical callers without classification do not invent counts.
            assertTrue(store.workload(10).allVerseIds.isEmpty())
            val card = HifzScheduler.grade(ReviewCard(1001), RecallGrade.AGAIN, 10)
            val memory = MemoryCoach.record(MemoryFingerprint(1001), RecallGrade.AGAIN, 0, false)
            store.recordAssessment(card, memory, event, firstAttempt = true)
            assertEquals(card, ProgressStore(context).card(1001))
            assertEquals(memory, ProgressStore(context).memory(1001))
            store.observe(1001, event.copy(grade = RecallGrade.GOOD), firstAttempt = false)
            store.observe(1002, event, firstAttempt = false)
            val loaded = ProgressStore(context).workload(10)
            assertEquals(setOf(1001), loaded.newVerseIds)
            assertEquals(setOf(1002), loaded.reviewVerseIds)
            assertTrue(store.workload(11).allVerseIds.isEmpty())
            store.observe(1003, event.copy(epochDay = 11), firstAttempt = false)
            assertEquals(setOf(1003), store.workload(11).reviewVerseIds)
            assertTrue(store.workload(10).allVerseIds.isEmpty())
            context.getSharedPreferences("hifz_progress_v1", 0).edit().putString("daily_work", "broken").commit()
            assertTrue(store.workload(11).allVerseIds.isEmpty())
            assertTrue(store.clear())
            assertTrue(store.workload(11).allVerseIds.isEmpty())
        } finally { store.clear() }
    }

    @Test fun notebookIndividualEditsAndDeletionPreserveUnrelatedRecords() {
        val notebook = TeacherNotebook(context)
        notebook.clear()
        try {
            val corpus = QuranCorpus.load(context)
            notebook.addStudent("ملف أ", true); notebook.addStudent("ملف ب", true)
            val a = notebook.students()[0]; val b = notebook.students()[1]
            notebook.assignRange(a.id, 1, 1, 3, "2026-11-10", corpus)
            notebook.assignRange(a.id, 1, 4, 7, "2026-11-11", corpus)
            notebook.assignRange(b.id, 114, 1, 6, "2026-11-12", corpus)
            val task = notebook.assignments().first()
            val untouched = notebook.assignments().drop(1).toSet()
            notebook.completeAssignment(task.id, true)
            val before = notebook.assignments().toSet()
            for (operation in listOf<() -> Unit>(
                { notebook.updateAssignment(task.id, 1, 1, 8, "2026-11-13", corpus) },
                { notebook.updateAssignment(task.id, 1, 4, 1, "2026-11-13", corpus) },
                { notebook.updateAssignment(task.id, 1, 1, 3, "2026-02-30", corpus) },
                { notebook.updateAssignment("missing", 1, 1, 3, "2026-11-13", corpus) },
                { notebook.removeAssignment("missing") })) {
                assertTrue(runCatching(operation).isFailure)
                assertEquals(before, notebook.assignments().toSet())
            }
            notebook.updateAssignment(task.id, 1, 1, 3, "2026-11-14", corpus)
            assertTrue(notebook.assignments().first { it.id == task.id }.completed)
            notebook.updateAssignment(task.id, 2, 1, 5, "2026-11-15", corpus)
            val changed = TeacherNotebook(context).assignments().first { it.id == task.id }
            assertEquals(a.id, changed.studentId); assertEquals(2, changed.chapter)
            assertFalse(changed.completed)
            notebook.removeAssignment(task.id)
            assertEquals(untouched, TeacherNotebook(context).assignments().toSet())

            notebook.addLesson(a.id, "اختبار أ", "2026-11-10T18:30", "ملاحظة أ")
            notebook.addLesson(a.id, "اختبار أ٢", "2026-11-11T18:30", "ملاحظة أ٢")
            notebook.addLesson(b.id, "اختبار ب", "2026-11-12T18:30", "ملاحظة ب")
            val lesson = notebook.lessons().first()
            val otherLessons = notebook.lessons().drop(1)
            notebook.recordComplaint(lesson.id, "ستحذف")
            notebook.recordComplaint(otherLessons.first().id, "تبقى")
            val students = notebook.students()
            assertTrue(runCatching { notebook.removeLesson("missing") }.isFailure)
            assertTrue(runCatching { notebook.updateLesson("missing", "", true) }.isFailure)
            assertEquals(3, notebook.lessons().size)
            assertEquals(2, notebook.complaints().size)
            notebook.removeLesson(lesson.id)
            val loaded = TeacherNotebook(context)
            assertEquals(otherLessons, loaded.lessons())
            assertEquals(listOf(NotebookComplaint(otherLessons.first().id, "تبقى")), loaded.complaints())
            assertEquals(students, loaded.students())
            assertEquals(untouched, loaded.assignments().toSet())
        } finally { notebook.clear() }
    }

    @Test fun notebookAssignmentBoundsReschedulingAndCascadingDeletion() {
        val notebook = TeacherNotebook(context)
        notebook.clear()
        try {
            val corpus = QuranCorpus.load(context)
            notebook.addStudent("اختبار محلي أ", true)
            notebook.addStudent("اختبار محلي ب", true)
            val first = notebook.students()[0]; val second = notebook.students()[1]
            assertTrue(runCatching { notebook.assignRange(first.id, 1, 1, 8, "2026-11-10", corpus) }.isFailure)
            notebook.assignRange(first.id, 1, 1, 7, "2026-11-10", corpus)
            notebook.assignRange(second.id, 114, 1, 6, "2026-11-11", corpus)
            val task = notebook.assignments().first()
            notebook.completeAssignment(task.id, true)
            assertTrue(TeacherNotebook(context).assignments().first().completed)
            notebook.addLesson(first.id, "معلم اختبار", "2026-11-10T18:30", "قبل")
            val lesson = notebook.lessons().single()
            notebook.rescheduleLesson(lesson.id, "2026-11-11T19:30", "بعد")
            assertEquals("بعد", notebook.lessons().single().note)
            assertEquals("2026-11-11T19:30", notebook.lessons().single().dateTime)
            notebook.removeStudent(first.id)
            assertEquals(second.id, notebook.assignments().single().studentId)
            assertTrue(notebook.lessons().isEmpty())
        } finally { notebook.clear() }
    }

    @Test fun localTeacherDataConsentSchedulingAndDeletion() {
        val notebook = TeacherNotebook(context)
        notebook.clear()
        assertTrue(runCatching { notebook.addStudent("اختبار", false) }.isFailure)
        notebook.addStudent("طالب تجريبي", true)
        val student = notebook.students().single()
        notebook.addLesson(student.id, "معلم اختبار غير موثق", "2026-10-20T18:30", "ملاحظة تجريبية")
        val lesson = TeacherNotebook(context).lessons().single()
        notebook.updateLesson(lesson.id, "ملاحظة مراجعة", true)
        assertTrue(notebook.lessons().single().completed)
        notebook.recordComplaint(lesson.id, "ملاحظة محلية تجريبية")
        assertEquals(1, notebook.complaints().size)
        notebook.removeStudent(student.id)
        assertTrue(notebook.students().isEmpty())
        assertTrue(notebook.lessons().isEmpty())
        assertTrue(notebook.complaints().isEmpty())
        notebook.clear()
    }

    @Test fun deletingOneStudentPreservesOtherStudentLessonsAndComplaints() {
        val notebook = TeacherNotebook(context)
        notebook.clear()
        try {
            notebook.addStudent("طالب أ تجريبي", true)
            notebook.addStudent("طالب ب تجريبي", true)
            val first = notebook.students()[0]
            val second = notebook.students()[1]
            notebook.addLesson(first.id, "معلم اختبار", "2026-10-20T18:30", "ملاحظة أ")
            notebook.addLesson(second.id, "معلم اختبار", "2026-10-21T18:30", "ملاحظة ب")
            val firstLesson = notebook.lessons().first { it.studentId == first.id }
            val secondLesson = notebook.lessons().first { it.studentId == second.id }
            notebook.recordComplaint(firstLesson.id, "بلاغ أ")
            notebook.recordComplaint(secondLesson.id, "بلاغ ب")
            assertTrue(runCatching { notebook.removeStudent("missing") }.isFailure)
            assertEquals(2, notebook.complaints().size)
            notebook.removeStudent(first.id)
            val reloaded = TeacherNotebook(context)
            assertEquals(listOf(second), reloaded.students())
            assertEquals(listOf(secondLesson), reloaded.lessons())
            assertEquals(listOf(NotebookComplaint(secondLesson.id, "بلاغ ب")), reloaded.complaints())
        } finally { notebook.clear() }
    }

    @Test fun recordPlayEraseAndCrashResidueCleanup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val audio = PrivateRecitationPractice(context)
        try {
            ui.awaitHifzReady()
            assertFalse(audio.hasRecording)
            assertFalse(audio.play())
            audio.start()
            assertTrue(audio.isRecording)
            Thread.sleep(1200) // A real AAC capture; not a correctness-of-recitation test.
            assertTrue(audio.stop())
            assertTrue(audio.hasRecording)
            assertTrue(audio.play())
            audio.erase()
            assertFalse(audio.isRecording)
            assertFalse(audio.hasRecording)
            val residue = File(context.cacheDir, "hifz_private_session.m4a")
            residue.writeBytes(byteArrayOf(1, 2, 3))
            PrivateRecitationPractice(context).close()
            assertFalse(residue.exists())
            audio.start()
            audio.erase() // Rapid start/stop must fail safely and erase any output.
            assertFalse(audio.hasRecording)
        } finally { audio.close(); scenario.close() }
    }
}
