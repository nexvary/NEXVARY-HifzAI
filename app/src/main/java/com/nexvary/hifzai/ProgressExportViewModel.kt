package com.nexvary.hifzai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvary.hifzai.learning.ProgressCsv
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Retains an explicitly requested export across Activity recreation. */
internal class ProgressExportViewModel : ViewModel() {
    enum class Status(val message: String) {
        IDLE(""),
        PICKING("اختر مكان حفظ ملف التقدم"),
        WRITING("جارٍ حفظ ملف التقدم…"),
        SAVED("حُفظ ملف التقدم"),
        CANCELLED("أُلغي حفظ ملف التقدم"),
        ACCESS_DENIED("تعذر الوصول إلى وجهة حفظ ملف التقدم"),
        WRITE_FAILED("تعذر حفظ ملف التقدم؛ قد يكون الملف ناقصًا"),
        PICKER_UNAVAILABLE("لا يتوفر تطبيق لاختيار مكان حفظ ملف التقدم");

        val busy: Boolean get() = this == PICKING || this == WRITING
    }

    private val mutableStatus = MutableStateFlow(Status.IDLE)
    val status = mutableStatus.asStateFlow()
    private var job: Job? = null
    private var generation = 0L

    fun beginPicking(): Boolean {
        if (status.value.busy) return false
        mutableStatus.value = Status.PICKING
        return true
    }

    fun cancelPicking() { mutableStatus.value = Status.CANCELLED }
    fun pickerUnavailable() { mutableStatus.value = Status.PICKER_UNAVAILABLE }

    fun write(payload: () -> String, open: () -> OutputStream?) {
        if (status.value == Status.WRITING) return
        val request = ++generation
        mutableStatus.value = Status.WRITING
        job = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ProgressCsv.write(payload(), open) }
            if (request == generation) {
                mutableStatus.value = when (result) {
                    ProgressCsv.WriteResult.SAVED -> Status.SAVED
                    ProgressCsv.WriteResult.ACCESS_DENIED -> Status.ACCESS_DENIED
                    ProgressCsv.WriteResult.WRITE_FAILED -> Status.WRITE_FAILED
                }
            }
        }
    }

    fun clearForReset() {
        generation++
        job?.cancel()
        job = null
        mutableStatus.value = Status.IDLE
    }
}
