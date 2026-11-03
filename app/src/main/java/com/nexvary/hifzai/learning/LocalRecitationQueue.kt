package com.nexvary.hifzai.learning

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate

/** Foreground, explicit canonical selection only. No service, network or persistent voice storage. */
class LocalRecitationQueue(context: Context, private val lifecycle: Lifecycle) : AutoCloseable {
    enum class Phase { EMPTY, LOADING, READY, PLAYING, PAUSED, COMPLETED, FAILED }
    enum class Failure { INVALID_SELECTION, FILE_OR_PLAYBACK }
    data class Snapshot(val phase: Phase, val index: Int?, val size: Int, val completedItems: Int,
        val recordingId: String?, val address: RecordingAddress?,
        val playback: LocalAudioPlayer.Snapshot?, val failure: Failure? = null,
        val rangePass: Int = 0, val rangePasses: Int = 0)

    private val thread = HandlerThread("local-recitation-queue")
    private val worker: Handler
    private val player: LicensedRecitationPlayer
    @Volatile private var foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    @Volatile private var disposed = false
    @Volatile private var generation = 0L
    @Volatile private var state = Snapshot(Phase.EMPTY, null, 0, 0, null, null, null)
    // The fields below belong exclusively to the worker looper.
    private var items = emptyList<BoundRecitation>()
    private var index = -1
    private var traversal: RecitationRangeCursor? = null
    private var wanted = false
    private var repeats = 0
    private var speed = 1f
    private var activeGeneration = -1L
    private var lastInterruption = 0L
    private val tick = Runnable { inspectPlayback() }
    private val observer: LifecycleEventObserver

    init {
        requireMainThread()
        require(lifecycle.currentState != Lifecycle.State.DESTROYED)
        thread.start()
        worker = Handler(thread.looper)
        player = LicensedRecitationPlayer(context.applicationContext, worker)
        observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    val request = ++generation
                    worker.post { if (request == generation) clearWorker() }
                }
                Lifecycle.Event.ON_DESTROY -> close()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
    }

    /** Bind files on an IO worker first. Selection validation is synchronous; decode/hash is not. */
    fun load(catalog: RecitationCatalog, assets: List<BoundRecitation>, extraRepeats: Int = 0,
        playbackSpeed: Float = 1f, extraRangeRepeats: Int = 0) {
        requireMainThread(); check(!disposed)
        val request = ++generation
        val selected = try {
            require(foreground) { "Playback requires a visible lifecycle" }
            require(extraRepeats in 0..20)
            require(extraRangeRepeats in 0..20)
            require(playbackSpeed.isFinite() && playbackSpeed in 0.5f..1.5f)
            val copy = assets.toList()
            val records = catalog.sequence(copy.map { it.recording.rights.recordingId }, LocalDate.now().toEpochDay())
            require(copy.map { it.recording } == records) { "Bound recordings do not match the selected catalogue" }
            copy
        } catch (error: IllegalArgumentException) {
            worker.post { if (request == generation) {
                clearWorker(); state = state.copy(phase = Phase.FAILED, failure = Failure.INVALID_SELECTION)
            } }
            throw error
        }
        worker.post {
            if (request != generation) return@post
            clearWorker()
            if (disposed || !foreground) return@post
            activeGeneration = request
            lastInterruption = player.snapshot().interruptions
            items = selected; repeats = extraRepeats; speed = playbackSpeed
            traversal = RecitationRangeCursor(items.size, extraRangeRepeats)
            prepare(0)
        }
    }

    /** Asynchronous commands; immutable snapshots report actual worker/player state. */
    fun play() {
        requireMainThread(); check(!disposed)
        if (!foreground) return
        val request = generation
        worker.post {
            if (request != activeGeneration || !isCurrent() || state.phase !in setOf(Phase.READY, Phase.PAUSED)) return@post
            wanted = true
            lastInterruption = player.snapshot().interruptions
            if (player.snapshot().phase == LocalAudioPlayer.Phase.COMPLETED) advance()
            else if (player.play()) { publish(Phase.PLAYING); scheduleInspection() }
            else fail()
        }
    }
    fun pause() {
        requireMainThread(); check(!disposed)
        val request = generation
        worker.post {
            if (request == activeGeneration && isCurrent() && state.phase in setOf(Phase.READY, Phase.PLAYING, Phase.PAUSED)) {
                wanted = false; worker.removeCallbacks(tick); player.pause(); publish(Phase.PAUSED)
            }
        }
    }
    /** Change an already loaded selection without seeking, starting it or discarding pause state. */
    fun setPlaybackSpeed(value: Float) {
        requireMainThread(); check(!disposed)
        require(value.isFinite() && value in 0.5f..1.5f)
        val request = generation
        worker.post {
            if (request != activeGeneration || !isCurrent() ||
                state.phase !in setOf(Phase.READY, Phase.PLAYING, Phase.PAUSED)) return@post
            try {
                player.speed(value); speed = value; publish(state.phase)
            } catch (_: Exception) { fail() }
        }
    }
    fun clear() {
        requireMainThread(); check(!disposed)
        val request = ++generation
        worker.post { if (request == generation) clearWorker() }
    }
    fun snapshot(): Snapshot = state

    private fun prepare(next: Int) {
        index = next
        publish(Phase.LOADING)
        try {
            // Opening every item rechecks current rights, length and digest. No file hashing on UI.
            player.open(items[index], repeats)
            player.speed(speed)
            if (!isCurrent()) { clearWorker(); return }
            if (player.snapshot().interruptions != lastInterruption) wanted = false
            if (wanted) {
                if (!player.play()) { fail(); return }
                publish(Phase.PLAYING); scheduleInspection()
            } else if (player.snapshot().interruptions != lastInterruption) {
                player.pause(); publish(Phase.PAUSED)
            } else publish(Phase.READY)
        } catch (_: Exception) { fail() }
    }
    private fun inspectPlayback() {
        if (!isCurrent()) { clearWorker(); return }
        if (!wanted) return
        if (player.snapshot().interruptions != lastInterruption) {
            wanted = false; player.pause(); publish(Phase.PAUSED); return
        }
        when (player.snapshot().phase) {
            LocalAudioPlayer.Phase.COMPLETED -> advance()
            LocalAudioPlayer.Phase.EMPTY -> fail()
            LocalAudioPlayer.Phase.PAUSED -> { wanted = false; publish(Phase.PAUSED) }
            else -> { publish(Phase.PLAYING); scheduleInspection() }
        }
    }
    private fun advance() {
        val next = checkNotNull(traversal).completeItem()
        if (next == null) {
            wanted = false
            publish(Phase.COMPLETED)
            player.close()
        } else prepare(next)
    }
    private fun scheduleInspection() {
        worker.removeCallbacks(tick)
        worker.postDelayed(tick, 100)
    }
    private fun publish(phase: Phase, failure: Failure? = null) {
        val item = items.getOrNull(index)
        state = Snapshot(phase, index.takeIf { item != null }, items.size, traversal?.completedItems ?: 0,
            item?.recording?.rights?.recordingId, item?.recording?.address, player.snapshot(), failure,
            traversal?.pass ?: 0, traversal?.passes ?: 0)
    }
    private fun fail() {
        wanted = false; worker.removeCallbacks(tick); player.close()
        publish(Phase.FAILED, Failure.FILE_OR_PLAYBACK)
    }
    private fun clearWorker() {
        wanted = false; worker.removeCallbacks(tick); player.close()
        items = emptyList(); index = -1; traversal = null
        activeGeneration = -1L
        state = Snapshot(Phase.EMPTY, null, 0, 0, null, null, null)
    }
    /** Must be called on the main thread, like lifecycle observer registration/removal. */
    override fun close() {
        requireMainThread()
        if (disposed) return
        disposed = true; foreground = false; generation++
        lifecycle.removeObserver(observer)
        worker.post { clearWorker(); thread.quitSafely() }
    }
    private fun isCurrent(): Boolean = !disposed && foreground && activeGeneration == generation
    private fun requireMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) {
        "Queue commands must run on the main thread"
    }
}
