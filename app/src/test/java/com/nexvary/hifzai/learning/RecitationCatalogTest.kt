package com.nexvary.hifzai.learning

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

/** Synthetic bytes and register entries only; no real reciter/content permission claim. */
class RecitationCatalogTest {
    private fun corpus() = QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
        File("src/main/assets/quran/surah-metadata.txt").readText())
    private fun entry(id: String = "fixture-a", chapter: Int = 1, ayah: Int = 1,
        bytes: ByteArray = byteArrayOf(1, 2, 3), reciter: String = "synthetic-generator"): CatalogRecording {
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        return CatalogRecording(RecitationRights(id, reciter, "https://example.invalid/synthetic", sha,
            "synthetic-license", "synthetic-review-only", "hafs-an-asim", true, true),
            RecordingAddress(chapter, ayah), bytes.size.toLong(), "synthetic-reference-review-only")
    }
    private fun catalogue(items: List<CatalogRecording>) = RecitationCatalog.create(items, corpus(),
        setOf("synthetic-generator", "other-synthetic-generator"))

    @Test fun unknownAyatDuplicateIdsUnknownRecitersAndOtherNarrationsAreRejected() {
        val first = entry()
        assertTrue(runCatching { catalogue(listOf(entry(ayah = 8))) }.isFailure)
        assertTrue(runCatching { catalogue(listOf(first, first)) }.isFailure)
        assertTrue(runCatching { catalogue(listOf(entry(reciter = "unknown"))) }.isFailure)
        assertTrue(runCatching { catalogue(listOf(first.copy(rights = first.rights.copy(riwayah = "warsh")))) }.isFailure)
    }
    @Test fun pendingReferenceReviewExpiryAndRevocationNeverAppearAvailable() {
        val first = entry()
        val blocked = listOf(first.copy(referenceReview = ""),
            first.copy(rights = first.rights.copy(expiresEpochDay = 9)),
            first.copy(rights = first.rights.copy(revoked = true)),
            first.copy(rights = first.rights.copy(permitsOffline = false)))
        for (record in blocked) {
            val catalog = catalogue(listOf(record))
            assertTrue(catalog.available(record.address, "synthetic-generator", 10).isEmpty())
            assertTrue(runCatching { catalog.sequence(listOf(record.rights.recordingId), 10) }.isFailure)
        }
    }
    @Test fun explicitSequenceUsesCanonicalAdjacencyIncludingSurahBoundary() {
        val a = entry("a", 1, 7); val b = entry("b", 2, 1)
        val catalog = catalogue(listOf(a, b))
        assertEquals(listOf(a, b), catalog.sequence(listOf("a", "b"), 10))
        assertTrue(runCatching { catalog.sequence(listOf("b", "a"), 10) }.isFailure)
        assertTrue(runCatching { catalog.sequence(listOf("a", "a"), 10) }.isFailure)
    }
    @Test fun gapsMissingTracksAndMixedRecitersCannotBeSilentlyChosen() {
        val catalog = catalogue(listOf(entry("a", ayah = 1), entry("b", ayah = 3),
            entry("c", ayah = 2, reciter = "other-synthetic-generator")))
        assertTrue(runCatching { catalog.sequence(listOf("a", "b"), 10) }.isFailure)
        assertTrue(runCatching { catalog.sequence(listOf("a", "c"), 10) }.isFailure)
        assertTrue(runCatching { catalog.sequence(listOf("missing"), 10) }.isFailure)
    }
    @Test fun selectsInclusiveRangeInCanonicalOrderForOneReciter() {
        val a = entry("a", 1, 7)
        val b = entry("b", 2, 1)
        val other = entry("other", 2, 1, reciter = "other-synthetic-generator")
        val catalog = catalogue(listOf(other, b, a))
        assertEquals(listOf(a, b), catalog.selectRange(a.address, b.address, a.rights.reciterId, 10))
        assertEquals(listOf(b), catalog.selectRange(b.address, b.address, a.rights.reciterId, 10))
    }

    @Test fun rangeRejectsMissingAmbiguousExpiredAndInvalidEndpoints() {
        val a = entry("a", ayah = 1)
        val b = entry("b", ayah = 2)
        val c = entry("c", ayah = 3)
        val expired = b.copy(rights = b.rights.copy(expiresEpochDay = 9))
        val reciter = a.rights.reciterId
        for (items in listOf(listOf(a, c), listOf(a, expired, c),
            listOf(a, b, entry("duplicate", ayah = 2), c))) {
            val catalog = catalogue(items)
            assertThrows(IllegalArgumentException::class.java) {
                catalog.selectRange(a.address, c.address, reciter, 10)
            }
        }
        val catalog = catalogue(listOf(a, b, c))
        for (last in listOf(a.address, RecordingAddress(1, 8), RecordingAddress(114, 6))) {
            assertThrows(IllegalArgumentException::class.java) {
                catalog.selectRange(b.address, last, reciter, 10)
            }
        }
    }

    @Test fun bindingRequiresExactBytesAndKeepsTheReference() {
        val directory = Files.createTempDirectory("catalog-test").toFile()
        try {
            val file = File(directory, "fixture.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val catalog = catalogue(listOf(entry()))
            val bound = catalog.bind("fixture-a", file, 10)
            assertEquals(RecordingAddress(1, 1), bound.recording.address)
            file.writeBytes(byteArrayOf(1, 2, 4))
            assertTrue(runCatching { catalog.bind("fixture-a", file, 10) }.isFailure)
            file.writeBytes(byteArrayOf(1))
            assertTrue(runCatching { catalog.bind("fixture-a", file, 10) }.isFailure)
            assertTrue(runCatching { catalog.bind("fixture-a", File(directory, "missing"), 10) }.isFailure)
        } finally { directory.deleteRecursively() }
    }
    @Test fun catalogueTakesAnImmutableSnapshot() {
        val items = mutableListOf(entry())
        val catalog = catalogue(items)
        items.clear()
        assertEquals(1, catalog.recordings.size)
        assertTrue(runCatching { (catalog.recordings as MutableList<CatalogRecording>).clear() }.isFailure)
    }
}
