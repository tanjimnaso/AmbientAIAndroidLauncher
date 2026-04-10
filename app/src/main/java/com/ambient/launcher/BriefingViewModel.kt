package com.ambient.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BriefingViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _briefing = MutableStateFlow<String?>(null)
    val briefing: StateFlow<String?> = _briefing.asStateFlow()

    fun generateBriefing(headlines: List<String>, customInstruction: String? = null) {
        if (headlines.isEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val instruction = customInstruction ?: "Summarize these headlines into a single short, professional 'Vibe' or briefing sentence for a workstation home screen:"
            val prompt = "$instruction\n" + headlines.joinToString("\n")
            
            val summary = if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                runCatching { fetchBriefingResponse(prompt) }.getOrNull()
            } else {
                null
            }
            
            _briefing.value = summary
        }
    }

    private fun fetchBriefingResponse(prompt: String): String? {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val parts = root.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts") ?: return null
            
            return parts.optJSONObject(0)?.optString("text")?.trim()
        }
    }
}
