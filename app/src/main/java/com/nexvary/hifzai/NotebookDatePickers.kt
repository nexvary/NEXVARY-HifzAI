package com.nexvary.hifzai

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowInsets
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
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
        val release = presentAfterOwnerFocus(context, dialog)
        onDispose { release() }
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
        val release = presentAfterOwnerFocus(context, dialog)
        onDispose { release() }
    }
}

/**
 * Removing a Compose editor and showing its platform picker in the same apply
 * pass can let the old window's teardown take focus back from the new picker.
 * Wait for the actual Activity window-focus event, not a delay or a test retry.
 * The date -> time transition follows the same ownership handshake.
 */
private fun presentAfterOwnerFocus(context: Context, dialog: Dialog): () -> Unit {
    var current = context
    while (current is ContextWrapper && current !is Activity) current = current.baseContext
    val activity = requireNotNull(current as? Activity) { "Notebook pickers need an Activity owner" }
    val decor = activity.window.decorView
    val observer = decor.viewTreeObserver
    var active = true
    var presented = false
    fun present(focused: Boolean) {
        val keyboardVisible = Build.VERSION.SDK_INT >= 30 &&
            decor.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        if (active && focused && !keyboardVisible && !presented && !activity.isFinishing && !activity.isDestroyed) {
            presented = true
            dialog.show()
        }
    }
    val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
        // A hide sent during modal teardown can target the old input window.
        // Repeat the request from the Activity once it owns focus again.
        if (focused && !presented && Build.VERSION.SDK_INT >= 30) {
            decor.windowInsetsController?.hide(WindowInsets.Type.ime())
        }
        present(focused)
    }
    // On API30+ an IME hide can finish after focus is already true. Read the
    // applied insets at draw time instead of racing its asynchronous teardown.
    val beforeDraw = ViewTreeObserver.OnPreDrawListener { present(decor.hasWindowFocus()); true }
    observer.addOnWindowFocusChangeListener(listener)
    observer.addOnPreDrawListener(beforeDraw)
    // A date/time picker replaces text input. Do not leave an IME window
    // competing for input while waiting for the old modal to detach.
    val keyboard = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    keyboard.hideSoftInputFromWindow(activity.currentFocus?.windowToken ?: decor.windowToken, 0)
    present(decor.hasWindowFocus())
    return {
        active = false
        if (observer.isAlive) {
            observer.removeOnWindowFocusChangeListener(listener)
            observer.removeOnPreDrawListener(beforeDraw)
        }
        dialog.setOnDismissListener(null)
        dialog.dismiss()
    }
}
