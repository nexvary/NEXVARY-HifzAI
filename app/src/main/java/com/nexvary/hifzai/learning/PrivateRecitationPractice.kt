package com.nexvary.hifzai.learning

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * An optional private self-listening exercise.
 *
 * No network, upload, ASR, voiceprint or automated tajweed verdict.
 * Recording is held in private cache, never shared. Leave the practice
 * session to erase it; users can erase it immediately too.
 */
class PrivateRecitationPractice(context: Context) {
    private val file = File(context.cacheDir, "hifz_private_session.m4a")
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private val player = LocalAudioPlayer(appContext)

    init {
        // A previous process may have crashed before session cleanup.
        // No recording is carried into a new application session.
        file.delete()
    }

    val isRecording: Boolean get() = recorder != null
    val hasRecording: Boolean get() = !isRecording && file.exists() && file.length() > 0L

    @Suppress("DEPRECATION")
    fun start() {
        erase()
        val next = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(96000)
            next.setAudioSamplingRate(44100)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            recorder = next
        } catch (e: Exception) {
            next.release()
            file.delete()
            throw e
        }
    }

    fun stop(): Boolean {
        val active = recorder ?: return false
        recorder = null
        return try {
            active.stop()
            file.exists() && file.length() > 0
        } catch (_: RuntimeException) {
            file.delete()
            false
        } finally {
            active.release()
        }
    }

    fun play(): Boolean {
        if (!hasRecording) return false
        return runCatching {
            if (player.snapshot().phase == LocalAudioPlayer.Phase.EMPTY) {
                val settings = player.snapshot()
                player.open(file)
                player.speed(settings.speed)
                player.repeats(settings.extraRepeats)
            }
            player.play()
        }.getOrDefault(false)
    }
    fun pause() = player.pause()
    fun playback() = player.snapshot()
    fun speed(value: Float) = player.speed(value)
    fun repeats(value: Int) = player.repeats(value)
    fun seek(positionMs: Int) = player.seek(positionMs)

    fun erase() {
        player.close()
        if (isRecording) stop()
        file.delete()
    }

    fun close() = erase()
}
