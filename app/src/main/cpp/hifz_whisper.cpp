// Original JNI wrapper, MIT. whisper.cpp/ggml and model licenses are bundled separately.
#include <jni.h>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <memory>
#include <string>
#include <vector>
#include "whisper.h"

struct Session {
    std::atomic<bool> cancelled{false};
    std::atomic<int> status{0}; // Technical state only; never transcript/audio.
    std::chrono::steady_clock::time_point deadline;
};
static bool abort_decode(void * raw) {
    auto * s = static_cast<Session *>(raw);
    return s->cancelled.load() || std::chrono::steady_clock::now() > s->deadline;
}
static void quiet_log(enum ggml_log_level, const char *, void *) { /* No private transcript/audio in logs. */ }
extern "C" JNIEXPORT jlong JNICALL
Java_com_nexvary_hifzai_learning_NativeWhisper_createSession(JNIEnv *, jclass) {
    try { return reinterpret_cast<jlong>(new Session()); } catch (...) { return 0; }
}
extern "C" JNIEXPORT void JNICALL
Java_com_nexvary_hifzai_learning_NativeWhisper_cancel(JNIEnv *, jclass, jlong handle) {
    if (handle) reinterpret_cast<Session *>(handle)->cancelled.store(true);
}
extern "C" JNIEXPORT jint JNICALL
Java_com_nexvary_hifzai_learning_NativeWhisper_status(JNIEnv *, jclass, jlong handle) {
    return handle ? reinterpret_cast<Session *>(handle)->status.load() : -1;
}
extern "C" JNIEXPORT void JNICALL
Java_com_nexvary_hifzai_learning_NativeWhisper_release(JNIEnv *, jclass, jlong handle) {
    delete reinterpret_cast<Session *>(handle);
}
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_nexvary_hifzai_learning_NativeWhisper_transcribe(JNIEnv * env, jclass,
    jlong handle, jstring model, jfloatArray input) {
    auto * s = reinterpret_cast<Session *>(handle);
    if (!s) return nullptr;
    s->status.store(1);
    if (s->cancelled.load()) { s->status.store(4); return nullptr; }
    if (!model || !input) return nullptr;
    const jsize count = env->GetArrayLength(input);
    if (count < 8000 || count > 480000) return nullptr; // 0.5–30 s, 16 kHz mono only.
    try {
        std::vector<float> pcm(count);
        env->GetFloatArrayRegion(input, 0, count, pcm.data());
        if (env->ExceptionCheck()) return nullptr;
        for (float value : pcm) if (!std::isfinite(value) || std::abs(value)>1.f) return nullptr;
        const char * path = env->GetStringUTFChars(model, nullptr);
        if (!path) return nullptr;
        std::string model_path(path);
        env->ReleaseStringUTFChars(model, path);
        s->status.store(2);
        whisper_log_set(quiet_log, nullptr);
        auto cp = whisper_context_default_params(); cp.use_gpu = false;
        std::unique_ptr<whisper_context, decltype(&whisper_free)> ctx(
            whisper_init_from_file_with_params(model_path.c_str(), cp), whisper_free);
        if (!ctx) return nullptr;
        if (s->cancelled.load()) { s->status.store(4); return nullptr; }
        s->deadline = std::chrono::steady_clock::now()+std::chrono::seconds(60);
        auto p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        p.n_threads = 2; p.translate = false; p.language = "ar";
        p.no_context = true; p.no_timestamps = true; p.single_segment = true;
        p.print_special = false; p.print_progress = false;
        p.print_realtime = false; p.print_timestamps = false;
        p.suppress_nst = true; p.temperature = 0.f; p.temperature_inc = 0.f;
        p.max_tokens = 128; p.abort_callback = abort_decode; p.abort_callback_user_data = s;
        s->status.store(5);
        const int result = whisper_full(ctx.get(), p, pcm.data(), count);
        std::fill(pcm.begin(), pcm.end(), 0.f);
        if (result != 0 || abort_decode(s)) {
            if (s->cancelled.load()) s->status.store(4);
            else if (std::chrono::steady_clock::now() > s->deadline) s->status.store(3);
            return nullptr;
        }
        s->status.store(6);
        std::string draft;
        for (int i=0; i<whisper_full_n_segments(ctx.get()); i++) {
            draft += whisper_full_get_segment_text(ctx.get(), i);
            if (draft.size()>8192) return nullptr;
        }
        // Bytes, not NewStringUTF: model output is UTF-8 and may contain non-BMP characters.
        jbyteArray output = env->NewByteArray(static_cast<jsize>(draft.size()));
        if (output) env->SetByteArrayRegion(output, 0, static_cast<jsize>(draft.size()),
                                          reinterpret_cast<const jbyte *>(draft.data()));
        if (output) s->status.store(7);
        return output;
    } catch (...) { s->status.store(8); return nullptr; }
}
