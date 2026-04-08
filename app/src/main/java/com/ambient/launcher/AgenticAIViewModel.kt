package com.ambient.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

class AgenticAIViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = query, isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            // Simulate network delay for Fireworks API
            delay(1500)
            
            val aiResponse = ChatMessage(
                text = "Ambient AI response to: \"$query\". Integration with Fireworks API will be here. I am tracking your app usage for contextual awareness.",
                isUser = false
            )
            _messages.value = _messages.value + aiResponse
            _isLoading.value = false
        }
    }
}
