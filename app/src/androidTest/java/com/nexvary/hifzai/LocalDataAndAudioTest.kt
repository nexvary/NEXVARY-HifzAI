package com.nexvary.hifzai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
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
