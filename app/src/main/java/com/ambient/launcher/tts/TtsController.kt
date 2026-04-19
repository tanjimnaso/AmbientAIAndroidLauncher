package com.ambient.launcher.tts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped facade for [TtsPlaybackService]. Survives screen changes;
 * a single active read-aloud session at a time.
 *
 * Call [ensureBound] once from MainActivity. UI layer uses [state] + [toggle].
 */
internal object TtsController {

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var service: TtsPlaybackService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? TtsPlaybackService.LocalBinder)?.service ?: return
            service = svc
            bound = true
            svc.listener = { _state.value = it }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service?.listener = null
            service = null
            bound = false
        }
    }

    fun ensureBound(context: Context) {
        if (bound) return
        val ctx = context.applicationContext
        val intent = Intent(ctx, TtsPlaybackService::class.java)
        // startForegroundService is called lazily (inside service.start()); here we only bind.
        ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Start/pause/resume the given session. Idempotent:
     * - same sessionId, currently playing → pause
     * - same sessionId, paused          → resume
     * - different sessionId             → stop previous, start new
     */
    fun toggle(context: Context, sessionId: String, title: String, chunks: List<TtsChunk>) {
        val svc = ensureStarted(context) ?: return
        val s = _state.value
        when {
            s.sessionId == sessionId && s.isPlaying -> svc.pause()
            s.sessionId == sessionId && s.isPaused  -> svc.resume()
            else -> svc.startStructured(sessionId, title, chunks)
        }
    }

    /** Legacy string entrypoint: treat as one paragraph, sentence-split internally. */
    fun toggle(context: Context, sessionId: String, title: String, text: String) {
        toggle(context, sessionId, title, TtsChunker.chunkParagraphs(listOf(text)))
    }

    /** Jump to an absolute chunk index and play from there. */
    fun seekTo(context: Context, index: Int) {
        val svc = ensureStarted(context) ?: return
        svc.seekToIndex(index)
    }

    /** Move by +1 / -1 sentence (chunk). */
    fun seekSentence(context: Context, delta: Int) {
        val svc = service ?: return
        svc.seekByChunk(delta)
    }

    /** Move to first chunk of the next / previous paragraph. */
    fun seekParagraph(context: Context, delta: Int) {
        val svc = service ?: return
        svc.seekByParagraph(delta)
    }

    fun stop() { service?.stopPlayback() }

    private fun ensureStarted(context: Context): TtsPlaybackService? {
        val ctx = context.applicationContext
        ContextCompat.startForegroundService(ctx, Intent(ctx, TtsPlaybackService::class.java))
        if (service == null) { ensureBound(ctx); return null }
        return service
    }
}

/**
 * Splits long-form text into speak-able chunks.
 *
 * WHY this matters (and why it's your call):
 * - Chunk boundaries determine where the voice pauses for breath. Too long = run-on; too short = choppy.
 * - [TtsPlaybackService] uses chunk index for progress and skip granularity. Finer chunks = smoother
 *   progress bar but more overhead per onDone callback.
 * - TextToSpeech has a per-utterance character ceiling (around 4000 on most engines). Chunks must stay under that.
 */
internal data class TtsChunk(val text: String, val paragraphIndex: Int)

internal object TtsChunker {
    private const val TARGET_MAX = 360
    private const val HARD_MAX = 3500

    private val SENTENCE_BOUNDARY = Regex("""(?<=[.!?])\s+(?=[A-Z0-9"'"])""")
    private val COMMA_BOUNDARY = Regex(""",\s+""")

    /**
     * Split paragraphs into sentence-level chunks, preserving paragraph identity.
     * Each resulting chunk is one "seek unit" — tapping a sentence jumps to its index,
     * double-tapping prev/next jumps to the first chunk of the prev/next paragraph.
     */
    fun chunkParagraphs(paragraphs: List<String>): List<TtsChunk> {
        val out = mutableListOf<TtsChunk>()
        for ((pIdx, para) in paragraphs.withIndex()) {
            val clean = para.replace(Regex("""\s+"""), " ").trim()
            if (clean.isEmpty()) continue
            for (s in splitSentences(clean)) {
                out += TtsChunk(s.take(HARD_MAX), pIdx)
            }
        }
        return out
    }

    /**
     * Public sentence splitter used by the article renderer to wrap text in seek-able spans.
     */
    fun splitSentences(text: String): List<String> {
        val out = mutableListOf<String>()
        for (rawSentence in text.split(SENTENCE_BOUNDARY)) {
            val s = rawSentence.trim()
            if (s.isEmpty()) continue
            if (s.length <= TARGET_MAX) { out += s; continue }
            // Run-on sentence: sub-split at commas, pack into <= TARGET_MAX groups.
            val parts = s.split(COMMA_BOUNDARY)
            val buf = StringBuilder()
            for ((i, part) in parts.withIndex()) {
                val piece = if (i < parts.lastIndex) "$part," else part
                if (buf.isNotEmpty() && buf.length + piece.length + 1 > TARGET_MAX) {
                    out += buf.toString().trim(); buf.clear()
                }
                if (buf.isNotEmpty()) buf.append(' ')
                buf.append(piece)
            }
            if (buf.isNotEmpty()) out += buf.toString().trim()
        }
        return out
    }

    /** Legacy single-paragraph chunker — kept for callers that don't care about paragraphs. */
    fun chunk(text: String): List<String> {
        val normalized = text
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s*\n\s*"""), " ")
            .trim()
        if (normalized.isEmpty()) return emptyList()

        val out = mutableListOf<String>()
        var buffer = StringBuilder()

        fun flush() {
            val s = buffer.toString().trim()
            if (s.isNotEmpty()) out += s.take(HARD_MAX)
            buffer = StringBuilder()
        }

        for (sentence in normalized.split(SENTENCE_BOUNDARY)) {
            val s = sentence.trim()
            if (s.isEmpty()) continue

            if (s.length > TARGET_MAX) {
                flush()
                val parts = s.split(COMMA_BOUNDARY)
                val sub = StringBuilder()
                for ((i, part) in parts.withIndex()) {
                    val piece = if (i < parts.lastIndex) "$part," else part
                    if (sub.length + piece.length + 1 > TARGET_MAX && sub.isNotEmpty()) {
                        out += sub.toString().trim().take(HARD_MAX)
                        sub.clear()
                    }
                    if (sub.isNotEmpty()) sub.append(' ')
                    sub.append(piece)
                }
                if (sub.isNotEmpty()) out += sub.toString().trim().take(HARD_MAX)
                continue
            }

            if (buffer.length + s.length + 1 > TARGET_MAX) flush()
            if (buffer.isNotEmpty()) buffer.append(' ')
            buffer.append(s)
        }
        flush()
        return out
    }
}
