package com.nexvary.hifzai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.VerifiedVerse

/** Canonical addresses only; removing a place never changes learning or the reading stop. */
@Composable
internal fun SavedPlacesScreen(corpus: QuranCorpus, places: List<VerifiedVerse>,
    open: (VerifiedVerse) -> Unit, remove: (VerifiedVerse) -> Unit) {
    var page by remember(places.map { it.id }) { mutableIntStateOf(0) }
    Text("العلامات المرجعية • ${places.size} من 50")
    Text("مواضع محلية مستقلة عن موضع القراءة وتقدم الحفظ")
    if (places.isEmpty()) Text("لا توجد علامات بعد؛ أضف علامة من بطاقة الآية في المصحف")
    places.drop(page * 5).take(5).forEach { verse ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = "${corpus.info(verse.chapter).arabicName} • الآية ${verse.verse}"
            OutlinedButton(onClick = { open(verse) }, modifier = Modifier.fillMaxWidth()) {
                Text("انتقل إلى $label")
            }
            OutlinedButton(onClick = { remove(verse) }, modifier = Modifier.fillMaxWidth()) {
                Text("حذف علامة $label")
            }
        }
    }
    if (places.size > 5) {
        OutlinedButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.fillMaxWidth()) {
            Text("العلامات السابقة")
        }
        Text("صفحة ${page + 1} من ${(places.size + 4) / 5}")
        OutlinedButton(onClick = { page++ }, enabled = (page + 1) * 5 < places.size,
            modifier = Modifier.fillMaxWidth()) { Text("العلامات التالية") }
    }
}
