package com.ambient.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RssFeedItem(
    val title: String,
    val source: String,
    val timestamp: String
)

class DashboardViewModel : ViewModel() {

    private val _feedItems = MutableStateFlow<List<RssFeedItem>>(emptyList())
    val feedItems: StateFlow<List<RssFeedItem>> = _feedItems.asStateFlow()

    init {
        fetchRssFeeds()
    }

    private fun fetchRssFeeds() {
        viewModelScope.launch {
            // Simulate RSS parsing
            delay(1000)
            _feedItems.value = listOf(
                RssFeedItem("Chrome PWA update released", "TechCrunch", "10m ago"),
                RssFeedItem("Jetpack Compose performance tips", "Android Dev", "1h ago"),
                RssFeedItem("Local weather: 72°F, Clear skies", "Weather", "2h ago")
            )
        }
    }
}
