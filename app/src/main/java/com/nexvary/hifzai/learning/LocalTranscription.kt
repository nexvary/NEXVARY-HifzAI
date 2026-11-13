package com.nexvary.hifzai.learning

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Captures at most 30s of PCM in RAM, transcribes locally, never persists or grades it. */
internal class LocalTranscription(context: Context, private val changed: (State) -> Unit) {
    sealed class State {
        data object Idle : State()
        data object Preparing : State()
        data object Recording : State()
        data object Processing : State()
        data class Unable(val reason: String) : State()
        data class Draft(val text: String, val processingMillis: Long) : State()
    }
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val generation = AtomicInteger(0)
    @Volatile private var stopCapture = false
    @Volatile private var recorder: AudioRecord? = null
    private var nativeSession = 0L // All handle mutation/cancellation/release is synchronized.
    @Volatile private var closed = false

    private fun emit(id: Int, value: State) { main.post {
        if (!closed && generation.get()==id) changed(value)
    } }
    @Synchronized fun start() {
        if (closed) return
        // Cancel stops the previous capture. The serial worker releases its recorder
        // before starting a replacement, even when the user retries immediately.
        cancel()
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            changed(State.Unable("يلزم إذن الميكروفون لهذا التسميع المحلي")); return
        }
        val manager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        if (manager.isLowRamDevice || memory.availMem < 512L*1024*1024) {
            changed(State.Unable("الذاكرة المتاحة لا تكفي لتشغيل النموذج بأمان الآن")); return
        }
        val id = generation.incrementAndGet()
        stopCapture = false
        changed(State.Preparing)
        worker.execute {
            if (generation.get() != id) return@execute
            var samples: FloatArray? = null
            var active: AudioRecord? = null
            try {
                val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(minimum > 0)
                val input = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(maxOf(minimum,4096)).build()
                active = input
                check(input.state == AudioRecord.STATE_INITIALIZED)
                recorder = input
                if (generation.get()!=id) return@execute
                if (stopCapture) { emit(id,State.Unable("التسجيل قصير جدًا للتفريغ")); return@execute }
                val buffer = FloatArray(AudioInputGate.MAX_SAMPLES)
                samples = buffer
                input.startRecording()
                check(input.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                emit(id, State.Recording)
                val count = readLocalPcm(buffer, shouldStop = { stopCapture || generation.get() != id }) { chunk, length ->
                    input.read(chunk, 0, length, AudioRecord.READ_BLOCKING)
                }
                runCatching { input.stop() }; input.release(); active = null; recorder = null
                if (generation.get()!=id) return@execute
                if (count == null) {
                    emit(id, State.Unable("انقطع تسجيل الميكروفون. أعد المحاولة."))
                    return@execute
                }
                val pcm = if (count == buffer.size) buffer else buffer.copyOf(count).also { buffer.fill(0f) }
                try {
                    if (!AudioInputGate.usable(pcm)) { emit(id,State.Unable("غير قادر على التأكد: التسجيل قصير أو منخفض الصوت")); return@execute }
                    val processingStarted = SystemClock.elapsedRealtime()
                    emit(id,State.Processing)
                    val model = LocalWhisperModel.verifiedFile(app)
                    val handle = synchronized(this) {
                        if (generation.get()!=id || closed) 0L else NativeWhisper.createSession().also { nativeSession = it }
                    }
                    if (handle == 0L) return@execute
                    try {
                        val bytes = NativeWhisper.transcribe(handle,model.absolutePath,pcm)
                        val draft = bytes?.toString(Charsets.UTF_8)?.trim()?.take(2000)
                        val result = if (draft.isNullOrBlank()) {
                            State.Unable("غير قادر على التأكد من التفريغ")
                        } else {
                            State.Draft(draft, SystemClock.elapsedRealtime() - processingStarted)
                        }
                        emit(id, result)
                        bytes?.fill(0)
                    } finally { synchronized(this) {
                        if (nativeSession==handle) nativeSession = 0
                        NativeWhisper.release(handle)
                    } }
                } finally { pcm.fill(0f) }
            } catch (_: Throwable) {
                // Native loading/resource failures are not a successful assessment.
                emit(id,State.Unable("غير قادر على التأكد؛ تعذر تشغيل التفريغ المحلي على هذا الجهاز"))
            } finally {
                runCatching { active?.stop() }; active?.release(); recorder = null; samples?.fill(0f)
            }
        }
    }
    fun finishCapture() { stopCapture = true; runCatching { recorder?.stop() } }
    @Synchronized fun cancel() {
        generation.incrementAndGet(); stopCapture = true
        runCatching { recorder?.stop() }
        if (nativeSession != 0L) NativeWhisper.cancel(nativeSession)
    }
    @Synchronized fun close() { cancel(); closed = true; worker.shutdownNow() }
}
