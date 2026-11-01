package com.nexvary.hifzai

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    private fun click(text: String) { rule.onNodeWithText(text).performScrollTo().performClick(); rule.waitForIdle() }
    @Before fun reset() {
        ProgressStore(context).clear(); TeacherNotebook(context).clear()
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
    }
    @Test fun goalAndFavoritePersistAndDeletionResetsRuntimeSession() {
        click("الحفظ")
        repeat(3) { click("المرحلة التالية في سُلّم الاسترجاع") }
        click("تلميح")
        assertEquals(1, ProgressStore(context).session()?.hints)
        click("الإعدادات")
        click("مراجعة فقط")
        assertEquals(StudyMode.REVIEW_ONLY, ProgressStore(context).learningGoal.mode)
        click("دليل القراء")
        rule.onAllNodesWithText("اختيار قارئ مفضل")[0].performScrollTo().performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.awaitHifzReady()
        assertEquals(StudyMode.REVIEW_ONLY, ProgressStore(context).learningGoal.mode)
        assertEquals(ReciterCatalog.all.first().slug, ProgressStore(context).favoriteReciter)
        click("الإعدادات")
        click("حذف تقدم الحفظ من الجهاز")
        click("حذف البيانات")
        rule.awaitHifzReady()
        assertNull(ProgressStore(context).session())
        assertNull(ProgressStore(context).favoriteReciter)
        assertEquals(LearningGoal(), ProgressStore(context).learningGoal)
        click("الحفظ")
        val session = ProgressStore(context).session()!!
        assertEquals(PracticeKind.READ, session.stage)
        assertEquals(0, session.hints)
        assertFalse(session.exposed)
        assertEquals(0, session.rangeStart)
    }
}
