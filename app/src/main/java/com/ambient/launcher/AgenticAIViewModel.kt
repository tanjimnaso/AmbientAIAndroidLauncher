package com.ambient.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ambient.launcher.BuildConfig
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

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

class AgenticAIViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = query, isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val responseText = runCatching { generateResponse(query) }
                .getOrElse { "Ambient AI could not reach Gemini right now. ${fallbackResponse(query)}" }

            val aiResponse = ChatMessage(text = responseText, isUser = false)
            _messages.value = _messages.value + aiResponse
            _isLoading.value = false
        }
    }

    private fun generateResponse(query: String): String {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return fallbackResponse(query)
        }

        val requestBody = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                "You are Ambient AI inside a calm Android launcher. Reply briefly, clearly, and usefully. Prefer digest-style help over chatty responses."
                            )
                        )
                    )
                }
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", query)
                            )
                        )
                    }
                )
            )
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return fallbackResponse(query)
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                return fallbackResponse(query)
            }

            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return fallbackResponse(query)
            val first = candidates.optJSONObject(0) ?: return fallbackResponse(query)
            val content = first.optJSONObject("content") ?: return fallbackResponse(query)
            val parts = content.optJSONArray("parts") ?: return fallbackResponse(query)
            val text = buildString {
                for (index in 0 until parts.length()) {
                    val partText = parts.optJSONObject(index)?.optString("text").orEmpty().trim()
                    if (partText.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(partText)
                    }
                }
            }

            return text.ifBlank { fallbackResponse(query) }
        }
    }

    private fun fallbackResponse(query: String): String {
        return "Noted: \"$query\". I can hold that as a launcher prompt, draft a next action, or turn it into a task once the live AI key is configured."
    }

    fun getPerspectivePrompt(perspectiveId: String): String {
        return when (perspectiveId) {
            "career" -> "Analyze from the perspective of a Data Engineer in Sydney, focusing on tech stack and market impact."
            "local" -> "Analyze for Sydney/NSW impact, focusing on cost of living and local vibe."
            "global" -> "Analyze from a global social impact perspective, focusing on inequality and wealth gaps."
            "contrarian" -> "Provide a critical, contrarian view that disagrees with the mainstream headline."
            else -> "Provide a balanced, professional analysis."
        }
    }

    fun sendPerspectiveQuery(headline: String, perspectiveId: String) {
        val lensPrompt = getPerspectivePrompt(perspectiveId)
        val finalQuery = """
            Headline: $headline
            Perspective Lens: $lensPrompt
            Analyze the impact on: 
            1. My role as a Data Engineer in Sydney.
            2. Local NSW/Australia conditions.
            3. Global social impact (Income/Wealth gaps).
        """.trimIndent()
        
        sendMessage(finalQuery)
    }
}
