package com.ambient.launcher.tts

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.ui.theme.AmbientTheme

/**
 * Bottom media strip for TTS control.
 *
 * Transport:
 * - Prev:  tap = previous sentence,  double-tap = previous paragraph
 * - Play/Pause: tap toggles this session
 * - Next:  tap = next sentence,      double-tap = next paragraph
 */
@Composable
internal fun TtsMediaStrip(
    sessionId: String,
    title: String,
    chunks: List<TtsChunk>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by TtsController.state.collectAsStateWithLifecycle()
    val isActiveSession = state.sessionId == sessionId
    val isPlaying = isActiveSession && state.isPlaying

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            icon = Icons.Rounded.SkipPrevious,
            enabled = isActiveSession,
            onClick = { TtsController.seekSentence(context, -1) },
            onDoubleClick = { TtsController.seekParagraph(context, -1) }
        )

        Spacer(Modifier.size(20.dp))

        // Center play/pause — bigger, pill-shaped
        val playIcon: ImageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
        val interaction = remember { MutableInteractionSource() }
        Icon(
            imageVector = playIcon,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = AmbientTheme.palette.textPrimary,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.08f))
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        TtsController.toggle(context, sessionId, title, chunks)
                    }
                )
                .padding(16.dp)
        )

        Spacer(Modifier.size(20.dp))

        TransportButton(
            icon = Icons.Rounded.SkipNext,
            enabled = isActiveSession,
            onClick = { TtsController.seekSentence(context, 1) },
            onDoubleClick = { TtsController.seekParagraph(context, 1) }
        )
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val tint = AmbientTheme.palette.textPrimary.copy(alpha = if (enabled) 0.75f else 0.25f)
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
            .padding(12.dp)
    )
    Spacer(Modifier.height(0.dp))
}
