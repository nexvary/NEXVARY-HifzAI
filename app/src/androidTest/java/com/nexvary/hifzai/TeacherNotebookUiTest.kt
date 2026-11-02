package com.nexvary.hifzai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeacherNotebookUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val notebook get() = TeacherNotebook(context)
    private fun click(label: String, first: Boolean = false) {
        val node = if (first) rule.onAllNodesWithText(label)[0] else rule.onNodeWithText(label)
        if (label in setOf("الإعدادات", "احفظ النطاق", "إلغاء", "احذف من الدفتر")) node.performClick()
        else node.performScrollTo().performClick()
        rule.waitForIdle()
    }
    private fun openNotebook() {
        click("الإعدادات"); click("دفتر المعلم المحلي")
    }
    @Before fun seed() {
        rule.awaitHifzReady()
        notebook.clear()
        notebook.addStudent("ملف اختبار أ", true)
        notebook.addStudent("ملف اختبار ب", true)
    }
    @After fun clean() { notebook.clear() }

    @Test fun assignmentEditorRejectsInvalidReferencesReopensChangedRangeAndConfirmsDeletion() {
        val corpus = QuranCorpus.load(context)
        val students = notebook.students()
        notebook.assignRange(students[0].id, 1, 1, 3, "2026-11-10", corpus)
        notebook.assignRange(students[1].id, 114, 1, 6, "2026-11-11", corpus)
        val original = notebook.assignments().first()
        val other = notebook.assignments().last()
        notebook.completeAssignment(original.id, true)
        openNotebook()
        click("تعديل نطاق المراجعة وموعده", first = true)
        rule.onNodeWithText("النهاية في السورة نفسها • سورة:آية").performScrollTo().performTextReplacement("1:8")
        click("احفظ النطاق")
        assertTrue(rule.onAllNodesWithText("نطاق الآيات غير موجود في المصدر").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("تعديل مهمة المراجعة").assertExists()
        assertEquals(3, notebook.assignments().first().lastAyah)
        click("إلغاء")
        click("تعديل نطاق المراجعة وموعده", first = true)
        rule.onNodeWithText("النهاية في السورة نفسها • سورة:آية").performScrollTo().performTextReplacement("1:4")
        click("احفظ النطاق")
        val changed = notebook.assignments().first { it.id == original.id }
        assertEquals(4, changed.lastAyah); assertFalse(changed.completed)
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        openNotebook()
        click("حذف مهمة المراجعة", first = true)
        click("إلغاء")
        assertEquals(2, notebook.assignments().size)
        click("حذف مهمة المراجعة", first = true)
        click("احذف من الدفتر")
        assertEquals(listOf(other), TeacherNotebook(context).assignments())
        assertEquals(students, notebook.students())
    }

    @Test fun lessonDeletionCancelsThenRemovesOnlyItsOwnNotesAndSurvivesReload() {
        val students = notebook.students()
        notebook.addLesson(students[0].id, "معلم اختبار أ", "2026-11-10T18:30", "الجلسة أ")
        notebook.addLesson(students[1].id, "معلم اختبار ب", "2026-11-11T18:30", "الجلسة ب")
        val lessons = notebook.lessons()
        notebook.recordComplaint(lessons[0].id, "مشكلة أ")
        notebook.recordComplaint(lessons[1].id, "مشكلة ب")
        openNotebook(); click("الجلسات")
        click("حذف هذه الجلسة", first = true); click("إلغاء")
        assertEquals(lessons, notebook.lessons())
        assertEquals(2, notebook.complaints().size)
        click("حذف هذه الجلسة", first = true); click("احذف من الدفتر")
        assertEquals(listOf(lessons[1]), notebook.lessons())
        assertEquals(listOf(NotebookComplaint(lessons[1].id, "مشكلة ب")), notebook.complaints())
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        openNotebook(); click("الجلسات")
        rule.onNodeWithText("الجلسة ب").assertExists()
        rule.onAllNodesWithText("الجلسة أ").assertCountEquals(0)
        assertEquals(students, notebook.students())
    }
}
