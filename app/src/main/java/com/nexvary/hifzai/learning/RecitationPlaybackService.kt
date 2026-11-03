package com.nexvary.hifzai.learning

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nexvary.hifzai.MainActivity
import com.nexvary.hifzai.R

/**
 * Non-exported, local reviewed-catalogue playback only. No recorder, URLs, file
 * paths or serialized rights are accepted from intents. Nothing restarts on boot
 * or process death. The user must choose a range and explicitly press Play.
 */
class RecitationPlaybackService : Service(), LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var queue: LocalRecitationQueue
    private lateinit var session: MediaSession
    private lateinit var notifications: NotificationManager
    private var foregroundStarted = false
    private var loaded = false
    private var closed = false
    private var lastPublished: LocalRecitationQueue.Snapshot? = null
    private var inactiveSince = 0L
    private val binder = PlaybackBinder()
    private val refresh = object : Runnable {
        override fun run() {
            if (closed || !foregroundStarted) return
            val state = queue.snapshot()
            if (loaded && state.phase in setOf(LocalRecitationQueue.Phase.COMPLETED,
                    LocalRecitationQueue.Phase.FAILED, LocalRecitationQueue.Phase.EMPTY)) {
                stopPlayback(); return
            }
            if (state != lastPublished) { publish(state); lastPublished = state }
            val now = android.os.SystemClock.elapsedRealtime()
            if (state.phase == LocalRecitationQueue.Phase.PLAYING) inactiveSince = now
            // Do not keep an empty/paused foreground service alive indefinitely.
            if (now - inactiveSince >= IDLE_TIMEOUT_MS) { stopPlayback(); return }
            handler.postDelayed(this, 200)
        }
    }

    inner class PlaybackBinder : Binder() {
        /** Must be invoked on main. All byte hashing/decoding remains on the queue worker. */
        fun load(catalog: RecitationCatalog, assets: List<BoundRecitation>, extraRepeats: Int = 0,
            speed: Float = 1f, extraRangeRepeats: Int = 0) {
            requireMain(); check(foregroundStarted && !closed)
            // Prevent the periodic observer treating the asynchronous load as an
            // already completed empty selection. publish only after load leaves EMPTY.
            loaded = false
            queue.load(catalog, assets, extraRepeats, speed, extraRangeRepeats)
            inactiveSince = android.os.SystemClock.elapsedRealtime()
        }
        fun play() { requireMain(); if (!closed && foregroundStarted) queue.play() }
        fun pause() { requireMain(); if (!closed && foregroundStarted) queue.pause() }
        fun speed(value: Float) { requireMain(); check(!closed); queue.setPlaybackSpeed(value) }
        fun stop() { requireMain(); stopPlayback() }
        fun snapshot(): LocalRecitationQueue.Snapshot = queue.snapshot()
        fun sessionToken(): MediaSession.Token = session.sessionToken
    }

    override fun onCreate() {
        super.onCreate()
        current = this
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        queue = LocalRecitationQueue(this, lifecycle, keepAwake = true)
        notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL,
            "التلاوات المحلية", NotificationManager.IMPORTANCE_LOW).apply {
            description = "التحكم في تشغيل التلاوات المحلية المأذونة"; setSound(null, null)
        })
        session = MediaSession(this, "local-reviewed-recitation")
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { binder.play() }
            override fun onPause() { binder.pause() }
            override fun onStop() { stopPlayback() }
        }, handler)
        session.isActive = true
        inactiveSince = android.os.SystemClock.elapsedRealtime()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (closed) { stopSelf(); return START_NOT_STICKY }
        when (intent?.action) {
            ACTION_START -> {
                val notification = notification(queue.snapshot())
                if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                else startForeground(NOTIFICATION_ID, notification)
                foregroundStarted = true
                handler.removeCallbacks(refresh); handler.post(refresh)
            }
            ACTION_PLAY -> binder.play()
            ACTION_PAUSE -> binder.pause()
            ACTION_STOP -> stopPlayback()
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    private fun publish(state: LocalRecitationQueue.Snapshot) {
        if (state.phase !in setOf(LocalRecitationQueue.Phase.EMPTY, LocalRecitationQueue.Phase.LOADING)) loaded = true
        val playing = state.phase == LocalRecitationQueue.Phase.PLAYING
        val mediaState = when (state.phase) {
            LocalRecitationQueue.Phase.PLAYING -> PlaybackState.STATE_PLAYING
            LocalRecitationQueue.Phase.READY, LocalRecitationQueue.Phase.PAUSED -> PlaybackState.STATE_PAUSED
            LocalRecitationQueue.Phase.LOADING -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_NONE
        }
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP)
            .setState(mediaState, state.playback?.positionMs?.toLong() ?: 0L,
                if (playing) state.playback?.speed ?: 1f else 0f).build())
        session.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title(state))
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.playback?.durationMs?.toLong() ?: 0L).build())
        notifications.notify(NOTIFICATION_ID, notification(state))
    }

    private fun title(state: LocalRecitationQueue.Snapshot): String = state.address?.let {
        "تلاوة محلية • سورة ${it.chapter} آية ${it.ayah}"
    } ?: "التلاوات المحلية"

    private fun command(action: String, id: Int): PendingIntent = PendingIntent.getService(this, id,
        Intent(this, RecitationPlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun notification(state: LocalRecitationQueue.Snapshot): Notification {
        val playing = state.phase == LocalRecitationQueue.Phase.PLAYING
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_hifz_logo)
            .setContentTitle(title(state)).setContentText(if (playing) "قيد التشغيل" else "متوقف مؤقتًا • اضغط تشغيل للاستماع")
            .setContentIntent(open).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT).setOngoing(playing)
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this,
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                if (playing) "إيقاف مؤقت" else "تشغيل", command(if (playing) ACTION_PAUSE else ACTION_PLAY, 1)).build())
            .addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,
                android.R.drawable.ic_menu_close_clear_cancel), "إيقاف", command(ACTION_STOP, 2)).build())
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1))
            .build()
    }

    private fun stopPlayback() {
        if (closed) return
        closed = true
        foregroundStarted = false
        handler.removeCallbacks(refresh)
        queue.close()
        session.isActive = false
        session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0L, 0f).build())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onTaskRemoved(rootIntent: Intent?) { stopPlayback(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() {
        stopPlayback()
        if (current === this) current = null
        session.release()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
    private fun requireMain() = check(Looper.myLooper() == Looper.getMainLooper())

    companion object {
        private var current: RecitationPlaybackService? = null
        private const val CHANNEL = "local-recitation"
        const val NOTIFICATION_ID = 6201
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L
        private const val ACTION_START = "com.nexvary.hifzai.recitation.START"
        private const val ACTION_PLAY = "com.nexvary.hifzai.recitation.PLAY"
        private const val ACTION_PAUSE = "com.nexvary.hifzai.recitation.PAUSE"
        private const val ACTION_STOP = "com.nexvary.hifzai.recitation.STOP"

        /** Call only from an explicit user action in a visible Activity. */
        fun start(activity: ComponentActivity) {
            check(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            activity.startForegroundService(Intent(activity, RecitationPlaybackService::class.java).setAction(ACTION_START))
        }

        /** Stops an existing instance even if an Activity still holds its local binder. */
        fun stopForDataReset() {
            check(Looper.myLooper() == Looper.getMainLooper())
            current?.stopPlayback()
        }
    }
}
