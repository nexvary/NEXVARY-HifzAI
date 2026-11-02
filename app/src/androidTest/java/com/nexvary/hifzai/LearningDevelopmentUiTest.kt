package com.nexvary.hifzai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        click("دليل القراء")
        rule.onAllNodesWithText("اختيار قارئ مفضل")[0].performScrollTo().performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        assertEquals(StudyMode.REVIEW_ONLY, ProgressStore(context).learningGoal.mode)
        assertEquals(ReciterCatalog.all.first().slug, ProgressStore(context).favoriteReciter)
        assertTrue(ProgressStore(context).gentleReturnEnabled)
        click("الإعدادات")
        click("حذف تقدم الحفظ من الجهاز")
        click("حذف البيانات")
        rule.awaitHifzReady()
        assertNull(ProgressStore(context).session())
        assertNull(ProgressStore(context).favoriteReciter)
        assertEquals(LearningGoal(), ProgressStore(context).learningGoal)
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
}
