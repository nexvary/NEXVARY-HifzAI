package com.nexvary.hifzai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexvary.hifzai.learning.BridgeTrainer
import com.nexvary.hifzai.learning.DemoVerses
import com.nexvary.hifzai.learning.HifzScheduler
import com.nexvary.hifzai.learning.MemoryCoach
import com.nexvary.hifzai.learning.MemoryFingerprint
import com.nexvary.hifzai.learning.PracticeKind
import com.nexvary.hifzai.learning.ProgressStore
import com.nexvary.hifzai.learning.RecallGrade
import com.nexvary.hifzai.learning.ReviewCard
import com.nexvary.hifzai.learning.VerseMask
import java.time.LocalDate

private val navy = Color(0xFF071722)
private val panel = Color(0xFF142C38)
private val silver = Color(0xFFE0E7ED)
private val gold = Color(0xFFE4BE6A)
private val green = Color(0xFF45DDB0)
private val muted = Color(0xFFA5BAC5)

private enum class Screen { HOME, PRACTICE, INSIGHTS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = gold, onPrimary = navy,
                    background = navy, onBackground = silver,
                    surface = panel, onSurface = silver
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember(context) { ProgressStore(context) }
    val verses = DemoVerses.alIkhlas
    val cards = remember(store) {
        mutableStateMapOf<Int, ReviewCard>().apply {
            verses.forEach { put(it.id, store.card(it.id)) }
        }
    }
    val memories = remember(store) {
        mutableStateMapOf<Int, MemoryFingerprint>().apply {
            verses.forEach { put(it.id, store.memory(it.id)) }
        }
    }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var index by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(PracticeKind.READ) }
    var hintCount by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val verse = verses[index]
    val currentCard = cards[verse.id] ?: ReviewCard(verse.id)
    val fingerprint = memories[verse.id] ?: MemoryFingerprint(verse.id)
    val recommendation = MemoryCoach.recommend(currentCard, fingerprint)
    val dueCount = MemoryCoach.dueCount(cards.values, today)
    val studied = cards.values.count { it.repetitions > 0 }
    val independent = MemoryCoach.independentRecallPercent(memories.values)
    val attempts = memories.values.sumOf { it.attempts }

    fun openVerse(newIndex: Int, training: PracticeKind = PracticeKind.READ) {
        index = newIndex.coerceIn(0, verses.lastIndex)
        stage = training
        hintCount = 0
        revealed = false
        screen = Screen.PRACTICE
    }

    fun focusToday() {
        val first = MemoryCoach.studyOrder(verses, cards, today).first()
        val nextIndex = verses.indexOfFirst { it.id == first.id }.coerceAtLeast(0)
        openVerse(nextIndex, MemoryCoach.recommend(
            cards[first.id] ?: ReviewCard(first.id),
            memories[first.id] ?: MemoryFingerprint(first.id)
        ).kind)
    }

    fun grade(grade: RecallGrade) {
        // A hinted attempt must not be presented as an unassisted "easy" recall.
        val effectiveGrade = if (hintCount > 0 && grade == RecallGrade.EASY) RecallGrade.GOOD else grade
        val newCard = HifzScheduler.grade(currentCard, effectiveGrade, today)
        val newMemory = MemoryCoach.record(
            fingerprint, grade, hintCount, stage == PracticeKind.BRIDGE
        )
        cards[verse.id] = newCard
        memories[verse.id] = newMemory
        store.save(newCard, newMemory)
        message = "تم تسجيل تقييم الآية " + verse.verse + " • المراجعة القادمة بعد " +
            newCard.intervalDays + " يوم"
        val next = (index + 1) % verses.size
        openVerse(next)
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Scaffold(
        containerColor = navy,
        bottomBar = {
            NavigationBar(containerColor = panel) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = screen == Screen.PRACTICE,
                    onClick = { screen = Screen.PRACTICE },
                    icon = { Icon(Icons.Rounded.AutoStories, contentDescription = null) },
                    label = { Text("الحفظ") }
                )
                NavigationBarItem(
                    selected = screen == Screen.INSIGHTS,
                    onClick = { screen = Screen.INSIGHTS },
                    icon = { Icon(Icons.Rounded.Insights, contentDescription = null) },
                    label = { Text("تقدمي") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("الإعدادات") }
                )
            }
        }
    ) { inset ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inset)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("حِفْظ | NEXVARY", color = gold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("مصحف تدريبي يتكيّف مع طريقة استرجاعك", color = muted, fontSize = 14.sp)
            if (message.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF103B37))) {
                    Text(message, modifier = Modifier.padding(12.dp), color = green)
                }
            }
            when (screen) {
                Screen.HOME -> {
                    Section("خطة اليوم", "سورة الإخلاص • نموذج تدريبي محدود")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("مراجعات مستحقة", dueCount.toString(), Modifier.weight(1f))
                        MetricCard("آيات دُرست", studied.toString() + " / " + verses.size, Modifier.weight(1f))
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panel),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.School, contentDescription = null, tint = gold)
                            Text("رحلة الحفظ المتكيّف", fontSize = 23.sp, color = gold)
                            Text("اقرأ، ثم استرجع الكلمات المخفية، ثم سمّع دون النظر. سجّل نتيجتك لتحديد المراجعة القادمة.")
                            PrimaryButton("ابدأ جلسة اليوم", Modifier.fillMaxWidth()) { focusToday() }
                        }
                    }
                    Section("الخطوات", "تعلّم → إخفاء → استرجاع → ربط → مراجعة")
                    Text("التقييم في هذه النسخة ذاتي، ولا يوجد تصحيح صوتي أو حكم آلي على التجويد.",
                        color = muted, fontSize = 13.sp)
                }
                Screen.PRACTICE -> {
                    Section("جلسة الحفظ", "الآية " + verse.verse + " من " + verses.size)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StageButton("قراءة", stage == PracticeKind.READ, Modifier.weight(1f)) {
                            stage = PracticeKind.READ; hintCount = 0; revealed = false
                        }
                        StageButton("إخفاء", stage == PracticeKind.MASK_HEAVY, Modifier.weight(1f)) {
                            stage = PracticeKind.MASK_HEAVY; hintCount = 0; revealed = false
                        }
                        StageButton("تسميع", stage == PracticeKind.RECALL, Modifier.weight(1f)) {
                            stage = PracticeKind.RECALL; hintCount = 0; revealed = false
                        }
                        StageButton("ربط", stage == PracticeKind.BRIDGE, Modifier.weight(1f)) {
                            stage = PracticeKind.BRIDGE; hintCount = 0; revealed = false
                        }
                    }
                    if (stage == PracticeKind.BRIDGE) {
                        Text(
                            "من نهاية الآية السابقة:",
                            color = muted, fontSize = 14.sp
                        )
                        Text(
                            BridgeTrainer.tailOfPrevious(verses, index)
                                ?: "هذه أول آية، اختر الآية التالية لتدريب الربط.",
                            color = green, fontSize = 20.sp
                        )
                    }
                    val fraction = when {
                        revealed || stage == PracticeKind.READ -> 0f
                        stage == PracticeKind.MASK_LIGHT -> 0.35f
                        stage == PracticeKind.MASK_HEAVY -> recommendation.maskFraction.coerceAtLeast(0.65f)
                        hintCount >= 2 -> 0.55f
                        hintCount == 1 -> 0.8f
                        else -> 1f
                    }
                    val visualText = VerseMask.display(verse.text, fraction, verse.id)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = panel)
                    ) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp)) {
                            Text(
                                visualText, modifier = Modifier.fillMaxWidth(),
                                fontSize = 29.sp, lineHeight = 52.sp,
                                fontWeight = FontWeight.Medium, color = silver,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Text(recommendation.explanation, color = muted, fontSize = 14.sp)
                    if (stage != PracticeKind.READ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!revealed && hintCount < 2) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { hintCount += 1 }
                                ) { Text("تلميح") }
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { revealed = !revealed }
                            ) { Text(if (revealed) "إخفاء الإجابة" else "كشف الآية") }
                        }
                    }
                    if (stage == PracticeKind.RECALL || stage == PracticeKind.BRIDGE) {
                        Text("قيّم استرجاعك بعد التسميع من الذاكرة",
                            color = gold, fontSize = 17.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            ScoreButton("نسيت", Modifier.weight(1f)) { grade(RecallGrade.AGAIN) }
                            ScoreButton("صعب", Modifier.weight(1f)) { grade(RecallGrade.HARD) }
                            ScoreButton("جيد", Modifier.weight(1f)) { grade(RecallGrade.GOOD) }
                            ScoreButton("سهل", Modifier.weight(1f)) { grade(RecallGrade.EASY) }
                        }
                    } else {
                        PrimaryButton(
                            "انتقل إلى التسميع", Modifier.fillMaxWidth()
                        ) { stage = PracticeKind.RECALL; hintCount = 0; revealed = false }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { openVerse(index - 1) },
                            enabled = index > 0
                        ) { Text("الآية السابقة") }
                        OutlinedButton(
                            onClick = { openVerse(index + 1) },
                            enabled = index < verses.lastIndex
                        ) { Text("الآية التالية") }
                    }
                    Text("لا تستخدم الذكاء الاصطناعي للحكم على صحة نطقك في هذه النسخة.",
                        color = muted, fontSize = 12.sp)
                }
                Screen.INSIGHTS -> {
                    Section("بصمة الذاكرة", "المقاييس تعتمد على تقييماتك الذاتية")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("محاولات الاسترجاع", attempts.toString(), Modifier.weight(1f))
                        MetricCard("بدون تلميح", independent.toString() + "%", Modifier.weight(1f))
                    }
                    verses.forEachIndexed { i, item ->
                        val memory = memories[item.id] ?: MemoryFingerprint(item.id)
                        val card = cards[item.id] ?: ReviewCard(item.id)
                        val advice = MemoryCoach.recommend(card, memory)
                        Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                            Column(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("الآية " + item.verse + " • " + memory.attempts + " محاولات",
                                    color = gold, fontSize = 17.sp)
                                Text(advice.explanation, color = silver, fontSize = 14.sp)
                                Text(
                                    if (card.repetitions == 0) "لم تُراجع بعد"
                                    else if (card.dueEpochDay <= today) "المراجعة مستحقة"
                                    else "المراجعة بعد " + (card.dueEpochDay - today) + " يوم",
                                    color = green, fontSize = 13.sp
                                )
                                OutlinedButton(
                                    onClick = { openVerse(i, advice.kind) }
                                ) { Text("تدريب هذه الآية") }
                            }
                        }
                    }
                }
                Screen.SETTINGS -> {
                    Section("خصوصية البيانات", "كل تقدمك محفوظ محليًا على هذا الهاتف")
                    Text("لا توجد حسابات أو رفع تسجيلات صوتية أو اتصال بخوادم في النموذج الأولي.",
                        color = silver)
                    PrimaryButton("مشاركة ملخص التقدم", Modifier.fillMaxWidth()) {
                        val summary = "حِفْظ | NEXVARY HifzAI\n" +
                            "آيات دُرست: " + studied + " من " + verses.size + "\n" +
                            "محاولات: " + attempts + "\n" +
                            "نسبة استرجاع بدون تلميح (تقييم ذاتي): " + independent + "%"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة التقدم"))
                    }
                    if (confirmClear) {
                        Text("تأكيد: سيُحذف كل تقدمك المحلي ولا يمكن استعادته.",
                            color = gold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { confirmClear = false }) { Text("إلغاء") }
                            Button(onClick = {
                                if (store.clear()) {
                                    cards.keys.toList().forEach { cards[it] = ReviewCard(it) }
                                    memories.keys.toList().forEach { memories[it] = MemoryFingerprint(it) }
                                    message = "حُذفت بيانات التدريب من هذا الجهاز"
                                } else {
                                    message = "تعذر حذف بعض البيانات؛ حاول مرة أخرى"
                                }
                                confirmClear = false
                            }) { Text("حذف البيانات") }
                        }
                    } else {
                        OutlinedButton(onClick = { confirmClear = true }) {
                            Text("حذف تقدم الحفظ من الجهاز")
                        }
                    }
                    Text("مصدر الآيات في نموذج سورة الإخلاص: Quran.com/112. استيراد المصحف الكامل والتحقق من سلامته مرحلة مستقلة.",
                        color = muted, fontSize = 12.sp)
                    Text("الإصدار التجريبي 0.2.0 — لا يوجد تقييم تجويد معتمد",
                        color = muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Section(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = gold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = muted, fontSize = 14.sp)
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(value, color = green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, color = silver, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PrimaryButton(title: String, modifier: Modifier = Modifier, action: () -> Unit) {
    Button(
        onClick = action, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = navy)
    ) { Text(title, fontSize = 15.sp) }
}

@Composable
private fun StageButton(title: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    val padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 3.dp)
    if (selected) {
        Button(onClick = action, modifier = modifier, contentPadding = padding) {
            Text(title, fontSize = 12.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = action, modifier = modifier, contentPadding = padding) {
            Text(title, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ScoreButton(title: String, modifier: Modifier, action: () -> Unit) {
    Button(
        onClick = action, modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = navy)
    ) { Text(title, fontSize = 12.sp, maxLines = 1) }
}
