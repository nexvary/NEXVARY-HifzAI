package com.nexvary.hifzai.learning

import java.io.File
import java.security.MessageDigest
import java.util.Collections

data class RecordingAddress(val chapter: Int, val ayah: Int) {
    val verseId: Int get() = chapter * 1000 + ayah
    init { require(chapter in 1..114 && ayah in 1..286) }
}

/** Metadata from a reviewed register, not evidence that an audio file was listened to. */
data class CatalogRecording(val rights: RecitationRights, val address: RecordingAddress,
    val byteLength: Long, val referenceReview: String) {
    init { require(byteLength in 1..100_000_000L) }
    fun eligible(today: Long): Boolean = referenceReview.isNotBlank() && rights.permitsUse(true, todayEpochDay = today)
}

/** Produced only after canonical address, rights, exact length and digest checks. */
class BoundRecitation internal constructor(val recording: CatalogRecording, val file: File)

class RecitationCatalog private constructor(records: List<CatalogRecording>, corpus: QuranCorpus) {
    val recordings: List<CatalogRecording> = Collections.unmodifiableList(records.toList())
    private val byId = recordings.associateBy { it.rights.recordingId }
    private val offsets = corpus.verses.mapIndexed { i, verse -> verse.id to i }.toMap()

    fun available(address: RecordingAddress, reciterId: String, today: Long): List<CatalogRecording> =
        recordings.filter { it.address == address && it.rights.reciterId == reciterId && it.eligible(today) }

    /** An explicit, contiguous selection; never chooses a recording from an ambiguous catalogue. */
    fun sequence(recordingIds: List<String>, today: Long): List<CatalogRecording> {
        require(recordingIds.size in 1..2000 && recordingIds.distinct().size == recordingIds.size)
        val selected = recordingIds.map { requireNotNull(byId[it]) { "تسجيل غير موجود في السجل" } }
        require(selected.all { it.eligible(today) }) { "حقوق التسجيل أو مراجعة مرجعه غير مكتملة" }
        require(selected.map { it.rights.reciterId }.distinct().size == 1) { "لا نخلط القراء في تسلسل واحد" }
        require(selected.map { it.rights.riwayah }.distinct().size == 1)
        require(selected.zipWithNext().all { (first, second) ->
            offsets.getValue(second.address.verseId) == offsets.getValue(first.address.verseId) + 1
        }) { "تسلسل الآيات متقطع أو معكوس" }
        return Collections.unmodifiableList(selected)
    }

    /** Worker-thread operation. No paths or URLs supplied by catalogue metadata are opened. */
    fun bind(recordingId: String, file: File, today: Long): BoundRecitation {
        val record = requireNotNull(byId[recordingId]) { "تسجيل غير موجود في السجل" }
        require(record.eligible(today)) { "التسجيل غير مصرح للتشغيل المحلي الآن" }
        require(file.isFile && file.length() == record.byteLength) { "حجم ملف الصوت لا يطابق السجل" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        require(sha == record.rights.sha256) { "بصمة الصوت لا تطابق التسجيل المرتبط بالآية" }
        return BoundRecitation(record, file)
    }

    companion object {
        fun create(records: List<CatalogRecording>, corpus: QuranCorpus,
            reciterIds: Set<String> = ReciterCatalog.all.map { it.slug }.toSet()): RecitationCatalog {
            require(records.size <= 10_000)
            require(records.map { it.rights.recordingId }.distinct().size == records.size) { "معرف تسجيل مكرر" }
            records.forEach {
                require(it.rights.recordingId.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
                require(it.rights.reciterId in reciterIds) { "القارئ غير موجود في السجل" }
                require(corpus.find(it.address.chapter, it.address.ayah) != null) { "مرجع الآية غير موجود في الأصل" }
                require(it.rights.sha256.matches(Regex("[0-9a-f]{64}")))
                require(it.rights.riwayah == "hafs-an-asim") { "رواية غير مدعومة في هذا المصحف" }
            }
            return RecitationCatalog(records, corpus)
        }
    }
}
