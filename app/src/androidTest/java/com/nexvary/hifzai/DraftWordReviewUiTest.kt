package com.nexvary.hifzai

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftWordReviewUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun syntheticDraftPositionsAppearOnlyOnRequest() {
        rule.setContent {
            Column {
                DraftWordReview("ألف باء جيم دال هاء", "ألف، ألف باء جيم هاء")
            }
        }
        val omission = "حذف محتمل · كلمات الآية: 4"
        val repetition = "تكرار محتمل · كلمات المسودة: 1"
        rule.onNodeWithText(omission).assertDoesNotExist()
        rule.onNodeWithText("عرض مواضع المراجعة المحتملة").performClick()
        rule.onNodeWithText("مواضع غير مؤكدة:", substring = true).assertExists()
        rule.onNodeWithText(omission).assertExists()
        rule.onNodeWithText(repetition).assertExists()
        rule.onNodeWithText("إخفاء مواضع المراجعة").performClick()
        rule.onNodeWithText(repetition).assertDoesNotExist()
    }
}
