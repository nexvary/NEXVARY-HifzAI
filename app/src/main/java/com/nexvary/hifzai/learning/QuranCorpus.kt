package com.nexvary.hifzai.learning

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** A locally bundled, verbatim copy of Tanzil Uthmani Version 1.1.
 *
 * The source text is NEVER edited. The original file preserves the Tanzil
 * CC BY 3.0 copyright notice and follows Tanzil's unchanged-copy policy.
 * Source: dotquran/corpus, src/resources/uthmani.txt, git blob:
 * 9813920ae5afce9e50d2ac44562457e29bb4bfca
 *
 * Technical byte verification is NOT a committee's scholarly certification.
 */
data class SurahInfo(val number: Int, val arabicName: String, val ayahCount: Int)

class QuranCorpus private constructor(
    val verses: List<VerifiedVerse>,
    val surahs: List<SurahInfo>
) {
    private val chapters: Map<Int, List<VerifiedVerse>> = verses.groupBy { it.chapter }
    val searchIndex by lazy { QuranSearchIndex(verses) }
    /** Unnumbered opening is a verbatim reference, never a generated verse. */
    fun openingBasmala(chapter: Int): VerifiedVerse? {
        require(chapter in 1..114)
        return if (chapter == 1 || chapter == 9) null else find(1, 1)
    }
    fun chapter(chapter: Int): List<VerifiedVerse> = chapters[chapter].orEmpty()
    fun info(chapter: Int): SurahInfo = surahs.first { it.number == chapter }
    fun find(chapter: Int, ayah: Int): VerifiedVerse? =
        chapters[chapter]?.firstOrNull { it.verse == ayah }

    companion object {
        const val ASSET = "quran/uthmani-v1.1.txt"
        const val SOURCE_SHA256 = "6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f"
        const val SOURCE_BLOB_SHA1 = "9813920ae5afce9e50d2ac44562457e29bb4bfca"
        const val METADATA_SHA256 = "ec86874d26dd3bcd5474b9dd6afb92541484e13b8c8d0da8138e070885bc5b51"
        const val SOURCE_CREDIT = "النص القرآني: مشروع تنزيل Tanzil — الرسم العثماني 1.1"
        const val SOURCE_LINK = "https://tanzil.net/"

        private fun gitBlobSha1(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-1")
            md.update(("blob " + bytes.size + "\u0000").toByteArray(StandardCharsets.UTF_8))
            md.update(bytes)
            return md.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        }

        fun load(context: Context): QuranCorpus {
            val original = context.assets.open(ASSET).use { it.readBytes() }
            val digest = MessageDigest.getInstance("SHA-256").digest(original)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            check(digest == SOURCE_SHA256 && gitBlobSha1(original) == SOURCE_BLOB_SHA1) {
                "Quran source integrity check failed; display must be blocked"
            }
            val text = original.toString(StandardCharsets.UTF_8)
            val metadataBytes = context.assets.open("quran/surah-metadata.txt").use { it.readBytes() }
            val metadataHash = MessageDigest.getInstance("SHA-256").digest(metadataBytes)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            check(metadataHash == METADATA_SHA256) { "Surah metadata integrity failed" }
            val metadata = metadataBytes.toString(StandardCharsets.UTF_8)
            return parse(text, metadata)
        }

        /** Can be unit-tested without Android or modifying canonical text. */
        fun parse(original: String, metadata: String): QuranCorpus {
            check(original.contains("Tanzil Quran Text (Uthmani, Version 1.1)"))
            check(original.contains("CHANGING IT IS NOT ALLOWED"))
            val surahs = metadata.lineSequence().filter { it.isNotBlank() }.map {
                val parts = it.split("|", limit = 3)
                require(parts.size == 3)
                SurahInfo(parts[0].toInt(), parts[1], parts[2].toInt())
            }.toList()
            require(surahs.size == 114 && surahs.map { it.number } == (1..114).toList()) {
                "Invalid Surah metadata"
            }
            val verses = original.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map {
                    val parts = it.split("|", limit = 3)
                    require(parts.size == 3) { "Unexpected source line" }
                    val s = parts[0].toInt()
                    val a = parts[1].toInt()
                    require(parts[2].isNotBlank()) { "Empty canonical verse" }
                    VerifiedVerse(s, a, parts[2])
                }.toList()
            require(verses.size == 6236) { "Expected 6236, found " + verses.size }
            val counts = verses.groupBy { it.chapter }
            surahs.forEach { info ->
                val chapter = counts[info.number].orEmpty()
                require(chapter.size == info.ayahCount)
                require(chapter.map { it.verse } == (1..info.ayahCount).toList())
            }
            require(counts.size == 114) { "Incomplete Quran" }
            require(verses.map { it.id } == surahs.flatMap { s ->
                (1..s.ayahCount).map { s.number * 1000 + it }
            }) { "Quran chapter order changed" }
            return QuranCorpus(verses, surahs)
        }
    }
}
