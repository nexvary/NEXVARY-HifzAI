package com.nexvary.hifzai.learning

/** An adapter must be separately evaluated and licensed before being supplied to the app. */
interface OfflineRecitationRecognizer {
    val modelId: String
    fun recognize(privateAudioPath: String): RecognitionResult
}
data class RecognitionResult(val text: String, val confidence: Double, val riwayah: String)
sealed class RecitationAssessment {
    data class UnableToConfirm(val reason: String) : RecitationAssessment()
    /** Lexical index mismatches ONLY. Never a tajweed, articulation or religious verdict. */
    data class LexicalComparison(val editDistance: Int, val referenceWords: Int) : RecitationAssessment()
}

object RecitationComparator {
    /** Null threshold = no independent evaluation; fail closed even with confident ASR output. */
    fun compare(reference: VerifiedVerse, result: RecognitionResult,
        independentlyCalibratedThreshold: Double? = null): RecitationAssessment {
        if (independentlyCalibratedThreshold == null || !independentlyCalibratedThreshold.isFinite() ||
            independentlyCalibratedThreshold !in 0.0..1.0) return RecitationAssessment.UnableToConfirm("النموذج لم يُقيّم مستقلاً")
        if (!result.confidence.isFinite() || result.confidence !in 0.0..1.0 ||
            result.confidence < independentlyCalibratedThreshold) return RecitationAssessment.UnableToConfirm("غير قادر على التأكد: ثقة منخفضة")
        if (result.riwayah != "hafs-an-asim") return RecitationAssessment.UnableToConfirm("الرواية غير مطابقة للمرجع")
        val expected = QuranSearchIndex.normalize(reference.text).trim().split(Regex("\\s+"))
        val heard = QuranSearchIndex.normalize(result.text).trim().split(Regex("\\s+"))
        if (result.text.isBlank() || expected.size > 512 || heard.size > 512)
            return RecitationAssessment.UnableToConfirm("التسميع غير كافٍ أو أطول من نطاق التقييم")
        var previous = IntArray(heard.size + 1) { it }
        expected.forEachIndexed { i, word ->
            val current = IntArray(heard.size + 1)
            current[0] = i + 1
            heard.forEachIndexed { j, other ->
                current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1,
                    previous[j] + if (word == other) 0 else 1)
            }
            previous = current
        }
        return RecitationAssessment.LexicalComparison(previous.last(), expected.size)
    }
}
