package com.nexvary.hifzai

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.AppAppearance
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DraftWordReviewUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun syntheticDraftPositionsAppearOnlyOnRequest() {
        rule.setContent {
            HifzTheme(AppAppearance.DARK) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("اختبار ببيانات نصية اصطناعية")
                            DraftWordReview("ألف باء جيم دال هاء", "ألف، ألف باء جيم هاء")
                        }
                    }
                }
            }
        }
        val omission = "حذف محتمل · كلمات الآية: 4"
        val repetition = "تكرار محتمل · كلمات المسودة: 1"
        rule.onNodeWithText(omission).assertDoesNotExist()
        rule.onNodeWithText("عرض مواضع المراجعة المحتملة").performClick()
        rule.onNodeWithText("مواضع غير مؤكدة:", substring = true).assertExists()
        rule.onNodeWithText(omission).assertExists()
        rule.onNodeWithText(repetition).assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "hifz-asr-draft-review.png").outputStream().use {
            check(rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        rule.onNodeWithText("إخفاء مواضع المراجعة").performClick()
        rule.onNodeWithText(repetition).assertDoesNotExist()
    }
}
