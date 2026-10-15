package com.nexvary.hifzai.learning

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Offline adapter for future approved assets. No external recordings bundled or fetched. */
class LicensedRecitationPlayer(context: Context) {
    private val player = LocalAudioPlayer(context)
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
    }
    fun play(): Boolean = player.play()
    fun pause() = player.pause()
    fun speed(value: Float) = player.speed(value)
    fun seek(positionMs: Int) = player.seek(positionMs)
    fun snapshot() = player.snapshot()
    fun close() = player.close()
}
