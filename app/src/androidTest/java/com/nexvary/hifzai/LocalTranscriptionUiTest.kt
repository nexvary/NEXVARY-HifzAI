package com.nexvary.hifzai

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.FileObserver
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Actual AudioRecord + background transition. No human speech or accuracy claim. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LocalTranscriptionUiTest {
    @get:Rule val microphone = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) { rule.onNodeWithText(text).performScrollTo().performClick(); rule.waitForIdle() }
    private fun has(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test fun microphoneOrExplicitResourceRefusalStaysPrivateAndStopsInBackground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        rule.onNodeWithText("الإعدادات").performClick()
        click("تفريغ صوتي محلي تجريبي")
        val before = context.getSharedPreferences("hifz_progress_v1", Context.MODE_PRIVATE).all.toMap()
        val cacheBefore = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(context.cacheDir).path }.toSet()
        val recordingBefore = audio.activeRecordingConfigurations.size
        val cacheCreated = AtomicBoolean(false)
        @Suppress("DEPRECATION")
        val observer = object : FileObserver(context.cacheDir.absolutePath, FileObserver.CREATE or FileObserver.MOVED_TO) {
            override fun onEvent(event: Int, path: String?) { cacheCreated.set(true) }
        }
        observer.startWatching()
        try {
            click("ابدأ تسميعًا محليًا تجريبيًا")
            val recordingLabel = "الميكروفون يعمل الآن • الصوت في الذاكرة المؤقتة فقط"
            val resourceLabel = "الذاكرة المتاحة لا تكفي لتشغيل النموذج بأمان الآن"
            rule.waitUntil(7000) { has(recordingLabel) || has(resourceLabel) }
            val captured = has(recordingLabel)
            if (captured) {
                // Emitted only after AudioRecord reports RECORDSTATE_RECORDING.
                SystemClock.sleep(900)
                rule.onNodeWithText("أوقف التسجيل وافرغ محليًا").assertExists()
            } else rule.onNodeWithText(resourceLabel).assertExists()
            val image = rule.onRoot().captureToImage().asAndroidBitmap()
            File(context.filesDir,"hifz-asr-microphone.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG,100,it))
            }
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            val deadline = SystemClock.elapsedRealtime()+5000
            while (audio.activeRecordingConfigurations.size > recordingBefore && SystemClock.elapsedRealtime()<deadline)
                SystemClock.sleep(50)
            assertTrue(audio.activeRecordingConfigurations.size <= recordingBefore)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.waitForIdle()
            rule.onNodeWithText("لا تسجيل ولا تفريغ محفوظ في هذه الصفحة.").assertExists()
            rule.onNodeWithText("ابدأ تسميعًا محليًا تجريبيًا").assertExists()
            assertEquals(before,context.getSharedPreferences("hifz_progress_v1",Context.MODE_PRIVATE).all)
            val cacheAfter = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(context.cacheDir).path }.toSet()
            assertEquals("ASR must not create a voice file",cacheBefore,cacheAfter)
            assertFalse("No cache file may be created even if later deleted",cacheCreated.get())
            assertFalse(File(context.cacheDir,"hifz_private_session.m4a").exists())
            File(context.filesDir,"hifz-asr-microphone.json").writeText(JSONObject()
                .put("microphone_started",captured).put("explicit_memory_guard",!captured)
                .put("background_returns_to_idle",true).put("learning_data_unchanged",true)
                .put("no_new_cache_files",true).put("no_cache_create_events",true)
                .put("recordings_before",recordingBefore).put("recordings_after",audio.activeRecordingConfigurations.size)
                .put("real_human_speech_samples",0).put("accuracy_evaluated",false).toString(2))
        } finally { observer.stopWatching() }
    }
}
