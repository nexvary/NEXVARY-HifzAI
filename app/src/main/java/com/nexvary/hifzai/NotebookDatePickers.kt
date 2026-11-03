package com.nexvary.hifzai

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One platform modal at a time, owned and dismissed by its composition. */
@Composable
internal fun NotebookDatePicker(initial: LocalDate, selected: (LocalDate) -> Unit, dismissed: () -> Unit) {
    val context = LocalContext.current
    val selection by rememberUpdatedState(selected)
    val cancellation by rememberUpdatedState(dismissed)
    val theme = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
        android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert
    DisposableEffect(context, initial, theme) {
        var confirmed = false
        val dialog = DatePickerDialog(context, theme, { _, year, month, day ->
            confirmed = true
            selection(LocalDate.of(year, month + 1, day))
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth)
        dialog.setOnDismissListener { if (!confirmed) cancellation() }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    }
}

@Composable
internal fun NotebookDateTimePicker(initial: LocalDateTime, selected: (LocalDateTime) -> Unit, dismissed: () -> Unit) {
    var day by remember(initial) { mutableStateOf<LocalDate?>(null) }
    val chosenDay = day
    if (chosenDay == null) {
        NotebookDatePicker(initial.toLocalDate(), selected = { day = it }, dismissed = dismissed)
    } else {
        NotebookTimePicker(initial.toLocalTime(), selected = { selected(LocalDateTime.of(chosenDay, it)) }, dismissed = dismissed)
    }
}

@Composable
private fun NotebookTimePicker(initial: LocalTime, selected: (LocalTime) -> Unit, dismissed: () -> Unit) {
    val context = LocalContext.current
    val selection by rememberUpdatedState(selected)
    val cancellation by rememberUpdatedState(dismissed)
    val theme = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
        android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert
    DisposableEffect(context, initial, theme) {
        var confirmed = false
        val dialog = TimePickerDialog(context, theme, { _, hour, minute ->
            confirmed = true
            selection(LocalTime.of(hour, minute))
        }, initial.hour, initial.minute, true)
        dialog.setOnDismissListener { if (!confirmed) cancellation() }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    }
}
