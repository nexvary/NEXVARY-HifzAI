package com.nexvary.hifzai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.LicensedRecitationPlayer
import com.nexvary.hifzai.learning.LocalAudioPlayer
import com.nexvary.hifzai.learning.RecitationRights
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.sin

/** Real Android MediaPlayer with generated test tones, NEVER a Quran recording or model evaluation. */
@RunWith(AndroidJUnit4::class)
class LocalAudioPlaybackTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun tone(name: String, durationMs: Int): File {
        val rate = 16000
        val samples = rate * durationMs / 1000
        val data = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + samples*2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples*2)
        repeat(samples) { i -> data.putShort((sin(2*Math.PI*440*i/rate)*1000).toInt().toShort()) }
        return File(context.cacheDir, name).apply { writeBytes(data.array()) }
    }
    private fun await(message: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis()+7000
        while (!condition() && System.currentTimeMillis()<end) Thread.sleep(25)
        assertTrue(message, condition())
    }
    @Test fun localPauseSeekSpeedAndRepeatsUseRealMediaPlayer() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val file = tone("playback-test.wav", 800)
        val player = LocalAudioPlayer(context)
        try {
            player.open(file)
            assertTrue(player.snapshot().durationMs >= 790)
            player.speed(0.75f)
            Thread.sleep(100)
            assertEquals(LocalAudioPlayer.Phase.READY, player.snapshot().phase)
            assertEquals(0, player.snapshot().positionMs)
            assertTrue(player.play())
            await("Audio must advance", { player.snapshot().positionMs > 80 })
            player.pause()
            player.speed(1.5f)
            val stoppedAt = player.snapshot().positionMs
            Thread.sleep(200)
            assertEquals(LocalAudioPlayer.Phase.PAUSED, player.snapshot().phase)
            assertTrue(kotlin.math.abs(player.snapshot().positionMs - stoppedAt) < 60)
            player.seek(100)
            await("Paused seek must finish", { kotlin.math.abs(player.snapshot().positionMs - 100) < 60 })
            assertEquals(LocalAudioPlayer.Phase.PAUSED, player.snapshot().phase)
            player.repeats(2)
            assertTrue(player.play())
            await("Three plays must end without an endless loop", { player.snapshot().phase == LocalAudioPlayer.Phase.COMPLETED })
            assertEquals(3, player.snapshot().completedPlays)
            player.seek(0)
            assertEquals(LocalAudioPlayer.Phase.PAUSED, player.snapshot().phase)
            assertTrue(player.play())
            await("A new cycle after seek must retain the repeat setting", { player.snapshot().phase == LocalAudioPlayer.Phase.COMPLETED })
            assertEquals(3, player.snapshot().completedPlays)
            player.close()
            assertEquals(LocalAudioPlayer.Phase.EMPTY, player.snapshot().phase)
            assertFalse(player.play())
        } finally { player.close(); file.delete(); scenario.close() }
    }

    @Test fun audioFocusLossPausesAndDoesNotResumeAutomatically() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val file = tone("focus-test.wav", 3000)
        val player = LocalAudioPlayer(context)
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val competitor = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }.build()
        try {
            player.open(file)
            assertTrue(player.play())
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(competitor))
            await("Focus loss must pause playback", { player.snapshot().phase == LocalAudioPlayer.Phase.PAUSED })
            manager.abandonAudioFocusRequest(competitor)
            Thread.sleep(250)
            assertEquals(LocalAudioPlayer.Phase.PAUSED, player.snapshot().phase)
        } finally { manager.abandonAudioFocusRequest(competitor); player.close(); file.delete(); scenario.close() }
    }

    @Test fun rightsHashAndFileErrorsFailClosedAndReleaseOldPlayback() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val file = tone("rights-test.wav", 1000)
        val invalidMedia = File(context.cacheDir, "invalid-media.bin").apply { writeText("not audio") }
        val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
        // Explicitly synthetic self-generated tone; not a real reciter/rights/committee approval.
        val rights = RecitationRights("synthetic-tone", "test-generator", "https://example.invalid/test-tone",
            sha, "Generated test waveform, no Quran content", "synthetic-test-fixture-only", "hafs-an-asim", true, true)
        val player = LicensedRecitationPlayer(context)
        try {
            player.open(file, rights)
            assertTrue(player.play())
            assertTrue(runCatching { player.open(file, rights.copy(sha256 = "0".repeat(64))) }.isFailure)
            assertEquals(LocalAudioPlayer.Phase.EMPTY, player.snapshot().phase)
            assertFalse(player.play())
            assertTrue(runCatching { player.open(file, rights.copy(permitsOffline = false)) }.isFailure)
            assertTrue(runCatching { player.open(file, rights, 21) }.isFailure)
            assertTrue(runCatching { player.speed(Float.NaN) }.isFailure)
            assertTrue(runCatching { player.open(File(context.cacheDir, "missing.wav"), rights) }.isFailure)
            val raw = LocalAudioPlayer(context)
            try {
                assertTrue(runCatching { raw.open(invalidMedia) }.isFailure)
                assertFalse(raw.play())
            } finally { raw.close() }
        } finally { player.close(); file.delete(); invalidMedia.delete(); scenario.close() }
    }
}
