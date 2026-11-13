package com.nexvary.hifzai.learning

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPcmCaptureTest {
    @Test fun readErrorDiscardsAnOtherwiseUsablePartialRecording() {
        for (error in listOf(0, -6)) {
            val output = FloatArray(16000)
            var reads = 0
            val result = readLocalPcm(output, shouldStop = { false }) { chunk, length ->
                if (reads++ < 4) {
                    chunk.fill(4096)
                    length
                } else {
                    error
                }
            }
            assertNull(result)
            assertEquals(5, reads)
        }
    }

    @Test fun stoppingDuringBlockingReadReturnsThePreviouslyReadSamples() {
        val output = FloatArray(4096)
        var stop = false
        var reads = 0
        lateinit var scratch: ShortArray
        val result = readLocalPcm(output, shouldStop = { stop }) { chunk, length ->
            scratch = chunk
            if (reads++ == 0) {
                chunk.fill(4096)
                length
            } else {
                stop = true
                -3
            }
        }
        assertEquals(2048, result)
        assertArrayEquals(FloatArray(4096) { if (it < 2048) 0.125f else 0f }, output, 0f)
        assertArrayEquals(ShortArray(scratch.size), scratch)
    }

    @Test fun captureEndsAtTheBufferLimit() {
        val output = FloatArray(2049)
        val requests = mutableListOf<Int>()
        val result = readLocalPcm(output, shouldStop = { false }) { chunk, length ->
            requests += length
            chunk.fill(-32768)
            length
        }
        assertEquals(listOf(2048, 1), requests)
        assertEquals(output.size, result)
        assertArrayEquals(FloatArray(output.size) { -1f }, output, 0f)
    }
}
