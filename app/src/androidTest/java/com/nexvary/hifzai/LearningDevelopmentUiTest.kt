package com.nexvary.hifzai

import android.graphics.Bitmap
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityEvent
import java.util.UUID
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextReplacement
import java.io.File
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import java.time.LocalDate
import com.nexvary.hifzai.learning.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LearningDevelopmentUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun click(text: String) {
        val node = rule.onNodeWithText(text)
        if (text == "الحفظ" || text == "الإعدادات") node.performClick()
        else node.performScrollTo().performClick()
        rule.waitForIdle()
    }
    @Before fun reset() {
        rule.awaitHifzReady()
        click("الإعدادات")
        click("حذف تقدم الحفظ من الجهاز")
        click("حذف البيانات")
        rule.awaitHifzReady()
    }
    @Test fun goalAndFavoritePersistAndDeletionResetsRuntimeSession() {
        click("الحفظ")
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        assertEquals(1, ProgressStore(context).session()?.hints)
        click("الإعدادات")
        click("تفعيل تذكير العودة الاختياري")
        assertTrue(ProgressStore(context).gentleReturnEnabled)
        click("مراجعة فقط")
        assertEquals(StudyMode.REVIEW_ONLY, ProgressStore(context).learningGoal.mode)
        // The preview does not silently replace the chosen goals.
        val beforeTimeSuggestion = ProgressStore(context).learningGoal
        assertNull(ProgressStore(context).dailyStudyMinutes)
        click("تطبيق اقتراح الوقت")
        assertEquals(15, ProgressStore(context).dailyStudyMinutes)
        assertEquals(StudyTimeBudget.suggest(15, StudyMode.REVIEW_ONLY), ProgressStore(context).learningGoal)
        assertEquals(beforeTimeSuggestion.mode, ProgressStore(context).learningGoal.mode)
        click("دليل القراء")
        rule.onAllNodesWithText("اختيار قارئ مفضل")[0].performScrollTo().performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        assertEquals(StudyMode.REVIEW_ONLY, ProgressStore(context).learningGoal.mode)
        assertEquals(15, ProgressStore(context).dailyStudyMinutes)
        assertEquals(ReciterCatalog.all.first().slug, ProgressStore(context).favoriteReciter)
        assertTrue(ProgressStore(context).gentleReturnEnabled)
        click("الإعدادات")
        click("حذف تقدم الحفظ من الجهاز")
        click("حذف البيانات")
        rule.awaitHifzReady()
        assertNull(ProgressStore(context).session())
        assertNull(ProgressStore(context).favoriteReciter)
        assertEquals(LearningGoal(), ProgressStore(context).learningGoal)
        assertNull(ProgressStore(context).dailyStudyMinutes)
        assertFalse(ProgressStore(context).gentleReturnEnabled)
        assertNull(ProgressStore(context).gentleReturnAnchorDay)
        click("الحفظ")
        val session = ProgressStore(context).session()!!
        assertEquals(PracticeKind.READ, session.stage)
        assertEquals(0, session.hints)
        assertFalse(session.exposed)
        assertEquals(0, session.rangeStart)
        // A subsequent Activity recreation must preserve the reset, not an old
        // saved-state generation that still contains the pre-deletion attempt.
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        val recreated = ProgressStore(context).session()!!
        assertEquals(PracticeKind.READ, recreated.stage)
        assertEquals(0, recreated.hints)
        assertFalse(recreated.exposed)
    }

    private fun UiAutomation.shell(command: String): ByteArray =
        ParcelFileDescriptor.AutoCloseInputStream(executeShellCommand(command)).use { it.readBytes() }

    private fun UiAutomation.pickerRoot(): AccessibilityNodeInfo? =
        windows.asSequence().filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { it.root }
            .firstOrNull { it.packageName?.toString()?.endsWith(".documentsui") == true }

    private fun UiAutomation.appHasFocus(): Boolean =
        windows.any { it.isFocused && it.root?.packageName?.toString() == context.packageName }

    private fun UiAutomation.keyboardVisible(): Boolean =
        windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

    private fun withProgressPicker(block: (UiAutomation) -> Unit) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val info = checkNotNull(automation.serviceInfo)
        val originalFlags = info.flags
        info.flags = originalFlags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = info
        try {
            click("الإعدادات")
            rule.onNodeWithText("حفظ التقدم في ملف CSV").performScrollTo().performClick()
            rule.waitUntil(15000) {
                automation.pickerRoot()?.findNode { it.viewIdResourceName == "android:id/title" } != null
            }
            block(automation)
        } catch (failure: Throwable) {
            runCatching {
                val windows = automation.windows.joinToString("\n") { window ->
                    "$window package=${window.root?.packageName}"
                }
                File(context.filesDir, "hifz-export-picker-failure.txt").writeText(windows)
                automation.takeScreenshot()?.let { screenshot ->
                    try {
                        File(context.filesDir, "hifz-export-picker-failure.png").outputStream().use {
                            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                        }
                    } finally { screenshot.recycle() }
                }
            }.exceptionOrNull()?.let { failure.addSuppressed(it) }
            throw failure
        } finally {
            // Leave no external window over subsequent tests if an assertion fails.
            if (!automation.appHasFocus() && automation.pickerRoot() != null) {
                if (automation.keyboardVisible()) {
                    automation.performGlobalAction(GLOBAL_ACTION_BACK)
                    automation.waitForIdle(300, 5000)
                }
                if (!automation.appHasFocus() && automation.pickerRoot() != null) {
                    automation.performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
            info.flags = originalFlags
            automation.serviceInfo = info
        }
    }

    @Test fun cancellingAndroidDocumentPickerReturnsToSettingsWithoutChangingProgress() {
        val store = ProgressStore(context)
        val before = MemoryFingerprint(1001, attempts = 1, recalled = 1)
        store.save(ReviewCard(1001), before)
        withProgressPicker { automation ->
            // Wait for the filename editor/IME to settle before deciding where
            // Back is delivered. Each step traverses actual system UI.
            automation.waitForIdle(300, 5000)
            var backSteps = 0
            while (!automation.appHasFocus() && automation.pickerRoot() != null && backSteps < 3) {
                // The global action is queued by the system. Wait for a real
                // window event before testing focus or issuing another Back.
                val transition = automation.executeAndWaitForEvent(
                    { assertTrue("System must accept Back", automation.performGlobalAction(GLOBAL_ACTION_BACK)) },
                    { event -> event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                        event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED },
                    5000
                )
                transition.recycle()
                automation.waitForIdle(300, 5000)
                backSteps++
            }
            rule.waitUntil(15000) { automation.appHasFocus() }
            rule.waitUntil(15000) {
                rule.onAllNodesWithText("أُلغي حفظ ملف التقدم").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("أُلغي حفظ ملف التقدم").performScrollTo().assertExists()
        }
        assertEquals(before, store.memory(1001))
    }

    @Test fun savesExactCsvBytesThroughAndroidDocumentPicker() {
        val store = ProgressStore(context)
        val later = MemoryFingerprint(2001, attempts = 2, hinted = 1)
        val earlier = MemoryFingerprint(1007, attempts = 1, recalled = 1)
        store.save(ReviewCard(2001), later)
        store.save(ReviewCard(1007), earlier)
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()

        val filename = "hifz-proof-${UUID.randomUUID()}.csv"
        // UiAutomation executes argv directly, so use find -exec instead of shell syntax.
        val findFile = "find /storage/emulated/0 -name $filename -type f"
        withProgressPicker { automation ->
            try {
                val name = checkNotNull(automation.pickerRoot()?.findNode {
                    it.isEditable && it.viewIdResourceName == "android:id/title"
                })
                assertTrue(name.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, filename)
                }))
                var save: AccessibilityNodeInfo? = null
                rule.waitUntil(15000) {
                    save = automation.pickerRoot()?.findNode {
                        it.isClickable && it.isEnabled && it.viewIdResourceName == "android:id/button1"
                    }
                    save != null
                }
                assertTrue(checkNotNull(save).performAction(AccessibilityNodeInfo.ACTION_CLICK))
                rule.waitUntil(15000) { automation.pickerRoot() == null }
                rule.waitUntil(15000) {
                    rule.onAllNodesWithText("حُفظ ملف التقدم").fetchSemanticsNodes().isNotEmpty()
                }
                rule.onNodeWithText("حُفظ ملف التقدم").performScrollTo().assertExists()
                val paths = automation.shell(findFile).decodeToString().lineSequence().filter { it.isNotBlank() }.toList()
                assertEquals("Expected one document in shared storage", 1, paths.size)
                val expected = "surah,ayah,attempts,independent_recall,hinted,transition_misses,word_misses,similarity_misses\r\n" +
                    "1,7,1,1,0,0,0,0\r\n2,1,2,0,1,0,0,0\r\n"
                assertArrayEquals(expected.toByteArray(Charsets.UTF_8),
                    automation.shell("$findFile -exec cat {} ;"))
                assertEquals(earlier, store.memory(1007))
                assertEquals(later, store.memory(2001))
            } finally {
                automation.shell("$findFile -exec rm -f {} ;")
            }
        }
    }

    private fun AccessibilityNodeInfo.findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(this)) return this
        for (index in 0 until childCount) {
            getChild(index)?.findNode(predicate)?.let { return it }
        }
        return null
    }

    @Test fun recitationRangeChecksArabicAddressesAndKeepsDraftOnRecreation() {
        click("دليل القرّاء المصريين والسعوديين")
        rule.onNodeWithText("اختر قارئًا مفضلًا من القائمة أدناه للتحقق من توفر نطاق له.").assertExists()
        rule.onAllNodesWithText("اختيار قارئ مفضل")[0].performScrollTo().performClick()
        click("اختيار سورة للتلاوة")
        rule.onNodeWithText("ابحث باسم السورة أو رقمها").performTextReplacement("٢")
        rule.onNodeWithText("سورة البقرة").performClick()
        rule.onNodeWithText("2:1").assertExists()
        rule.onNodeWithText("2:286").assertExists()
        rule.onNodeWithText("فهرس التسجيلات المأذونة المرفق فارغ حاليًا.").assertExists()
        rule.onNodeWithText("تجهيز النطاق").assertDoesNotExist()
        val first = rule.onNodeWithText("بداية نطاق التلاوة")
        val last = rule.onNodeWithText("نهاية نطاق التلاوة")
        first.performScrollTo().performTextReplacement("١:٧")
        last.performScrollTo().performTextReplacement("١:٨")
        click("تحقق من توفر التلاوة")
        rule.onNodeWithText("نهاية النطاق خارج المصحف").assertExists()

        last.performScrollTo().performTextReplacement("٢:١")
        rule.onNodeWithText("نهاية النطاق خارج المصحف").assertDoesNotExist()
        click("تحقق من توفر التلاوة")
        val unavailable = rule.onNodeWithText("لا تتوفر تسجيلات مأذونة لكل آيات هذا النطاق")
        unavailable.assertExists()
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        rule.onNodeWithText("١:٧").assertExists()
        rule.onNodeWithText("٢:١").assertExists()
        unavailable.performScrollTo()
        rule.onNodeWithText("تشغيل", substring = false).assertDoesNotExist()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.filesDir, "hifz-recitation-range.png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    @Test fun optedInSuggestionAppearsOnReturnAndCanBePostponedOrDisabled() {
        click("الإعدادات")
        click("تفعيل تذكير العودة الاختياري")
        val store = ProgressStore(context)
        val today = LocalDate.now().toEpochDay()
        // Synthetic local date fixture, not evidence of a real learner study.
        fun returnAfterWeek() {
            store.setGentleReturn(true, today - 7)
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.waitForIdle()
        }
        returnAfterWeek()
        rule.onNodeWithText("الرئيسية").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("مرّ أسبوع دون تدريب مسجل.", substring = true).assertExists()
        click("ذكّرني بعد أسبوع")
        assertEquals(today, store.gentleReturnAnchorDay)
        assertTrue(rule.onAllNodesWithText("مرّ أسبوع دون تدريب مسجل.", substring = true).fetchSemanticsNodes().isEmpty())
        returnAfterWeek()
        click("أوقف اقتراح العودة")
        assertFalse(store.gentleReturnEnabled)
        assertNull(store.gentleReturnAnchorDay)
        assertTrue(rule.onAllNodesWithText("مرّ أسبوع دون تدريب مسجل.", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test fun dailyBudgetCountsUniqueGradedVersesAcrossSessionsAndStillAllowsManualPractice() {
        fun attempt(label: String) {
            repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
            click(label)
        }
        val today = LocalDate.now().toEpochDay()
        val store = ProgressStore(context)
        // Recreate and dispose the daily-plan composition repeatedly. A later
        // unchanged-code run crashed here in SlotWriter at 1.5x font on API 35.
        repeat(4) {
            click("الإعدادات")
            click("حذف تقدم الحفظ من الجهاز")
            click("حذف البيانات")
            rule.awaitHifzReady()
            click("الحفظ")
            attempt("نسيت")
            attempt("جيد") // Retry the same verse: still one new verse, no charged review.
            assertEquals(1, store.workload(today).newVerseIds.size)
            assertTrue(store.workload(today).reviewVerseIds.isEmpty())
            attempt("جيد")
            rule.onNodeWithText("الرئيسية").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("آيات قيّمتها اليوم: جديد 2/2 • مراجعة 0/15").assertExists()
            rule.onNodeWithText("الآيات المستحقة: 0 • آيات جديدة مقترحة: 0").assertExists()
            click("الحفظ") // The daily proposal is exhausted; explicit manual practice remains available.
            attempt("جيد")
            assertEquals(3, store.workload(today).newVerseIds.size)
            rule.activityRule.scenario.recreate()
            rule.awaitHifzReady()
            assertEquals(3, ProgressStore(context).workload(today).newVerseIds.size)
        }

    }

    @Test fun listedGlobalReviewsAndGuidedSessionShareOnePlanAndKeepTheNewChapterOnResume() {
        val today = LocalDate.now().toEpochDay()
        val store = ProgressStore(context)
        val corpus = QuranCorpus.load(context)
        fun seed(id: Int, due: Long) = store.save(
            ReviewCard(id, repetitions = 1, intervalDays = 1, dueEpochDay = due),
            MemoryFingerprint(id, attempts = 1, recalled = 1))
        fun grade(label: String) {
            repeat(5) {
                if (rule.onAllNodesWithText(label).fetchSemanticsNodes().isEmpty())
                    click("المرحلة التالية في سُلّم الاسترجاع")
            }
            click(label)
        }
        seed(114006, today - 2); seed(2255, today - 1); seed(1001, today + 1)
        store.learningGoal = LearningGoal(1, 2)
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        rule.onNodeWithText("الآيات المستحقة: 2 • آيات جديدة مقترحة: 1").assertExists()
        click("تفاصيل الخطة اليومية والمراجعات")
        rule.onNodeWithText(corpus.info(114).arabicName + " • الآية 6").assertExists()
        rule.onNodeWithText(corpus.info(2).arabicName + " • الآية 255").assertExists()
        click("ابدأ الخطة المقترحة")
        assertEquals(114, store.session()!!.chapter); assertEquals(6, store.session()!!.ayah)
        assertEquals(1, store.session()!!.planChapter)
        grade("نسيت")
        assertEquals(114, store.session()!!.chapter)
        assertEquals(setOf(114006), store.workload(today).reviewVerseIds)
        grade("جيد")
        assertEquals(2, store.session()!!.chapter); assertEquals(255, store.session()!!.ayah)
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        assertEquals(1, store.session()!!.planChapter)
        rule.onNodeWithText("الرئيسية").performClick(); rule.waitForIdle()
        rule.onNodeWithText("الآيات المستحقة: 1 • آيات جديدة مقترحة: 1").assertExists()
        click("استكمل جلسة الحفظ السابقة")
        grade("جيد")
        assertEquals(1, store.session()!!.chapter); assertEquals(2, store.session()!!.ayah)
        assertEquals(1, store.session()!!.planChapter)
        grade("جيد")
        rule.onNodeWithText("الخطة اليومية").assertExists()
        assertNull(store.session())
        assertEquals(setOf(114006, 2255), store.workload(today).reviewVerseIds)
        assertEquals(setOf(1002), store.workload(today).newVerseIds)
        assertEquals(today + 1, store.card(1001).dueEpochDay)
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        assertNull(store.session())
    }

    @Test fun guidedDailyModesResumeAndEndAtTheBudgetInsteadOfAdvancingIntoOtherModes() {
        for ((offset, mode) in listOf(StudyMode.NEW_ONLY, StudyMode.REVIEW_ONLY).withIndex()) {
            if (offset > 0) reset()
            val today = LocalDate.now().toEpochDay()
            val store = ProgressStore(context)
            store.save(ReviewCard(1001, repetitions = 1, intervalDays = 1, dueEpochDay = today - 1),
                MemoryFingerprint(1001, attempts = 1, recalled = 1))
            store.learningGoal = LearningGoal(1, 1, mode)
            rule.activityRule.scenario.recreate()
            rule.awaitHifzReady()
            click("ابدأ جلسة اليوم")
            assertTrue(ProgressStore(context).session()!!.followsDailyPlan)
            rule.activityRule.scenario.recreate()
            rule.awaitHifzReady()
            assertTrue(ProgressStore(context).session()!!.followsDailyPlan)
            repeat(5) {
                if (rule.onAllNodesWithText("جيد").fetchSemanticsNodes().isEmpty())
                    click("المرحلة التالية في سُلّم الاسترجاع")
            }
            click("جيد")
            rule.onNodeWithText("الخطة اليومية").assertExists()
            assertNull(store.session())
            val work = store.workload(today)
            if (mode == StudyMode.NEW_ONLY) {
                assertEquals(setOf(1002), work.newVerseIds)
                assertTrue(work.reviewVerseIds.isEmpty())
            } else {
                assertEquals(setOf(1001), work.reviewVerseIds)
                assertTrue(work.newVerseIds.isEmpty())
            }
        }
    }
}
