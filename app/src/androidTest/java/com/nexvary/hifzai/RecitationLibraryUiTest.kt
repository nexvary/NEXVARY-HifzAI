package com.nexvary.hifzai

import android.app.NotificationManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RecitationLibraryUiTest {
    @get:Rule val rule = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val playback = RecitationLibraryViewModel()
    private lateinit var directory: File

    private fun render(durationMs: Int, missing: Boolean = false) {
        val rate = 16000
        val samples = rate * durationMs / 1000
        val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { i -> buffer.putShort((if ((i / 20) % 2 == 0) 500 else -500).toShort()) }
        val wave = buffer.array()
        val hash = MessageDigest.getInstance("SHA-256").digest(wave)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val reader = ReciterProfile("test-generator", "نغمات اختبار مولّدة", ReciterCountry.EGYPT)
        val record = CatalogRecording(RecitationRights("ui-generated-wave", reader.slug,
            "https://example.invalid/generated-wave", hash, "Generated PCM", "synthetic-test-only",
            "hafs-an-asim", true, true), RecordingAddress(1, 1), wave.size.toLong(), "synthetic-reference-only")
        val corpus = QuranCorpus.load(context)
        directory = File(context.cacheDir, "recitation-ui-${System.nanoTime()}")
        val library = LocalRecitationLibrary(
            RecitationCatalog.create(listOf(record), corpus, setOf(reader.slug)), directory
        ) {
            if (missing) throw FileNotFoundException("Synthetic missing asset")
            ByteArrayInputStream(wave)
        }
        rule.setContent {
            HifzTheme(AppAppearance.DARK) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text("اختبار بنغمات اصطناعية")
                        RecitationRangePreview(corpus, reader, corpus.verses.first(),
                            LocalDate.now().toEpochDay(), library, playback)
                    }
                }
            }
        }
        click("تحقق من توفر التلاوة")
        click("تجهيز النطاق")
    }

    private fun click(label: String) = rule.onNodeWithText(label).performScrollTo().performClick()

    private fun await(phase: LocalRecitationQueue.Phase) {
        rule.waitUntil(12000) { playback.state.value.playback?.phase == phase }
    }

    @After fun cleanup() {
        instrumentation.runOnMainSync { playback.clear() }
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    @Test fun preparesThenPlaysPausesChangesSpeedResumesAndClears() {
        render(20000)
        await(LocalRecitationQueue.Phase.READY)
        click("تشغيل النطاق المجهز")
        await(LocalRecitationQueue.Phase.PLAYING)
        rule.waitUntil(8000) { (playback.state.value.playback?.playback?.positionMs ?: 0) > 100 }
        click("إيقاف التلاوة مؤقتًا")
        await(LocalRecitationQueue.Phase.PAUSED)
        rule.onNodeWithContentDescription("سرعة التلاوة الحالية").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1.5f) }
        rule.waitUntil(8000) { playback.state.value.playback?.playback?.speed == 1.5f }
        assertEquals(LocalRecitationQueue.Phase.PAUSED, playback.state.value.playback?.phase)
        click("استئناف التلاوة")
        await(LocalRecitationQueue.Phase.PLAYING)
        click("إيقاف التلاوة ومسح القائمة")
        rule.waitUntil(8000) {
            context.getSystemService(NotificationManager::class.java).activeNotifications
                .none { it.id == RecitationPlaybackService.NOTIFICATION_ID }
        }
    }

    @Test fun completionRemainsVisibleAfterTheServiceReleasesPlayback() {
        render(300)
        await(LocalRecitationQueue.Phase.READY)
        click("تشغيل النطاق المجهز")
        rule.waitUntil(12000) { playback.state.value.message == "اكتمل تشغيل النطاق" }
        rule.onNodeWithText("اكتمل تشغيل النطاق").performScrollTo().assertExists()
    }

    @Test fun missingAssetShowsAnErrorAndOffersNoPlayButton() {
        render(300, missing = true)
        rule.waitUntil(12000) { playback.state.value.message == "تعذر قراءة ملفات التلاوة أو تجهيزها" }
        rule.onNodeWithText("تعذر قراءة ملفات التلاوة أو تجهيزها").performScrollTo().assertExists()
        rule.onNodeWithText("تشغيل النطاق المجهز").assertDoesNotExist()
    }
}
