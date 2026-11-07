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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nexvary.hifzai.learning.AppAppearance
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import com.nexvary.hifzai.learning.TeacherNotebook
import com.nexvary.hifzai.learning.PracticeSession
import com.nexvary.hifzai.learning.RecallObservation
import com.nexvary.hifzai.learning.QuranPartitions
import com.nexvary.hifzai.learning.QuranSearchIndex
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
import com.nexvary.hifzai.learning.LocalDataReset
import com.nexvary.hifzai.learning.RuntimeResetStore
import com.nexvary.hifzai.learning.RecitationPlaybackService
import com.nexvary.hifzai.learning.ProgressCsv
import com.nexvary.hifzai.learning.ProgressStore
import com.nexvary.hifzai.learning.ReciterCatalog
import com.nexvary.hifzai.learning.RecallGrade
import com.nexvary.hifzai.learning.SimilarityIndex
import com.nexvary.hifzai.learning.VerifiedVerse
import com.nexvary.hifzai.learning.ReviewCard
import com.nexvary.hifzai.learning.VerseMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nexvary.hifzai.learning.LearningGoal
import com.nexvary.hifzai.learning.LearningTrends
import com.nexvary.hifzai.learning.GentleReturnPolicy
import java.time.LocalDate

internal val quranTypeface = FontFamily(Font(R.font.amiri_quran))

private val navy: Color @Composable get() = MaterialTheme.colorScheme.background
private val panel: Color @Composable get() = MaterialTheme.colorScheme.surface
private val silver: Color @Composable get() = MaterialTheme.colorScheme.onSurface
private val gold: Color @Composable get() = MaterialTheme.colorScheme.primary
private val green: Color @Composable get() = MaterialTheme.colorScheme.secondary
private val muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private enum class Screen { HOME, PRACTICE, MUSHAF, CHAPTERS, SIMILAR, INSIGHTS, RECITERS, SETTINGS, SEARCH, TODAY, PRIVACY, SOURCES, ABOUT, HELP, TEACHERS, EXERCISES, PARTITIONS, RANGE_PRACTICE, LOCAL_TRANSCRIPTION, SAVED_PLACES }

class MainActivity : ComponentActivity() {
    private lateinit var resets: RuntimeResetStore
    private var creationToken = "initial"
    private lateinit var dataReset: LocalDataReset
    override fun onCreate(savedInstanceState: Bundle?) {
        resets = RuntimeResetStore(this)
        if (resets.pending) RecitationPlaybackService.stopForDataReset()
        dataReset = LocalDataReset(resets, { ProgressStore(this).clear() }, { TeacherNotebook(this).clear() })
        val resetResult = dataReset.recover()
        creationToken = resets.token
        val restored = savedInstanceState?.takeIf { resets.acceptsSaved(it.getString(RESET_STATE_KEY)) }
        super.onCreate(restored)
        java.io.File(cacheDir, "hifz_private_session.m4a").delete()
        if (resetResult == LocalDataReset.Result.PENDING) {
            showStartupMessage("تعذر التحقق من حالة البيانات؛ لن تُستعاد الجلسة قبل معالجة الخطأ.", retry = true)
            return
        }
        showStartupMessage("جارٍ التحقق من النص القرآني المحلي…")
        // Verify outside composition. A successful Activity gets one unconditional
        // verified root, never a loading subtree that is replaced inside Compose.
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { QuranCorpus.load(applicationContext) } }
            if (isFinishing || isDestroyed) return@launch
            if (!resets.acceptsSaved(creationToken)) { recreate(); return@launch }
            result.fold(onSuccess = { corpus -> showVerifiedContent(corpus) }, onFailure = { error ->
                android.util.Log.e("HifzCorpus", "Bundled Quran verification failed", error)
                showStartupMessage("تعذر التحقق من سلامة النص القرآني. تم إيقاف عرض المصحف؛ أعد تثبيت نسخة سليمة.")
            })
        }
    }

    /** Native startup/error view: no saved Compose session is consumed before verification. */
    private fun showStartupMessage(message: String, retry: Boolean = false) {
        val appearance = ProgressStore(this).appearance
        val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val colors = if (appearance.usesDarkColors(night)) HifzDarkColors else HifzLightColors
        val space = (24 * resources.displayMetrics.density).toInt()
        val view = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setPadding(space, space, space, space)
            setBackgroundColor(colors.background.toArgb())
            addView(android.widget.TextView(this@MainActivity).apply {
                text = message; textSize = 20f; setTextColor(colors.onBackground.toArgb())
            })
            if (retry) addView(android.widget.Button(this@MainActivity).apply {
                text = "إعادة المحاولة"; setOnClickListener { recreate() }
            })
        }
        setContentView(view)
    }

    private fun showVerifiedContent(corpus: QuranCorpus) {
        val export = androidx.lifecycle.ViewModelProvider(this)[ProgressExportViewModel::class.java]
        setContent {
            val preferences = remember { ProgressStore(this@MainActivity) }
            var appearance by remember { mutableStateOf(preferences.appearance) }
            HifzTheme(appearance) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VerifiedAppContent(corpus, appearance, export, changeAppearance = {
                        preferences.appearance = it
                        appearance = it
                    }, resetRuntime = {
                        val result = dataReset.request()
                        if (result != LocalDataReset.Result.NOT_STARTED) {
                            RecitationPlaybackService.stopForDataReset()
                            export.clearForReset()
                        }
                        if (result != LocalDataReset.Result.NOT_STARTED) recreate()
                        result != LocalDataReset.Result.NOT_STARTED
                    })
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(RESET_STATE_KEY, creationToken)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (resets.acceptsSaved(savedInstanceState.getString(RESET_STATE_KEY)))
            super.onRestoreInstanceState(savedInstanceState)
    }

    private companion object { const val RESET_STATE_KEY = "hifz_reset_generation" }
}

/** The Activity installs this root only after canonical verification succeeds. */
@Composable
private fun VerifiedAppContent(corpus: QuranCorpus, appearance: AppAppearance, export: ProgressExportViewModel, changeAppearance: (AppAppearance) -> Unit, resetRuntime: () -> Boolean) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val store = remember(context) { ProgressStore(context) }
    val voicePractice = remember(context) { PrivateRecitationPractice(context) }
    var voiceMessage by remember { mutableStateOf("التسجيل الصوتي اختياري ويبقى داخل الهاتف") }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && screen == Screen.PRACTICE) {
            voiceMessage = try {
                voicePractice.start()
                "يتم تسجيل صوتك محليًا. اضغط على إيقاف عند الانتهاء."
            } catch (_: Exception) {
                "تعذّر بدء التسجيل؛ راجع إعدادات الميكروفون."
            }
        } else {
            voiceMessage = if (granted) "أُغلق تدريب التسميع، لذلك لم يبدأ التسجيل."
                else "لم يتم السماح باستخدام الميكروفون. يمكنك استخدام التسميع الكتابي."
        }
    }
    DisposableEffect(voicePractice) {
        onDispose { voicePractice.close() }
    }
    val partitions = remember(corpus, context) { runCatching { QuranPartitions.load(context, corpus) } }
    val savedSession = remember(store, corpus) {
        store.session()?.takeIf { corpus.find(it.chapter, it.ayah) != null }
    }
    var rangeStart by rememberSaveable { mutableIntStateOf(savedSession?.rangeStart ?: 0) }
    var rangeEnd by rememberSaveable { mutableIntStateOf(savedSession?.rangeEnd ?: 0) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(savedSession?.chapter ?: 1) }
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
    var index by rememberSaveable { mutableIntStateOf((savedSession?.ayah ?: 1) - 1) }
    var readerPage by rememberSaveable { mutableIntStateOf(index / 8) }
    var chapterQuery by remember { mutableStateOf("") }
    var insightsPage by remember { mutableIntStateOf(0) }
    var typedRecall by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var quranFontSize by remember { mutableIntStateOf(store.fontSize) }
    var advanced by remember { mutableStateOf(store.advanced) }
    var learningGoal by remember { mutableStateOf(store.learningGoal) }
    var dailyStudyMinutes by remember { mutableStateOf(store.dailyStudyMinutes) }
    var savedPlaces by remember { mutableStateOf(store.savedPlaces(corpus)) }
    var favoriteReciter by remember { mutableStateOf(store.favoriteReciter) }
    var gentleReturnEnabled by remember { mutableStateOf(store.gentleReturnEnabled) }
    var gentleReturnAnchor by remember { mutableStateOf(store.gentleReturnAnchorDay) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var jumpQuery by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableStateOf(savedSession?.stage ?: PracticeKind.READ) }
    var hintCount by rememberSaveable { mutableIntStateOf(savedSession?.hints ?: 0) }
    var revealed by remember { mutableStateOf(false) }
    var answerWasRevealed by rememberSaveable { mutableStateOf(savedSession?.exposed ?: false) }
    var difficulty by rememberSaveable { mutableStateOf(savedSession?.difficulty ?: DifficultyTag.NONE) }
    var followsDailyPlan by rememberSaveable { mutableStateOf(savedSession?.followsDailyPlan ?: false) }
    var dailyPlanChapter by rememberSaveable { mutableIntStateOf(savedSession?.planChapter?.takeIf { it > 0 } ?: selectedSurah) }
    var message by remember { mutableStateOf("") }
    val exportState by export.status.collectAsState()
    val createProgressFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val snapshot = memories.toMap()
            val resolver = context.applicationContext.contentResolver
            export.write({ ProgressCsv.encode(corpus.verses, snapshot) }) {
                resolver.openOutputStream(uri, "wt")
            }
        } else {
            export.cancelPicking()
        }
    }
    var confirmClear by remember { mutableStateOf(false) }
    var today by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val verse = verses[index]
    val currentCard = cards[verse.id] ?: ReviewCard(verse.id)
    val fingerprint = memories[verse.id] ?: MemoryFingerprint(verse.id)
    val recommendation = LearningTrends.recentRecommendation(store.history(verse.id), today)
        ?: MemoryCoach.recommend(currentCard, fingerprint)
    val dueCount = MemoryCoach.dueCount(cards.values, today)
    val dayWork = store.workload(today)
    val planChapter = if (followsDailyPlan) dailyPlanChapter else selectedSurah
    val dailyPlan = learningGoal.planFor(corpus, cards, today, planChapter, dayWork)
    val studied = memories.values.count { it.attempts > 0 }
    val independent = MemoryCoach.independentRecallPercent(memories.values)
    val attempts = memories.values.sumOf { it.attempts }

    fun openExternal(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) {
            message = "لا يوجد متصفح لفتح الرابط؛ ثبّت متصفحًا ثم أعد المحاولة"
        }
    }

    fun chooseSurah(chapter: Int) {
        followsDailyPlan = false
        selectedSurah = chapter.coerceIn(1, 114)
        index = 0
        stage = PracticeKind.READ; hintCount = 0; revealed = false; answerWasRevealed = false
        difficulty = DifficultyTag.NONE; rangeStart = 0; rangeEnd = 0
        readerPage = 0
        insightsPage = 0
        chapterQuery = ""
        typedRecall = ""
        screen = Screen.MUSHAF
    }

    fun openPracticeVerse(chosen: VerifiedVerse, training: PracticeKind, followPlan: Boolean) {
        followsDailyPlan = followPlan
        rangeStart = 0; rangeEnd = 0
        selectedSurah = chosen.chapter
        index = chosen.verse - 1
        readerPage = index / 8
        stage = training
        hintCount = 0
        revealed = false
        answerWasRevealed = false
        typedRecall = ""
        difficulty = DifficultyTag.NONE
        screen = Screen.PRACTICE
    }

    fun openCorpusVerse(chosen: VerifiedVerse) = openPracticeVerse(chosen, PracticeKind.MASK_LIGHT, false)

    fun openVerse(newIndex: Int, training: PracticeKind = PracticeKind.READ, followPlan: Boolean = false) {
        openPracticeVerse(verses[newIndex.coerceIn(0, verses.lastIndex)], training, followPlan)
    }

    fun focusToday() {
        today = LocalDate.now().toEpochDay()
        val origin = if (followsDailyPlan) dailyPlanChapter else selectedSurah
        val livePlan = learningGoal.planFor(corpus, cards, today, origin, store.workload(today))
        val firstId = livePlan.allVerseIds.firstOrNull()
        if (firstId == null) { message = "لا توجد آيات ضمن هدفك الحالي؛ عدّل الخطة أو اختر آية يدويًا"; return }
        val first = corpus.verses.first { it.id == firstId }
        dailyPlanChapter = origin
        openPracticeVerse(first, MemoryCoach.recommend(
            cards[first.id] ?: ReviewCard(first.id),
            memories[first.id] ?: MemoryFingerprint(first.id)
        ).kind, followPlan = true)
    }

    fun changeStage(next: PracticeKind) {
        if (next == PracticeKind.READ && stage != PracticeKind.READ) answerWasRevealed = true
        stage = next
        revealed = false
        // Hint/exposure history is retained until a different verse/attempt starts.
    }

    fun jumpTo(chapter: Int, ayah: Int) {
        val found = corpus.find(chapter, ayah)
        if (found == null) { message = "رقم السورة أو الآية غير موجود"; return }
        followsDailyPlan = false
        selectedSurah = chapter
        index = ayah - 1
        stage = PracticeKind.READ; hintCount = 0; revealed = false; answerWasRevealed = false
        difficulty = DifficultyTag.NONE; rangeStart = 0; rangeEnd = 0
        readerPage = index / 8
        screen = Screen.MUSHAF
        message = "تم الانتقال إلى " + corpus.info(chapter).arabicName + " • " + ayah
    }

    fun grade(grade: RecallGrade) {
        today = LocalDate.now().toEpochDay()
        // Revealed answers must never count as independent recall.
        val assisted = hintCount + if (answerWasRevealed) 1 else 0
        val effectiveGrade = RecallLadder.boundedGrade(grade, hintCount, answerWasRevealed)
        val newCard = HifzScheduler.grade(currentCard, effectiveGrade, today)
        val newMemory = MemoryCoach.record(
            fingerprint, effectiveGrade, assisted, stage == PracticeKind.BRIDGE, difficulty
        )
        cards[verse.id] = newCard
        memories[verse.id] = newMemory
        store.recordAssessment(newCard, newMemory, RecallObservation(today, effectiveGrade, hintCount, answerWasRevealed, difficulty, stage),
            firstAttempt = fingerprint.attempts == 0 && currentCard.repetitions == 0 && currentCard.lapses == 0)
        gentleReturnAnchor = store.gentleReturnAnchorDay
        message = "سُجل تقييم الآية " + verse.verse +
            (if (assisted > 0) " (بمساعدة)" else " (بدون كشف)") +
            " • موعد المراجعة بعد " + newCard.intervalDays + " يوم"
        if (followsDailyPlan && effectiveGrade != RecallGrade.AGAIN) {
            val nextId = learningGoal.planFor(corpus, cards, today, dailyPlanChapter, store.workload(today)).allVerseIds.firstOrNull()
            if (nextId == null) {
                store.clearSession(); followsDailyPlan = false
                stage = PracticeKind.READ; hintCount = 0; revealed = false; answerWasRevealed = false
                typedRecall = ""; difficulty = DifficultyTag.NONE
                if (selectedSurah != dailyPlanChapter) { selectedSurah = dailyPlanChapter; index = 0; readerPage = 0 }
                message += " • انتهت الخطة المقترحة لليوم؛ يبقى التدريب اليدوي متاحًا"
                screen = Screen.TODAY
            } else {
                val advice = MemoryCoach.recommend(cards.getValue(nextId), memories.getValue(nextId))
                openPracticeVerse(corpus.verses.first { it.id == nextId }, advice.kind, followPlan = true)
            }
        } else {
            val next = if (effectiveGrade == RecallGrade.AGAIN) index else (index + 1) % verses.size
            openVerse(next, followPlan = followsDailyPlan)
        }
    }

    DisposableEffect(context, voicePractice) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                today = LocalDate.now().toEpochDay()
                gentleReturnEnabled = store.gentleReturnEnabled
                gentleReturnAnchor = store.gentleReturnAnchorDay
            }
            if (event == Lifecycle.Event.ON_STOP) {
                voicePractice.erase()
                typedRecall = ""
                voiceMessage = "تم حذف الصوت عند انتقال التطبيق إلى الخلفية"
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(screen, selectedSurah, index, stage, hintCount, answerWasRevealed, difficulty, followsDailyPlan, dailyPlanChapter) {
        if (screen == Screen.PRACTICE) store.saveSession(
            PracticeSession(selectedSurah, index + 1, stage, hintCount, answerWasRevealed, difficulty,
                followsDailyPlan = followsDailyPlan, planChapter = if (followsDailyPlan) dailyPlanChapter else 0)
        )
    }
    LaunchedEffect(quranFontSize, advanced) {
        store.fontSize = quranFontSize
        store.advanced = advanced
    }
    LaunchedEffect(screen, selectedSurah, index) {
        voicePractice.erase()
        voiceMessage = "التسجيل الصوتي اختياري ولا يغادر هاتفك"
    }
    LaunchedEffect(screen, selectedSurah, readerPage, insightsPage) {
        today = LocalDate.now().toEpochDay()
        scrollState.scrollTo(0)
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.CHAPTERS || screen == Screen.SAVED_PLACES) Screen.MUSHAF else Screen.HOME
    }

    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val windowWidth = with(LocalDensity.current) { windowWidthPx.toDp() }
    val contentPadding = maxOf(18.dp, (windowWidth - 840.dp) / 2)
    Scaffold(
        containerColor = navy,
        bottomBar = {
            AdaptiveBottomNavigation(windowWidth, listOf(
                NavigationDestination("الرئيسية", Icons.Rounded.Home, screen == Screen.HOME) { screen = Screen.HOME },
                NavigationDestination("الحفظ", Icons.Rounded.AutoStories, screen == Screen.PRACTICE) {
                    followsDailyPlan = false; screen = Screen.PRACTICE
                },
                NavigationDestination("المصحف", Icons.AutoMirrored.Rounded.MenuBook,
                    screen in setOf(Screen.MUSHAF, Screen.CHAPTERS, Screen.SAVED_PLACES)) { screen = Screen.MUSHAF },
                NavigationDestination("تقدمي", Icons.Rounded.Insights, screen == Screen.INSIGHTS) { screen = Screen.INSIGHTS },
                NavigationDestination("الإعدادات", Icons.Rounded.Settings, screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
            ))
        }
    ) { inset ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inset)
                .verticalScroll(scrollState)
                .padding(horizontal = contentPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "نسخة تطوير داخلية • غير جاهزة للمراجعة النهائية",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("حِفْظ | NEXVARY", color = gold, fontSize = 20.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (screen != Screen.HOME) OutlinedButton(onClick = {
                    screen = if (screen == Screen.CHAPTERS || screen == Screen.SAVED_PLACES) Screen.MUSHAF else Screen.HOME
                }) { Text("رجوع") }
            }
            if (screen == Screen.HOME) Text("مصحف تدريبي يتكيّف مع طريقة استرجاعك", color = muted, fontSize = 14.sp)
            if (message.isNotEmpty() && screen != Screen.LOCAL_TRANSCRIPTION) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            when (screen) {
                Screen.HOME -> {
                    if (GentleReturnPolicy.shouldSuggest(gentleReturnEnabled, gentleReturnAnchor, today)) {
                        Text("مرّ أسبوع دون تدريب مسجل. إن رغبت، عد بجلسة قصيرة على مهلك.", color = gold)
                        OutlinedButton(onClick = {
                            store.postponeGentleReturn(today); gentleReturnAnchor = today
                        }) { Text("ذكّرني بعد أسبوع") }
                        OutlinedButton(onClick = {
                            store.setGentleReturn(false, today); gentleReturnEnabled = false; gentleReturnAnchor = null
                        }) { Text("أوقف اقتراح العودة") }
                    }
                    if (store.session() != null) {
                        OutlinedButton(onClick = {
                            val active = store.session()
                            if (active != null && corpus.find(active.chapter, active.ayah) != null) {
                                selectedSurah = active.chapter; index = active.ayah - 1
                                stage = active.stage; hintCount = active.hints
                                answerWasRevealed = active.exposed; difficulty = active.difficulty
                                followsDailyPlan = active.followsDailyPlan
                                dailyPlanChapter = active.planChapter.takeIf { it > 0 } ?: active.chapter
                                rangeStart = active.rangeStart; rangeEnd = active.rangeEnd
                                revealed = false
                                screen = if (rangeStart > 0) Screen.RANGE_PRACTICE else Screen.PRACTICE
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("استكمل جلسة الحفظ السابقة") }
                    }
                    Section("خطة اليوم", "مراجعات المصحف كله • الحفظ الجديد: سورة " + corpus.info(planChapter).arabicName)
                    PrimaryButton("اختبار استرجاع من غير بداية السورة", Modifier.fillMaxWidth()) {
                        val next = (index + 1) % verses.size
                        openVerse(next, PracticeKind.RECALL)
                    }
                    Text("الآيات المستحقة: " + dailyPlan.dueVerseIds.size +
                        " • آيات جديدة مقترحة: " + dailyPlan.newVerseIds.size,
                        color = green, fontSize = 14.sp)
                    Text("آيات قيّمتها اليوم: جديد " + dayWork.newVerseIds.size + "/" + learningGoal.newVerses +
                        " • مراجعة " + dayWork.reviewVerseIds.size + "/" + learningGoal.reviews, color = muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("مراجعات مستحقة", dueCount.toString(), Modifier.weight(1f))
                        MetricCard("آيات دُرست", studied.toString() + " / " + corpus.verses.size, Modifier.weight(1f))
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
                    OutlinedButton(onClick = { screen = Screen.TODAY }, modifier = Modifier.fillMaxWidth()) {
                        Text("تفاصيل الخطة اليومية والمراجعات")
                    }
                    OutlinedButton(onClick = { screen = Screen.EXERCISES }, modifier = Modifier.fillMaxWidth()) {
                        Text("تدريبات الاسترجاع وترتيب الآيات")
                    }
                    OutlinedButton(onClick = { screen = Screen.PARTITIONS }, modifier = Modifier.fillMaxWidth()) {
                        Text("الحفظ بالأجزاء والصفحات")
                    }
                    Section("الخطوات", "تعلّم → إخفاء → استرجاع → ربط → مراجعة")
                    Text("التقييم في هذه النسخة ذاتي، ولا يوجد تصحيح صوتي أو حكم آلي على التجويد.",
                        color = muted, fontSize = 13.sp)
                }
                Screen.PRACTICE -> {
                    Section("جلسة الحفظ", "سورة " + corpus.info(selectedSurah).arabicName +
                        " • الآية " + verse.verse + " من " + verses.size)
                    Text("سُلّم الاسترجاع • ٥ مراحل تعليمية", color = green, fontSize = 14.sp)
                    OutlinedButton(onClick = { screen = Screen.SIMILAR },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("تدرّب على الآيات المتشابهة لفظيًا")
                    }
                    if (advanced) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StageButton("١ قراءة", stage == PracticeKind.READ, Modifier.weight(1f)) {
                            changeStage(PracticeKind.READ)
                        }
                        StageButton("٢ إخفاء خفيف", stage == PracticeKind.MASK_LIGHT, Modifier.weight(1f)) {
                            changeStage(PracticeKind.MASK_LIGHT)
                        }
                        StageButton("٣ إخفاء أكبر", stage == PracticeKind.MASK_HEAVY, Modifier.weight(1f)) {
                            changeStage(PracticeKind.MASK_HEAVY)
                        }
                    }
                    if (advanced) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StageButton("٤ تسميع حر", stage == PracticeKind.RECALL, Modifier.weight(1f)) {
                            changeStage(PracticeKind.RECALL)
                        }
                        StageButton("٥ ربط الآيات", stage == PracticeKind.BRIDGE, Modifier.weight(1f)) {
                            changeStage(PracticeKind.BRIDGE)
                        }
                    }
                    if (stage == PracticeKind.BRIDGE) {
                        Text(
                            "من نهاية الآية السابقة:",
                            color = muted, fontSize = 14.sp
                        )
                        Text(
                            BridgeTrainer.tailOfPrevious(verses, index, textForExercise = corpus::ayahText)
                                ?: "هذه أول آية، اختر الآية التالية لتدريب الربط.",
                            color = green, fontFamily = quranTypeface, fontSize = 20.sp
                        )
                    }
                    val fraction = RecallLadder.concealment(stage, hintCount, revealed)
                    val visualText = VerseMask.display(corpus.ayahText(verse), fraction, verse.id)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = panel)
                    ) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp)) {
                            if (fraction >= 1f && (stage == PracticeKind.RECALL || stage == PracticeKind.BRIDGE)) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.Mic, contentDescription = null, tint = green)
                                    Text("الآية مخفية • سمّعها من ذاكرتك", color = silver,
                                        fontSize = 20.sp, textAlign = TextAlign.Center)
                                }
                            } else if (stage == PracticeKind.READ) CanonicalQuranText(corpus, verse)
                            else Text(
                                visualText, fontFamily = quranTypeface, modifier = Modifier.fillMaxWidth(),
                                fontSize = 29.sp, lineHeight = 52.sp,
                                fontWeight = FontWeight.Medium, color = silver,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Text(recommendation.explanation, color = muted, fontSize = 14.sp)
                    Text(recommendation.evidence.description, color = muted, fontSize = 13.sp)
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
                    if (stage == PracticeKind.RECALL) OutlinedButton(onClick = { changeStage(PracticeKind.BRIDGE) },
                        modifier = Modifier.fillMaxWidth()) { Text("انتقل إلى ربط الآيات") }
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
                            PrivateAudioControls(voicePractice) { voiceMessage = it }
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
                            changeStage(RecallLadder.next(stage))
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
                    var reportAyah by rememberSaveable(selectedSurah) { mutableStateOf<Int?>(null) }
                    reportAyah?.let { ayah ->
                        corpus.find(selectedSurah, ayah)?.let { ContentReportDialog(it) { reportAyah = null } }
                    }
                    Section("المصحف الشريف", "تصفح آيات " + corpus.info(selectedSurah).arabicName +
                        " كما وردت من المصدر • دون تعديل أو إخفاء")
                    OutlinedButton(onClick = { screen = Screen.CHAPTERS },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("اختيار السورة • " + corpus.info(selectedSurah).arabicName)
                    }
                    OutlinedButton(onClick = { screen = Screen.SEARCH }, modifier = Modifier.fillMaxWidth()) {
                        Text("البحث في القرآن والانتقال المباشر")
                    }
                    OutlinedButton(onClick = { screen = Screen.SAVED_PLACES }, modifier = Modifier.fillMaxWidth()) {
                        Text("العلامات المرجعية")
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
                                CanonicalQuranText(corpus, item, quranFontSize, showAyahNumber = true)
                                OutlinedButton(onClick = {
                                    store.bookmark(item.chapter, item.verse)
                                    message = "حُفظ موضع القراءة • " + item.verse
                                }) { Text("احفظ موضع القراءة") }
                                OutlinedButton(onClick = {
                                    val saved = store.addSavedPlace(item.chapter, item.verse, corpus)
                                    savedPlaces = store.savedPlaces(corpus)
                                    message = if (saved) "أُضيفت العلامة المرجعية" else "بلغت حد العلامات؛ احذف علامة لإضافة موضع جديد"
                                }) { Text("أضف علامة للآية ${item.verse}") }
                                OutlinedButton(onClick = { openVerse(readerPage * 8 + pageIndex) }) {
                                    Text("تدريب الحفظ لهذه الآية")
                                }
                                OutlinedButton(onClick = { reportAyah = item.verse }) {
                                    Text("أبلغ عن مشكلة في الآية ${item.verse}")
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
                Screen.SAVED_PLACES -> SavedPlacesScreen(corpus, savedPlaces,
                    open = { jumpTo(it.chapter, it.verse) },
                    remove = {
                        store.removeSavedPlace(it.chapter, it.verse, corpus)
                        savedPlaces = store.savedPlaces(corpus)
                        message = "حُذفت العلامة المرجعية فقط"
                    })
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
                Screen.SIMILAR -> {
                    Section("اقتراحات المتشابهات اللفظية",
                        "مقارنة نصية آلية للتدريب فقط، وليست تصنيفًا معتمدًا للمتشابهات")
                    Text("الآية الأساسية: " + corpus.info(selectedSurah).arabicName +
                        " " + verse.verse, color = gold)
                    Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                        Box(Modifier.fillMaxWidth().padding(18.dp)) { CanonicalQuranText(corpus, verse, 26) }
                    }
                    val suggestions = remember(verse.id, corpus) {
                        SimilarityIndex.similarTo(verse, corpus.verses, 0.45, corpus::ayahText).take(12)
                    }
                    if (suggestions.isEmpty()) {
                        Text("لا توجد اقتراحات لفظية فوق معيار التشابه الحالي. " +
                            "هذا لا يعني عدم وجود متشابهات يقررها المختصون.",
                            color = muted)
                    } else {
                        suggestions.forEach { candidate ->
                            Card(colors = CardDefaults.cardColors(containerColor = panel),
                                shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("سورة " + corpus.info(candidate.chapter).arabicName +
                                        " • الآية " + candidate.verse,
                                        color = gold, fontSize = 15.sp)
                                    CanonicalQuranText(corpus, candidate, 23)
                                    OutlinedButton(onClick = { openCorpusVerse(candidate) }) {
                                        Text("تدريب هذا الموضع")
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { screen = Screen.PRACTICE },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("العودة إلى التسميع")
                    }
                }
                Screen.INSIGHTS -> {
                    Section("بصمة الذاكرة", "سورة " + corpus.info(selectedSurah).arabicName +
                        " • مقاييس التقييم الذاتي")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("محاولات الاسترجاع", attempts.toString(), Modifier.weight(1f))
                        MetricCard("بدون تلميح", independent.toString() + "%", Modifier.weight(1f))
                    }
                    var observations by remember(attempts) { mutableStateOf<List<RecallObservation>?>(null) }
                    LaunchedEffect(attempts, store, corpus) {
                        observations = withContext(Dispatchers.IO) { corpus.verses.flatMap { store.history(it.id) } }
                    }
                    val retained = observations
                    if (retained == null) Text("جارٍ قراءة المحاولات المحلية…")
                    else {
                        LearningPeriodCard("آخر ٧ أيام", "الأسبوع السابق",
                            LearningTrends.week(retained, today), LearningTrends.week(retained, today - 7))
                        LearningPeriodCard("آخر ٣٠ يومًا", "فترة الثلاثين يومًا السابقة",
                            LearningTrends.last30Days(retained, today), LearningTrends.last30Days(retained, today - 30))
                        Text("تقييم ذاتي من آخر ٣٠ محاولة محفوظة لكل آية؛ لا يثبت تحسنًا سببيًا أو احتفاظًا طويل المدى.")
                        LearningTrends.byMode(retained, today).forEach { (kind, summary) ->
                            Text((if (kind == PracticeKind.BRIDGE) "ربط الآيات" else "استرجاع الآية") +
                                " • " + summary.attempts + " محاولة • " +
                                (summary.independentPercent?.let { "$it% دون مساعدة" } ?: "لا بيانات"))
                        }
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
                Screen.LOCAL_TRANSCRIPTION -> LocalTranscriptionScreen()
                Screen.RECITERS -> {
                    Section("دليل قرّاء مصر والسعودية", "أسماء مبدئية • التسجيلات تحتاج مراجعة التراخيص")
                    RecitationRangePreview(corpus, favoriteReciter, verse, today)
                    ReciterCatalog.all.groupBy { it.country }.forEach { (country, people) ->
                        Text(country.displayArabic, color = green, fontSize = 21.sp)
                        people.forEach { person ->
                            Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(person.nameArabic, color = gold, fontSize = 18.sp)
                                    OutlinedButton(onClick = {
                                        favoriteReciter = if (favoriteReciter == person.slug) null else person.slug
                                        store.favoriteReciter = favoriteReciter
                                    }) { Text(if (favoriteReciter == person.slug) "قارئي المفضل • إلغاء" else "اختيار قارئ مفضل") }
                                    Text("بيانات تعريفية فقط — التلاوات قيد فحص حقوق الاستخدام",
                                        color = muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Screen.SEARCH -> {
                    Section("البحث والانتقال", "فهرس البحث منفصل عن النص القرآني الأصلي")
                    OutlinedTextField(value = jumpQuery, onValueChange = { jumpQuery = it.take(15) },
                        label = { Text("سورة:آية مثل ٢:٢٥٥") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("انتقل إلى الآية", Modifier.fillMaxWidth()) {
                        val address = QuranSearchIndex.address(jumpQuery)
                        if (address == null) message = "اكتب رقم السورة ثم : ثم رقم الآية"
                        else jumpTo(address.first, address.second)
                    }
                    store.bookmark()?.let { bookmark ->
                        OutlinedButton(onClick = { jumpTo(bookmark.first, bookmark.second) }, modifier = Modifier.fillMaxWidth()) {
                            Text("موضع القراءة المحفوظ • " + bookmark.first + ":" + bookmark.second)
                        }
                    }
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it.take(100) },
                        label = { Text("ابحث بكلمتين أو جزء من آية") }, modifier = Modifier.fillMaxWidth())
                    val found = remember(searchQuery, corpus) { corpus.searchIndex.search(searchQuery) }
                    if (searchQuery.length >= 2 && found.isEmpty()) Text("لا توجد نتائج مطابقة في فهرس البحث", color = muted)
                    found.forEach { result ->
                        Card(colors = CardDefaults.cardColors(containerColor = panel)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(corpus.info(result.chapter).arabicName + " • " + result.verse, color = gold)
                                CanonicalQuranText(corpus, result, 24)
                                OutlinedButton(onClick = { jumpTo(result.chapter, result.verse) }) { Text("عرض الآية في المصحف") }
                            }
                        }
                    }
                    if (found.size == 40) Text("أول ٤٠ نتيجة؛ أضف كلمات لتضييق البحث", color = muted)
                }
                Screen.TODAY -> {
                    Section("الخطة اليومية", "مراجعات المصحف كله ثم حفظ جديد في السورة المختارة")
                    val plan = dailyPlan
                    Text("المستحقة اليوم: " + dueCount + " • المعروض حسب خطتك: " + plan.dueVerseIds.size, color = green)
                    Text("الوضع: " + learningGoal.mode.arabic + " • الحفظ الجديد: سورة " + corpus.info(planChapter).arabicName + " • الهدف: " + learningGoal.newVerses)
                    plan.dueVerseIds.forEach { id ->
                        val item = corpus.verses.first { it.id == id }
                        OutlinedButton(onClick = { openCorpusVerse(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text(corpus.info(item.chapter).arabicName + " • الآية " + item.verse)
                        }
                    }
                    if (plan.dueVerseIds.isEmpty()) Text("لا توجد مراجعات مقترحة إضافية ضمن حد اليوم. يمكنك اختيار آية يدويًا.", color = muted)
                    Text("آيات قيّمتها اليوم: جديد " + dayWork.newVerseIds.size + " • مراجعة " + dayWork.reviewVerseIds.size +
                        ". إعادة تقييم الآية نفسها لا يخصم موضعًا إضافيًا من حد اليوم.", color = muted)
                    val covered = dayWork.allVerseIds
                    val retries = corpus.verses.filter { item ->
                        val card = cards[item.id]
                        item.id in covered && card != null && card.lapses > 0 && card.dueEpochDay <= today
                    }.take(15)
                    if (retries.isNotEmpty()) Text("إعادة محاولة اختيارية لمواضع اليوم المتعثرة • أول ١٥ موضعًا", color = gold)
                    retries.forEach { item ->
                        OutlinedButton(onClick = { openCorpusVerse(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text("أعد المحاولة • " + corpus.info(item.chapter).arabicName + " • " + item.verse)
                        }
                    }
                    PrimaryButton("ابدأ الخطة المقترحة", Modifier.fillMaxWidth()) { focusToday() }
                    Text("أسبوعي وشهري: مواعيد الآيات تُحسب من تقييمك الذاتي؛ لا تُضاعف الخوارزمية جلسات إلزامية دون حاجة.", color = muted)
                }
                Screen.PRIVACY -> {
                    Section("خصوصيتك وحماية الأطفال", "وضع محلي دون حساب أو رفع صوت")
                    Text("التقدم وموضع القراءة والجلسة محفوظة داخل مساحة التطبيق الخاصة. تسجيل الاستماع الذاتي ملف خاص مؤقت يُحذف عند مغادرة التدريب أو وضع التطبيق في الخلفية. صوت التفريغ التجريبي في RAM فقط؛ مسودته لا تُحفظ وتُمحى بالمغادرة. لا تُحفظ إجابات التسميع الكتابية.")
                    Text("لا إذن إنترنت أو جهات اتصال أو موقع. يطلب الميكروفون فقط عند اختيار التسجيل. تُستبعد بيانات التطبيق من النسخ الاحتياطي.")
                    Text("لا توجد محادثات عامة أو اتصال بين أطفال ومعلمين عبر هذا الإصدار. دفتر المعلم للتنظيم المحلي بإشراف ولي الأمر؛ ليس أكاديمية متصلة أو اعتمادًا لمؤهلات معلم.")
                    OutlinedButton(onClick = { screen = Screen.SETTINGS }, modifier = Modifier.fillMaxWidth()) { Text("إدارة التقدم وحذف البيانات") }
                }
                Screen.SOURCES -> {
                    Section("المصادر والتراخيص", "الكود والمحتوى لهما حقوق منفصلة")
                    Text(QuranCorpus.SOURCE_CREDIT, color = gold)
                    Text("نسخة حرفية مطابقة لتنزيل Tanzil الرسمي • 114 سورة و6236 آية. المطابقة التقنية لا تعني اعتماد اللجنة.")
                    Text("CC BY 3.0 مع شرط Tanzil بعدم تغيير النص. جميع علامات النص محفوظة؛ فهرس البحث مستقل.")
                    Text("SHA-256: " + QuranCorpus.SOURCE_SHA256, fontSize = 12.sp)
                    Text("حدود الأجزاء والصفحات: Tanzil Metadata 1.0، المصدر يحدد cc-by دون رقم إصدار؛ توضيح إصدار الرخصة والتدقيق المتخصص ضمن موانع الاعتماد النهائي.")
                    OutlinedButton(onClick = { openExternal("https://tanzil.net/docs/Quran_Metadata") }, modifier = Modifier.fillMaxWidth()) {
                        Text("مصدر حدود الأجزاء والصفحات")
                    }
                    Text("خط المصحف Amiri Quran: SIL Open Font License 1.1، إشعار الحقوق مضمن. تغطية المحارف اختُبرت تقنيًا؛ تدقيق تشكيلها بصريًا ما زال مطلوبًا.")
                    BundledLicenseText("شروط Tanzil المحفوظة مع النص الأصلي", "licenses/TANZIL-NOTICE.txt")
                    BundledLicenseText("ترخيص خط Amiri Quran", "licenses/AMIRI-OFL.txt")
                    BundledLicenseText("ترخيص كود NEXVARY MIT", "licenses/NEXVARY-MIT.txt")
                    BundledLicenseText("نص Apache 2.0 لمكونات AndroidX وKotlin", "licenses/Apache-2.0.txt")
                    Text("الكود الأصلي: MIT. مكونات AndroidX وCompose: Apache 2.0. إشعارات الحقوق كاملة ضمن ملفات المصدر.")
                    OutlinedButton(onClick = { openExternal("https://tanzil.net/docs/Text_License") },
                        modifier = Modifier.fillMaxWidth()) { Text("شروط Tanzil الأصلية") }
                    Text("لا تسجيلات تلاوة خارجية مضمنة قبل توثيق الحقوق. خوارزمية المراجعة مستلهمة من الاسترجاع والمراجعة المتباعدة؛ نسب الإخفاء سياسة تصميم لم تُثبت فعاليتها لهذا التطبيق بعد.")
                    Text("التفريغ المحلي التجريبي: Whisper Tiny من OpenAI وwhisper.cpp 1.9.5 / ggml، بتراخيص MIT. لا اعتماد لدقة القرآن أو التجويد. النموذج محفوظ محليًا، وبصمته تُفحص قبل التشغيل.")
                    BundledLicenseText("ترخيص نموذج Whisper", "licenses/Whisper-model-MIT.txt")
                    BundledLicenseText("ترخيص whisper.cpp / ggml", "licenses/whisper-cpp-MIT.txt")
                    BundledLicenseText("إشعارات وقت التشغيل الأصلي Android NDK", "licenses/Android-NDK-NOTICE.txt")
                }
                Screen.ABOUT -> {
                    Section("حول NEXVARY HifzAI", "نسخة تطوير داخلية 0.5.0-dev • مفتوحة المصدر")
                    Text("المصحف دون إنترنت، الحفظ التدريجي، المراجعات والتسميع الذاتي. لا اعتماد علمي أو إجازة قرآنية آلية.")
                    Text("لا يُرسل إلى اللجنة إلا ملف واحد بعد اجتياز جميع شروط القبول. هذه النسخة غير جاهزة لذلك الطلب.")
                    OutlinedButton(onClick = { openExternal("https://github.com/nexvary/NEXVARY-HifzAI") },
                        modifier = Modifier.fillMaxWidth()) { Text("الشفرة المصدرية والتوثيق") }
                }
                Screen.HELP -> {
                    Section("دليل الاستخدام", "ابدأ بجلسة قصيرة ثم راجع بتقييم صادق")
                    Text("١. افتح المصحف واختر السورة، أو ابحث وانتقل برقم سورة:آية. النص المعروض هو الأصل دون تغيير.")
                    Text("٢. ابدأ الحفظ: قراءة، إخفاء 35%، إخفاء 70%، استرجاع، ربط. الوضع المتقدم يتيح اختيار المرحلة مباشرة.")
                    Text("٣. التلميح وكشف الإجابة يُحتسبان مساعدة؛ تبديل المرحلة لا يمحو المساعدة. النتائج تقييم ذاتي.")
                    Text("٤. سجّل صوتك اختياريًا واستمع لنفسك. ليس هناك تصحيح تجويد آلي. الصوت يُحذف عند الخروج أو الخلفية.")
                    Text("٥. زر استكمال الجلسة يعيد موضعك ومرحلتك دون الصوت أو الإجابة المكتوبة. يمكنك حذف التقدم من الإعدادات.")
                    Text("التفريغ التجريبي من الإعدادات يعمل بنموذج محلي دون اتصال؛ قد يخطئ ولا يغيّر تقييمك أو نص المصحف. الإلغاء والمغادرة يمحوان المسودة.")
                    Text("٦. تقسيم العرض كل 8 آيات ليس ترقيم صفحات مصحف المدينة. المتشابهات اقتراحات لفظية غير معتمدة.")
                }
                Screen.PARTITIONS -> {
                    val source = partitions.getOrNull()
                    if (source == null) Text("تعذر التحقق من بيانات حدود الأجزاء والصفحات؛ أُوقف عرضها")
                    else PartitionPickerScreen(corpus, source) { chosen ->
                        rangeStart = chosen.first().id; rangeEnd = chosen.last().id
                        store.saveSession(PracticeSession(chosen.first().chapter, chosen.first().verse,
                            PracticeKind.READ, 0, false, DifficultyTag.NONE, rangeStart, rangeEnd))
                        screen = Screen.RANGE_PRACTICE
                    }
                }
                Screen.RANGE_PRACTICE -> {
                    val range = remember(corpus, rangeStart, rangeEnd) {
                        corpus.verses.filter { it.id in rangeStart..rangeEnd }
                    }
                    RangePracticeScreen(corpus, range, store, record = { item, result, hints, exposed, bridge ->
                        today = LocalDate.now().toEpochDay()
                        val bounded = RecallLadder.boundedGrade(result, hints, exposed)
                        val beforeCard = cards[item.id] ?: ReviewCard(item.id)
                        val beforeMemory = memories[item.id] ?: MemoryFingerprint(item.id)
                        val card = HifzScheduler.grade(beforeCard, bounded, today)
                        val memory = MemoryCoach.record(beforeMemory,
                            bounded, hints + if (exposed) 1 else 0, bridge)
                        cards[item.id] = card; memories[item.id] = memory
                        store.recordAssessment(card, memory, RecallObservation(today, bounded, hints, exposed, DifficultyTag.NONE, if (bridge) PracticeKind.BRIDGE else PracticeKind.RECALL),
                            firstAttempt = beforeMemory.attempts == 0 && beforeCard.repetitions == 0 && beforeCard.lapses == 0)
                        gentleReturnAnchor = store.gentleReturnAnchorDay
                    }, finish = {
                        message = "انتهت جلسة النطاق؛ راجع خطة اليوم للمواعيد التالية"
                        screen = Screen.TODAY
                    })
                }
                Screen.TEACHERS -> TeacherNotebookScreen(corpus, LocalDate.ofEpochDay(today))
                Screen.EXERCISES -> VerseExercisesScreen(corpus, verses, index, ::openCorpusVerse)
                Screen.SETTINGS -> {
                    OutlinedButton(onClick = { screen = Screen.LOCAL_TRANSCRIPTION }, modifier = Modifier.fillMaxWidth()) {
                        Text("تفريغ صوتي محلي تجريبي")
                    }
                    Section("الإعدادات", "خصص تجربتك واختر الصفحات التالية")
                    AppearanceControls(appearance, changeAppearance)
                    LearningGoalControls(learningGoal, dailyStudyMinutes, { value ->
                        dailyStudyMinutes = value; store.dailyStudyMinutes = value
                    }) { value ->
                        learningGoal = value; store.learningGoal = value
                    }
                    Text("تذكير العودة داخل التطبيق", style = MaterialTheme.typography.titleLarge)
                    Text("اختياري: بعد أسبوع دون تدريب مسجل نقترح جلسة قصيرة عند فتح الرئيسية. لا إشعارات أو إرسال بيانات. الإيقاف يمحو تاريخ هذا التذكير.")
                    OutlinedButton(onClick = {
                        gentleReturnEnabled = !gentleReturnEnabled
                        store.setGentleReturn(gentleReturnEnabled, today)
                        gentleReturnAnchor = store.gentleReturnAnchorDay
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (gentleReturnEnabled) "إيقاف تذكير العودة" else "تفعيل تذكير العودة الاختياري")
                    }
                    OutlinedButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (advanced) "الوضع الحالي: متقدم • التحويل إلى مبسط" else "الوضع الحالي: مبسط • التحويل إلى متقدم")
                    }
                    listOf(Screen.PRIVACY to "الخصوصية وحماية الأطفال", Screen.SOURCES to "المصادر والتراخيص",
                        Screen.TEACHERS to "دفتر المعلم المحلي", Screen.HELP to "المساعدة", Screen.ABOUT to "حول التطبيق").forEach { (destination, label) ->
                        OutlinedButton(onClick = { screen = destination }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                    Section("إدارة البيانات", "كل تقدمك محفوظ محليًا على هذا الهاتف")
                    OutlinedButton(onClick = { screen = Screen.RECITERS },
                        modifier = Modifier.fillMaxWidth()) { Text("دليل القراء") }
                    Text(QuranCorpus.SOURCE_CREDIT, color = gold)
                    OutlinedButton(onClick = {
                        openExternal(QuranCorpus.SOURCE_LINK)
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
                    Text("ملف CSV يحتوي على أرقام الآيات وعدادات التقدم. اختر مكان حفظ تثق به.")
                    OutlinedButton(onClick = {
                        if (export.beginPicking()) {
                            try {
                                createProgressFile.launch("hifz-progress.csv")
                            } catch (_: android.content.ActivityNotFoundException) {
                                export.pickerUnavailable()
                            }
                        }
                    }, enabled = !exportState.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("حفظ التقدم في ملف CSV")
                    }
                    if (exportState != ProgressExportViewModel.Status.IDLE) {
                        Text(exportState.message,
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (confirmClear) {
                        Text("تأكيد: سيُحذف التقدم والجلسة ودفتر المعلم المحلي ولا يمكن استعادتها.",
                            color = gold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { confirmClear = false }) { Text("إلغاء") }
                            Button(onClick = {
                                voicePractice.erase()
                                typedRecall = ""
                                if (!resetRuntime()) message = "تعذر بدء حذف البيانات؛ حاول مرة أخرى"
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
                    PrimaryButton("ملخص التطوير الداخلي — ليس طلب اعتماد", Modifier.fillMaxWidth()) {
                        val brief = "NEXVARY HifzAI — ملخص تطوير داخلي\\n" +
                            "نسخة تطوير تجريبية غير معتمدة للنشر العام.\\n" +
                            "النص المضمّن: Tanzil Uthmani 1.1 كاملًا، 114 سورة و6236 آية؛ العرض لم يعتمد علميًا.\\n" +
                            "الاختبارات: الإخفاء، سُلّم الاسترجاع، المراجعة المتباعدة، الربط، البصمة.\\n" +
                            "لا يوجد تحقق صوتي أو فتوى أو حكم آلي على التجويد.\\n" +
                            "تحضير داخلي: فحص الرسم وضبط الآيات والإخفاء وتمارين الربط؛ " +
                            "لا ترسل الملف إلى اللجنة قبل إكمال جميع الوظائف والاختبارات."
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, brief)
                        }, "مشاركة ملخص التطوير الداخلي"))
                    }
                    Text("الإصدار التجريبي 0.5.0-dev — غير معتمد للنشر العام",
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
        Text(title, color = gold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        Text(sub, color = muted, fontSize = 14.sp)
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(value, color = green, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
            Text(title, color = silver, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PrimaryButton(title: String, modifier: Modifier = Modifier, action: () -> Unit) {
    Button(
        onClick = action, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = MaterialTheme.colorScheme.onPrimary)
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
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) { Text(title, fontSize = 12.sp, maxLines = 1) }
}

@Composable
private fun DifficultyButton(title: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    StageButton(title, selected, modifier, action)
}
