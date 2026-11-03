package com.nexvary.hifzai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.io.File

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TeacherNotebookUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val notebook get() = TeacherNotebook(context)
    private fun click(label: String, first: Boolean = false) {
        val node = if (first) rule.onAllNodesWithText(label)[0] else rule.onNodeWithText(label)
        if (label in setOf("الإعدادات", "احفظ النطاق", "احفظ التعديل", "إلغاء", "احذف من الدفتر", "ملف اختبار أ", "ملف اختبار ب", "كل الطلاب")) node.performClick()
        else node.performScrollTo().performClick()
        rule.waitForIdle()
    }
    private fun pickerButton(widget: Class<out android.view.View>, confirm: Boolean = true) {
        val root = androidx.test.espresso.matcher.RootMatchers.withDecorView(
            androidx.test.espresso.matcher.ViewMatchers.hasDescendant(
                androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(widget)))
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(widget))
            .inRoot(root).check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
        // Capture the actual platform window; Compose captureToImage only sees
        // the Activity beneath this native picker. No generated UI or fixtures.
        val image = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val page = if (widget == android.widget.DatePicker::class.java) "date" else "time"
        try {
            File(context.filesDir, "hifz-notebook-$page-picker.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { image.recycle() }
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(
            if (confirm) android.R.id.button1 else android.R.id.button2))
            .inRoot(root).perform(androidx.test.espresso.action.ViewActions.click())
        rule.waitForIdle()
    }
    private fun openNotebook() {
        click("الإعدادات"); click("دفتر المعلم المحلي")
    }
    private fun screenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.filesDir, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
    @Before fun seed() {
        rule.awaitHifzReady()
        notebook.clear()
        notebook.addStudent("ملف اختبار أ", true)
        notebook.addStudent("ملف اختبار ب", true)
    }
    @After fun clean() { notebook.clear() }

    @Test fun agendaReportsCorruptReferencesAndDatesUntilTheUserExplicitlyRepairsThem() {
        val today = LocalDate.now()
        val corpus = QuranCorpus.load(context)
        val student = notebook.students().first()
        notebook.assignRange(student.id, 1, 1, 2, today.toString(), corpus)
        val prefs = context.getSharedPreferences("teacher_notebook_v1", 0)
        val data = org.json.JSONArray(prefs.getString("assignments", "[]"))
        data.getJSONObject(0).put("chapter", 0).put("due", "broken")
        prefs.edit().putString("assignments", data.toString()).commit()
        openNotebook(); click("جدول المواعيد والمهام")
        rule.onNodeWithText("تعذر عرض 1 سجل في الجدول", substring = true).assertExists()
        rule.onNodeWithText("لا توجد سجلات ضمن هذا الاختيار").assertExists()
        assertEquals(0, notebook.assignments().single().chapter)
        assertEquals("broken", notebook.assignments().single().dueDate)
        click("الطلاب")
        click("تعديل نطاق المراجعة وموعده")
        rule.onNodeWithText("البداية • سورة:آية").performScrollTo().performTextReplacement("1:1")
        rule.onNodeWithText("النهاية في السورة نفسها • سورة:آية").performScrollTo().performTextReplacement("1:2")
        click("تاريخ المراجعة: broken")
        // Actual cancel/confirm interactions, never a test-only date write.
        rule.onAllNodesWithText("تعديل مهمة المراجعة").assertCountEquals(0)
        pickerButton(android.widget.DatePicker::class.java, confirm = false)
        rule.onNodeWithText("تعديل مهمة المراجعة").assertExists()
        rule.onNodeWithText("البداية • سورة:آية").assertTextContains("1:1")
        rule.onNodeWithText("النهاية في السورة نفسها • سورة:آية").assertTextContains("1:2")
        assertEquals("broken", notebook.assignments().single().dueDate)
        click("تاريخ المراجعة: broken")
        pickerButton(android.widget.DatePicker::class.java)
        assertEquals("broken", notebook.assignments().single().dueDate)
        click("احفظ النطاق")
        assertEquals(1, notebook.assignments().single().chapter)
        assertEquals(today.toString(), notebook.assignments().single().dueDate)
        click("جدول المواعيد والمهام")
        rule.onNodeWithText("اليوم • 1 • محدد").assertExists()
        rule.onAllNodesWithText("تعذر عرض", substring = true).assertCountEquals(0)
    }

    @Test fun nativeLessonDateAndTimePickersReturnToTheEditorAndPreserveCancelledDrafts() {
        val today = LocalDate.now()
        openNotebook(); click("الجلسات")
        rule.onNodeWithText("ملف اختبار أ").performScrollTo().performClick()
        rule.onNodeWithText("اسم المعلم المعروف لك • غير موثق من التطبيق").performScrollTo().performTextReplacement("معلم اصطناعي")
        rule.onNodeWithText("هدف الجلسة وملاحظات الحفظ").performScrollTo().performTextReplacement("موعد من المنتقي")
        click("اختر يوم ووقت الجلسة")
        pickerButton(android.widget.DatePicker::class.java)
        pickerButton(android.widget.TimePicker::class.java)
        assertTrue(notebook.lessons().isEmpty())
        click("احفظ الموعد المحلي")
        val original = notebook.lessons().single()
        assertEquals(today, java.time.LocalDateTime.parse(original.dateTime).toLocalDate())
        click("تعديل الموعد والملاحظات", first = true)
        rule.onNodeWithText("ملاحظات الجلسة").performTextReplacement("مسودة محفوظة بعد الإلغاء")
        fun chooseEditorTime() {
            rule.onNode(hasText(NotebookRules.displayDateTime(original.dateTime)) and hasAnyAncestor(isDialog()))
                .performScrollTo().performClick()
            rule.waitForIdle()
        }
        repeat(3) {
            // Three real cancellation cycles must all succeed; never retry a failure.
            chooseEditorTime()
            rule.onAllNodesWithText("تعديل الجلسة المحلية").assertCountEquals(0)
            pickerButton(android.widget.DatePicker::class.java)
            pickerButton(android.widget.TimePicker::class.java, confirm = false)
            rule.onNodeWithText("تعديل الجلسة المحلية").assertExists()
            rule.onNodeWithText("ملاحظات الجلسة").assertTextContains("مسودة محفوظة بعد الإلغاء")
            assertEquals(original, notebook.lessons().single())
        }
        chooseEditorTime()
        pickerButton(android.widget.DatePicker::class.java)
        pickerButton(android.widget.TimePicker::class.java)
        assertEquals(original, notebook.lessons().single())
        click("احفظ التعديل")
        val changed = notebook.lessons().single()
        assertEquals("مسودة محفوظة بعد الإلغاء", changed.note)
        assertEquals(original.dateTime, changed.dateTime)
        assertEquals(original.studentId, changed.studentId)
    }

    @Test fun agendaFiltersCompletionAndStudentSelectionSurviveReloadWithoutChangingPersonalProgress() {
        val today = LocalDate.now()
        val students = notebook.students()
        val corpus = QuranCorpus.load(context)
        val progress = ProgressStore(context)
        val beforeCard = progress.card(1001); val beforeMemory = progress.memory(1001); val beforeHistory = progress.history(1001)
        notebook.addLesson(students[0].id, "اختبار أ", "${today}T10:00", "موعد اليوم أ")
        notebook.addLesson(students[1].id, "اختبار ب", "${today}T11:00", "موعد اليوم ب")
        notebook.addLesson(students[0].id, "اختبار سابق", "${today.minusDays(1)}T10:00", "موعد سابق")
        notebook.addLesson(students[1].id, "اختبار مكتمل", "${today}T12:00", "موعد مكتمل")
        notebook.updateLesson(notebook.lessons().last().id, "موعد مكتمل", true)
        notebook.assignRange(students[0].id, 1, 1, 2, today.toString(), corpus)
        notebook.assignRange(students[1].id, 114, 1, 6, today.plusDays(1).toString(), corpus)
        val task = notebook.assignments().first()
        val future = notebook.assignments().last()
        openNotebook(); click("جدول المواعيد والمهام")
        click("تحديث تاريخ الجدول")
        rule.onNodeWithText("اليوم • 3 • محدد").assertExists()
        rule.onNodeWithText("القادمة • 1").assertExists()
        rule.onNodeWithText("السابقة غير المكتملة • 1").assertExists()
        rule.onNodeWithText("المكتملة • 1").assertExists()
        rule.onNodeWithText("الكل • 6").performScrollTo()
        screenshot("hifz-agenda-filters.png")
        rule.onAllNodesWithText("حذف هذه الجلسة")[0].performScrollTo()
        screenshot("hifz-agenda.png")
        click("عرض: كل الطلاب"); click("ملف اختبار أ")
        rule.onNodeWithText("اليوم • 2 • محدد").assertExists()
        rule.onAllNodesWithText("موعد اليوم ب").assertCountEquals(0)
        click("سجّل اكتمال المراجعة")
        assertTrue(notebook.assignments().first { it.id == task.id }.completed)
        rule.onNodeWithText("اليوم • 1 • محدد").assertExists()
        click("المكتملة • 1")
        rule.onNodeWithText("ملف اختبار أ • مهمة مراجعة").assertExists()
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        rule.onNodeWithText("جدول الدفتر المحلي").assertExists()
        rule.onNodeWithText("عرض: ملف اختبار أ").assertExists()
        rule.onNodeWithText("المكتملة • 1 • محدد").assertExists()
        click("مكتملة • أعد فتح المهمة")
        assertFalse(notebook.assignments().first { it.id == task.id }.completed)
        rule.onNodeWithText("لا توجد سجلات ضمن هذا الاختيار").assertExists()
        assertEquals(future, notebook.assignments().first { it.id == future.id })
        assertEquals(beforeCard, progress.card(1001)); assertEquals(beforeMemory, progress.memory(1001))
        assertEquals(beforeHistory, progress.history(1001))
    }

    @Test fun agendaPagingRoutesEditsAndConfirmedDeletionToTheCorrectLesson() {
        val today = LocalDate.now()
        val student = notebook.students().first()
        repeat(7) { i ->
            notebook.addLesson(student.id, "اختبار ${i + 1}", today.atTime(8 + i, 0).toString(), "جلسة الصفحة ${i + 1}")
        }
        val original = notebook.lessons()
        notebook.recordComplaint(original[5].id, "مشكلة السادسة")
        notebook.recordComplaint(original[6].id, "مشكلة السابعة")
        openNotebook(); click("جدول المواعيد والمهام")
        rule.onNodeWithText("صفحة 1 من 2").assertExists()
        rule.onNodeWithText("جلسة الصفحة 5").assertExists()
        rule.onAllNodesWithText("جلسة الصفحة 6").assertCountEquals(0)
        click("الصفحة التالية في الجدول")
        rule.onNodeWithText("صفحة 2 من 2").assertExists()
        rule.onNodeWithText("جلسة الصفحة 6").assertExists()
        rule.onAllNodesWithText("جلسة الصفحة 1").assertCountEquals(0)
        click("تعديل الموعد والملاحظات", first = true)
        rule.onNodeWithText("ملاحظات الجلسة").performTextReplacement("جلسة معدلة من الجدول")
        click("احفظ التعديل")
        assertEquals("جلسة معدلة من الجدول", notebook.lessons().first { it.id == original[5].id }.note)
        click("الصفحة التالية في الجدول")
        click("حذف هذه الجلسة", first = true); click("إلغاء")
        assertEquals(7, notebook.lessons().size)
        click("حذف هذه الجلسة", first = true); click("احذف من الدفتر")
        assertEquals(6, notebook.lessons().size)
        assertEquals(listOf(NotebookComplaint(original[6].id, "مشكلة السابعة")), notebook.complaints())
        click("الصفحة التالية في الجدول")
        rule.onNodeWithText("جلسة الصفحة 7").assertExists()
        click("حذف هذه الجلسة"); click("احذف من الدفتر")
        rule.onNodeWithText("صفحة 1 من 1").assertExists()
        rule.onNodeWithText("الصفحة التالية في الجدول").assertIsNotEnabled()
        assertEquals(original.take(5), TeacherNotebook(context).lessons())
        assertTrue(notebook.complaints().isEmpty())
    }

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
