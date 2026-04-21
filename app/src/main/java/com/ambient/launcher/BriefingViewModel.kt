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
// Briefing refreshes once per day at 7 AM. Cache is stale if generated before today's 7 AM.
private val BRIEFING_SLOT_HOURS = listOf(7)

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
// Pro for depth and high-quality summarization
private const val ANALYSIS_MODEL = "gemini-1.5-pro-002"
private const val NOTIF_CHANNEL_ID = "ambient_analysis"
private const val NOTIF_ID = 1001

internal class BriefingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val appContext = application.applicationContext

    private val client = HttpClient.instance
    
    private val geminiClient = HttpClient.instance.newBuilder()
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var analysisJob: kotlinx.coroutines.Job? = null
    private var analysisCall: okhttp3.Call? = null

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

    private val _briefingHasError = MutableStateFlow(false)
    val briefingHasError: StateFlow<Boolean> = _briefingHasError.asStateFlow()

    // null = idle/not yet requested; non-null = ready
    private val _analysis = MutableStateFlow<String?>(null)
    val analysis: StateFlow<String?> = _analysis.asStateFlow()

    private val _isAnalysisLoading = MutableStateFlow(false)
    val isAnalysisLoading: StateFlow<Boolean> = _isAnalysisLoading.asStateFlow()

    private val _isBriefingLoading = MutableStateFlow(false)
    val isBriefingLoading: StateFlow<Boolean> = _isBriefingLoading.asStateFlow()

    private val _analysisHasError = MutableStateFlow(false)
    val analysisHasError: StateFlow<Boolean> = _analysisHasError.asStateFlow()

    init {
        val cached = prefs.getString(KEY_BRIEFING, null)
        if (!cached.isNullOrBlank()) {
            _briefing.value = cached
        }
    }

    private var lastHeadlinesHash: Int = 0

    fun generateBriefing(items: List<RssFeedItem>, location: String = "Padstow, Sydney, NSW", force: Boolean = false) {
        if (items.isEmpty()) return

        // Hash on identity fields only — descriptions can churn on re-fetch without the story changing.
        val itemsHash = items.map { "${it.source}::${it.title}::${it.url}" }.hashCode()
        val cachedAt = prefs.getLong(KEY_TIMESTAMP, 0L)
        val isExpired = cachedAt < lastSlotTime()

        if (!force) {
            // Persistent slot cache hit — covers cold starts within the same 7am/7pm window
            if (!isExpired && _briefing.value != null && !_briefingHasError.value) return
            // In-session dedup — same headlines already triggered a call this session
            if (itemsHash == lastHeadlinesHash && _briefing.value != null && !_briefingHasError.value) return
        }

        lastHeadlinesHash = itemsHash
        // Clear stale analysis whenever briefing input changes
        _analysis.value = null
        _analysisHasError.value = false

        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _briefing.value = null // show "No data" in UI
            return
        }

        _isBriefingLoading.value = true
        _briefingHasError.value = false

        // Split into weighted tiers. Top 5 get description bodies (primary analysis targets);
        // the next 15 are ambient context — titles only, for cross-reference.
        val primary = items.take(5)
        val ambient = items.drop(5).take(15)
        val primaryBlock = primary.joinToString("\n") { item ->
            val desc = item.description.take(200)
            if (desc.isNotBlank()) "- [${item.source}] ${item.title} — $desc"
            else "- [${item.source}] ${item.title}"
        }
        val ambientBlock = ambient.joinToString("\n") { "- [${it.source}] ${it.title}" }

        viewModelScope.launch(Dispatchers.IO) {
            val systemInstruction = """
                ## Role & Objective
                Act as an Elite Intelligence & Market Analyst for Tanjim Islam. 
                Synthesize global news from the last 12-24 hours into a single, high-density briefing sentence.

                ## User Context (Tanjim Islam)
                - Location: Sydney, NSW
                - Career: Transitioning from Motion Design to Data Engineering (GCP, dbt, Python, BigQuery).
                - Interests: Structural systems, civilizational shifts, philosophy, underreported geopolitical events.

                ## The Taxonomy of Signal (What to Include vs Ignore)
                IN-REGISTER (Include these):
                - Structural over spectacular (how systems, civilizations, minds work; e.g., 'quantum jamming', 'how the mind renders reality').
                - Durable over ephemeral (still interesting in a month).
                - Underreported signals (e.g., DRC or South Sudan news > 30th Ukraine update).
                - Cross-pollinating (physics result with civilizational weight, demographic drift like 'Gen Z looks to Nepal').
                - Culture and philosophy (e.g., 'love is never dead Hemingway', Marginalian essays).

                OUT-OF-REGISTER (Drop these entirely):
                - Attention-economy content (celebrity passings, gossip, influencers, 'alpha male' topics).
                - Ephemeral lifestyle (food like schnitzels, animal mating clickbait).
                - Generic sports (water polo, daily scores).

                ## Processing & Analysis Rules
                1. Two input tiers. PRIMARY items are your direct targets (read their descriptions). AMBIENT items are context.
                2. Convergence Rule: If multiple sources cover the exact same story (e.g., "NK launches missiles"), compress them mentally into a single confirmed signal, rather than letting it dominate the output. Treat convergence as confirmation, not a multiplier.
                3. Ensure at least one philosophical/cultural/structural thought piece makes it into the synthesis if present, balancing major hard news.
                4. Output STRICTLY a single, punchy, prudent/dry 'Signal' sentence that weaves together the most important hard news and structural/cultural signals.
                5. Tone: Analytical, grounded, non-sensational, intellectual.
                6. NO markdown, NO asterisks, NO bolding.
            """.trimIndent()

            val userPrompt = """
                [Launcher Variables]
                Current Date & Time: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
                Local Region: $location

                [PRIMARY — analyze these directly]
                $primaryBlock

                [AMBIENT — cross-reference only]
                $ambientBlock

                Execute the Daily Intelligence Briefing. Output ONLY the single 'Signal' sentence.
            """.trimIndent()

            val finalPrompt = "$systemInstruction\n\n$userPrompt"

            Log.d(TAG, "Calling Claude (sonnet-4-6) with Tanjim's Context…")
            val result = runCatching { fetchBriefingResponse(finalPrompt) }

            result.onSuccess { summary ->
                val clean = summary?.stripMarkdown()
                if (!clean.isNullOrBlank()) {
                    _briefing.value = clean
                    _briefingHasError.value = false
                    prefs.edit()
                        .putString(KEY_BRIEFING, clean)
                        .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                } else {
                    _briefing.value = null // empty response → "No data" in UI
                    _briefingHasError.value = true
                }
            }

            result.onFailure { e ->
                Log.e(TAG, "Gemini call failed: ${e.message}")
                _briefingHasError.value = true
                // Keep existing briefing if present; otherwise show "No data"
                if (_briefing.value.isNullOrBlank()) _briefing.value = null
            }

            _isBriefingLoading.value = false
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
        _analysisHasError.value = false

        analysisJob = viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                You are an elite intelligence analyst. Provide a structured deep analysis of the following news signal.

                Signal: "$briefingText"

                Important Register Rule: Focus on the structural, systemic, and geopolitical elements of the signal. If the signal contains philosophical or human-focused cultural pieces (e.g. essays on love, youth demographics in Nepal), ignore them in this deep analysis unless they map directly to civilizational/macro-economic infrastructure. Keep the deep analysis strictly on hard systems.

                Structure your response as exactly 5 numbered sections:

                1. Systemic Explanation — How does this work structurally? What are the underlying forces and feedback loops?

                2. Historical Parallel — What is the closest historical precedent? What happened then, and what were the outcomes?

                3. Literature & Philosophy — Which thinkers, texts, or frameworks best explain this macro-movement?

                4. Tech, Infrastructure & Economic Analysis — Multi-lens impact: software/hardware/cloud implications, supply chain, energy, capital flows.

                5. Probabilistic Forecast — What happens next? Give a 6-month outlook and a 5-year outlook with rough probabilities.

                Rules: No markdown. No asterisks. No bolding. Write in dense, analytical prose. Be specific, not general.
            """.trimIndent()

            val startMs = System.currentTimeMillis()
            Log.d(TAG, "Analysis request started via Claude Opus 4.6")
            val result = runCatching { fetchAnalysisResponse(prompt) }
            val elapsedMs = System.currentTimeMillis() - startMs
            Log.d(TAG, "Analysis finished in ${elapsedMs}ms — success=${result.isSuccess}")

            result.onSuccess { text ->
                val clean = text?.stripMarkdown()
                if (!clean.isNullOrBlank()) {
                    _analysis.value = clean
                    _analysisHasError.value = false
                    prefs.edit()
                        .putString(KEY_ANALYSIS, clean)
                        .putInt(KEY_ANALYSIS_HASH, inputHash)
                        .putLong(KEY_ANALYSIS_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                    postSystemNotification("Analysis ready", "Tap to read the full intelligence brief.")
                } else {
                    Log.w(TAG, "Analysis: empty/null response body after ${elapsedMs}ms")
                    _analysis.value = null
                    _analysisHasError.value = true
                    postSystemNotification("Analysis failed", "Empty response from Gemini.")
                }
            }

            result.onFailure { e ->
                if (e is java.util.concurrent.CancellationException || e is java.io.IOException && e.message == "Canceled") {
                    Log.d(TAG, "Analysis canceled by user")
                    _analysis.value = null
                    _analysisHasError.value = false
                    _isAnalysisLoading.value = false
                    return@launch
                }

                val isTimeout = e is java.net.SocketTimeoutException ||
                    e.message?.contains("timeout", ignoreCase = true) == true
                Log.e(TAG, "Analysis failed after ${elapsedMs}ms — ${if (isTimeout) "TIMEOUT" else e.javaClass.simpleName}: ${e.message}")
                _analysis.value = null
                _analysisHasError.value = true
                postSystemNotification(
                    if (isTimeout) "Analysis timed out" else "Analysis failed",
                    if (isTimeout) "Timed out after ${elapsedMs / 1000}s. Check Cloud billing." else "Check Google Cloud billing/quota."
                )
            }

            _isAnalysisLoading.value = false
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisCall?.cancel()
        _isAnalysisLoading.value = false
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

    private fun fetchBriefingResponse(prompt: String): String? {
        // Using Claude Sonnet 4.6 via Vertex AI for the briefing
        return fetchClaude(prompt, "claude-sonnet-4-6@default")
    }

    private fun fetchAnalysisResponse(prompt: String): String? {
        // Using Claude Opus 4.6 for deep analysis
        return fetchClaude(prompt, "claude-opus-4-6@default") { call ->
            analysisCall = call
        }
    }

    private fun fetchClaude(prompt: String, model: String, onCallCreated: ((okhttp3.Call) -> Unit)? = null): String? {
        // Project ID in Vertex must be the actual Project ID (usually lowercase with numbers, e.g., realtime-energy-data-123)
        // NOT the human readable Project Name. Let's make sure this matches exactly what's in GCP console.
        val vertexProjectId = "realtimeenergydata" // Lowercased, without spaces
        val vertexRegion = "us-east5" // Based on screenshot, us-east5 is an available region
        
        val url = "https://$vertexRegion-aiplatform.googleapis.com/v1/projects/$vertexProjectId/locations/$vertexRegion/publishers/anthropic/models/$model:rawPredict"
        val token = getVertexAccessToken() ?: return null

        val requestBody = JSONObject().apply {
            put("anthropic_version", "vertex-2023-10-16")
            // Expanded to accommodate the massive 128k output limit (though Vertex API caps may limit this per-request)
            put("max_tokens", 8192)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }.toString()

        val activeClient = if (model.contains("sonnet") || model.contains("opus")) geminiClient else client

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val call = activeClient.newCall(request)
        onCallCreated?.invoke(call)

        call.execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().take(200)
                Log.e(TAG, "Claude ($model) HTTP ${response.code}: $errorBody")
                return null
            }
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val contentArray = root.optJSONArray("content") ?: return null
            return contentArray.optJSONObject(0)?.optString("text")?.trim()
        }
    }

    private fun fetchGemini(prompt: String, isPro: Boolean, onCallCreated: ((okhttp3.Call) -> Unit)? = null): String? {
        val model = if (isPro) ANALYSIS_MODEL else BuildConfig.GEMINI_MODEL
        val activeClient = if (isPro) geminiClient else client

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }.toString()

        val isVertexAiEnabled = true // Enabled Vertex AI!
        val vertexProjectId = "RealTimeEnergyData"
        val vertexRegion = "us-central1"

        val request = if (isVertexAiEnabled) {
            val url = "https://$vertexRegion-aiplatform.googleapis.com/v1/projects/$vertexProjectId/locations/$vertexRegion/publishers/google/models/$model:generateContent"
            val token = getVertexAccessToken() ?: return null
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
        } else {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            Request.Builder()
                .url(url)
                .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
        }

        val call = activeClient.newCall(request)
        onCallCreated?.invoke(call)

        call.execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().take(200)
                Log.e(TAG, "Gemini ($model) HTTP ${response.code}: $errorBody")
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

    /** Force-clears slot cache so the next generateBriefing() call goes to the network. */
    fun clearBriefingCache() {
        lastHeadlinesHash = 0
        _briefing.value = null
        _analysis.value = null
        _analysisHasError.value = false
        prefs.edit()
            .remove(KEY_BRIEFING)
            .remove(KEY_TIMESTAMP)
            .apply()
    }

    /**
     * Returns the single cached analysis entry (text, epoch-millis timestamp), or null if none.
     * Reads SharedPreferences synchronously — safe to call from composition via remember{}.
     */
    fun getCachedAnalysisEntry(): Pair<String, Long>? {
        val text = prefs.getString(KEY_ANALYSIS, null)
        val ts = prefs.getLong(KEY_ANALYSIS_TIMESTAMP, 0L)
        return if (!text.isNullOrBlank() && ts > 0L) text to ts else null
    }

    private fun generateLocalBriefing(@Suppress("UNUSED_PARAMETER") headlines: List<String>): String = ""

    private fun getVertexAccessToken(): String? {
        return try {
            val stream = appContext.resources.openRawResource(R.raw.vertex_sa)
            val credentials = com.google.auth.oauth2.GoogleCredentials.fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Vertex Service Account from res/raw/vertex_sa.json: ${e.message}")
            null
        }
    }
}
