package com.nexvary.hifzai

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexvary.hifzai.learning.LocalWhisperModel
import com.nexvary.hifzai.learning.NativeWhisper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.sin

/** Real model CPU inference on generated nonspeech. ZERO accuracy/recitation claims. */
@RunWith(AndroidJUnit4::class)
class NativeAsrSmokeTest {
    @Test fun verifiedBundledModelLoadsRunsAndHonorsCancellation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = LocalWhisperModel.verifiedFile(context)
        assertEquals(77691713L,model.length())
        val input = FloatArray(16000) { (sin(2*Math.PI*440*it/16000)*0.02).toFloat() }
        val session = NativeWhisper.createSession()
        assertTrue(session != 0L)
        val start = SystemClock.elapsedRealtime()
        try {
            val result = NativeWhisper.transcribe(session,model.absolutePath,input)
            assertNotNull("CPU inference must succeed; native status=${NativeWhisper.status(session)}, elapsed=${SystemClock.elapsedRealtime()-start}ms",result)
            assertTrue(result!!.size <= 8192)
            result.fill(0)
        } finally { NativeWhisper.release(session); input.fill(0f) }
        val cancelled = NativeWhisper.createSession()
        try {
            NativeWhisper.cancel(cancelled)
            assertNull(NativeWhisper.transcribe(cancelled,model.absolutePath,FloatArray(16000)))
        } finally { NativeWhisper.release(cancelled) }
        File(context.filesDir,"hifz-native-asr.json").writeText(JSONObject()
            .put("native_inference_completed",true).put("asr_accuracy_evaluated",false)
            .put("real_speech_samples",0).put("input","mathematically generated 440Hz tone, not Quran")
            .put("model_sha256",LocalWhisperModel.SHA256)
            .put("model_revision","5359861c739e955e79d9a303bcbc70fb988958b1")
            .put("native_revision","d1be6fde11ac6e0407606b4e42fe72d34add8037")
            .put("elapsed_ms",SystemClock.elapsedRealtime()-start).toString(2))
    }
}
