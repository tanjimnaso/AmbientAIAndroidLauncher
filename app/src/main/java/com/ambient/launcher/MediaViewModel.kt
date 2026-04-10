package com.ambient.launcher

import android.app.Application
import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val _mediaInfo = MutableStateFlow<MediaInfo?>(null)
    val mediaInfo: StateFlow<MediaInfo?> = _mediaInfo

    init {
        // Simple polling for now, or consider MediaSessionManager.OnActiveSessionsChangedListener
        viewModelScope.launch {
            // Note: This is a placeholder for a robust media observer.
            // Professional implementation would use MediaSessionManager.getActiveSessions()
        }
    }
}
