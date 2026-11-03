package com.nexvary.hifzai

import android.content.Context
import android.content.ContextWrapper
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.LicensedRecitationPlayer
import com.nexvary.hifzai.learning.LocalAudioPlayer
import com.nexvary.hifzai.learning.RecitationRights
import com.nexvary.hifzai.learning.RecordingAddress
import com.nexvary.hifzai.learning.CatalogRecording
import com.nexvary.hifzai.learning.RecitationCatalog
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.BoundRecitation
import com.nexvary.hifzai.learning.LocalRecitationQueue
import androidx.lifecycle.Lifecycle
import java.time.LocalDate
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
    /** Capture the actual registered receiver; simulate delivery, never forge a protected system broadcast. */
    @Suppress("UnspecifiedRegisterReceiverFlag") // The production player uses this overload only before API 33.
    private class NoisyReceiverContext(base: Context) : ContextWrapper(base) {
        @Volatile private var receiver: BroadcastReceiver? = null
        override fun getApplicationContext(): Context = this
        override fun registerReceiver(candidate: BroadcastReceiver?, filter: IntentFilter): Intent? {
            val result = super.registerReceiver(candidate, filter)
            if (filter.hasAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) receiver = candidate
            return result
        }
        override fun registerReceiver(candidate: BroadcastReceiver?, filter: IntentFilter, flags: Int): Intent? {
            val result = super.registerReceiver(candidate, filter, flags)
            if (filter.hasAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) receiver = candidate
            return result
        }
        override fun unregisterReceiver(candidate: BroadcastReceiver) {
            super.unregisterReceiver(candidate)
            if (receiver === candidate) receiver = null
        }
        fun deliverNoisy() = checkNotNull(receiver).onReceive(this, Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }
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
    private fun queueFixture(durationMs: Int): Triple<RecitationCatalog, List<BoundRecitation>, List<File>> {
        val today = LocalDate.now().toEpochDay()
        val files = (0..2).map { tone("queue-$it.wav", durationMs) }
        val addresses = listOf(RecordingAddress(1, 6), RecordingAddress(1, 7), RecordingAddress(2, 1))
        val records = files.mapIndexed { i, file ->
            val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            val rights = RecitationRights("synthetic-queue-$i", "test-generator", "https://example.invalid/test-tone",
                sha, "Generated waveform, no Quran content", "synthetic-fixture-only", "hafs-an-asim", true, true)
            CatalogRecording(rights, addresses[i], file.length(), "synthetic-reference-only")
        }
        val catalog = RecitationCatalog.create(records, QuranCorpus.load(context), setOf("test-generator"))
        return Triple(catalog, records.mapIndexed { i, record -> catalog.bind(record.rights.recordingId, files[i], today) }, files)
    }

    @Test fun foregroundSequenceRepeatsEachItemCrossesSurahAndStopsAtTheEnd() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(450)
        lateinit var queue: LocalRecitationQueue
        scenario.onActivity { queue = LocalRecitationQueue(context, it.lifecycle) }
        try {
            scenario.onActivity { queue.load(catalog, assets, extraRepeats = 1, playbackSpeed = 1.25f) }
            await("First file must be prepared without starting", { queue.snapshot().phase == LocalRecitationQueue.Phase.READY })
            Thread.sleep(100)
            assertEquals(0, queue.snapshot().playback!!.positionMs)
            assertEquals(RecordingAddress(1, 6), queue.snapshot().address)
            scenario.onActivity { queue.play() }
            await("All three files must finish", { queue.snapshot().phase == LocalRecitationQueue.Phase.COMPLETED })
            val final = queue.snapshot()
            assertEquals(3, final.completedItems); assertEquals(2, final.index)
            assertEquals(RecordingAddress(2, 1), final.address)
            val playback = checkNotNull(final.playback)
            assertEquals(2, playback.completedPlays)
            assertEquals(1.25f, playback.speed, 0f)
            scenario.onActivity { queue.play() } // Finished selection does not silently loop.
            Thread.sleep(200)
            assertEquals(LocalRecitationQueue.Phase.COMPLETED, queue.snapshot().phase)
            scenario.onActivity { queue.clear() }
            await("Clear must release the selection", { queue.snapshot().phase == LocalRecitationQueue.Phase.EMPTY })
        } finally { scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
    }

    @Test fun wholeRangeRepetitionRevalidatesWrappedFilesAndStopsAfterTheChosenPasses() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(300)
        lateinit var queue: LocalRecitationQueue
        scenario.onActivity { queue = LocalRecitationQueue(context, it.lifecycle) }
        try {
            scenario.onActivity { queue.load(catalog, assets, extraRepeats = 1,
                playbackSpeed = 1.5f, extraRangeRepeats = 1); queue.play() }
            await("Two complete range passes must end", { queue.snapshot().phase == LocalRecitationQueue.Phase.COMPLETED })
            val final = queue.snapshot()
            assertEquals(6, final.completedItems)
            assertEquals(2, final.rangePass); assertEquals(2, final.rangePasses)
            assertEquals(2, final.playback!!.completedPlays)
            assertEquals(RecordingAddress(2, 1), final.address)
            scenario.onActivity { queue.play() }
            Thread.sleep(150)
            assertEquals(6, queue.snapshot().completedItems)
        } finally { scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
    }

    @Test fun wrappedRangeRechecksFirstFileBeforePlayingItAgain() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(600)
        lateinit var queue: LocalRecitationQueue
        scenario.onActivity { queue = LocalRecitationQueue(context, it.lifecycle) }
        try {
            scenario.onActivity { queue.load(catalog, assets, extraRangeRepeats = 1); queue.play() }
            await("Second ayah proves the first file has been released", { queue.snapshot().index == 1 })
            val original = files[0].readBytes()
            files[0].writeBytes(original.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() })
            await("Wrapped first file must fail validation", { queue.snapshot().phase == LocalRecitationQueue.Phase.FAILED })
            assertEquals(3, queue.snapshot().completedItems)
            assertEquals(0, queue.snapshot().index)
            assertEquals(2, queue.snapshot().rangePass)
            assertEquals(LocalRecitationQueue.Failure.FILE_OR_PLAYBACK, queue.snapshot().failure)
            assertEquals(LocalAudioPlayer.Phase.EMPTY, queue.snapshot().playback!!.phase)
        } finally { scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
    }

    @Test fun changingQueueSpeedPreservesPauseAndRejectsInvalidInputWithoutStartingAudio() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(1000)
        lateinit var queue: LocalRecitationQueue
        scenario.onActivity { queue = LocalRecitationQueue(context, it.lifecycle) }
        try {
            scenario.onActivity { queue.load(catalog, assets) }
            await("Prepare without starting", { queue.snapshot().phase == LocalRecitationQueue.Phase.READY })
            scenario.onActivity { queue.setPlaybackSpeed(0.75f) }
            await("Speed applied while ready", { queue.snapshot().playback?.speed == 0.75f })
            assertEquals(LocalRecitationQueue.Phase.READY, queue.snapshot().phase)
            assertEquals(0, queue.snapshot().playback!!.positionMs)
            scenario.onActivity { queue.play() }
            await("Must play", { (queue.snapshot().playback?.positionMs ?: 0) > 80 })
            scenario.onActivity { queue.setPlaybackSpeed(1.1f) }
            await("Live speed change must keep playing", { queue.snapshot().playback?.speed == 1.1f })
            assertEquals(LocalRecitationQueue.Phase.PLAYING, queue.snapshot().phase)
            scenario.onActivity { queue.pause() }
            await("Must pause", { queue.snapshot().phase == LocalRecitationQueue.Phase.PAUSED })
            val position = queue.snapshot().playback!!.positionMs
            scenario.onActivity { queue.setPlaybackSpeed(1.25f) }
            await("Speed applied while paused", { queue.snapshot().playback?.speed == 1.25f })
            assertEquals(LocalRecitationQueue.Phase.PAUSED, queue.snapshot().phase)
            Thread.sleep(150)
            assertTrue(kotlin.math.abs(position - queue.snapshot().playback!!.positionMs) < 60)
            scenario.onActivity {
                for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0.49f, 1.51f))
                    assertTrue(runCatching { queue.setPlaybackSpeed(value) }.isFailure)
            }
            assertEquals(1.25f, queue.snapshot().playback!!.speed, 0f)
            scenario.onActivity { queue.clear() }
            await("Clear", { queue.snapshot().phase == LocalRecitationQueue.Phase.EMPTY })
            assertEquals(0, queue.snapshot().rangePasses)
            scenario.onActivity { assertTrue(runCatching { queue.load(catalog, assets, extraRangeRepeats = 21) }.isFailure) }
            await("Invalid range must fail closed", { queue.snapshot().failure == LocalRecitationQueue.Failure.INVALID_SELECTION })
        } finally { scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
    }

    @Test fun sequencePauseFocusLossAndBackgroundNeverResumeWithoutAnExplicitRequest() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(2000)
        lateinit var queue: LocalRecitationQueue
        val receiverContext = NoisyReceiverContext(context)
        scenario.onActivity { queue = LocalRecitationQueue(receiverContext, it.lifecycle) }
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val competitor = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { }.build()
        try {
            scenario.onActivity { queue.load(catalog, assets); queue.play() }
            await("Sequence must advance", { (queue.snapshot().playback?.positionMs ?: 0) > 80 })
            scenario.onActivity { queue.pause() }
            await("Pause must be observed", { queue.snapshot().phase == LocalRecitationQueue.Phase.PAUSED })
            val position = queue.snapshot().playback!!.positionMs
            Thread.sleep(200)
            assertTrue(kotlin.math.abs(queue.snapshot().playback!!.positionMs - position) < 60)
            scenario.onActivity { queue.play() }
            await("Explicit resume must play", { queue.snapshot().phase == LocalRecitationQueue.Phase.PLAYING })
            assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(competitor))
            await("Focus loss must stop the queue", { queue.snapshot().phase == LocalRecitationQueue.Phase.PAUSED })
            manager.abandonAudioFocusRequest(competitor)
            Thread.sleep(200)
            assertEquals(LocalRecitationQueue.Phase.PAUSED, queue.snapshot().phase)
            scenario.onActivity { queue.play() }
            await("Explicit resume after focus loss", { queue.snapshot().phase == LocalRecitationQueue.Phase.PLAYING })
            val interruptions = queue.snapshot().playback!!.interruptions
            scenario.onActivity { receiverContext.deliverNoisy() }
            await("Noisy-output interruption must pause the entire queue", { queue.snapshot().phase == LocalRecitationQueue.Phase.PAUSED })
            assertTrue(queue.snapshot().playback!!.interruptions > interruptions)
            scenario.onActivity { queue.play() }
            await("Explicit resume after noisy output", { queue.snapshot().phase == LocalRecitationQueue.Phase.PLAYING })
            scenario.moveToState(Lifecycle.State.CREATED)
            await("Background clears selection and releases playback", { queue.snapshot().phase == LocalRecitationQueue.Phase.EMPTY })
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { queue.play() }
            Thread.sleep(200)
            assertEquals(LocalRecitationQueue.Phase.EMPTY, queue.snapshot().phase)
            scenario.onActivity { queue.close(); queue.close() }
            scenario.onActivity { assertTrue(runCatching { queue.load(catalog, assets) }.isFailure) }
        } finally { manager.abandonAudioFocusRequest(competitor); scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
    }

    @Test fun sequenceRejectsInvalidSelectionAndStopsBeforeAMutatedLaterFile() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val (catalog, assets, files) = queueFixture(450)
        lateinit var queue: LocalRecitationQueue
        scenario.onActivity { queue = LocalRecitationQueue(context, it.lifecycle) }
        try {
            scenario.onActivity { assertTrue(runCatching { queue.load(catalog, assets.reversed()) }.isFailure) }
            await("Invalid selection must fail closed", { queue.snapshot().phase == LocalRecitationQueue.Phase.FAILED })
            assertEquals(LocalRecitationQueue.Failure.INVALID_SELECTION, queue.snapshot().failure)
            val original = files[1].readBytes()
            files[1].writeBytes(original.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() })
            scenario.onActivity { queue.load(catalog, assets); queue.play() }
            await("Mutation must stop the queue before the second file plays", {
                queue.snapshot().phase == LocalRecitationQueue.Phase.FAILED &&
                    queue.snapshot().failure == LocalRecitationQueue.Failure.FILE_OR_PLAYBACK
            })
            assertEquals(1, queue.snapshot().completedItems)
            assertEquals(RecordingAddress(1, 7), queue.snapshot().address)
            assertEquals(LocalRecitationQueue.Failure.FILE_OR_PLAYBACK, queue.snapshot().failure)
            assertEquals(LocalAudioPlayer.Phase.EMPTY, queue.snapshot().playback!!.phase)
            Thread.sleep(200)
            assertEquals(1, queue.snapshot().completedItems)
            val today = LocalDate.now().toEpochDay()
            val expired = assets[0].recording.copy(rights = assets[0].recording.rights.copy(expiresEpochDay = today - 1))
            val past = RecitationCatalog.create(listOf(expired), QuranCorpus.load(context), setOf("test-generator"))
            val expiredAsset = past.bind(expired.rights.recordingId, files[0], today - 2)
            scenario.onActivity { assertTrue(runCatching { queue.load(past, listOf(expiredAsset)) }.isFailure) }
            await("Expired selection must be rejected", { queue.snapshot().failure == LocalRecitationQueue.Failure.INVALID_SELECTION })
            assertEquals(0, queue.snapshot().completedItems)
        } finally { scenario.onActivity { queue.close() }; files.forEach { it.delete() }; scenario.close() }
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

    @Test fun canonicalMappedSyntheticAssetPlaysAndRechecksMutationAndExpiry() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val file = tone("mapped-playback-test.wav", 650)
        val original = file.readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(original).joinToString("") { "%02x".format(it.toInt() and 255) }
        val rights = RecitationRights("mapped-synthetic-tone", "test-generator", "https://example.invalid/test-tone",
            sha, "Generated test tone only", "synthetic-fixture-only", "hafs-an-asim", true, true)
        val record = CatalogRecording(rights, RecordingAddress(1, 1), original.size.toLong(), "synthetic-reference-fixture-only")
        val source = QuranCorpus.load(context)
        val today = LocalDate.now().toEpochDay()
        val catalog = RecitationCatalog.create(listOf(record), source, setOf("test-generator"))
        val player = LicensedRecitationPlayer(context)
        try {
            val asset = catalog.bind(rights.recordingId, file, today)
            player.open(asset, repeatCount = 1)
            assertTrue(player.play())
            await("Mapped local asset must complete both plays", { player.snapshot().phase == LocalAudioPlayer.Phase.COMPLETED })
            assertEquals(2, player.snapshot().completedPlays)
            file.writeBytes(original.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() })
            assertTrue(runCatching { player.open(asset) }.isFailure)
            assertEquals(LocalAudioPlayer.Phase.EMPTY, player.snapshot().phase)
            file.writeBytes(original)
            val expired = record.copy(rights = rights.copy(expiresEpochDay = today - 1))
            val past = RecitationCatalog.create(listOf(expired), source, setOf("test-generator"))
                .bind(rights.recordingId, file, today - 2)
            assertTrue(runCatching { player.open(past) }.isFailure)
            assertFalse(player.play())
        } finally { player.close(); file.delete(); scenario.close() }
    }
}
