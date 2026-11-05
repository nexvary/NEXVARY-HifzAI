package com.nexvary.hifzai

import androidx.compose.foundation.layout.Column
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
import com.nexvary.hifzai.learning.RecitationCatalog
import com.nexvary.hifzai.learning.ReciterCatalog
import com.nexvary.hifzai.learning.RecordingAddress
import com.nexvary.hifzai.learning.SurahInfo
import com.nexvary.hifzai.learning.VerifiedVerse

@Composable
internal fun RecitationRangePreview(corpus: QuranCorpus, favorite: String?, current: VerifiedVerse, today: Long) {
    Text("نطاق التلاوة", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
    val reciter = ReciterCatalog.all.firstOrNull { it.slug == favorite }
    if (reciter == null) {
        Text("اختر قارئًا مفضلًا من القائمة أدناه للتحقق من توفر نطاق له.")
        return
    }

    // Only reviewed, bundled records can populate this catalogue.
    val catalog = remember(corpus) { RecitationCatalog.create(emptyList(), corpus) }
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
        val status = remember(catalog, first, last, reciter.slug, today) {
            val start = QuranSearchIndex.address(first)?.let { (chapter, ayah) -> RecordingAddress(chapter, ayah) }
            val end = QuranSearchIndex.address(last)?.let { (chapter, ayah) -> RecordingAddress(chapter, ayah) }
            if (start == null || end == null) "أدخل بداية ونهاية بصيغة سورة:آية"
            else try {
                val records = catalog.selectRange(start, end, reciter.slug, today)
                "عدد التسجيلات المأذونة في النطاق: ${records.size}"
            } catch (error: IllegalArgumentException) {
                error.message ?: "تعذر التحقق من توفر النطاق"
            }
        }
        Text(status, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
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
