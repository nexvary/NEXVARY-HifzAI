package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.nexvary.hifzai.learning.*
import kotlin.math.roundToInt

@Composable
internal fun LearningGoalControls(goal: LearningGoal, minutes: Int?,
    timeChanged: (Int?) -> Unit, changed: (LearningGoal) -> Unit) {
    Text("خطتك اليومية", style = MaterialTheme.typography.titleLarge)
    Text("حدود مقترحة لليوم حسب تاريخ الهاتف والتقييمات المسجلة. يبقى اختيار أي آية يدويًا متاحًا، دون سلسلة أيام إلزامية أو وعد بنتيجة تعليمية.")
    StudyMode.entries.forEach { mode ->
        OutlinedButton(onClick = { changed(goal.copy(mode = mode)) }, modifier = Modifier.fillMaxWidth()) {
            Text(mode.arabic + if (goal.mode == mode) " • محدد" else "")
        }
    }
    Text("الوقت اليومي المتاح: " + (minutes?.let { "$it دقيقة" } ?: "غير محدد"))
    val selectedMinutes = minutes ?: 15
    Slider(value = StudyTimeBudget.choices.indexOf(selectedMinutes).toFloat(),
        onValueChange = { timeChanged(StudyTimeBudget.choices[it.roundToInt()]) },
        valueRange = 0f..StudyTimeBudget.choices.lastIndex.toFloat(),
        steps = StudyTimeBudget.choices.size - 2,
        modifier = Modifier.semantics { contentDescription = "الوقت اليومي المتاح بالدقائق" })
    val suggestion = StudyTimeBudget.suggest(selectedMinutes, goal.mode)
    Text("اقتراح لـ $selectedMinutes دقيقة: ${suggestion.newVerses} آية جديدة و" +
        (if (goal.mode == StudyMode.NEW_ONLY) "٠ مراجعات" else "${suggestion.reviews} مراجعات"))
    Text("تقدير قابل للتعديل يفترض ٣ دقائق للآية الجديدة ودقيقة للمراجعة؛ قد تحتاج الآية وقتًا أطول. تغيير الوقت لا يعدّل أهدافك حتى تختار تطبيق الاقتراح.")
    OutlinedButton(onClick = { timeChanged(selectedMinutes); changed(suggestion) },
        modifier = Modifier.fillMaxWidth()) { Text("تطبيق اقتراح الوقت") }
    if (minutes != null) TextButton(onClick = { timeChanged(null) }) { Text("إلغاء تحديد الوقت") }
    Text("هدف الحفظ الجديد: ${goal.newVerses} آية في اليوم")
    Slider(value = goal.newVerses.toFloat(), onValueChange = { changed(goal.copy(newVerses = it.roundToInt())) },
        valueRange = 0f..20f, steps = 19, modifier = Modifier.semantics { contentDescription = "عدد آيات الحفظ الجديد" })
    Text("حد المراجعات المقترحة: ${goal.reviews} آية في اليوم")
    Slider(value = goal.reviews.toFloat(), onValueChange = { changed(goal.copy(reviews = it.roundToInt())) },
        valueRange = 1f..100f, steps = 98, modifier = Modifier.semantics { contentDescription = "حد آيات المراجعة" })
    if (goal.mode == StudyMode.NEW_ONLY && goal.newVerses == 0)
        Text("الهدف الحالي صفر؛ ارفع عدد الآيات أو اختر وضع المراجعة.")
}

@Composable
internal fun WeeklyLearningCard(summary: WeeklyLearningSummary, previous: WeeklyLearningSummary) {
    Text("آخر ٧ أيام", style = MaterialTheme.typography.titleLarge)
    Text("محاولات: ${summary.attempts} • باسترجاع مستقل: ${summary.independent} • بمساعدة: ${summary.assisted}")
    Text("تعثر: ${summary.forgotten} • أيام ممارسة: ${summary.activeDays}")
    Text("نسبة الاسترجاع المستقل: " + (summary.independentPercent?.let { "$it%" } ?: "لا توجد بيانات"))
    Text("الأسبوع السابق: ${previous.attempts} محاولة • " +
        (previous.independentPercent?.let { "$it% استرجاع مستقل" } ?: "لا توجد بيانات"))
    Text("تقييم ذاتي من آخر ٣٠ محاولة محفوظة لكل آية؛ لا يثبت تحسنًا سببيًا أو احتفاظًا طويل المدى.")
}
