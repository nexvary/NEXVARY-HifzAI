package com.nexvary.hifzai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
        rule.onNodeWithText("افتح المصحف الكامل • ١١٤ سورة").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("المصحف الشريف").assertExists()
        rule.onNodeWithText("اختيار السورة • الفاتحة").assertExists()
        screenshot("hifz-reader.png")
    }
}
