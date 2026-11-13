package com.nexvary.hifzai.learning

import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalRecitationLibraryTest {
    @get:Rule val folder = TemporaryFolder()
    private val today = LocalDate.now().toEpochDay()
    private val corpus by lazy {
        QuranCorpus.parse(File("src/main/assets/quran/uthmani-v1.1.txt").readText(),
            File("src/main/assets/quran/surah-metadata.txt").readText())
    }
    private val bytes = byteArrayOf(1, 2, 3)
    private val address = RecordingAddress(1, 1)

    private fun entry(revoked: Boolean = false): CatalogRecording {
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return CatalogRecording(RecitationRights("test", "generator", "https://example.invalid/test",
            hash, "Synthetic bytes", "synthetic-test-only", "hafs-an-asim", true, true, revoked = revoked),
            address, bytes.size.toLong(), "synthetic-reference")
    }

    @Test fun reusesValidBytesAndReplacesACorruptCacheEntry() {
        var reads = 0
        val directory = folder.newFolder()
        val library = LocalRecitationLibrary(RecitationCatalog.create(listOf(entry()), corpus, setOf("generator")),
            directory) { reads++; ByteArrayInputStream(bytes) }
        fun bind() = library.bindRange(address, address, "generator", today).single().file
        val cached = bind()
        assertArrayEquals(bytes, cached.readBytes())
        assertEquals(cached, bind())
        assertEquals(1, reads)
        cached.writeBytes(byteArrayOf(3, 2, 1))
        assertArrayEquals(bytes, bind().readBytes())
        assertEquals(2, reads)
        assertEquals(listOf(cached), directory.listFiles()!!.toList())
    }

    @Test fun rejectsWithdrawnRightsBeforeReadingSourceBytes() {
        val directory = folder.newFolder()
        val library = LocalRecitationLibrary(RecitationCatalog.create(listOf(entry(revoked = true)),
            corpus, setOf("generator")), directory) { error("Withdrawn recording must not be opened") }
        assertTrue(runCatching { library.bindRange(address, address, "generator", today) }
            .exceptionOrNull() is IllegalArgumentException)
        assertTrue(directory.list()!!.isEmpty())
    }
}
