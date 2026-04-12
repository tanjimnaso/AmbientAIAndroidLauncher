package com.ambient.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "BriefingViewModel"

private fun String.stripMarkdown(): String = this
    .replace(Regex("\\*{1,3}(.+?)\\*{1,3}"), "$1")
    .replace(Regex("_{1,2}(.+?)_{1,2}"), "$1")
    .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
    .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("`(.+?)`"), "$1")
    .replace(Regex("\\[(.+?)]\\(.+?\\)"), "$1")
    .trim()

private const val PREFS_NAME = "briefing_cache"
private const val KEY_BRIEFING = "last_briefing"
private const val KEY_TIMESTAMP = "last_briefing_timestamp"
// Briefing slots: 7 AM and 7 PM. Cache is stale if generated before the last slot boundary.
private val BRIEFING_SLOT_HOURS = listOf(7, 19)

private fun lastSlotTime(): Long {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val lastSlot = BRIEFING_SLOT_HOURS
        .map { today.atTime(it, 0) }
        .filter { !it.isAfter(now) }
        .maxOrNull()
        ?: today.minusDays(1).atTime(BRIEFING_SLOT_HOURS.last(), 0) // yesterday 7 PM
    return lastSlot.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private const val KEY_ANALYSIS = "last_analysis"
private const val KEY_ANALYSIS_HASH = "last_analysis_hash"
private const val KEY_ANALYSIS_TIMESTAMP = "last_analysis_timestamp"
private const val ANALYSIS_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
// Pro for depth — Flash handles the ambient one-liner, Pro fires only on user tap
private const val ANALYSIS_MODEL = "gemini-2.5-pro"
private const val NOTIF_CHANNEL_ID = "ambient_analysis"
private const val NOTIF_ID = 1001

internal class BriefingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val appContext = application.applicationContext

    private val client = HttpClient.instance

    init {
        // Register notification channel once at startup (idempotent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Intelligence Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Analysis ready and error alerts" }
            appContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private val _briefing = MutableStateFlow<String?>(null)
    val briefing: StateFlow<String?> = _briefing.asStateFlow()

    // null = idle/not yet requested; non-null = ready (content or error message)
    private val _analysis = MutableStateFlow<String?>(null)
    val analysis: StateFlow<String?> = _analysis.asStateFlow()

    private val _isAnalysisLoading = MutableStateFlow(false)
    val isAnalysisLoading: StateFlow<Boolean> = _isAnalysisLoading.asStateFlow()

    init {
        val cached = prefs.getString(KEY_BRIEFING, null)
        if (!cached.isNullOrBlank()) {
            _briefing.value = cached
        }
    }

    private var lastHeadlinesHash: Int = 0

    fun generateBriefing(headlines: List<String>, customInstruction: String? = null, location: String = "Padstow, Sydney, NSW") {
        if (headlines.isEmpty()) return

        val headlinesHash = headlines.hashCode()
        val cachedAt = prefs.getLong(KEY_TIMESTAMP, 0L)
        val isExpired = cachedAt < lastSlotTime()

        // Persistent slot cache hit — covers cold starts within the same 7am/7pm window
        if (!isExpired && _briefing.value != null) return
        // In-session dedup — same headlines already triggered a call this session
        if (headlinesHash == lastHeadlinesHash && _briefing.value != null) return

        lastHeadlinesHash = headlinesHash

        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _briefing.value = generateLocalBriefing(headlines)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val systemInstruction = """
                ## Role & Objective
                Act as an Elite Intelligence & Market Analyst for Tanjim Islam. 
                Synthesize global news from the last 12-24 hours into a high-density briefing.

                ## User Context (Tanjim Islam)
                - Location: Padstow, Sydney, NSW (Multi-generational household).
                - Career: Transitioning from Motion Design (ABC/Seven) to Junior Data Engineering (GCP, dbt, Python, BigQuery).
                - Projects: Realtime AEMO Energy Dashboard, BetterCam (computational photography), Custom Android Launcher.
                - Interests: Energy market economics (AEMO), AI structural impact, local LLMs, edge computing, photography (aesthetic/philosophical).
                
                ## Processing & Analysis Rules
                1. Prioritize high-signal, low-hype reporting. Avoid celebrity gossip or surface-level AI hype.
                2. Identify structurally significant global events.
                3. For top developments, explain via systems and tradeoffs: 
                   - Tech/Professional: Impact on software, hardware, or cloud (GCP/BigQuery).
                   - Macro-Economic: Inflation, supply chains, energy grid transitions.
                   - Local Context: How this affects NSW/Australia.
                4. Output STRICTLY a single, punchy, ambient 'Signal' sentence.
                5. Tone: Analytical, grounded, non-sensational, intellectual—like a high-end magazine header.
                6. NO markdown, NO asterisks, NO bolding.
            """.trimIndent()

            val userPrompt = """
                [Launcher Variables]
                Current Date & Time: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
                Local Region: $location
                
                [Headlines]
                ${headlines.joinToString("\n")}
                
                Execute the Daily Intelligence Briefing. Output ONLY the single 'Signal' sentence.
            """.trimIndent()

            val finalPrompt = "$systemInstruction\n\n$userPrompt"

            Log.d(TAG, "Calling Gemini (${BuildConfig.GEMINI_MODEL}) with Tanjim's Context…")
            val result = runCatching { fetchBriefingResponse(finalPrompt) }

            result.onSuccess { summary ->
                val clean = summary?.stripMarkdown()
                if (!clean.isNullOrBlank()) {
                    _briefing.value = clean
                    prefs.edit()
                        .putString(KEY_BRIEFING, clean)
                        .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                } else {
                    _briefing.value = generateLocalBriefing(headlines)
                }
            }

            result.onFailure { e ->
                Log.e(TAG, "Gemini call failed: ${e.message}")
                if (_briefing.value == null) {
                    _briefing.value = generateLocalBriefing(headlines)
                }
            }
        }
    }

    /**
     * Synchronous cache check — used by the UI to decide whether to open
     * the analysis window immediately or start background generation.
     */
    fun isAnalysisCached(briefingText: String): Boolean {
        if (briefingText.isBlank()) return false
        val inputHash = briefingText.hashCode()
        val cachedHash = prefs.getInt(KEY_ANALYSIS_HASH, 0)
        val cachedAt = prefs.getLong(KEY_ANALYSIS_TIMESTAMP, 0L)
        val isExpired = (System.currentTimeMillis() - cachedAt) > ANALYSIS_CACHE_TTL_MS
        if (inputHash == cachedHash && !isExpired) {
            return !prefs.getString(KEY_ANALYSIS, null).isNullOrBlank()
        }
        return false
    }

    /**
     * Load cached analysis into state without triggering a network call.
     * Called when the user opens the analysis window and cache is known to be valid.
     */
    fun loadCachedAnalysis(briefingText: String) {
        val inputHash = briefingText.hashCode()
        if (prefs.getInt(KEY_ANALYSIS_HASH, 0) == inputHash) {
            val cached = prefs.getString(KEY_ANALYSIS, null)
            if (!cached.isNullOrBlank()) _analysis.value = cached
        }
    }

    /**
     * On-demand deep analysis using Gemini Pro.
     * Does NOT open the analysis window — the caller decides when to show it
     * based on isAnalysisCached() / isAnalysisLoading state.
     * 24-hour cache keyed by briefing text hash.
     */
    fun generateAnalysis(briefingText: String) {
        if (briefingText.isBlank() || BuildConfig.GEMINI_API_KEY.isBlank()) return
        if (_isAnalysisLoading.value) return // already in-flight

        val inputHash = briefingText.hashCode()
        val cachedHash = prefs.getInt(KEY_ANALYSIS_HASH, 0)
        val cachedAt = prefs.getLong(KEY_ANALYSIS_TIMESTAMP, 0L)
        val isExpired = (System.currentTimeMillis() - cachedAt) > ANALYSIS_CACHE_TTL_MS

        // Serve from cache if same briefing and not expired
        if (inputHash == cachedHash && !isExpired) {
            val cached = prefs.getString(KEY_ANALYSIS, null)
            if (!cached.isNullOrBlank()) {
                _analysis.value = cached
                return
            }
        }

        _isAnalysisLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                You are an elite intelligence analyst. Provide a structured deep analysis of the following news signal.

                Signal: "$briefingText"

                Structure your response as exactly 5 numbered sections:

                1. Systemic Explanation — How does this work structurally? What are the underlying forces and feedback loops?

                2. Historical Parallel — What is the closest historical precedent? What happened then, and what were the outcomes?

                3. Literature & Philosophy — Which thinkers, texts, or frameworks best explain this moment?

                4. Tech, Infrastructure & Economic Analysis — Multi-lens impact: software/hardware/cloud implications, supply chain, energy, capital flows.

                5. Probabilistic Forecast — What happens next? Give a 6-month outlook and a 5-year outlook with rough probabilities.

                Rules: No markdown. No asterisks. No bolding. Write in dense, analytical prose. Be specific, not general.
            """.trimIndent()

            val startMs = System.currentTimeMillis()
            Log.d(TAG, "Analysis request started via $ANALYSIS_MODEL")
            val result = runCatching { fetchAnalysisResponse(prompt) }
            val elapsedMs = System.currentTimeMillis() - startMs
            Log.d(TAG, "Analysis finished in ${elapsedMs}ms — success=${result.isSuccess}")

            result.onSuccess { text ->
                val clean = text?.stripMarkdown()
                if (!clean.isNullOrBlank()) {
                    _analysis.value = clean
                    prefs.edit()
                        .putString(KEY_ANALYSIS, clean)
                        .putInt(KEY_ANALYSIS_HASH, inputHash)
                        .putLong(KEY_ANALYSIS_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                    postSystemNotification("Analysis ready", "Tap to read the full intelligence brief.")
                } else {
                    Log.w(TAG, "Analysis: empty/null response body after ${elapsedMs}ms")
                    _analysis.value = "Analysis unavailable — empty response from Gemini."
                    postSystemNotification("Analysis failed", "Empty response from Gemini.")
                }
            }

            result.onFailure { e ->
                val isTimeout = e is java.net.SocketTimeoutException ||
                    e.message?.contains("timeout", ignoreCase = true) == true
                val msg = if (isTimeout) "Timed out after ${elapsedMs / 1000}s — try again." else "Analysis unavailable. ${e.message}"
                Log.e(TAG, "Analysis failed after ${elapsedMs}ms — ${if (isTimeout) "TIMEOUT" else e.javaClass.simpleName}: ${e.message}")
                _analysis.value = msg
                postSystemNotification(
                    if (isTimeout) "Analysis timed out" else "Analysis failed",
                    msg
                )
            }

            _isAnalysisLoading.value = false
        }
    }

    private fun postSystemNotification(title: String, body: String) {
        val ctx = appContext
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Skipping notification — POST_NOTIFICATIONS not granted")
                return
            }
        }
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notification)
    }

    private fun fetchAnalysisResponse(prompt: String): String? {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }.toString()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$ANALYSIS_MODEL:generateContent"

        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        // Pro model needs more time — override both call and read timeouts (inherited read=15s would fire first)
        client.newBuilder()
            .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            .newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().take(200)
                Log.e(TAG, "Analysis HTTP ${response.code}: $errorBody")
                return null
            }
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val parts = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: return null
            return parts.optJSONObject(0)?.optString("text")?.trim()
        }
    }

    private fun generateLocalBriefing(headlines: List<String>): String {
        return "Synchronizing global intelligence across ${headlines.size} sources. Standby for editorial synthesis."
    }

    private fun fetchBriefingResponse(prompt: String): String? {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }.toString()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent"

        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().take(200)
                Log.e(TAG, "Briefing HTTP ${response.code}: $errorBody")
                return null
            }
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val parts = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: return null
            return parts.optJSONObject(0)?.optString("text")?.trim()
        }
    }
}
