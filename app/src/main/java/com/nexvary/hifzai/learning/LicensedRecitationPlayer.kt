package com.nexvary.hifzai.learning

import android.content.Context
import java.io.File
import java.security.MessageDigest
import android.os.Handler
import android.os.Looper

/** Offline adapter for future approved assets. No external recordings bundled or fetched. */
class LicensedRecitationPlayer(context: Context,
    private val rightsHandler: Handler = Handler(Looper.getMainLooper())) {
    private val player = LocalAudioPlayer(context)
    private var currentRights: RecitationRights? = null
    private val expiryCheck = object : Runnable {
        override fun run() {
            if (currentRights?.permitsUse(offline = true) != true) { close(); return }
            rightsHandler.postDelayed(this, 1000)
        }
    }
    fun open(file: File, rights: RecitationRights, repeatCount: Int = 0) {
        close()
        require(rights.permitsUse(offline = true)) { "Recording rights are not cleared" }
        require(repeatCount in 0..20)
        require(file.isFile && file.length() in 1..100_000_000L)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == rights.sha256) { "Recording hash mismatch" }
        player.open(file)
        player.repeats(repeatCount)
        currentRights = rights
        rightsHandler.post(expiryCheck)
    }
    /** Mapped catalogue playback; the original raw adapter remains for existing private tests. */
    fun open(asset: BoundRecitation, repeatCount: Int = 0) {
        close()
        require(asset.recording.referenceReview.isNotBlank())
        require(asset.file.length() == asset.recording.byteLength)
        // Recheck rights and digest at open, even if bytes changed after bind().
        open(asset.file, asset.recording.rights, repeatCount)
    }
    fun play(): Boolean {
        if (currentRights?.permitsUse(offline = true) != true) { close(); return false }
        return player.play()
    }
    fun pause() = player.pause()
    fun speed(value: Float) = player.speed(value)
    fun seek(positionMs: Int) = player.seek(positionMs)
    fun snapshot() = player.snapshot()
    fun close() {
        rightsHandler.removeCallbacks(expiryCheck)
        currentRights = null
        player.close()
    }
}
