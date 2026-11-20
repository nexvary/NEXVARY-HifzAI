package com.nexvary.hifzai.learning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/** Local files only. Does not record, upload, classify recitation or grant recording rights. */
class LocalAudioPlayer(context: Context) {
    enum class Phase { EMPTY, READY, PLAYING, PAUSED, COMPLETED }
    data class Snapshot(val phase: Phase, val positionMs: Int, val durationMs: Int,
        val speed: Float, val extraRepeats: Int, val completedPlays: Int)
    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var player: MediaPlayer? = null
    private var phase = Phase.EMPTY
    private var desiredSpeed = 1f
    private var extraRepeats = 0
    private var remaining = 0
    private var completedPlays = 0
    private var replayAfterSeek = false
    private var seekPending = false
    private var registered = false
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) pause()
        }, Handler(Looper.getMainLooper())).build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    @Synchronized fun open(file: File) {
        close()
        require(file.isFile && file.length() in 1..100_000_000L) { "ملف الصوت غير صالح" }
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(attributes)
            next.setDataSource(file.absolutePath)
            next.prepare()
            check(next.duration > 0) { "مدة الصوت غير صالحة" }
            player = next
            phase = Phase.READY
            next.setOnCompletionListener { active -> synchronized(this) {
                if (player === active && phase == Phase.PLAYING && !seekPending) {
                    completedPlays++
                    if (remaining > 0 && phase == Phase.PLAYING) {
                        remaining--
                        replayAfterSeek = true
                        seekPending = true
                        active.seekTo(0)
                    } else {
                        phase = Phase.COMPLETED
                        manager.abandonAudioFocusRequest(focus)
                    }
                }
            } }
            next.setOnSeekCompleteListener { active -> synchronized(this) {
                if (player === active) {
                    seekPending = false
                    if (replayAfterSeek && phase == Phase.PLAYING) {
                        replayAfterSeek = false
                        active.playbackParams = PlaybackParams().setSpeed(desiredSpeed).setPitch(1f)
                        active.start()
                    }
                }
            } }
            next.setOnErrorListener { active, _, _ -> synchronized(this) {
                if (player === active) close()
                true
            } }
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, filter, Context.RECEIVER_NOT_EXPORTED)
            else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                app.registerReceiver(noisy, filter)
            }
            registered = true
        } catch (error: Exception) {
            if (player === next) close() else next.release()
            throw error
        }
    }

    @Synchronized fun play(): Boolean {
        val active = player ?: return false
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        return try {
            if (phase == Phase.COMPLETED) {
                remaining = extraRepeats
                completedPlays = 0
                seekPending = true
                active.seekTo(0)
            }
            phase = Phase.PLAYING
            if (seekPending) { replayAfterSeek = true; return true }
            // A nonzero PlaybackParams speed can start MediaPlayer. Apply it only
            // after an explicit Play with audio focus, never while merely editing a paused speed.
            active.playbackParams = PlaybackParams().setSpeed(desiredSpeed).setPitch(1f)
            active.start()
            true
        } catch (_: Exception) { close(); false }
    }

    @Synchronized fun pause() {
        replayAfterSeek = false
        player?.let { active ->
            if (phase != Phase.COMPLETED) {
                runCatching { if (active.isPlaying) active.pause() }
                phase = Phase.PAUSED
            }
        }
        manager.abandonAudioFocusRequest(focus)
    }

    @Synchronized fun speed(value: Float) {
        require(value.isFinite() && value in 0.5f..1.5f)
        desiredSpeed = value
        if (phase == Phase.PLAYING && !seekPending) player?.playbackParams = PlaybackParams().setSpeed(value).setPitch(1f)
    }
    @Synchronized fun repeats(value: Int) {
        require(value in 0..20)
        extraRepeats = value
        remaining = value
    }
    @Synchronized fun seek(positionMs: Int) {
        val active = checkNotNull(player)
        require(positionMs in 0..active.duration)
        replayAfterSeek = phase == Phase.PLAYING
        if (replayAfterSeek && active.isPlaying) active.pause()
        seekPending = true
        active.seekTo(positionMs)
        if (phase == Phase.COMPLETED) {
            phase = Phase.PAUSED
            remaining = extraRepeats
            completedPlays = 0
        }
    }
    @Synchronized fun snapshot(): Snapshot {
        val active = player
        return Snapshot(phase, active?.let { runCatching { it.currentPosition }.getOrDefault(0) } ?: 0,
            active?.let { runCatching { it.duration }.getOrDefault(0) } ?: 0,
            desiredSpeed, extraRepeats, completedPlays)
    }
    @Synchronized fun close() {
        replayAfterSeek = false
        seekPending = false
        player?.release()
        player = null
        phase = Phase.EMPTY
        desiredSpeed = 1f
        extraRepeats = 0
        remaining = 0
        completedPlays = 0
        manager.abandonAudioFocusRequest(focus)
        if (registered) { app.unregisterReceiver(noisy); registered = false }
    }
}
