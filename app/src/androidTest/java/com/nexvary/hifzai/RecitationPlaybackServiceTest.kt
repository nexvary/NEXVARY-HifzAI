package com.nexvary.hifzai

import android.app.NotificationManager
import android.app.ActivityManager
import android.content.Intent
import android.media.session.MediaController
import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.nexvary.hifzai.learning.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.LocalDate
import kotlin.math.sin

/** Generated WAV only. No external recording, participant or headset-hardware claim. */
@RunWith(AndroidJUnit4::class)
class RecitationPlaybackServiceTest {
    @get:Rule val serviceRule = ServiceTestRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun await(message: String, condition: () -> Boolean) {
        val end = android.os.SystemClock.elapsedRealtime() + 8000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < end) Thread.sleep(25)
        assertTrue(message, condition())
    }
    private fun notification() = context.getSystemService(NotificationManager::class.java)
        .activeNotifications.firstOrNull { it.id == RecitationPlaybackService.NOTIFICATION_ID }?.notification

    private fun fixture(durationMs: Int): Triple<RecitationCatalog, List<BoundRecitation>, File> {
        val rate = 16000
        val samples = rate * durationMs / 1000
        val bytes = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { i -> bytes.putShort((sin(2 * Math.PI * 440 * i / rate) * 1000).toInt().toShort()) }
        val file = File(context.cacheDir, "service-generated-wave.wav").apply { writeBytes(bytes.array()) }
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val rights = RecitationRights("service-test-wave", "test-generator", "https://example.invalid/generated-wave",
            hash, "Generated waveform; no Quran recording", "synthetic-test-only", "hafs-an-asim", true, true)
        val record = CatalogRecording(rights, RecordingAddress(1, 1), file.length(), "synthetic-reference-only")
        val catalog = RecitationCatalog.create(listOf(record), QuranCorpus.load(context), setOf("test-generator"))
        return Triple(catalog, listOf(catalog.bind(rights.recordingId, file, LocalDate.now().toEpochDay())), file)
    }
    private fun start(scenario: ActivityScenario<MainActivity>): RecitationPlaybackService.PlaybackBinder {
        scenario.onActivity { RecitationPlaybackService.start(it) }
        val binder = serviceRule.bindService(Intent(context, RecitationPlaybackService::class.java))
            as RecitationPlaybackService.PlaybackBinder
        await("Foreground media notification must be present") { notification() != null }
        return binder
    }

    @Test fun backgroundPlaybackMediaButtonsNotificationAndActivityRecreation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, file) = fixture(6000)
        val binder = start(scenario)
        try {
            main { binder.load(catalog, assets) }
            await("Prepared without automatic playback") { binder.snapshot().phase == LocalRecitationQueue.Phase.READY }
            val controller = MediaController(context, binder.sessionToken())
            controller.transportControls.play()
            await("MediaSession play starts actual player") { binder.snapshot().phase == LocalRecitationQueue.Phase.PLAYING }
            scenario.moveToState(Lifecycle.State.CREATED)
            val position = binder.snapshot().playback!!.positionMs
            await("Audio position advances with Activity stopped") {
                binder.snapshot().phase == LocalRecitationQueue.Phase.PLAYING && binder.snapshot().playback!!.positionMs > position + 200
            }
            // Dispatch real framework media-button events (not a physical headset test).
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            await("Media button pauses") { binder.snapshot().phase == LocalRecitationQueue.Phase.PAUSED }
            main { binder.speed(1.5f) }
            await("Paused speed updates without starting") { binder.snapshot().playback?.speed == 1.5f }
            assertEquals(LocalRecitationQueue.Phase.PAUSED, binder.snapshot().phase)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            assertEquals(LocalRecitationQueue.Phase.PAUSED, binder.snapshot().phase)
            await("Notification exposes actual paused action") { notification()?.actions?.firstOrNull()?.title == "تشغيل" }
            notification()!!.actions[0].actionIntent.send()
            await("Notification resumes playback") { binder.snapshot().phase == LocalRecitationQueue.Phase.PLAYING }
            assertEquals(1.5f, binder.snapshot().playback!!.speed)
            controller.transportControls.stop()
            await("Stop releases player and removes notification") {
                binder.snapshot().phase == LocalRecitationQueue.Phase.EMPTY && notification() == null
            }
        } finally { main { binder.stop() }; scenario.close(); file.delete() }
    }

    @Test fun finiteRangeCompletionStopsServiceAndReleasesNotification() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, file) = fixture(400)
        val binder = start(scenario)
        try {
            main { binder.load(catalog, assets, extraRangeRepeats = 1) }
            await("Ready") { binder.snapshot().phase == LocalRecitationQueue.Phase.READY }
            main { binder.play() }
            await("Second range pass is traversed") { binder.snapshot().rangePass == 2 }
            await("Finite playback removes notification and resources") {
                notification() == null && binder.snapshot().phase == LocalRecitationQueue.Phase.EMPTY
            }
        } finally { main { binder.stop() }; scenario.close(); file.delete() }
    }

    @Test fun tamperedBoundAudioFailsClosedAndRemovesForegroundNotification() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, file) = fixture(400)
        val binder = start(scenario)
        try {
            file.writeBytes(ByteArray(file.length().toInt()))
            main { binder.load(catalog, assets) }
            await("Tampered file must end service instead of playing") {
                notification() == null && binder.snapshot().phase == LocalRecitationQueue.Phase.EMPTY
            }
        } finally { main { binder.stop() }; scenario.close(); file.delete() }
    }

    @Test fun dataResetStopsPlaybackEvenWhileAnActivityKeepsTheBinder() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, file) = fixture(6000)
        val binder = start(scenario)
        try {
            main { binder.load(catalog, assets) }
            await("Ready") { binder.snapshot().phase == LocalRecitationQueue.Phase.READY }
            main { binder.play() }
            await("Playing") { binder.snapshot().phase == LocalRecitationQueue.Phase.PLAYING }
            main { RecitationPlaybackService.stopForDataReset() }
            await("Reset stops the bound service") {
                binder.snapshot().phase == LocalRecitationQueue.Phase.EMPTY && notification() == null
            }
            main { binder.play() }
            assertEquals(LocalRecitationQueue.Phase.EMPTY, binder.snapshot().phase)
        } finally { main { binder.stop() }; scenario.close(); file.delete() }
    }

    @Suppress("DEPRECATION") // Android exposes only this app's services to this caller.
    @Test fun staleNotificationCommandsCannotResurrectAnEmptyPlaybackService() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            for (action in listOf("PLAY", "PAUSE")) {
                scenario.onActivity {
                    it.startService(Intent(context, RecitationPlaybackService::class.java)
                        .setAction("com.nexvary.hifzai.recitation.$action"))
                }
                await("Stale notification action must stop its empty service") {
                    context.getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
                        .none { it.service.className == RecitationPlaybackService::class.java.name }
                }
                assertNull(notification())
            }
        } finally { context.stopService(Intent(context, RecitationPlaybackService::class.java)); scenario.close() }
    }
}
