package com.ambient.launcher.tts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.ui.theme.AmbientTheme

@Composable
internal fun TtsIconButton(
    sessionId: String,
    title: String,
    textProvider: () -> String,
    modifier: Modifier = Modifier,
    tint: Color = AmbientTheme.palette.textPrimary
) {
    val context = LocalContext.current
    val state by TtsController.state.collectAsStateWithLifecycle()
    val isThisSession = state.sessionId == sessionId && (state.isPlaying || state.isPaused)

    val icon = when {
        isThisSession && state.isPlaying -> Icons.Rounded.Pause
        isThisSession && state.isPaused -> Icons.Rounded.PlayArrow
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }

    Icon(
        imageVector = icon,
        contentDescription = if (isThisSession && state.isPlaying) "Pause read aloud" else "Read aloud",
        tint = tint.copy(alpha = if (isThisSession) 0.95f else 0.6f),
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AmbientTheme.palette.textPrimary.copy(alpha = if (isThisSession) 0.08f else 0.04f))
            .clickable {
                val text = textProvider()
                if (text.isNotBlank()) TtsController.toggle(context, sessionId, title, text)
            }
            .padding(8.dp)
    )
}
