package com.nexvary.hifzai

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.VerifiedVerse

private enum class ContentIssue(val label: String) {
    DISPLAY("عرض النص أو التشكيل"),
    ADDRESS("ترقيم الآية أو مرجعها"),
    OTHER("ملاحظة أخرى على المحتوى")
}

@Composable
internal fun ContentReportDialog(verse: VerifiedVerse, dismiss: () -> Unit) {
    val context = LocalContext.current
    var issue by rememberSaveable(verse.id) { mutableStateOf(ContentIssue.DISPLAY) }
    val draft = "بلاغ محتوى • NEXVARY HifzAI\n" +
        "مرجع الآية: ${verse.chapter}:${verse.verse}\n" +
        "نوع الملاحظة: ${issue.label}\n" + QuranCorpus.SOURCE_CREDIT
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("معاينة بلاغ المحتوى") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("اختر نوع الملاحظة. راجع النص أدناه قبل اختيار جهة المشاركة.")
                Column(Modifier.selectableGroup()) {
                    ContentIssue.entries.forEach { item ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = issue == item, onClick = { issue = item }, role = Role.RadioButton
                        ), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = issue == item, onClick = null)
                            Text(item.label)
                        }
                    }
                }
                Text(draft)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, draft)
                }
                context.startActivity(Intent.createChooser(send, "مشاركة بلاغ المحتوى"))
            }) { Text("مشاركة المسودة") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("إلغاء البلاغ") } }
    )
}
