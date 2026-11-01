package com.nexvary.hifzai.learning

import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class OfflineAudioStoreTest {
    private fun rights(bytes: ByteArray) = RecitationRights("synthetic", "synthetic", "https://example.com/synthetic",
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) },
        "synthetic", "synthetic", "hafs-an-asim", true, true)
    @Test fun installsPinnedBytesDeduplicatesAndDeletes() {
        val directory = Files.createTempDirectory("audio-store-test").toFile()
        try {
            val bytes = byteArrayOf(1, 2, 3) // Not an audio/recitation validation fixture.
            val store = OfflineAudioStore(directory, 3)
            val first = store.install(bytes.inputStream(), rights(bytes))
            assertArrayEquals(bytes, first.readBytes())
            assertEquals(first, store.install(bytes.inputStream(), rights(bytes)))
            assertEquals(1, directory.listFiles()!!.size)
            assertTrue(store.remove(rights(bytes)))
            assertFalse(first.exists())
        } finally { directory.deleteRecursively() }
    }
    @Test fun quotaHashExpiryAndRightsRejectionLeaveNoPartialFiles() {
        val directory = Files.createTempDirectory("audio-store-test").toFile()
        try {
            val bytes = byteArrayOf(1, 2, 3)
            val store = OfflineAudioStore(directory, 2)
            assertTrue(runCatching { store.install(bytes.inputStream(), rights(bytes)) }.isFailure)
            assertTrue(runCatching { OfflineAudioStore(directory).install(byteArrayOf(9).inputStream(), rights(bytes)) }.isFailure)
            assertTrue(runCatching { store.install(bytes.inputStream(), rights(bytes).copy(permitsOffline = false)) }.isFailure)
            assertTrue(runCatching { store.install(bytes.inputStream(), rights(bytes).copy(expiresEpochDay = 1)) }.isFailure)
            assertEquals(0, directory.listFiles()!!.size)
        } finally { directory.deleteRecursively() }
    }
}
