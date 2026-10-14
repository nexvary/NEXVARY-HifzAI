package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexvary.hifzai.learning.*

@Composable
internal fun RangePracticeScreen(
    corpus: QuranCorpus, verses: List<VerifiedVerse>, store: ProgressStore,
    record: (VerifiedVerse, RecallGrade, Int, Boolean, Boolean) -> Unit, finish: () -> Unit
) {
    if (verses.isEmpty()) { Text("تعذر تحميل نطاق الحفظ"); return }
    val saved = remember(verses) { store.session()?.takeIf {
        it.rangeStart == verses.first().id && it.rangeEnd == verses.last().id
    } }
    var index by rememberSaveable(verses.first().id, verses.last().id) {
        mutableIntStateOf(saved?.let { s -> verses.indexOfFirst { it.chapter == s.chapter && it.verse == s.ayah } }?.coerceAtLeast(0) ?: 0)
    }
    var stage by rememberSaveable(verses.first().id, verses.last().id) { mutableStateOf(saved?.stage ?: PracticeKind.READ) }
    var hints by rememberSaveable { mutableIntStateOf(saved?.hints ?: 0) }
    var exposed by rememberSaveable { mutableStateOf(saved?.exposed ?: false) }
    var reveal by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) typed = "" }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    val verse = verses[index]
    fun next() {
        if (index == verses.lastIndex) { store.clearSession(); finish() }
        else { index++; stage = PracticeKind.READ; hints = 0; exposed = false; reveal = false; typed = "" }
    }
    LaunchedEffect(index, stage, hints, exposed) {
        store.saveSession(PracticeSession(verse.chapter, verse.verse, stage, hints, exposed,
            DifficultyTag.NONE, verses.first().id, verses.last().id))
    }
    Text("جلسة حفظ النطاق", style = MaterialTheme.typography.headlineSmall)
    Text("سورة " + corpus.info(verse.chapter).arabicName + " • الآية " + verse.verse +
        " • " + (index+1) + " من " + verses.size + " في النطاق")
    Text("تعبر الجلسة السور عند الحاجة وتتوقف عند نهاية الجزء أو الصفحة؛ النص الأصلي لا يتغير.")
    if (stage == PracticeKind.BRIDGE) {
        BridgeTrainer.tailOfPrevious(verses, index, textForExercise = corpus::ayahText)?.let { tail ->
            Text("تلميح الربط من السابق:")
            Text(tail, fontFamily = quranTypeface, fontSize = 23.sp, lineHeight = 44.sp)
        } ?: Text("لا نربط آيتين من سورتين مختلفتين؛ استرجع الآية الحالية وحدها")
    }
    val fraction = RecallLadder.concealment(stage, hints, reveal)
    Card {
        if (stage == PracticeKind.READ) Box(Modifier.padding(20.dp)) { CanonicalQuranText(corpus, verse) }
        else Text(if (fraction == 1f) "الآية مخفية • استرجعها من ذاكرتك" else VerseMask.display(corpus.ayahText(verse), fraction, verse.id),
            fontFamily = if (fraction == 1f) null else quranTypeface,
            fontSize = if (fraction == 1f) 20.sp else 29.sp, lineHeight = 52.sp,
            modifier = Modifier.fillMaxWidth().padding(20.dp))
    }
    if (stage != PracticeKind.READ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { hints++ }, enabled = hints < 2, modifier = Modifier.weight(1f)) { Text("تلميح للنطاق") }
            OutlinedButton(onClick = { if (!reveal) exposed = true; reveal = !reveal }, modifier = Modifier.weight(1f)) {
                Text(if (reveal) "أخفِ الإجابة" else "اكشف آية النطاق")
            }
        }
    }
    if (stage == PracticeKind.RECALL || stage == PracticeKind.BRIDGE) {
        OutlinedTextField(value = typed, onValueChange = { typed = it.take(1800) },
            label = { Text("إجابة كتابية اختيارية لا تُحفظ") }, modifier = Modifier.fillMaxWidth())
        Text("تقييم ذاتي؛ لا تصحيح تجويد أو مخارج")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(RecallGrade.AGAIN to "نسيت", RecallGrade.HARD to "صعب", RecallGrade.GOOD to "جيد", RecallGrade.EASY to "سهل").forEach { (grade,label) ->
                OutlinedButton(onClick = { record(verse, grade, hints, exposed, stage == PracticeKind.BRIDGE)
                    if (grade == RecallGrade.AGAIN) { stage = PracticeKind.READ; hints = 0; exposed = false; reveal = false; typed = "" }
                    else next() }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
        if (stage == PracticeKind.RECALL) OutlinedButton(onClick = { stage = PracticeKind.BRIDGE }) { Text("اربط داخل النطاق") }
    } else Button(onClick = { stage = RecallLadder.next(stage); reveal = false }, modifier = Modifier.fillMaxWidth()) {
        Text("تابع سُلّم حفظ النطاق")
    }
    OutlinedButton(onClick = { next() }, modifier = Modifier.fillMaxWidth()) { Text("تجاوز دون تسجيل تقييم") }
    Text("التلميح والكشف يظلان مساعدة للمحاولة. التسجيل الصوتي الذاتي متاح في جلسة الآية؛ جلسة النطاق لا تطلب الميكروفون.")
}
