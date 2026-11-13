package com.nexvary.hifzai

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.nexvary.hifzai.learning.LocalRecitationQueue.Phase
import com.nexvary.hifzai.learning.ReciterCatalog
import java.util.Locale

internal fun playbackSpeedLabel(speed: Float): String = String.format(Locale.ROOT, "%.2f×", speed)

@Composable
internal fun RecitationPlaybackPanel(controller: RecitationLibraryViewModel) {
    val state by controller.state.collectAsState()
    val snapshot = state.playback
    val phase = snapshot?.phase
    val status = state.message.ifEmpty {
        when (phase) {
            Phase.READY -> "النطاق جاهز للتشغيل"
            Phase.PLAYING -> "قيد تشغيل التلاوة"
            Phase.PAUSED -> "التلاوة متوقفة مؤقتًا"
            Phase.LOADING, Phase.EMPTY -> "جارٍ تجهيز التشغيل…"
            else -> ""
        }
    }
    if (status.isEmpty()) return
    Text(status, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    if (phase in setOf(Phase.READY, Phase.PLAYING, Phase.PAUSED)) {
        ReciterCatalog.all.firstOrNull { it.slug == snapshot?.reciterId }?.let { Text(it.nameArabic) }
        snapshot?.address?.let { Text("سورة ${it.chapter} • الآية ${it.ayah}") }
        if (snapshot != null) {
            Text("الملف ${(snapshot.index ?: 0) + 1} من ${snapshot.size} • دورة ${snapshot.rangePass} من ${snapshot.rangePasses}")
        }
        val speed = snapshot?.playback?.speed ?: 1f
        Text("سرعة التلاوة الحالية ${playbackSpeedLabel(speed)}")
        Slider(value = speed, onValueChange = controller::speed, valueRange = 0.5f..1.5f, steps = 3,
            modifier = Modifier.semantics { contentDescription = "سرعة التلاوة الحالية" })
        val playing = phase == Phase.PLAYING
        OutlinedButton(onClick = { if (playing) controller.pause() else controller.play() },
            modifier = Modifier.fillMaxWidth()) {
            Text(when (phase) {
                Phase.PLAYING -> "إيقاف التلاوة مؤقتًا"
                Phase.PAUSED -> "استئناف التلاوة"
                else -> "تشغيل النطاق المجهز"
            })
        }
    }
    OutlinedButton(onClick = controller::clear, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.preparing || phase in setOf(Phase.READY, Phase.PLAYING, Phase.PAUSED))
            "إيقاف التلاوة ومسح القائمة" else "إغلاق حالة التشغيل")
    }
}
