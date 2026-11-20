package com.nexvary.hifzai.learning

/** Resource/silence guard, NOT a validated voice-activity or recitation classifier. */
object AudioInputGate {
    const val SAMPLE_RATE = 16000
    const val MAX_SAMPLES = 30 * SAMPLE_RATE
    fun usable(samples: FloatArray): Boolean {
        if (samples.size !in 8000..MAX_SAMPLES) return false
        var energy = 0.0
        for (sample in samples) {
            if (!sample.isFinite() || sample !in -1f..1f) return false
            energy += sample.toDouble()*sample
        }
        return kotlin.math.sqrt(energy/samples.size) >= 0.005
    }
}
