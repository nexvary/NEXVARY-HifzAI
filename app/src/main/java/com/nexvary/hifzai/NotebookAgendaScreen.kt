package com.nexvary.hifzai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexvary.hifzai.learning.*
import java.time.LocalDate

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun NotebookAgendaScreen(students: List<NotebookStudent>, lessons: List<NotebookLesson>,
    assignments: List<NotebookAssignment>, corpus: QuranCorpus, today: LocalDate,
    editLesson: (NotebookLesson) -> Unit, completeLesson: (NotebookLesson) -> Unit,
    problem: (NotebookLesson) -> Unit, deleteLesson: (NotebookLesson) -> Unit,
    editAssignment: (NotebookAssignment) -> Unit, completeAssignment: (NotebookAssignment) -> Unit,
    deleteAssignment: (NotebookAssignment) -> Unit) {
    var filter by rememberSaveable { mutableStateOf(AgendaFilter.TODAY) }
    var day by remember(today) { mutableStateOf(today) }
    var selectedStudent by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var revealResults by remember { mutableIntStateOf(0) }
    val resultAnchor = remember { BringIntoViewRequester() }
    val studentId = selectedStudent?.takeIf { id -> students.any { it.id == id } }
    val snapshot = remember(students, lessons, assignments, corpus) {
        NotebookAgenda.snapshot(students, lessons, assignments, corpus)
    }
    var page by remember(snapshot, studentId, filter, day) { mutableIntStateOf(0) }
    val matching = remember(snapshot, studentId, filter, day) { NotebookAgenda.query(snapshot, day, filter, studentId) }
    val visible = NotebookAgenda.page(matching, page)
    LaunchedEffect(revealResults, snapshot, day) {
        if (revealResults > 0) resultAnchor.bringIntoView()
    }
    Text("جدول الدفتر المحلي", style = MaterialTheme.typography.titleLarge)
    Text("تاريخ الجدول: $day. مواعيد اليوم تبقى فيه حتى نهايته، والمهام بلا وقت محدد.")
    OutlinedButton(onClick = { day = LocalDate.now(); revealResults++ }, modifier = Modifier.fillMaxWidth()) { Text("تحديث تاريخ الجدول") }
    Box {
        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
            Text("عرض: " + (students.firstOrNull { it.id == studentId }?.alias ?: "كل الطلاب"))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 280.dp)) {
            DropdownMenuItem(text = { Text("كل الطلاب") }, onClick = { selectedStudent = null; menu = false; revealResults++ })
            students.forEach { student ->
                DropdownMenuItem(text = { Text(student.alias) }, onClick = { selectedStudent = student.id; menu = false; revealResults++ })
            }
        }
    }
    AgendaFilter.entries.forEach { choice ->
        val count = NotebookAgenda.query(snapshot, day, choice, studentId).size
        OutlinedButton(onClick = { filter = choice; revealResults++ }, modifier = Modifier.fillMaxWidth()) {
            Text(choice.arabic + " • " + count + if (filter == choice) " • محدد" else "")
        }
    }
    if (snapshot.unreadable > 0) Text("تعذر عرض ${snapshot.unreadable} سجل في الجدول لبيانات أو مراجع غير صالحة؛ لم تُحذف هذه السجلات.")
    Text("النتائج: ${visible.total} • حتى 5 سجلات في الصفحة", Modifier.bringIntoViewRequester(resultAnchor))
    if (visible.total == 0) Text("لا توجد سجلات ضمن هذا الاختيار")
    visible.entries.forEach { entry ->
        val alias = students.first { it.id == entry.studentId }.alias
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (entry) {
                    is AgendaEntry.Lesson -> {
                        val lesson = entry.record
                        Text(alias + " • جلسة محلية")
                        Text(lesson.teacherLabel + " • " + NotebookRules.displayDateTime(lesson.dateTime))
                        Text(lesson.note)
                        OutlinedButton(onClick = { editLesson(lesson) }) { Text("تعديل الموعد والملاحظات") }
                        OutlinedButton(onClick = { completeLesson(lesson) }) {
                            Text(if (lesson.completed) "مكتملة • أعد فتحها" else "سجّل اكتمال الجلسة")
                        }
                        OutlinedButton(onClick = { problem(lesson) }) { Text("دوّن مشكلة محليًا") }
                        OutlinedButton(onClick = { deleteLesson(lesson) }) { Text("حذف هذه الجلسة") }
                    }
                    is AgendaEntry.Assignment -> {
                        val task = entry.record
                        Text(alias + " • مهمة مراجعة")
                        Text("مراجعة " + corpus.info(task.chapter).arabicName + " • " + task.firstAyah + "–" + task.lastAyah +
                            " • " + task.dueDate)
                        OutlinedButton(onClick = { completeAssignment(task) }) {
                            Text(if (task.completed) "مكتملة • أعد فتح المهمة" else "سجّل اكتمال المراجعة")
                        }
                        OutlinedButton(onClick = { editAssignment(task) }) { Text("تعديل نطاق المراجعة وموعده") }
                        OutlinedButton(onClick = { deleteAssignment(task) }) { Text("حذف مهمة المراجعة") }
                    }
                }
            }
        }
    }
    if (visible.total > 0) {
        Text("صفحة ${visible.index + 1} من ${visible.pageCount}")
        OutlinedButton(onClick = { page--; revealResults++ }, enabled = visible.hasPrevious, modifier = Modifier.fillMaxWidth()) { Text("الصفحة السابقة في الجدول") }
        OutlinedButton(onClick = { page++; revealResults++ }, enabled = visible.hasNext, modifier = Modifier.fillMaxWidth()) { Text("الصفحة التالية في الجدول") }
    }
    Text("الاكتمال تسجيل تنظيمي منك؛ لا يعد تقييمًا موثقًا للحفظ ولا يغيّر تقدمك الشخصي.")
}
