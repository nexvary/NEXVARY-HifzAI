package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.VerifiedVerse
import com.nexvary.hifzai.learning.VerseExercises

@Composable
internal fun VerseExercisesScreen(corpus: QuranCorpus, verses: List<VerifiedVerse>, index: Int, train: (VerifiedVerse) -> Unit) {
    var start by remember(verses) { mutableIntStateOf(index) }
    var candidates by remember(verses, start) { mutableStateOf(VerseExercises.orderChallenge(verses, start)) }
    var selected by remember(verses, start) { mutableStateOf(emptyList<VerifiedVerse>()) }
    var message by remember { mutableStateOf("") }
    Text("تدريبات الاسترجاع", style = MaterialTheme.typography.headlineSmall)
    Text("اختر الموضع ثم رتّب الآيات بالنقر على النصوص الأصلية؛ هذا تمرين تعليمي خارج شاشة المصحف.")
    Slider(value = start.toFloat(), onValueChange = { start = it.toInt().coerceIn(0, verses.lastIndex) },
        valueRange = 0f..verses.lastIndex.coerceAtLeast(1).toFloat(), enabled = verses.size > 1)
    Text("بدءًا من الآية " + (start + 1))
    OutlinedButton(onClick = { train(verses[start]) }, modifier = Modifier.fillMaxWidth()) {
        Text("استرجاع من هذا الموضع دون النظر")
    }
    VerseExercises.continuation(verses, start)?.let { next ->
        OutlinedButton(onClick = { train(next) }, modifier = Modifier.fillMaxWidth()) {
            Text("أكمل الآية التالية بعد الموضع المختار")
        }
    }
    Text("ترتيب الآيات: اختر الآية الأولى ثم التي تليها", color = MaterialTheme.colorScheme.primary)
    candidates.forEach { verse ->
        OutlinedButton(onClick = {
            selected = selected + verse
            candidates = candidates.filter { it.id != verse.id }
            if (candidates.isEmpty()) message = if (VerseExercises.isCanonicalOrder(selected))
                "الترتيب مطابق لتسلسل المصدر؛ لا يُقيّم ذلك صحة التلاوة" else "الترتيب مختلف؛ أعد التدريب أو راجع التسلسل الأصلي"
        }, modifier = Modifier.fillMaxWidth()) {
            CanonicalQuranText(corpus, verse, 23)
        }
    }
    if (selected.isNotEmpty()) Text("اختيارك بأرقام الآيات: " + selected.joinToString(" ← ") { it.verse.toString() })
    if (message.isNotEmpty()) Text(message)
    OutlinedButton(onClick = {
        candidates = VerseExercises.orderChallenge(verses, start); selected = emptyList(); message = ""
    }) { Text("إعادة التدريب") }
}
