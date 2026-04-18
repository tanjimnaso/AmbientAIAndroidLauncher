package com.ambient.launcher.tts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.ambient.launcher.MainActivity
import java.util.Locale

private const val TAG = "TtsPlaybackService"
private const val CHANNEL_ID = "ambient_tts"
private const val NOTIF_ID = 2001

/**
 * Foreground service hosting a single TextToSpeech engine + MediaSessionCompat.
 * Lockscreen transport controls surface via MediaStyle notification.
 *
 * One service, one active session at a time. Starting a new session stops the old.
 */
internal class TtsPlaybackService : android.app.Service() {

    inner class LocalBinder : Binder() { val service: TtsPlaybackService = this@TtsPlaybackService }
    private val binder = LocalBinder()

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var mediaSession: MediaSessionCompat

    private var chunks: List<String> = emptyList()
    private var currentIndex = 0
    private var sessionId: String = ""
    private var sessionTitle: String = ""
    private var isPaused = false

    var listener: ((TtsState) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSessionCompat(this, "AmbientTts").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onStop() = stopPlayback()
            })
            isActive = true
        }
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts.language = Locale.getDefault()
                tts.setSpeechRate(1.15f) // slightly faster than default; still intelligible
                tts.setPitch(1.0f)
                // Prefer a high-quality network/neural voice when available (Google TTS engine).
                tts.voices
                    ?.filter { it.locale.language == Locale.getDefault().language }
                    ?.filterNot { it.isNetworkConnectionRequired && !it.features.contains("networkTts") }
                    ?.maxByOrNull { it.quality }
                    ?.let { tts.voice = it }
                tts.setOnUtteranceProgressListener(progressListener)
            } else {
                Log.e(TAG, "TextToSpeech init failed: $status")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_NOT_STICKY
    }

    fun start(newSessionId: String, title: String, text: List<String>) {
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready yet — ignoring start")
            return
        }
        if (text.isEmpty()) return

        if (newSessionId == sessionId && !isPaused && chunks.isNotEmpty()) {
            pause(); return
        }

        tts.stop()
        chunks = text
        currentIndex = 0
        sessionId = newSessionId
        sessionTitle = title
        isPaused = false

        startForegroundIfNeeded()
        speakFromCurrent()
        emit()
    }

    fun pause() {
        if (!ttsReady) return
        tts.stop() // TTS has no true pause; stop and resume from chunk boundary
        isPaused = true
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        emit()
    }

    fun resume() {
        if (!ttsReady || chunks.isEmpty() || !isPaused) return
        isPaused = false
        speakFromCurrent()
        emit()
    }

    fun stopPlayback() {
        if (::tts.isInitialized) tts.stop()
        chunks = emptyList()
        currentIndex = 0
        sessionId = ""
        isPaused = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        emit()
    }

    private fun speakFromCurrent() {
        for (i in currentIndex until chunks.size) {
            val mode = if (i == currentIndex) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunks[i], mode, null, "tts_$i")
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        postNotification()
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) { /* no-op */ }
        override fun onDone(utteranceId: String?) {
            val idx = utteranceId?.removePrefix("tts_")?.toIntOrNull() ?: return
            currentIndex = idx + 1
            if (currentIndex >= chunks.size) {
                stopPlayback()
            } else {
                emit()
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { stopPlayback() }
    }

    private fun startForegroundIfNeeded() {
        startForeground(NOTIF_ID, buildNotification())
    }

    private fun postNotification() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val isPlaying = !isPaused && chunks.isNotEmpty()
        val playPause = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(sessionTitle.ifBlank { "Ambient Reader" })
            .setContentText(if (isPlaying) "Reading aloud" else "Paused")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(playPause)
            .addAction(stopAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .build()
    }

    private fun updatePlaybackState(state: Int) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private fun emit() {
        listener?.invoke(
            TtsState(
                sessionId = sessionId,
                isPlaying = !isPaused && chunks.isNotEmpty(),
                isPaused = isPaused && chunks.isNotEmpty(),
                progress = if (chunks.isEmpty()) 0f else currentIndex.toFloat() / chunks.size
            )
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Read Aloud",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        if (::mediaSession.isInitialized) { mediaSession.isActive = false; mediaSession.release() }
    }
}

internal data class TtsState(
    val sessionId: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f
)
