package com.nexvary.hifzai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import android.os.ParcelFileDescriptor
import com.nexvary.hifzai.learning.RuntimeResetStore
import com.nexvary.hifzai.learning.ProgressStore
import com.nexvary.hifzai.learning.TeacherNotebook
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real emulator/device launch proof. Screenshots come from the running app;
 * no static mockups or fabricated UI evidence.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HifzUiSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun cleanSyntheticTestState() {
        rule.awaitHifzReady()
        rule.onNodeWithText("الإعدادات").performClick()
        rule.waitForIdle()
        click("حذف تقدم الحفظ من الجهاز")
        click("حذف البيانات")
        rule.awaitHifzReady()
    }

    private fun click(text: String) {
        rule.onNodeWithText(text).performScrollTo().performClick()
        rule.waitForIdle()
    }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, name)
        val img = rule.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { stream ->
            check(img.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun liveSystemFontChangeRecreatesWithoutLosingTheCurrentAttempt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val initial = context.resources.configuration.fontScale
        val changed = if (initial > 1.2f) 1f else 1.5f
        fun setScale(value: Float) {
            if (kotlin.math.abs(context.resources.configuration.fontScale - value) < 0.01f) {
                rule.awaitHifzReady(); return
            }
            val previous = rule.activity
            val descriptor = instrumentation.uiAutomation.executeShellCommand("settings put system font_scale $value")
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            rule.waitUntil(15000) {
                runCatching { rule.activity !== previous &&
                    kotlin.math.abs(rule.activity.resources.configuration.fontScale - value) < 0.01f
                }.getOrDefault(false)
            }
            rule.awaitHifzReady()
        }
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        val expected = ProgressStore(context).session()!!
        try {
            setScale(changed)
            org.junit.Assert.assertEquals(expected, ProgressStore(context).session())
            setScale(initial)
            org.junit.Assert.assertEquals(expected, ProgressStore(context).session())
        } finally { setScale(initial) }
    }

    @Test fun repeatedVerifiedLoadsRecreationAndDeletionKeepAttemptStateConsistent() {
        val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        repeat(4) {
            rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
            repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
            click("تلميح")
            val expected = store.session()!!
            rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
            org.junit.Assert.assertEquals(expected, store.session())
            rule.onNodeWithText("الإعدادات").performClick(); rule.waitForIdle()
            click("حذف تقدم الحفظ من الجهاز"); click("حذف البيانات")
            rule.awaitHifzReady()
            org.junit.Assert.assertNull(store.session())
            rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
            org.junit.Assert.assertNull(store.session())
        }
    }

    @Test fun rapidRecreationDuringVerificationKeepsOnlyTheLiveActivityAndTheValidAttempt() {
        val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        val expected = store.session()!!
        // Do not wait for corpus IO between recreations: destroyed Activities
        // must never publish a late composition into a newer Activity.
        repeat(6) { rule.activityRule.scenario.recreate() }
        rule.awaitHifzReady()
        org.junit.Assert.assertEquals(expected, store.session())
        rule.onNodeWithText("الإعدادات").performClick(); rule.waitForIdle()
        click("حذف تقدم الحفظ من الجهاز"); click("حذف البيانات")
        rule.awaitHifzReady()
        org.junit.Assert.assertNull(store.session())
    }

    @Test fun interruptedDeletionRecoversBeforeRestoringTheOldAssistedAttempt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProgressStore(context)
        val notebook = TeacherNotebook(context)
        notebook.addStudent("بيانات اصطناعية للاستعادة", true)
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        org.junit.Assert.assertTrue(store.session()!!.hints > 0)
        rule.runOnUiThread {
            check(RuntimeResetStore(context).begin())
            check(store.clear()) // Interruption before the second store is cleared.
        }
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        org.junit.Assert.assertNull(store.session())
        org.junit.Assert.assertTrue(notebook.students().isEmpty())
        org.junit.Assert.assertFalse(RuntimeResetStore(context).pending)
        rule.onNodeWithText("افتح المصحف الكامل • ١١٤ سورة").assertExists()
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        org.junit.Assert.assertEquals(0, store.session()!!.hints)
        org.junit.Assert.assertFalse(store.session()!!.exposed)
        val fresh = store.session()
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        org.junit.Assert.assertEquals(fresh, store.session())
    }

    @Test fun persistentResetRejectsSavedAttemptFromAnOlderDataGeneration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProgressStore(context)
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        org.junit.Assert.assertNotNull(store.session())
        rule.runOnUiThread {
            check(store.clear())
            check(RuntimeResetStore(context).advance())
        }
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        org.junit.Assert.assertNull(store.session())
        rule.onNodeWithText("افتح المصحف الكامل • ١١٤ سورة").assertExists()
        rule.onNodeWithText("الحفظ").performClick(); rule.waitForIdle()
        org.junit.Assert.assertEquals(0, store.session()!!.hints)
        org.junit.Assert.assertFalse(store.session()!!.exposed)
    }

    @Test
    fun launchAndOpenFullOfflineMushaf() {
        rule.waitForIdle()
        rule.onNodeWithText("حِفْظ | NEXVARY").assertExists()
        screenshot("hifz-home.png")
        click("افتح المصحف الكامل • ١١٤ سورة")
        rule.waitForIdle()
        rule.onNodeWithText("المصحف الشريف").assertExists()
        rule.onNodeWithText("اختيار السورة • الفاتحة").assertExists()
        screenshot("hifz-reader.png")
    }
    @Test fun multipleBookmarksSurviveRecreationNavigateAcrossChaptersAndDeleteOnlyThePlace() {
        val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.bookmark(1, 2)
        click("افتح المصحف الكامل • ١١٤ سورة")
        click("أضف علامة للآية 1")
        click("أضف علامة للآية 1")
        click("البحث في القرآن والانتقال المباشر")
        rule.onNodeWithText("سورة:آية مثل ٢:٢٥٥").performTextInput("٢:٢٥٥")
        click("انتقل إلى الآية")
        click("أضف علامة للآية 255")
        click("العلامات المرجعية")
        rule.onNodeWithText("العلامات المرجعية • 2 من 50").assertExists()
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        rule.onNodeWithText("العلامات المرجعية • 2 من 50").assertExists()
        screenshot("hifz-saved-places.png")
        click("رجوع")
        rule.onNodeWithText("اختيار السورة • البقرة").assertExists()
        click("العلامات المرجعية")
        click("حذف علامة الفاتحة • الآية 1")
        rule.onNodeWithText("العلامات المرجعية • 1 من 50").assertExists()
        org.junit.Assert.assertEquals(1 to 2, store.bookmark())
        click("انتقل إلى البقرة • الآية 255")
        rule.onNodeWithText("اختيار السورة • البقرة").assertExists()
        org.junit.Assert.assertEquals(listOf(2255), store.savedPlaces(
            com.nexvary.hifzai.learning.QuranCorpus.load(InstrumentationRegistry.getInstrumentation().targetContext)
        ).map { it.id })
        click("العلامات المرجعية")
        click("حذف علامة البقرة • الآية 255")
        rule.onNodeWithText("لا توجد علامات بعد؛ أضف علامة من بطاقة الآية في المصحف").assertExists()
        val corpus = com.nexvary.hifzai.learning.QuranCorpus.load(
            InstrumentationRegistry.getInstrumentation().targetContext)
        corpus.verses.take(6).forEach { store.addSavedPlace(it.chapter, it.verse, corpus) }
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        click("العلامات التالية")
        rule.onNodeWithText("صفحة 2 من 2").assertExists()
        click("حذف علامة الفاتحة • الآية 6")
        rule.onNodeWithText("العلامات المرجعية • 5 من 50").assertExists()
        rule.onNodeWithText("انتقل إلى الفاتحة • الآية 1").assertExists()
    }

    @Test
    fun navigateEveryPageAndCaptureRealScreens() {
        rule.waitForIdle()
        screenshot("hifz-home.png")
        click("تفاصيل الخطة اليومية والمراجعات")
        screenshot("hifz-today.png")
        rule.onNodeWithText("الرئيسية").performClick()
        click("تدريبات الاسترجاع وترتيب الآيات")
        screenshot("hifz-exercises.png")
        rule.onNodeWithText("الرئيسية").performClick()
        click("الحفظ بالأجزاء والصفحات")
        screenshot("hifz-partitions.png")
        rule.onNodeWithText("رقم الصفحة").performTextInput("604")
        click("ابدأ حفظ هذا النطاق")
        screenshot("hifz-range.png")
        rule.onNodeWithText("جلسة حفظ النطاق").assertExists()
        rule.onNodeWithText("بسملة افتتاح السورة • غير مرقمة").assertExists()
        click("تابع سُلّم حفظ النطاق")
        rule.onNodeWithText("المصحف").performClick()
        screenshot("hifz-reader.png")
        click("اختيار السورة • الفاتحة")
        screenshot("hifz-chapters.png")
        rule.onNodeWithText("رجوع").performClick()
        click("البحث في القرآن والانتقال المباشر")
        screenshot("hifz-search.png")
        rule.onNodeWithText("سورة:آية مثل ٢:٢٥٥").performTextInput("٢:٢٥٥")
        click("انتقل إلى الآية")
        rule.onNodeWithText("اختيار السورة • البقرة").assertExists()
        rule.onNodeWithText("الحفظ").performClick()
        screenshot("hifz-practice.png")
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        screenshot("hifz-recall.png")
        click("نسيت")
        rule.onNodeWithText("سورة البقرة • الآية 255 من 286").assertExists()
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تدرّب على الآيات المتشابهة لفظيًا")
        screenshot("hifz-similar.png")
        rule.onNodeWithText("تقدمي").performClick()
        screenshot("hifz-insights.png")
        rule.onNodeWithText("الإعدادات").performClick()
        screenshot("hifz-settings.png")
        listOf("الخصوصية وحماية الأطفال" to "privacy", "المصادر والتراخيص" to "sources",
            "دفتر المعلم المحلي" to "teachers", "المساعدة" to "help", "حول التطبيق" to "about").forEach { (label, name) ->
            click(label)
            screenshot("hifz-$name.png")
            if (name == "sources") {
                click("ترخيص كود NEXVARY MIT")
                rule.waitUntil(5000) {
                    rule.onAllNodesWithText("Copyright (c) 2026 NEXVARY", substring = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                click("إخفاء: ترخيص كود NEXVARY MIT")
                click("شروط Tanzil المحفوظة مع النص الأصلي")
                rule.waitUntil(5000) {
                    rule.onAllNodesWithText("CHANGING IT IS NOT ALLOWED", substring = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
                click("إخفاء: شروط Tanzil المحفوظة مع النص الأصلي")
            }
            rule.onNodeWithText("الإعدادات").performClick()
        }
        click("تفريغ صوتي محلي تجريبي")
        rule.onNodeWithText("ابدأ تسميعًا محليًا تجريبيًا").assertExists()
        rule.onAllNodesWithText("سُجل تقييم", substring = true).assertCountEquals(0)
        screenshot("hifz-asr.png")
        rule.onNodeWithText("الإعدادات").performClick()
        click("دليل القراء")
        screenshot("hifz-reciters.png")
    }

}
