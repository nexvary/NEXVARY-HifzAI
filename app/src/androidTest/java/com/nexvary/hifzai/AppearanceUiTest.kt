package com.nexvary.hifzai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.AppAppearance
import com.nexvary.hifzai.learning.ProgressStore
import com.nexvary.hifzai.learning.QuranCorpus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppearanceUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun click(label: String) {
        if (label in setOf("الحفظ", "المصحف", "الإعدادات")) rule.onNodeWithText(label).performClick()
        else rule.onNodeWithText(label).performScrollTo().performClick()
        rule.waitForIdle()
    }
    private fun screenshot(name: String) {
        File(context.filesDir, "hifz-$name.png").outputStream().use {
            check(rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
    @Test fun everyNavigationLabelFitsAndAllDestinationsRemainReachable() {
        rule.awaitHifzReady()
        for (label in listOf("الرئيسية", "الحفظ", "المصحف", "تقدمي", "الإعدادات")) {
            val node = rule.onNodeWithText(label)
            node.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Missing label layout: $label", layouts.isNotEmpty())
            assertTrue("Wrapped or clipped navigation: $label", layouts.all {
                it.lineCount == 1 && !it.hasVisualOverflow
            })
            node.performClick()
            rule.waitForIdle()
            rule.onNodeWithText(label).assertIsSelected()
        }
        screenshot("navigation")
    }
    @Test fun themesPreserveAttemptAcrossLiveChangesRecreationAndConfirmedReset() {
        rule.awaitHifzReady()
        click("الإعدادات"); click("حذف تقدم الحفظ من الجهاز"); click("حذف البيانات")
        rule.awaitHifzReady()
        click("الحفظ")
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        val store = ProgressStore(context)
        val attempt = store.session()!!
        val beforeHistory = store.history(1001)
        click("الإعدادات"); click("فاتح")
        rule.onNodeWithText("فاتح").assertIsSelected()
        assertEquals(AppAppearance.LIGHT, store.appearance)
        assertEquals(attempt, store.session())
        assertEquals(beforeHistory, store.history(1001))
        rule.onNodeWithText("المظهر الحالي: فاتح").assertExists()
        rule.onNodeWithText("مظهر التطبيق").performScrollTo()
        screenshot("appearance-settings")
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        assertEquals(AppAppearance.LIGHT, store.appearance)
        assertEquals(attempt, store.session())
        click("المصحف")
        rule.onNodeWithContentDescription("الآية 1").assertExists()
        val corpus = QuranCorpus.load(context)
        val firstAyah = corpus.ayahText(corpus.find(1, 1)!!)
        rule.onNodeWithText(firstAyah).performScrollTo()
        screenshot("reader-light")
        click("الإعدادات"); click("داكن")
        assertEquals(AppAppearance.DARK, store.appearance)
        assertEquals(attempt, store.session())
        click("المصحف")
        rule.onNodeWithText(firstAyah).performScrollTo()
        screenshot("reader-dark")
        click("الإعدادات"); click("حسب النظام")
        rule.activityRule.scenario.recreate(); rule.awaitHifzReady()
        assertEquals(AppAppearance.SYSTEM, store.appearance)
        assertEquals(attempt, store.session())
        click("حذف تقدم الحفظ من الجهاز"); click("حذف البيانات"); rule.awaitHifzReady()
        assertEquals(AppAppearance.DARK, store.appearance)
        assertNull(store.session())
        click("الإعدادات")
        rule.onNodeWithText("المظهر الحالي: داكن").assertExists()
    }
}
