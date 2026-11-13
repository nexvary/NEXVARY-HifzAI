package com.nexvary.hifzai

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import com.nexvary.hifzai.learning.CatalogRecording
import com.nexvary.hifzai.learning.LocalRecitationLibrary
import com.nexvary.hifzai.learning.ReciterProfile
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.QuranSearchIndex
import com.nexvary.hifzai.learning.RecordingAddress
import com.nexvary.hifzai.learning.SurahInfo
import com.nexvary.hifzai.learning.VerifiedVerse

@Composable
internal fun RecitationRangePreview(corpus: QuranCorpus, reciter: ReciterProfile?, current: VerifiedVerse,
    today: Long, library: LocalRecitationLibrary, playback: RecitationLibraryViewModel) {
    Text("نطاق التلاوة", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
    if (reciter == null) {
        Text("اختر قارئًا مفضلًا من القائمة أدناه للتحقق من توفر نطاق له.")
        RecitationPlaybackPanel(playback)
        return
    }

    val catalog = library.catalog
    val playbackState by playback.state.collectAsState()
    val context = LocalContext.current
    if (catalog.recordings.isEmpty()) Text("فهرس التسجيلات المأذونة المرفق فارغ حاليًا.")
    val initialAddress = "${current.chapter}:${current.verse}"
    var first by rememberSaveable(initialAddress) { mutableStateOf(initialAddress) }
    var last by rememberSaveable(initialAddress) { mutableStateOf(initialAddress) }
    var checked by rememberSaveable(initialAddress, reciter.slug) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    var choosingSurah by rememberSaveable { mutableStateOf(false) }
    Text("القارئ المختار: ${reciter.nameArabic}")
    OutlinedButton(onClick = { focus.clearFocus(); choosingSurah = true },
        modifier = Modifier.fillMaxWidth()) {
        Text("اختيار سورة للتلاوة")
    }
    if (choosingSurah) {
        RecitationSurahDialog(corpus.surahs, onDismiss = { choosingSurah = false }, select = { surah ->
            first = "${surah.number}:1"
            last = "${surah.number}:${surah.ayahCount}"
            checked = false
            choosingSurah = false
        })
    }
    Text("أدخل رقم السورة ثم رقم الآية، مثل ٢:٢٥٥.")
    OutlinedTextField(value = first, onValueChange = { first = it.take(15); checked = false },
        label = { Text("بداية نطاق التلاوة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = last, onValueChange = { last = it.take(15); checked = false },
        label = { Text("نهاية نطاق التلاوة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { focus.clearFocus(); checked = true }, modifier = Modifier.fillMaxWidth()) {
        Text("تحقق من توفر التلاوة")
    }
    if (checked) {
        val start = QuranSearchIndex.address(first)?.let { (chapter, ayah) -> RecordingAddress(chapter, ayah) }
        val end = QuranSearchIndex.address(last)?.let { (chapter, ayah) -> RecordingAddress(chapter, ayah) }
        val result: Result<List<CatalogRecording>> = remember(catalog, start, end, reciter.slug, today) {
            try {
                require(start != null && end != null) { "أدخل بداية ونهاية بصيغة سورة:آية" }
                Result.success(catalog.selectRange(start, end, reciter.slug, today))
            } catch (error: IllegalArgumentException) {
                Result.failure(error)
            }
        }
        val records = result.getOrNull()
        if (records == null) {
            Text(result.exceptionOrNull()?.message ?: "تعذر التحقق من توفر النطاق",
                Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else {
            Text("عدد التسجيلات المأذونة في النطاق: ${records.size}")
            LazyColumn(Modifier.heightIn(max = 160.dp)) {
                items(records, key = { it.rights.recordingId }) { record ->
                    Text("${corpus.info(record.address.chapter).arabicName} • الآية ${record.address.ayah}")
                }
            }
            var verseRepeats by rememberSaveable { mutableIntStateOf(0) }
            var rangeRepeats by rememberSaveable { mutableIntStateOf(0) }
            var speed by rememberSaveable { mutableFloatStateOf(1f) }
            RepeatChoice("الآية", verseRepeats) { verseRepeats = it }
            RepeatChoice("النطاق", rangeRepeats) { rangeRepeats = it }
            Text("سرعة النطاق الجديد ${playbackSpeedLabel(speed)}")
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..1.5f, steps = 3,
                modifier = Modifier.semantics { contentDescription = "سرعة النطاق الجديد" })
            OutlinedButton(onClick = {
                focus.clearFocus()
                playback.prepare(context as ComponentActivity, library, checkNotNull(start), checkNotNull(end),
                    reciter.slug, RecitationLibraryViewModel.Options(verseRepeats, rangeRepeats, speed))
            }, enabled = !playbackState.preparing && playbackState.playback?.phase !in
                setOf(com.nexvary.hifzai.learning.LocalRecitationQueue.Phase.READY,
                    com.nexvary.hifzai.learning.LocalRecitationQueue.Phase.PLAYING,
                    com.nexvary.hifzai.learning.LocalRecitationQueue.Phase.PAUSED),
                modifier = Modifier.fillMaxWidth()) { Text("تجهيز النطاق") }
        }
    }
    RecitationPlaybackPanel(playback)
}

@Composable
private fun RecitationSurahDialog(surahs: List<SurahInfo>, onDismiss: () -> Unit, select: (SurahInfo) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matching = remember(surahs, query) {
        val text = QuranSearchIndex.normalize(query).trim()
        val number = QuranSearchIndex.address("$text:1")?.first
        surahs.filter { it.number == number || QuranSearchIndex.normalize(it.arabicName).contains(text) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سور المصحف") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it.take(50) },
                    label = { Text("ابحث باسم السورة أو رقمها") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                if (matching.isEmpty()) Text("لا توجد سورة مطابقة")
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(matching, key = { it.number }) { surah ->
                        OutlinedButton(onClick = { select(surah) }, modifier = Modifier.fillMaxWidth()) {
                            Text("سورة ${surah.arabicName}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}


@Composable
private fun RepeatChoice(label: String, extra: Int, change: (Int) -> Unit) {
    Text("تكرار $label بعد المرة الأولى: $extra")
    Row {
        OutlinedButton(onClick = { change(extra - 1) }, enabled = extra > 0,
            modifier = Modifier.semantics { contentDescription = "تقليل تكرار $label" }) { Text("−") }
        OutlinedButton(onClick = { change(extra + 1) }, enabled = extra < 20,
            modifier = Modifier.semantics { contentDescription = "زيادة تكرار $label" }) { Text("+") }
    }
}
