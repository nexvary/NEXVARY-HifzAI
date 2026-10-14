package com.nexvary.hifzai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Before
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ProgressStore(context).clear()
        TeacherNotebook(context).clear()
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
            rule.onNodeWithText("الإعدادات").performClick()
        }
        click("دليل القراء")
        screenshot("hifz-reciters.png")
    }

}
