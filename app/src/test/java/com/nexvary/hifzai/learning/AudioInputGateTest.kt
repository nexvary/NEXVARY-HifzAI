package com.nexvary.hifzai.learning

import org.junit.Assert.*
import org.junit.Test

class AudioInputGateTest {
    @Test fun silenceAndTooShortOrOversizedInputAbstain() {
        assertFalse(AudioInputGate.usable(FloatArray(16000)))
        assertFalse(AudioInputGate.usable(FloatArray(7999) { 0.1f }))
        assertFalse(AudioInputGate.usable(FloatArray(480001) { 0.1f }))
    }
    @Test fun nonFiniteAndOutOfRangeSamplesCannotReachNativeInference() {
        for (bad in listOf(Float.NaN,Float.POSITIVE_INFINITY,1.01f,-1.01f)) {
            val input = FloatArray(16000) { 0.1f }; input[500] = bad
            assertFalse(AudioInputGate.usable(input))
        }
    }
    @Test fun validPcmIsNotMutatedAndNotCalledAccurateSpeech() {
        val input = FloatArray(16000) { if (it%2==0) 0.1f else -0.1f }
        val original = input.copyOf()
        assertTrue(AudioInputGate.usable(input))
        assertArrayEquals(original,input,0f)
    }
}
