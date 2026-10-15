package com.nexvary.hifzai.learning

/** CPU-only JNI. No network backend, no Quran prompt, no automatic correctness score. */
internal object NativeWhisper {
    init { System.loadLibrary("hifz_whisper") }
    @JvmStatic external fun createSession(): Long
    @JvmStatic external fun transcribe(session: Long, modelPath: String, pcm16kMono: FloatArray): ByteArray?
    @JvmStatic external fun cancel(session: Long)
    @JvmStatic external fun release(session: Long)
}
