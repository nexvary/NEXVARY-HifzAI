package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.LocalAudioPlayer
import com.nexvary.hifzai.learning.PrivateRecitationPractice
import kotlinx.coroutines.delay
import java.util.Locale

/** Optional controls for the user's temporary audio. Never labels it verified recitation. */
@Composable
internal fun PrivateAudioControls(audio: PrivateRecitationPractice, onMessage: (String) -> Unit) {
    var state by remember(audio) { mutableStateOf(audio.playback()) }
    var expanded by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(audio) {
        while (true) { state = audio.playback(); delay(200) }
    }
    fun update(operation: () -> Unit) {
        try { operation(); state = audio.playback() }
        catch (_: Exception) { onMessage("تعذر تعديل الاستماع؛ حاول إعادة تشغيل التسجيل") }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            update {
                if (state.phase == LocalAudioPlayer.Phase.PLAYING) { audio.pause(); onMessage("تم إيقاف الاستماع مؤقتًا") }
                else onMessage(if (audio.play()) "تشغيل تسجيلك الخاص" else "تعذر تشغيل التسجيل؛ تأكد من عدم وجود مكالمة")
            }
        }, modifier = Modifier.weight(1f)) {
            Text(if (state.phase == LocalAudioPlayer.Phase.PLAYING) "إيقاف مؤقت" else "استمع لصوتك")
        }
        OutlinedButton(onClick = { audio.erase(); onMessage("تم حذف التسجيل المحلي") }, modifier = Modifier.weight(1f)) {
            Text("احذف التسجيل")
        }
    }
    if (state.durationMs > 0) {
        Text("${clock(state.positionMs)} / ${clock(state.durationMs)} • مرات الوصول لنهاية التسجيل: ${state.completedPlays}")
        Slider(value = seeking ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat()),
            onValueChange = { seeking = it }, onValueChangeFinished = {
                seeking?.let { value -> update { audio.seek(value.toInt()) } }; seeking = null
            }, valueRange = 0f..state.durationMs.toFloat(),
            modifier = Modifier.semantics { contentDescription = "موضع الاستماع داخل التسجيل" })
    }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "إخفاء خيارات الاستماع" else "خيارات السرعة والتكرار")
    }
    if (expanded) {
        Text("سرعة الاستماع: ${String.format(Locale.ROOT, "%.2f", state.speed)}× • التسجيل الأصلي لا يتغير")
        Slider(value = state.speed, onValueChange = { value -> update { audio.speed(value) } },
            valueRange = 0.5f..1.5f, steps = 3,
            modifier = Modifier.semantics { contentDescription = "سرعة الاستماع" })
        Text("تكرار إضافي بعد المرة الأولى: ${state.extraRepeats}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { update { audio.repeats(state.extraRepeats - 1) } }, enabled = state.extraRepeats > 0) { Text("تقليل التكرار") }
            OutlinedButton(onClick = { update { audio.repeats(state.extraRepeats + 1) } }, enabled = state.extraRepeats < 20) { Text("زيادة التكرار") }
        }
        Text("يبقى صوتك مؤقتًا ويُحذف عند مغادرة التدريب أو وضع التطبيق في الخلفية. تغيير السرعة للاستماع الذاتي فقط؛ ليس تقييمًا للتجويد.")
    }
}

private fun clock(milliseconds: Int): String = String.format(Locale.ROOT, "%d:%02d",
    milliseconds / 60000, milliseconds / 1000 % 60)
