package com.nexvary.hifzai.learning

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Bounded local import for reviewed assets. No network downloader or recording rights inference. */
class OfflineAudioStore(private val directory: File, private val quotaBytes: Long = 250_000_000) {
    init { require(quotaBytes in 1..1_000_000_000L); check(directory.isDirectory || directory.mkdirs()) }
    private fun file(rights: RecitationRights) = File(directory, rights.sha256 + ".audio")
    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val n = input.read(buffer); if (n < 0) break; md.update(buffer, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    /** Owns and closes input, including on rejection. Call on an IO worker. */
    @Synchronized fun install(input: InputStream, rights: RecitationRights): File = input.use { source ->
        require(rights.permitsUse(offline = true)) { "حقوق الحفظ المحلي غير موثقة أو منتهية" }
        val output = file(rights)
        if (output.isFile && output.length() in 1..100_000_000L && digest(output) == rights.sha256) return@use output
        val used = directory.listFiles().orEmpty().filter { it.isFile }.sumOf { it.length() }
        val budget = minOf(100_000_000L, quotaBytes - used)
        require(budget > 0) { "امتلأت مساحة الصوت المحلية المحددة" }
        val temporary = File.createTempFile("import-", ".part", directory)
        try {
            var size = 0L
            temporary.outputStream().use { target ->
                val buffer = ByteArray(65536)
                while (true) {
                    val count = source.read(buffer); if (count < 0) break
                    size += count
                    require(size <= budget) { "تجاوز الملف حد التخزين المحلي" }
                    target.write(buffer, 0, count)
                }
            }
            require(size > 0 && digest(temporary) == rights.sha256) { "بصمة الصوت لا تطابق سجل الحقوق" }
            require(rights.permitsUse(offline = true)) { "انتهت صلاحية الاستخدام أثناء الاستيراد" }
            check(!output.exists() || output.delete())
            check(temporary.renameTo(output))
            output
        } finally { temporary.delete() }
    }
    @Synchronized fun remove(rights: RecitationRights): Boolean {
        require(rights.sha256.matches(Regex("[0-9a-f]{64}")))
        val existing = file(rights)
        return !existing.exists() || existing.delete()
    }
    @Synchronized fun clear(): Boolean = directory.listFiles().orEmpty().filter {
        it.isFile && (it.name.matches(Regex("[0-9a-f]{64}\\.audio")) || it.name.startsWith("import-"))
    }.map { it.delete() }.all { it }
}
