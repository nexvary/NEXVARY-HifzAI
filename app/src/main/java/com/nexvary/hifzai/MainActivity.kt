package com.nexvary.hifzai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.nexvary.hifzai.learning.DemoVerses
import com.nexvary.hifzai.learning.HifzScheduler
import com.nexvary.hifzai.learning.RecallGrade
import com.nexvary.hifzai.learning.ReviewCard
import com.nexvary.hifzai.learning.VerseMask
import java.time.LocalDate

private val navy = Color(0xFF081A25)
private val silver = Color(0xFFDCE4E8)
private val gold = Color(0xFFE4BD67)
private val green = Color(0xFF39D7A3)

private enum class PracticeStage { READ, HIDE, RECALL }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = gold,
                onPrimary = navy,
                background = navy,
                onBackground = silver,
                surface = Color(0xFF142C38),
                onSurface = silver
            )) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HifzScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HifzScreen() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("hifz_progress_v1", Context.MODE_PRIVATE)
    }
    val verses = DemoVerses.alIkhlas
    val cards = remember(preferences) {
        mutableStateMapOf<Int, ReviewCard>().apply {
            verses.forEach { verse ->
                val id = verse.id
                put(id, ReviewCard(
                    verseId = id,
                    repetitions = preferences.getInt("repetitions_$id", 0),
                    intervalDays = preferences.getInt("interval_$id", 0),
                    dueEpochDay = preferences.getLong("due_$id", 0L),
                    lapses = preferences.getInt("lapses_$id", 0),
                    ease = java.lang.Double.longBitsToDouble(
                        preferences.getLong("ease_$id", java.lang.Double.doubleToRawLongBits(2.5))
                    )
                ))
            }
        }
    }
    var verseIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(PracticeStage.READ) }
    var revealed by remember { mutableStateOf(false) }
    val verse = verses[verseIndex]
    val currentCard = cards[verse.id] ?: ReviewCard(verse.id)
    val today = LocalDate.now().toEpochDay()
    val completed = cards.values.count { it.repetitions > 0 }

    fun move(index: Int) {
        verseIndex = index.coerceIn(0, verses.lastIndex)
        stage = PracticeStage.READ
        revealed = false
    }

    fun submit(result: RecallGrade) {
        val updated = HifzScheduler.grade(currentCard, result, today)
        cards[verse.id] = updated
        preferences.edit()
            .putInt("repetitions_${verse.id}", updated.repetitions)
            .putInt("interval_${verse.id}", updated.intervalDays)
            .putLong("due_${verse.id}", updated.dueEpochDay)
            .putInt("lapses_${verse.id}", updated.lapses)
            .putLong("ease_${verse.id}", java.lang.Double.doubleToRawLongBits(updated.ease))
            .apply()
        move(if (verseIndex == verses.lastIndex) 0 else verseIndex + 1)
    }

    Column(
        Modifier.fillMaxSize().background(navy).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("حِفْظ | NEXVARY HifzAI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = gold)
        Text("احفظ بالفهم والتذكّر... وراجع ما تحتاج إليه فعلاً", fontSize = 15.sp, color = silver)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF112934))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("جلسة تجريبية • سورة الإخلاص", color = gold, fontSize = 18.sp)
                Text("تقدمك: $completed من 4 آيات • تتم المراجعة على هذا الجهاز", color = silver)
                Text("النسخة الأولية: التقييم ذاتي، وليس تصحيحًا آليًا للتلاوة.", color = silver, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("الآية ${verse.verse} من 4", fontSize = 19.sp, color = gold)
            Text(
                if (currentCard.repetitions == 0) "لم تبدأ مراجعتها"
                else if (currentCard.dueEpochDay <= today) "موعد المراجعة الآن"
                else "بعد ${currentCard.dueEpochDay - today} يوم",
                color = green, fontSize = 13.sp
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("١ • قراءة", stage == PracticeStage.READ, Modifier.weight(1f)) {
                stage = PracticeStage.READ; revealed = false
            }
            StepButton("٢ • إخفاء", stage == PracticeStage.HIDE, Modifier.weight(1f)) {
                stage = PracticeStage.HIDE; revealed = false
            }
            StepButton("٣ • تسميع", stage == PracticeStage.RECALL, Modifier.weight(1f)) {
                stage = PracticeStage.RECALL; revealed = false
            }
        }
        val shown = when {
            stage == PracticeStage.READ || revealed -> verse.text
            stage == PracticeStage.HIDE -> VerseMask.display(verse.text, 0.5f, verse.id)
            else -> VerseMask.display(verse.text, 1f, verse.id)
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF183542))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    shown,
                    fontSize = 29.sp,
                    lineHeight = 51.sp,
                    fontWeight = FontWeight.Medium,
                    color = silver,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (stage != PracticeStage.READ) {
                    OutlinedButton(onClick = { revealed = !revealed }) {
                        Text(if (revealed) "إخفاء الإجابة" else "إظهار الآية")
                    }
                }
            }
        }
        val guidance = when (stage) {
            PracticeStage.READ -> "اقرأ الآية وتأمل كلماتها، ثم انتقل إلى الإخفاء."
            PracticeStage.HIDE -> "استرجع الكلمات المخفية قبل الضغط على إظهار الآية."
            PracticeStage.RECALL -> "سمّع الآية من الذاكرة، ثم قيّم نفسك بصدق. لا يوجد تعرّف صوتي بعد."
        }
        Text(guidance, color = silver, fontSize = 15.sp)
        if (stage == PracticeStage.RECALL) {
            Text("كيف كان استرجاعك؟", color = gold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GradeButton("نسيت", Modifier.weight(1f)) { submit(RecallGrade.AGAIN) }
                GradeButton("صعب", Modifier.weight(1f)) { submit(RecallGrade.HARD) }
                GradeButton("جيد", Modifier.weight(1f)) { submit(RecallGrade.GOOD) }
                GradeButton("سهل", Modifier.weight(1f)) { submit(RecallGrade.EASY) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { move(verseIndex - 1) }, enabled = verseIndex > 0) {
                Text("السابقة")
            }
            OutlinedButton(onClick = { move(verseIndex + 1) }, enabled = verseIndex < verses.lastIndex) {
                Text("التالية")
            }
        }
        Text(
            "مصدر التحقق للنموذج: Quran.com/112 • تلاوات المشايخ والتحقق الصوتي قيد التطوير.",
            fontSize = 12.sp, color = Color(0xFF9AAFB9)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@androidx.compose.runtime.Composable
private fun StepButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@androidx.compose.runtime.Composable
private fun GradeButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = navy)
    ) {
        Text(label, fontSize = 13.sp, maxLines = 1)
    }
}
