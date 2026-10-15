package com.nexvary.hifzai.learning

import android.content.Context
import java.io.File
import java.security.MessageDigest

internal object LocalWhisperModel {
    const val SHA256 = "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21"
    const val SIZE = 77691713L
    const val ID = "whisper-tiny-ggml:$SHA256:whisper.cpp-1.9.5"
    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) { val count = stream.read(buffer); if (count < 0) break; md.update(buffer,0,count) }
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    /** On worker thread. Models are not personal data; no voice/transcript is written here. */
    @Synchronized fun verifiedFile(context: Context): File {
        val directory = File(context.noBackupFilesDir, "asr-models").apply { mkdirs() }
        val file = File(directory, "ggml-tiny.bin")
        if (file.isFile && file.length() == SIZE && digest(file) == SHA256) return file
        file.delete()
        val temporary = File(directory, "ggml-tiny.part")
        try {
            context.assets.open("asr/ggml-tiny.bin").use { input -> temporary.outputStream().use { output ->
                val buffer = ByteArray(65536); var size = 0L
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    size += count; check(size <= SIZE) { "ملف النموذج أكبر من النسخة المعتمدة تقنيًا" }
                    output.write(buffer,0,count)
                }
            } }
            check(temporary.length() == SIZE && digest(temporary) == SHA256) { "فشل التحقق من بصمة نموذج التفريغ" }
            check(temporary.renameTo(file))
            return file
        } finally { temporary.delete() }
    }
}
