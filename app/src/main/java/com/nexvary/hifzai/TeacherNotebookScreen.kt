package com.nexvary.hifzai

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.TeacherNotebook
import java.time.LocalDateTime
import com.nexvary.hifzai.learning.NotebookRules
import com.nexvary.hifzai.learning.QuranCorpus
import com.nexvary.hifzai.learning.QuranSearchIndex
import com.nexvary.hifzai.learning.NotebookLesson
import com.nexvary.hifzai.learning.NotebookAssignment
import java.time.LocalDate

@Composable
internal fun TeacherNotebookScreen(corpus: QuranCorpus, today: LocalDate) {
    val context = LocalContext.current
    val notebook = remember(context) { TeacherNotebook(context) }
    var revision by remember { mutableIntStateOf(0) }
    val students = remember(revision) { notebook.students() }
    val lessons = remember(revision) { notebook.lessons() }
    val assignments = remember(revision) { notebook.assignments() }
    var page by rememberSaveable { mutableIntStateOf(0) }
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
    var editing by remember { mutableStateOf<NotebookLesson?>(null) }
    var editedTime by remember { mutableStateOf("") }
    var editedNote by remember { mutableStateOf("") }
    var assignmentStudent by remember { mutableStateOf<String?>(null) }
    var editingAssignment by remember { mutableStateOf<String?>(null) }
    var deleteAssignment by remember { mutableStateOf<NotebookAssignment?>(null) }
    var deleteLesson by remember { mutableStateOf<NotebookLesson?>(null) }
    var assignmentStart by remember { mutableStateOf("") }
    var assignmentEnd by remember { mutableStateOf("") }
    var assignmentDue by remember { mutableStateOf(LocalDate.now().toString()) }
    fun act(operation: () -> Unit) {
        message = try { operation(); revision++; "تم حفظ التغيير محليًا" }
        catch (e: Exception) { e.message?.take(160) ?: "تعذر حفظ البيانات" }
    }
    fun assignmentChapterName(task: NotebookAssignment) = if (task.firstAyah <= task.lastAyah &&
        corpus.find(task.chapter, task.firstAyah) != null && corpus.find(task.chapter, task.lastAyah) != null)
        corpus.info(task.chapter).arabicName else "مرجع آيات غير صالح"
    fun beginLessonEdit(lesson: NotebookLesson) {
        editing = lesson; editedTime = lesson.dateTime; editedNote = lesson.note; message = ""
    }
    fun beginAssignmentEdit(task: NotebookAssignment) {
        assignmentStudent = task.studentId; editingAssignment = task.id
        assignmentStart = "${task.chapter}:${task.firstAyah}"
        assignmentEnd = "${task.chapter}:${task.lastAyah}"
        assignmentDue = task.dueDate; message = ""
    }
    Text("دفتر المعلم المحلي", style = MaterialTheme.typography.headlineSmall)
    Text("تنظيم الطلاب والجلسات على جهازك فقط، دون حساب أو خادم. الملاحظات لا تُرسل لأحد، والمواعيد ليست حجزًا لدى معلم خارجي. لا تحقق من المؤهلات أو إجازات آلية.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { page = 0 }, modifier = Modifier.weight(1f)) { Text("الطلاب") }
        OutlinedButton(onClick = { page = 1 }, modifier = Modifier.weight(1f)) { Text("الجلسات") }
    }
    OutlinedButton(onClick = { page = 2; message = "" }, enabled = page != 2,
        modifier = Modifier.fillMaxWidth()) { Text("جدول المواعيد والمهام") }
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
                    OutlinedButton(onClick = {
                        assignmentStudent = student.id; editingAssignment = null
                        assignmentStart = ""; assignmentEnd = ""; assignmentDue = LocalDate.now().toString(); message = ""
                    }) { Text("تعيين نطاق مراجعة محلي") }
                    assignments.filter { it.studentId == student.id }.forEach { task ->
                        Text("مراجعة " + assignmentChapterName(task) + " • " + task.firstAyah + "–" + task.lastAyah +
                            " • " + task.dueDate)
                        OutlinedButton(onClick = { act { notebook.completeAssignment(task.id, !task.completed) } }) {
                            Text(if (task.completed) "مكتملة • أعد فتح المهمة" else "سجّل اكتمال المراجعة")
                        }
                        OutlinedButton(onClick = { beginAssignmentEdit(task) }) { Text("تعديل نطاق المراجعة وموعده") }
                        OutlinedButton(onClick = { deleteAssignment = task }) { Text("حذف مهمة المراجعة") }
                    }
                    OutlinedButton(onClick = { deleteStudent = student.id }) { Text("حذف الطالب وجلساته") }
                }
            }
        }
    } else if (page == 1) {
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
                    OutlinedButton(onClick = { beginLessonEdit(lesson) }) { Text("تعديل الموعد والملاحظات") }
                    OutlinedButton(onClick = { act { notebook.updateLesson(lesson.id, lesson.note, !lesson.completed) } }) {
                        Text(if (lesson.completed) "مكتملة • أعد فتحها" else "سجّل اكتمال الجلسة")
                    }
                    OutlinedButton(onClick = { complaintLesson = lesson.id }) { Text("دوّن مشكلة محليًا") }
                    OutlinedButton(onClick = { deleteLesson = lesson }) { Text("حذف هذه الجلسة") }
                }
            }
        }
        notebook.complaints().forEach { Text("ملاحظة مشكلة محفوظة: " + it.note) }
    } else {
        NotebookAgendaScreen(students, lessons, assignments, corpus, today,
            editLesson = ::beginLessonEdit,
            completeLesson = { lesson -> act { notebook.updateLesson(lesson.id, lesson.note, !lesson.completed) } },
            problem = { complaintLesson = it.id }, deleteLesson = { deleteLesson = it },
            editAssignment = ::beginAssignmentEdit,
            completeAssignment = { task -> act { notebook.completeAssignment(task.id, !task.completed) } },
            deleteAssignment = { deleteAssignment = it })
    }
    deleteAssignment?.let { task ->
        NotebookDeleteDialog("حذف مهمة المراجعة؟",
            "تُحذف مهمة " + assignmentChapterName(task) + " • " + task.firstAyah + "–" + task.lastAyah +
                " بتاريخ " + task.dueDate + " من الدفتر فقط. تبقى بقية المهام وتقدم الحفظ كما هي.",
            onDelete = { act { notebook.removeAssignment(task.id) }; deleteAssignment = null },
            onDismiss = { deleteAssignment = null })
    }
    deleteLesson?.let { lesson ->
        NotebookDeleteDialog("حذف الجلسة المحلية؟",
            lesson.teacherLabel + " • " + NotebookRules.displayDateTime(lesson.dateTime) +
                "\nتُحذف هذه الجلسة وملاحظات المشكلة المرتبطة بها نهائيًا. تبقى بقية الجلسات وملف الطالب.",
            onDelete = { act { notebook.removeLesson(lesson.id) }; deleteLesson = null },
            onDismiss = { deleteLesson = null })
    }
    deleteStudent?.let { id ->
        AlertDialog(onDismissRequest = { deleteStudent = null }, title = { Text("حذف الطالب؟") },
            text = { Text("تُحذف بياناته وجلساته والملاحظات المحلية المرتبطة، ولا يمكن الاستعادة.") },
            confirmButton = { TextButton(onClick = {
                act { notebook.removeStudent(id) }; if (studentId == id) studentId = null; deleteStudent = null
            }) { Text("احذف") } }, dismissButton = { TextButton(onClick = { deleteStudent = null }) { Text("إلغاء") } })
    }
    editing?.let { lesson ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("تعديل الجلسة المحلية") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(onClick = {
                    val initial = runCatching { LocalDateTime.parse(editedTime) }.getOrDefault(LocalDateTime.now())
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            editedTime = LocalDateTime.of(year, month+1, day, hour, minute).toString()
                        }, initial.hour, initial.minute, true).show()
                    }, initial.year, initial.monthValue-1, initial.dayOfMonth).show()
                }) { Text(NotebookRules.displayDateTime(editedTime)) }
                OutlinedTextField(value = editedNote, onValueChange = { editedNote = it.take(500) },
                    label = { Text("ملاحظات الجلسة") })
                if (message.isNotEmpty()) Text(message)
            } }, confirmButton = { TextButton(onClick = { act {
                notebook.rescheduleLesson(lesson.id, editedTime, editedNote); editing = null
            } }) { Text("احفظ التعديل") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } })
    }
    assignmentStudent?.let { id ->
        AlertDialog(onDismissRequest = { assignmentStudent = null; editingAssignment = null },
            title = { Text(if (editingAssignment == null) "نطاق مراجعة محلي" else "تعديل مهمة المراجعة") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("تنظيم شخصي؛ لا تُرسل المهمة للطالب أو لمعلم خارجي.")
                if (editingAssignment != null) Text("تغيير الآيات يعيد فتح المهمة؛ تغيير التاريخ وحده يحفظ حالة اكتمالها.")
                OutlinedTextField(value = assignmentStart, onValueChange = { assignmentStart = it.take(15) },
                    label = { Text("البداية • سورة:آية") })
                OutlinedTextField(value = assignmentEnd, onValueChange = { assignmentEnd = it.take(15) },
                    label = { Text("النهاية في السورة نفسها • سورة:آية") })
                OutlinedButton(onClick = {
                    // Only the picker's initial position falls back. Stored data stays
                    // unchanged until the user explicitly chooses a day and saves.
                    val initial = runCatching { LocalDate.parse(assignmentDue) }.getOrDefault(LocalDate.now())
                    DatePickerDialog(context, { _, year, month, day ->
                        assignmentDue = LocalDate.of(year, month+1, day).toString()
                    }, initial.year, initial.monthValue-1, initial.dayOfMonth).show()
                }) { Text("تاريخ المراجعة: " + assignmentDue) }
                if (message.isNotEmpty()) Text(message)
            } }, confirmButton = { TextButton(onClick = { act {
                val first = QuranSearchIndex.address(assignmentStart)
                val last = QuranSearchIndex.address(assignmentEnd)
                require(first != null && last != null && first.first == last.first) { "اختر نطاقًا صحيحًا في سورة واحدة" }
                val taskId = editingAssignment
                if (taskId == null) notebook.assignRange(id, first.first, first.second, last.second, assignmentDue, corpus)
                else notebook.updateAssignment(taskId, first.first, first.second, last.second, assignmentDue, corpus)
                assignmentStudent = null; editingAssignment = null; assignmentStart = ""; assignmentEnd = ""
            } }, enabled = assignmentStart.isNotBlank() && assignmentEnd.isNotBlank()) { Text("احفظ النطاق") } },
            dismissButton = { TextButton(onClick = { assignmentStudent = null; editingAssignment = null }) { Text("إلغاء") } })
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

@Composable
private fun NotebookDeleteDialog(title: String, detail: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Text(detail, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDelete) { Text("احذف من الدفتر") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}
