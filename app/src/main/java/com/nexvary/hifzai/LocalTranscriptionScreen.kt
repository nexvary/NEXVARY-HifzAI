package com.nexvary.hifzai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nexvary.hifzai.learning.LocalTranscription

@Composable
internal fun LocalTranscriptionScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf<LocalTranscription.State>(LocalTranscription.State.Idle) }
    val engine = remember(context) { LocalTranscription(context) { state = it } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) engine.start() else state = LocalTranscription.State.Unable("لم يُمنح إذن الميكروفون")
    }
    DisposableEffect(engine,context) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event -> if (event==Lifecycle.Event.ON_STOP) {
            engine.cancel(); state = LocalTranscription.State.Idle
        } }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer); engine.close() }
    }
    Text("تفريغ صوتي محلي تجريبي", style=MaterialTheme.typography.headlineSmall)
    Text("المعالجة على هاتفك بنموذج Whisper Tiny، دون خادم. تحدث حتى 30 ثانية، ثم أوقف التسجيل لعرض مسودة التفريغ.")
    Text("غير قادر على التأكد من صحة التسميع: لم تُقَس دقة هذا النموذج للقرآن بشكل مستقل. لا درجات آلية أو حكم تجويد أو تعديل للمصحف.", color=MaterialTheme.colorScheme.primary)
    when (val value = state) {
        LocalTranscription.State.Preparing -> {
            Text("جارٍ تجهيز الميكروفون المحلي…")
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
        }
        LocalTranscription.State.Recording -> {
            Text("الميكروفون يعمل الآن • الصوت في الذاكرة المؤقتة فقط")
            Button(onClick={ engine.finishCapture(); state=LocalTranscription.State.Processing },modifier=Modifier.fillMaxWidth()) { Text("أوقف التسجيل وافرغ محليًا") }
        }
        LocalTranscription.State.Processing -> {
            Text("جارٍ تجهيز النموذج والتفريغ على الجهاز…")
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
        }
        is LocalTranscription.State.Draft -> {
            Text("مسودة آلية قد تخطئ أو تضيف كلامًا لم يُنطق • ليست نصًا قرآنيًا معتمدًا")
            Text(value.text, fontFamily=FontFamily.SansSerif)
        }
        is LocalTranscription.State.Unable -> Text(value.reason)
        else -> Text("لا تسجيل ولا تفريغ محفوظ في هذه الصفحة.")
    }
    val busy = state == LocalTranscription.State.Preparing || state == LocalTranscription.State.Recording || state == LocalTranscription.State.Processing
    if (!busy) Button(onClick={
        state=LocalTranscription.State.Idle
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) engine.start()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    },modifier=Modifier.fillMaxWidth()) { Text("ابدأ تسميعًا محليًا تجريبيًا") }
    OutlinedButton(onClick={ engine.cancel(); state=LocalTranscription.State.Idle },modifier=Modifier.fillMaxWidth()) { Text("إلغاء ومحو المسودة") }
    Text("المغادرة أو الخلفية توقف الجلسة وتمحو مسودتها. لا يُحفظ الصوت على القرص ولا تُحفظ النتيجة في تقدمك. لا تستخدم مسودة المحرك لتصحيح المصحف؛ ارجع للنص الأصلي.")
}
