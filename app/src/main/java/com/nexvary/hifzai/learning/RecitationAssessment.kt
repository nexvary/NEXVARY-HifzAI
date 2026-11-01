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
        if (result.text.isBlank() || reference.text.length > 20_000 || result.text.length > 20_000)
            return RecitationAssessment.UnableToConfirm("التسميع غير كافٍ أو أطول من نطاق التقييم")
        val aligned = runCatching { LexicalAligner.align(reference.text, result.text) }.getOrNull()
            ?: return RecitationAssessment.UnableToConfirm("النص أطول من نطاق التقييم")
        if (aligned.referenceWords == 0 || aligned.edits.none { it.heardIndex != null })
            return RecitationAssessment.UnableToConfirm("لا توجد كلمات كافية للمقارنة")
        return RecitationAssessment.LexicalComparison(aligned.distance, aligned.referenceWords)
    }
}
