package com.nexvary.hifzai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvary.hifzai.learning.LocalRecitationLibrary
import com.nexvary.hifzai.learning.LocalRecitationQueue
import com.nexvary.hifzai.learning.RecitationPlaybackService
import com.nexvary.hifzai.learning.RecordingAddress
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** All control calls are on main. The service owns media playback. */
internal class RecitationLibraryViewModel : ViewModel() {
    data class Options(val verseRepeats: Int, val rangeRepeats: Int, val speed: Float)
    data class State(
        val preparing: Boolean = false,
        val message: String = "",
        val playback: LocalRecitationQueue.Snapshot? = null
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var binding: Binding? = null
    private var worker: Job? = null

    private inner class Binding(val context: Context) : ServiceConnection {
        val connected = CompletableDeferred<RecitationPlaybackService.PlaybackBinder>()
        var player: RecitationPlaybackService.PlaybackBinder? = null
        var registered = false
        val intent get() = Intent(context, RecitationPlaybackService::class.java)

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (binding !== this) return
            val binder = service as? RecitationPlaybackService.PlaybackBinder
            if (binder == null) { fail("تعذر الاتصال بخدمة التلاوات"); return }
            player = binder
            connected.complete(binder)
        }

        override fun onServiceDisconnected(name: ComponentName) = disconnected()
        override fun onBindingDied(name: ComponentName) = disconnected()
        override fun onNullBinding(name: ComponentName) = disconnected()

        private fun disconnected() {
            if (binding === this) fail("انقطع الاتصال بخدمة التلاوات؛ جهّز النطاق من جديد")
        }
    }

    fun prepare(
        activity: ComponentActivity, library: LocalRecitationLibrary,
        first: RecordingAddress, last: RecordingAddress, reader: String, options: Options
    ) {
        clear()
        val connection = Binding(activity.applicationContext)
        binding = connection
        mutableState.value = State(preparing = true, message = "جارٍ التحقق من ملفات النطاق…")
        try {
            RecitationPlaybackService.start(activity)
            connection.registered = connection.context.bindService(connection.intent, connection, Context.BIND_AUTO_CREATE)
            if (!connection.registered) { fail("تعذر الاتصال بخدمة التلاوات"); return }
        } catch (_: IllegalStateException) {
            fail("تعذر بدء خدمة التلاوات؛ حاول من شاشة التطبيق")
            return
        } catch (_: SecurityException) {
            fail("تعذر الوصول إلى خدمة التلاوات")
            return
        }

        worker = viewModelScope.launch {
            try {
                val player = awaitService(connection) ?: return@launch
                val assets = withContext(Dispatchers.IO) {
                    library.bindRange(first, last, reader, LocalDate.now().toEpochDay())
                }
                player.load(library.catalog, assets, options.verseRepeats, options.speed, options.rangeRepeats)
                observe(player)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                fail(error.message ?: "تعذر التحقق من نطاق التلاوة")
            } catch (_: IOException) {
                fail("تعذر قراءة ملفات التلاوة أو تجهيزها")
            } catch (_: SecurityException) {
                fail("تعذر الوصول إلى ملفات التلاوة")
            } catch (_: IllegalStateException) {
                fail("تعذر تجهيز التلاوة في المساحة المحلية أو توقفت الخدمة")
            }
        }
    }

    /** Reattach controls without starting a service or resuming audio. */
    fun reconnect(context: Context) {
        if (binding != null || !RecitationPlaybackService.hasActivePlayback()) return
        val connection = Binding(context.applicationContext)
        binding = connection
        connection.registered = connection.context.bindService(connection.intent, connection, 0)
        if (!connection.registered) { binding = null; return }
        worker = viewModelScope.launch {
            awaitService(connection)?.let { observe(it) }
        }
    }

    private suspend fun awaitService(connection: Binding): RecitationPlaybackService.PlaybackBinder? {
        val player = withTimeoutOrNull(5000) {
            connection.connected.await().also { while (!it.isActive()) delay(50) }
        }
        if (player == null) fail("تعذر تجهيز خدمة التلاوات")
        return player
    }

    private suspend fun observe(player: RecitationPlaybackService.PlaybackBinder) {
        while (player.isActive()) {
            val snapshot = player.snapshot()
            val preparing = snapshot.phase == LocalRecitationQueue.Phase.EMPTY ||
                snapshot.phase == LocalRecitationQueue.Phase.LOADING
            mutableState.value = State(preparing, playback = snapshot)
            delay(200)
        }
        val outcome = player.lastOutcome()
        release(stop = false)
        mutableState.value = State(message = when (outcome?.phase) {
            LocalRecitationQueue.Phase.COMPLETED -> "اكتمل تشغيل النطاق"
            LocalRecitationQueue.Phase.FAILED -> "تعذر تشغيل ملف التلاوة؛ أعد تجهيز النطاق"
            else -> "توقف تشغيل التلاوة"
        }, playback = outcome)
    }

    fun play() { binding?.player?.play() }
    fun pause() { binding?.player?.pause() }
    fun speed(value: Float) {
        binding?.player?.takeIf { it.isActive() }?.speed(value)
    }

    fun clear() {
        release(stop = true)
        mutableState.value = State()
    }

    private fun fail(message: String) {
        release(stop = true)
        mutableState.value = State(message = message)
    }

    private fun release(stop: Boolean) {
        worker?.cancel()
        worker = null
        val connection = binding ?: return
        binding = null
        if (stop) {
            connection.player?.stop()
            connection.context.stopService(connection.intent)
        }
        if (connection.registered) connection.context.unbindService(connection)
    }

    override fun onCleared() {
        val phase = binding?.player?.snapshot()?.phase
        release(stop = phase != LocalRecitationQueue.Phase.PLAYING && phase != LocalRecitationQueue.Phase.PAUSED)
        super.onCleared()
    }
}
