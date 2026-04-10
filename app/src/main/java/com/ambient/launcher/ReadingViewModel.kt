package com.ambient.launcher

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReadingState(
    val title: String,
    val author: String = "",
    val progress: Float = 0f,
    val sourceApp: String = ""
)

class ReadingViewModel : ViewModel() {
    private val _readingState = MutableStateFlow<ReadingState?>(null)
    val readingState: StateFlow<ReadingState?> = _readingState

    fun updateReadingState(state: ReadingState?) {
        _readingState.value = state
    }
}
