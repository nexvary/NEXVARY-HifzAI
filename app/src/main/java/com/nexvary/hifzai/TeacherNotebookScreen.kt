package com.nexvary.hifzai

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.TeacherNotebook
import java.time.LocalDateTime
import com.nexvary.hifzai.learning.NotebookRules

@Composable
internal fun TeacherNotebookScreen() {
    val context = LocalContext.current
    val notebook = remember(context) { TeacherNotebook(context) }
    var revision by remember { mutableIntStateOf(0) }
    val students = remember(revision) { notebook.students() }
    val lessons = remember(revision) { notebook.lessons() }
    var page by remember { mutableIntStateOf(0) }
    var alias by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf<String?>(null) }
    var teacher by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var complaint by remember { mutableStateOf("") }
    var complaintLesson by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var deleteStudent by remember { mutableStateOf<String?>(null) }
    fun act(operation: () -> Unit) {
        message = try { operation(); revision++; "تم حفظ التغيير محليًا" }
        catch (e: Exception) { e.message?.take(160) ?: "تعذر حفظ البيانات" }
    }
    Text("دفتر المعلم المحلي", style = MaterialTheme.typography.headlineSmall)
    Text("تنظيم الطلاب والجلسات على جهازك فقط. لا حسابات معلمين موثقة أو إجازات آلية. الأكاديمية المتصلة والتحقق البشري لم يكتملَا.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { page = 0 }, modifier = Modifier.weight(1f)) { Text("الطلاب") }
        OutlinedButton(onClick = { page = 1 }, modifier = Modifier.weight(1f)) { Text("الجلسات") }
    }
    if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
    if (page == 0) {
        OutlinedTextField(value = alias, onValueChange = { alias = it.take(40) },
            label = { Text("اسم مستعار للطالب") }, modifier = Modifier.fillMaxWidth())
        Row {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("أقر بإشراف ولي الأمر للطفل أو أن الطالب بالغ؛ لا أسجل بيانات شخصية حساسة")
        }
        Button(onClick = { act { notebook.addStudent(alias, consent); alias = ""; consent = false } },
            enabled = alias.isNotBlank() && consent, modifier = Modifier.fillMaxWidth()) { Text("أضف طالبًا") }
        if (students.isEmpty()) Text("لم تُضف طلابًا بعد")
        students.forEach { student ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(student.alias)
                    Text("جلسات: " + lessons.count { it.studentId == student.id } +
                        " • مكتملة: " + lessons.count { it.studentId == student.id && it.completed })
                    OutlinedButton(onClick = { studentId = student.id; page = 1 }) { Text("جدولة جلسة لهذا الطالب") }
                    OutlinedButton(onClick = { deleteStudent = student.id }) { Text("حذف الطالب وجلساته") }
                }
            }
        }
    } else {
        Text("اختر الطالب")
        students.forEach { student ->
            OutlinedButton(onClick = { studentId = student.id }, modifier = Modifier.fillMaxWidth()) {
                Text(student.alias + if (studentId == student.id) " • محدد" else "")
            }
        }
        OutlinedTextField(value = teacher, onValueChange = { teacher = it.take(60) },
            label = { Text("اسم المعلم المعروف لك • غير موثق من التطبيق") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            val now = LocalDateTime.now()
            DatePickerDialog(context, { _, year, month, day ->
                TimePickerDialog(context, { _, hour, minute ->
                    time = LocalDateTime.of(year, month + 1, day, hour, minute).toString()
                }, now.hour, now.minute, true).show()
            }, now.year, now.monthValue - 1, now.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (time.isBlank()) "اختر يوم ووقت الجلسة" else
                NotebookRules.displayDateTime(time))
        }
        OutlinedTextField(value = note, onValueChange = { note = it.take(500) },
            label = { Text("هدف الجلسة وملاحظات الحفظ") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { act {
            notebook.addLesson(studentId!!, teacher, time, note); note = ""
        } }, enabled = studentId != null && teacher.isNotBlank() && time.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text("احفظ الموعد المحلي") }
        Text("المواعيد تنظيمية داخل الجهاز؛ لا ترسل دعوة للمعلم ولا تتأكد من تفرغه")
        lessons.forEach { lesson ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(students.firstOrNull { it.id == lesson.studentId }?.alias ?: "طالب محذوف")
                    Text(lesson.teacherLabel + " • " + NotebookRules.displayDateTime(lesson.dateTime))
                    Text(lesson.note)
                    OutlinedButton(onClick = { act { notebook.updateLesson(lesson.id, lesson.note, !lesson.completed) } }) {
                        Text(if (lesson.completed) "مكتملة • أعد فتحها" else "سجّل اكتمال الجلسة")
                    }
                    OutlinedButton(onClick = { complaintLesson = lesson.id }) { Text("دوّن مشكلة محليًا") }
                }
            }
        }
        notebook.complaints().forEach { Text("ملاحظة مشكلة محفوظة: " + it.note) }
    }
    deleteStudent?.let { id ->
        AlertDialog(onDismissRequest = { deleteStudent = null }, title = { Text("حذف الطالب؟") },
            text = { Text("تُحذف بياناته وجلساته والملاحظات المحلية المرتبطة، ولا يمكن الاستعادة.") },
            confirmButton = { TextButton(onClick = {
                act { notebook.removeStudent(id) }; if (studentId == id) studentId = null; deleteStudent = null
            }) { Text("احذف") } }, dismissButton = { TextButton(onClick = { deleteStudent = null }) { Text("إلغاء") } })
    }
    complaintLesson?.let { id ->
        AlertDialog(onDismissRequest = { complaintLesson = null }, title = { Text("تدوين مشكلة محليًا") },
            text = { Column {
                Text("هذه الملاحظة لا تُرسل لأي جهة. تواصل بنفسك مع ولي الأمر أو الجهة المناسبة عند الحاجة.")
                OutlinedTextField(value = complaint, onValueChange = { complaint = it.take(500) }, label = { Text("الملاحظة") })
            } }, confirmButton = { TextButton(onClick = {
                act { notebook.recordComplaint(id, complaint) }; complaint = ""; complaintLesson = null
            }, enabled = complaint.isNotBlank()) { Text("احفظ على الجهاز") } },
            dismissButton = { TextButton(onClick = { complaintLesson = null }) { Text("إلغاء") } })
    }
}
