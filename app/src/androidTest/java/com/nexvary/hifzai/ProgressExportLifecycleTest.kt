package com.nexvary.hifzai

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ProgressExportLifecycleTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun recreatingWhileWritingKeepsOneExportAndItsFinalFeedback() {
        rule.awaitHifzReady()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val payload = "اختبار حفظ مؤقت\r\n"
        val destination = object : ByteArrayOutputStream() {
            override fun write(bytes: ByteArray) {
                entered.countDown()
                check(release.await(20, TimeUnit.SECONDS))
                super.write(bytes)
            }
            override fun close() { super.close(); closed.countDown() }
        }
        lateinit var original: ProgressExportViewModel
        try {
            rule.activityRule.scenario.onActivity { activity ->
                original = ViewModelProvider(activity)[ProgressExportViewModel::class.java]
                original.clearForReset()
                assertTrue(original.beginPicking())
                original.write({ payload }) { destination }
                assertFalse(original.beginPicking())
            }
            assertTrue("Writer did not start", entered.await(10, TimeUnit.SECONDS))
            rule.activityRule.scenario.recreate()
            rule.awaitHifzReady()
            rule.activityRule.scenario.onActivity { activity ->
                val retained = ViewModelProvider(activity)[ProgressExportViewModel::class.java]
                assertSame(original, retained)
                assertEquals(ProgressExportViewModel.Status.WRITING, retained.status.value)
                assertFalse(retained.beginPicking())
            }
            rule.onNodeWithText("الإعدادات").performClick()
            rule.onNodeWithText("جارٍ حفظ ملف التقدم…").performScrollTo().assertExists()
            release.countDown()
            assertTrue("Writer did not close", closed.await(10, TimeUnit.SECONDS))
            rule.waitUntil(10000) { original.status.value == ProgressExportViewModel.Status.SAVED }
            rule.onNodeWithText("حُفظ ملف التقدم").performScrollTo().assertExists()
            assertArrayEquals(payload.toByteArray(Charsets.UTF_8), destination.toByteArray())
        } finally {
            release.countDown()
            rule.activityRule.scenario.onActivity { original.clearForReset() }
        }
    }

    @Test fun resetDiscardsALateExportResult() {
        rule.awaitHifzReady()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closed = CountDownLatch(1)
        lateinit var export: ProgressExportViewModel
        try {
            rule.activityRule.scenario.onActivity { activity ->
                export = ViewModelProvider(activity)[ProgressExportViewModel::class.java]
                export.write({ "synthetic counters" }) {
                    entered.countDown()
                    check(release.await(20, TimeUnit.SECONDS))
                    object : ByteArrayOutputStream() {
                        override fun close() { super.close(); closed.countDown() }
                    }
                }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            rule.activityRule.scenario.onActivity {
                export.clearForReset()
                assertEquals(ProgressExportViewModel.Status.IDLE, export.status.value)
            }
            release.countDown()
            assertTrue(closed.await(10, TimeUnit.SECONDS))
            rule.waitForIdle()
            assertEquals(ProgressExportViewModel.Status.IDLE, export.status.value)
        } finally {
            release.countDown()
        }
    }
}
