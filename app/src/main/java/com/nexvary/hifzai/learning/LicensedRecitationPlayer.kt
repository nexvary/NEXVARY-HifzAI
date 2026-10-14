package com.nexvary.hifzai.learning

import android.media.MediaPlayer
import android.media.PlaybackParams
import java.io.File
import java.security.MessageDigest

/** Offline adapter for future approved assets. No external recordings bundled or fetched. */
class LicensedRecitationPlayer {
    private var player: MediaPlayer? = null
    private var repeatsRemaining = 0
    fun open(file: File, rights: RecitationRights, repeatCount: Int = 0) {
        require(rights.permitsUse(offline = true)) { "Recording rights are not cleared" }
        require(repeatCount in 0..20)
        require(file.isFile && file.length() in 1..100_000_000L)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == rights.sha256) { "Recording hash mismatch" }
        close()
        val next = MediaPlayer()
        try {
            next.setDataSource(file.absolutePath)
            next.prepare()
            repeatsRemaining = repeatCount
            next.setOnCompletionListener {
                if (repeatsRemaining > 0) { repeatsRemaining--; it.seekTo(0); it.start() }
            }
            next.setOnErrorListener { _, _, _ -> close(); true }
            player = next
        } catch (e: Exception) { next.release(); throw e }
    }
    fun play() { checkNotNull(player).start() }
    fun pause() { player?.let { if (it.isPlaying) it.pause() } }
    fun speed(value: Float) {
        require(value.isFinite() && value in 0.5f..1.5f)
        checkNotNull(player).playbackParams = PlaybackParams().setSpeed(value).setPitch(1f)
    }
    fun close() { player?.release(); player = null; repeatsRemaining = 0 }
}
