package com.nexvary.hifzai.learning

import android.content.Context
import java.security.MessageDigest

/** Numeric verse boundaries from official Tanzil metadata, not a page facsimile. */
enum class PartitionKind(val label: String) { JUZ("الجزء"), PAGE("الصفحة") }
data class QuranPartition(val kind: PartitionKind, val number: Int, val firstId: Int, val lastId: Int)

class QuranPartitions private constructor(val partitions: List<QuranPartition>) {
    fun ofKind(kind: PartitionKind) = partitions.filter { it.kind == kind }
    fun verses(partition: QuranPartition, corpus: QuranCorpus): List<VerifiedVerse> =
        corpus.verses.filter { it.id in partition.firstId..partition.lastId }

    companion object {
        const val ASSET = "quran/metadata-v1.0.xml"
        fun load(context: Context, corpus: QuranCorpus): QuranPartitions {
            val bytes = context.assets.open(ASSET).use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            check(hash == SOURCE_SHA256) { "Partition metadata integrity failed" }
            return parse(bytes.toString(Charsets.UTF_8), corpus)
        }
        // Recorded from direct official https://tanzil.net/res/text/metadata/quran-data.xml
        const val SOURCE_SHA256 = "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a"
        fun parse(xml: String, corpus: QuranCorpus): QuranPartitions {
            require(xml.contains("version=\"1.0\"") && xml.contains("license=\"cc-by\""))
            require(!xml.contains("<!DOCTYPE", ignoreCase = true))
            val result = mutableListOf<QuranPartition>()
            val offsets = corpus.verses.mapIndexed { i, v -> v.id to i }.toMap()
            for ((kind, container, tag, count) in listOf(
                Quad(PartitionKind.JUZ, "juzs", "juz", 30), Quad(PartitionKind.PAGE, "pages", "page", 604))) {
                val section = Regex("<$container(?:\\s[^>]*)?>(.*?)</$container>", RegexOption.DOT_MATCHES_ALL)
                    .find(xml)?.groupValues?.get(1).orEmpty()
                require(section.isNotEmpty())
                val pattern = Regex("<$tag index=\"(\\d+)\" sura=\"(\\d+)\" aya=\"(\\d+)\" />")
                val starts = pattern.findAll(section).map { m ->
                    m.groupValues[1].toInt() to (m.groupValues[2].toInt()*1000+m.groupValues[3].toInt())
                }.toList()
                require(starts.size == count && starts.map { it.first } == (1..count).toList())
                val indices = starts.map { offsets[it.second] ?: error("Unknown partition start") }
                require(indices.first() == 0 && indices.zipWithNext().all { (a,b) -> a < b })
                starts.forEachIndexed { i, start ->
                    val next = indices.getOrElse(i+1) { corpus.verses.size }
                    result += QuranPartition(kind, start.first, start.second, corpus.verses[next-1].id)
                }
            }
            return QuranPartitions(java.util.Collections.unmodifiableList(result))
        }
        private data class Quad(val kind: PartitionKind, val container: String, val tag: String, val count: Int)
    }
}
