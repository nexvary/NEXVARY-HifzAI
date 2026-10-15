package com.nexvary.hifzai

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PrivateAudioUiTest {
    @get:Rule val microphone = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) { rule.onNodeWithText(text).performScrollTo().performClick(); rule.waitForIdle() }
    @Test fun recordControlAndEraseTemporaryAudioThroughActualUi() {
        rule.onNodeWithText("الحفظ").performClick()
        repeat(5) {
            if (rule.onAllNodesWithText("ابدأ تسجيل التسميع الخاص").fetchSemanticsNodes().isEmpty())
                click("المرحلة التالية في سُلّم الاسترجاع")
        }
        click("ابدأ تسجيل التسميع الخاص")
        rule.onNodeWithText("إيقاف التسجيل").assertExists()
        Thread.sleep(2500) // Actual emulator microphone recording, not a recitation-quality test.
        click("إيقاف التسجيل")
        click("خيارات السرعة والتكرار")
        repeat(3) { click("زيادة التكرار") }
        rule.onNodeWithText("تكرار إضافي بعد المرة الأولى: 3").assertExists()
        click("استمع لصوتك")
        click("إيقاف مؤقت")
        rule.onNodeWithText("استمع لصوتك").assertExists()
        click("إخفاء خيارات الاستماع")
        click("خيارات السرعة والتكرار")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.filesDir, "hifz-private-audio.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG,100,it))
        }
        click("احذف التسجيل")
        rule.onNodeWithText("تم حذف التسجيل المحلي").assertExists()
        assertFalse(File(context.cacheDir, "hifz_private_session.m4a").exists())
        assertTrue(rule.onAllNodesWithText("استمع لصوتك").fetchSemanticsNodes().isEmpty())
    }
}
