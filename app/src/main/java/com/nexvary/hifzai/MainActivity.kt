package com.nexvary.hifzai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.net.Uri
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
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.nexvary.hifzai.learning.DailyMemorizationPlan
import com.nexvary.hifzai.learning.DifficultyTag
import com.nexvary.hifzai.learning.RecallLadder
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.PrivateRecitationPractice
import com.nexvary.hifzai.learning.HifzScheduler
import com.nexvary.hifzai.learning.MemoryCoach
import com.nexvary.hifzai.learning.MemoryFingerprint
import com.nexvary.hifzai.learning.PracticeKind
import com.nexvary.hifzai.learning.ProgressStore
import com.nexvary.hifzai.learning.ReciterCatalog
import com.nexvary.hifzai.learning.RecallGrade
import com.nexvary.hifzai.learning.ReviewCard
import com.nexvary.hifzai.learning.VerseMask
import java.time.LocalDate

private val navy = Color(0xFF081521)
private val panel = Color(0xFF122B39)
private val silver = Color(0xFFEEF1F2)
private val gold = Color(0xFFD6B465)
private val green = Color(0xFF43C8AA)
private val muted = Color(0xFFB9C6CF)

private enum class Screen { HOME, PRACTICE, MUSHAF, CHAPTERS, INSIGHTS, RECITERS, SETTINGS }

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
    val corpusResult = remember(context) { runCatching { QuranCorpus.load(context) } }
    val corpus = corpusResult.getOrNull()
    if (corpus == null) {
        Column(
            Modifier.fillMaxSize().background(navy).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("تعذر التحقق من سلامة النص القرآني", color = gold, fontSize = 22.sp)
            Text("تم إيقاف عرض المصحف لحماية سلامة النص. أعد تثبيت نسخة سليمة من المصدر الرسمي.",
                color = silver)
        }
        return
    }
    val store = remember(context) { ProgressStore(context) }
    val voicePractice = remember(context) { PrivateRecitationPractice(context) }
    var voiceMessage by remember { mutableStateOf("التسجيل الصوتي اختياري ويبقى داخل الهاتف") }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceMessage = try {
                voicePractice.start()
                "يتم تسجيل صوتك محليًا. اضغط على إيقاف عند الانتهاء."
            } catch (_: Exception) {
                "تعذّر بدء التسجيل؛ راجع إعدادات الميكروفون."
            }
        } else {
            voiceMessage = "لم يتم السماح باستخدام الميكروفون. يمكنك استخدام التسميع الكتابي."
        }
    }
    DisposableEffect(voicePractice) {
        onDispose { voicePractice.close() }
    }
    var selectedSurah by remember { mutableIntStateOf(1) }
    val verses = remember(corpus, selectedSurah) { corpus.chapter(selectedSurah) }
    val cards = remember(store, corpus) {
        mutableStateMapOf<Int, ReviewCard>().apply {
            corpus.verses.forEach { put(it.id, store.card(it.id)) }
        }
    }
    val memories = remember(store, corpus) {
        mutableStateMapOf<Int, MemoryFingerprint>().apply {
            corpus.verses.forEach { put(it.id, store.memory(it.id)) }
        }
    }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var index by remember { mutableIntStateOf(0) }
    var readerPage by remember { mutableIntStateOf(0) }
    var chapterQuery by remember { mutableStateOf("") }
    var insightsPage by remember { mutableIntStateOf(0) }
    var typedRecall by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var quranFontSize by remember { mutableIntStateOf(30) }
    var stage by remember { mutableStateOf(PracticeKind.READ) }
    var hintCount by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var answerWasRevealed by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf(DifficultyTag.NONE) }
    var message by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val verse = verses[index]
    val currentCard = cards[verse.id] ?: ReviewCard(verse.id)
    val fingerprint = memories[verse.id] ?: MemoryFingerprint(verse.id)
    val recommendation = MemoryCoach.recommend(currentCard, fingerprint)
    val dueCount = MemoryCoach.dueCount(cards.values, today)
    val dailyPlan = DailyMemorizationPlan.create(verses, cards, today)
    val studied = cards.values.count { it.repetitions > 0 }
    val independent = MemoryCoach.independentRecallPercent(memories.values)
    val attempts = memories.values.sumOf { it.attempts }

    fun chooseSurah(chapter: Int) {
        selectedSurah = chapter.coerceIn(1, 114)
        index = 0
        readerPage = 0
        insightsPage = 0
        chapterQuery = ""
        typedRecall = ""
        screen = Screen.MUSHAF
    }

    fun openVerse(newIndex: Int, training: PracticeKind = PracticeKind.READ) {
        index = newIndex.coerceIn(0, verses.lastIndex)
        stage = training
        hintCount = 0
        revealed = false
        answerWasRevealed = false
        typedRecall = ""
        difficulty = DifficultyTag.NONE
        screen = Screen.PRACTICE
    }

    fun focusToday() {
        val first = MemoryCoach.studyOrder(verses, cards, today)
            .firstOrNull { it.id in dailyPlan.allVerseIds }
            ?: MemoryCoach.studyOrder(verses, cards, today).first()
        val nextIndex = verses.indexOfFirst { it.id == first.id }.coerceAtLeast(0)
        openVerse(nextIndex, MemoryCoach.recommend(
            cards[first.id] ?: ReviewCard(first.id),
            memories[first.id] ?: MemoryFingerprint(first.id)
        ).kind)
    }

    fun grade(grade: RecallGrade) {
        // Revealed answers must never count as independent recall.
        val assisted = hintCount + if (answerWasRevealed) 1 else 0
        val effectiveGrade = RecallLadder.boundedGrade(grade, hintCount, answerWasRevealed)
        val newCard = HifzScheduler.grade(currentCard, effectiveGrade, today)
        val newMemory = MemoryCoach.record(
            fingerprint, effectiveGrade, assisted, stage == PracticeKind.BRIDGE, difficulty
        )
        cards[verse.id] = newCard
        memories[verse.id] = newMemory
        store.save(newCard, newMemory)
        message = "سُجل تقييم الآية " + verse.verse +
            (if (assisted > 0) " (بمساعدة)" else " (بدون كشف)") +
            " • موعد المراجعة بعد " + newCard.intervalDays + " يوم"
        val next = (index + 1) % verses.size
        openVerse(next)
    }

    LaunchedEffect(screen, selectedSurah, index) {
        voicePractice.erase()
        voiceMessage = "التسجيل الصوتي اختياري ولا يغادر هاتفك"
    }
    LaunchedEffect(screen, selectedSurah, readerPage, insightsPage) {
        scrollState.scrollTo(0)
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.CHAPTERS) Screen.MUSHAF else Screen.HOME
    }

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
                    selected = screen == Screen.MUSHAF || screen == Screen.CHAPTERS,
                    onClick = { screen = Screen.MUSHAF },
                    icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    label = { Text("المصحف") }
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
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF5A2930))) {
                Text(
                    "نسخة مراجعة علمية غير معتمدة • لا يُعاد نشرها للجمهور",
                    color = Color(0xFFFFE0DE),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Text("حِفْظ | NEXVARY", color = gold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("مصحف تدريبي يتكيّف مع طريقة استرجاعك", color = muted, fontSize = 14.sp)
            if (message.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF103B37))) {
                    Text(message, modifier = Modifier.padding(12.dp), color = green)
                }
            }
            when (screen) {
                Screen.HOME -> {
                    Section("خطة اليوم", "سورة " + corpus.info(selectedSurah).arabicName +
                        " • " + verses.size + " آية")
                    PrimaryButton("اختبار استرجاع من غير بداية السورة", Modifier.fillMaxWidth()) {
                        val next = (index + 1) % verses.size
                        openVerse(next, PracticeKind.RECALL)
                    }
                    Text("الآيات المستحقة: " + dailyPlan.dueVerseIds.size +
                        " • آيات جديدة مقترحة: " + dailyPlan.newVerseIds.size,
                        color = green, fontSize = 14.sp)
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
                    PrimaryButton("افتح المصحف الكامل • ١١٤ سورة", Modifier.fillMaxWidth()) {
                        screen = Screen.MUSHAF
                    }
                    OutlinedButton(onClick = { screen = Screen.RECITERS }, modifier = Modifier.fillMaxWidth()) {
                        Text("دليل القرّاء المصريين والسعوديين")
                    }
                    Section("الخطوات", "تعلّم → إخفاء → استرجاع → ربط → مراجعة")
                    Text("التقييم في هذه النسخة ذاتي، ولا يوجد تصحيح صوتي أو حكم آلي على التجويد.",
                        color = muted, fontSize = 13.sp)
                }
                Screen.PRACTICE -> {
                    Section("جلسة الحفظ", "سورة " + corpus.info(selectedSurah).arabicName +
                        " • الآية " + verse.verse + " من " + verses.size)
                    Text("سُلّم الاسترجاع • ٥ مراحل تعليمية", color = green, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StageButton("١ قراءة", stage == PracticeKind.READ, Modifier.weight(1f)) {
                            stage = PracticeKind.READ; hintCount = 0; revealed = false; answerWasRevealed = false
                        }
                        StageButton("٢ إخفاء خفيف", stage == PracticeKind.MASK_LIGHT, Modifier.weight(1f)) {
                            stage = PracticeKind.MASK_LIGHT; hintCount = 0; revealed = false; answerWasRevealed = false
                        }
                        StageButton("٣ إخفاء أكبر", stage == PracticeKind.MASK_HEAVY, Modifier.weight(1f)) {
                            stage = PracticeKind.MASK_HEAVY; hintCount = 0; revealed = false; answerWasRevealed = false
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StageButton("٤ تسميع حر", stage == PracticeKind.RECALL, Modifier.weight(1f)) {
                            stage = PracticeKind.RECALL; hintCount = 0; revealed = false; answerWasRevealed = false
                        }
                        StageButton("٥ ربط الآيات", stage == PracticeKind.BRIDGE, Modifier.weight(1f)) {
                            stage = PracticeKind.BRIDGE; hintCount = 0; revealed = false; answerWasRevealed = false
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
                    val fraction = RecallLadder.concealment(stage, hintCount, revealed)
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
                                onClick = {
                                    if (!revealed) answerWasRevealed = true
                                    revealed = !revealed
                                }
                            ) { Text(if (revealed) "إخفاء الإجابة" else "كشف الآية") }
                        }
                    }
                    if (stage == PracticeKind.RECALL || stage == PracticeKind.BRIDGE) {
                        Text("تسميع بصوتك: استمع إلى تسجيلك وقارن بنفسك دون تقييم تجويد آلي.",
                            color = gold, fontSize = 15.sp)
                        Text(voiceMessage, color = muted, fontSize = 12.sp)
                        if (!voicePractice.isRecording) {
                            OutlinedButton(onClick = {
                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {
                                    voiceMessage = try {
                                        voicePractice.start()
                                        "يتم التسجيل الآن على الهاتف"
                                    } catch (_: Exception) {
                                        "تعذّر بدء تسجيل الصوت"
                                    }
                                } else {
                                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("ابدأ تسجيل التسميع الخاص")
                            }
                        } else {
                            OutlinedButton(onClick = {
                                voiceMessage = if (voicePractice.stop()) {
                                    "تم حفظ الصوت مؤقتًا داخل التطبيق، ويمكنك سماعه أو حذفه."
                                } else "تعذر تسجيل الصوت، يرجى إعادة المحاولة"
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("إيقاف التسجيل")
                            }
                        }
                        if (voicePractice.hasRecording) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = {
                                    voiceMessage = if (voicePractice.play()) "تشغيل صوتك الآن"
                                        else "تعذر تشغيل التسجيل"
                                }, modifier = Modifier.weight(1f)) { Text("استمع لصوتك") }
                                OutlinedButton(onClick = {
                                    voicePractice.erase()
                                    voiceMessage = "تم حذف التسجيل المحلي"
                                }, modifier = Modifier.weight(1f)) { Text("احذف التسجيل") }
                            }
                        }
                        Text("تمرين اختياري: اكتب ما تحفظه، ثم قارن بنفسك. لا يُستخدم لتقييم التجويد.",
                            color = muted, fontSize = 13.sp)
                        OutlinedTextField(
                            value = typedRecall,
                            onValueChange = { typedRecall = it.take(1800) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("إجابتك من الذاكرة (لا تُحفظ)") },
                            minLines = 2,
                            maxLines = 7
                        )
                        Text("موضع التعثر (تقييم ذاتي وليس تصحيحًا آليًا)", color = muted, fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DifficultyButton("لا يوجد", difficulty == DifficultyTag.NONE, Modifier.weight(1f)) { difficulty = DifficultyTag.NONE }
                            DifficultyButton("كلمات", difficulty == DifficultyTag.WORDS, Modifier.weight(1f)) { difficulty = DifficultyTag.WORDS }
                            DifficultyButton("انتقال", difficulty == DifficultyTag.TRANSITION, Modifier.weight(1f)) { difficulty = DifficultyTag.TRANSITION }
                            DifficultyButton("متشابهات", difficulty == DifficultyTag.SIMILAR_VERSE, Modifier.weight(1f)) { difficulty = DifficultyTag.SIMILAR_VERSE }
                        }
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
                            "المرحلة التالية في سُلّم الاسترجاع", Modifier.fillMaxWidth()
                        ) {
                            stage = RecallLadder.next(stage)
                            hintCount = 0; revealed = false; answerWasRevealed = false
                        }
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
                    Text("هذه درجات تقدير ذاتي وليست حكمًا على صحة التلاوة. كشف الإجابة يُحتسب كمساعدة.",
                        color = muted, fontSize = 12.sp)
                }
                Screen.MUSHAF -> {
                    Section("المصحف الشريف", "تصفح آيات " + corpus.info(selectedSurah).arabicName +
                        " كما وردت من المصدر • دون تعديل أو إخفاء")
                    OutlinedButton(onClick = { screen = Screen.CHAPTERS },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("اختيار السورة • " + corpus.info(selectedSurah).arabicName)
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("حجم الخط", color = muted)
                        Slider(
                            value = quranFontSize.toFloat(),
                            onValueChange = { quranFontSize = it.toInt().coerceIn(24, 44) },
                            valueRange = 24f..44f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(quranFontSize.toString(), color = green)
                    }
                    Text("الآيات " + (readerPage * 8 + 1) + "–" +
                        minOf(readerPage * 8 + 8, verses.size) + " من " + verses.size,
                        color = gold, fontSize = 14.sp)
                    verses.drop(readerPage * 8).take(8).forEachIndexed { pageIndex, item ->
                        Card(
                            border = BorderStroke(1.dp, gold.copy(alpha = 0.24f)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = panel)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(17.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("﴿ " + item.verse + " ﴾",
                                    color = gold, fontSize = 16.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth())
                                Text(
                                    item.text,
                                    color = silver,
                                    fontSize = quranFontSize.sp,
                                    lineHeight = (quranFontSize * 1.85f).sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(onClick = { openVerse(readerPage * 8 + pageIndex) }) {
                                    Text("تدريب الحفظ لهذه الآية")
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            enabled = readerPage > 0,
                            onClick = { readerPage = (readerPage - 1).coerceAtLeast(0) }
                        ) { Text("السابق") }
                        OutlinedButton(
                            enabled = readerPage * 8 + 8 < verses.size,
                            onClick = { readerPage += 1 }
                        ) { Text("التالي") }
                    }
                    Text(QuranCorpus.SOURCE_CREDIT,
                        color = muted, fontSize = 12.sp)
                }
                Screen.CHAPTERS -> {
                    Section("السور القرآنية", "١١٤ سورة • اختر السورة لعرض آياتها")
                    OutlinedTextField(
                        value = chapterQuery,
                        onValueChange = { chapterQuery = it.take(50) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ابحث باسم السورة أو رقمها") },
                        singleLine = true
                    )
                    corpus.surahs.filter {
                        chapterQuery.isBlank() ||
                            it.arabicName.contains(chapterQuery.trim()) ||
                            it.number.toString() == chapterQuery.trim()
                    }.forEach { info ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { chooseSurah(info.number) },
                        ) {
                            Text(info.number.toString() + " • " + info.arabicName +
                                " (" + info.ayahCount + " آية)",
                                color = silver,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start)
                        }
                    }
                }
                Screen.INSIGHTS -> {
                    Section("بصمة الذاكرة", "سورة " + corpus.info(selectedSurah).arabicName +
                        " • مقاييس التقييم الذاتي")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("محاولات الاسترجاع", attempts.toString(), Modifier.weight(1f))
                        MetricCard("بدون تلميح", independent.toString() + "%", Modifier.weight(1f))
                    }
                    verses.drop(insightsPage * 15).take(15).forEachIndexed { pageOffset, item ->
                        val i = insightsPage * 15 + pageOffset
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
                                Text("تعثر كلمات: " + memory.wordMisses +
                                    " • انتقال: " + memory.transitionsMissed +
                                    " • متشابهات: " + memory.similarityMisses,
                                    color = muted, fontSize = 12.sp)
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { insightsPage = (insightsPage - 1).coerceAtLeast(0) },
                            enabled = insightsPage > 0) { Text("الآيات السابقة") }
                        OutlinedButton(onClick = { insightsPage += 1 },
                            enabled = (insightsPage + 1) * 15 < verses.size) {
                            Text("آيات أخرى")
                        }
                    }
                }
                Screen.RECITERS -> {
                    Section("دليل قرّاء مصر والسعودية", "أسماء مبدئية • التسجيلات تحتاج مراجعة التراخيص")
                    ReciterCatalog.all.groupBy { it.country }.forEach { (country, people) ->
                        Text(country.displayArabic, color = green, fontSize = 21.sp)
                        people.forEach { person ->
                            Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(person.nameArabic, color = gold, fontSize = 18.sp)
                                    Text("بيانات تعريفية فقط — التلاوات قيد فحص حقوق الاستخدام",
                                        color = muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Screen.SETTINGS -> {
                    Section("خصوصية البيانات", "كل تقدمك محفوظ محليًا على هذا الهاتف")
                    OutlinedButton(onClick = { screen = Screen.RECITERS },
                        modifier = Modifier.fillMaxWidth()) { Text("دليل القراء") }
                    Text(QuranCorpus.SOURCE_CREDIT, color = gold)
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(QuranCorpus.SOURCE_LINK)))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("المصدر وترخيص النص: Tanzil.net")
                    }
                    Text("لا توجد حسابات ولا إرسال للصوت إلى خادم. تسجيل التسميع اختياري ومحلي ومؤقت؛ يُحذف عند مغادرة التدريب أو بالضغط على حذف.",
                        color = silver)
                    PrimaryButton("مشاركة ملخص التقدم", Modifier.fillMaxWidth()) {
                        val summary = "حِفْظ | NEXVARY HifzAI\n" +
                            "آيات دُرست: " + studied + " من " + corpus.verses.size + "\n" +
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
                    Text("النص: Tanzil Uthmani 1.1 • كامل 6236 آية. مصدر النسخة مثبت ببصمة Git، والتدقيق العلمي والعرضي النهائي معلّقان.",
                        color = muted, fontSize = 12.sp)
                    PrimaryButton("معلومات نسخة لجنة المراجعة", Modifier.fillMaxWidth()) {
                        val brief = "NEXVARY HifzAI — نموذج مراجعة علماء القرآن\\n" +
                            "نسخة تطوير تجريبية غير معتمدة للنشر العام.\\n" +
                            "النص المرفق: نموذج محدود من سورة الإخلاص فقط.\\n" +
                            "الاختبارات: الإخفاء، سُلّم الاسترجاع، المراجعة المتباعدة، الربط، البصمة.\\n" +
                            "لا يوجد تحقق صوتي أو فتوى أو حكم آلي على التجويد.\\n" +
                            "يرجى فحص الرسم وضبط الآيات والإخفاء وتمارين الربط؛ " +
                            "الملاحظات لا تُعتبر تصريحًا بالنشر."
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, brief)
                        }, "إرسال ملخص لجنة المراجعة"))
                    }
                    Text("الإصدار التجريبي 0.3.0-dev — غير معتمد للنشر العام",
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

@Composable
private fun DifficultyButton(title: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    StageButton(title, selected, modifier, action)
}
