package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.*

@Composable
internal fun PartitionPickerScreen(corpus: QuranCorpus, partitions: QuranPartitions, train: (List<VerifiedVerse>) -> Unit) {
    var kind by remember { mutableStateOf(PartitionKind.PAGE) }
    var query by remember { mutableStateOf("") }
    Text("الحفظ بالأجزاء والصفحات", style = MaterialTheme.typography.headlineSmall)
    Text("حدود الآيات من بيانات Tanzil لمصحف المدينة. العرض النصي يتدفق بحسب الخط والشاشة؛ ليس صورة مطابقة لتوزيع سطور الصفحة المطبوعة.")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { kind = PartitionKind.PAGE; query = "" }, modifier = Modifier.weight(1f)) { Text("604 صفحات") }
        OutlinedButton(onClick = { kind = PartitionKind.JUZ; query = "" }, modifier = Modifier.weight(1f)) { Text("30 جزءًا") }
    }
    OutlinedTextField(value = query, onValueChange = { query = it.take(3) }, label = { Text("رقم " + kind.label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true)
    val number = query.map { Character.digit(it, 10) }.takeIf { it.all { digit -> digit >= 0 } }
        ?.joinToString("")?.toIntOrNull()
    val choices = partitions.ofKind(kind)
    val visible = if (query.isBlank()) choices.take(15) else choices.filter { it.number == number }
    if (visible.isEmpty()) Text("رقم غير موجود؛ الصفحات 1–604 والأجزاء 1–30")
    if (query.isBlank()) Text("أول 15 اختيارًا؛ اكتب رقمًا للانتقال مباشرة")
    visible.forEach { partition ->
        val verses = partitions.verses(partition, corpus)
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(kind.label + " " + partition.number + " • " + verses.size + " آية")
                Text("من " + corpus.info(verses.first().chapter).arabicName + ":" + verses.first().verse +
                    " إلى " + corpus.info(verses.last().chapter).arabicName + ":" + verses.last().verse)
                Button(onClick = { train(verses) }, modifier = Modifier.fillMaxWidth()) { Text("ابدأ حفظ هذا النطاق") }
            }
        }
    }
}
