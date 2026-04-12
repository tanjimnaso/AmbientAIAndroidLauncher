package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.MediaViewModel
import com.ambient.launcher.ui.theme.AmbientTheme

@Composable
internal fun MediaStrip(viewModel: MediaViewModel) {
    val media by viewModel.mediaInfo.collectAsState()
    media?.let { info ->
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(LauncherLayout.sectionCardRadius),
            color = AmbientTheme.palette.elevatedPanel.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOW PLAYING",
                        style = ResponsiveTypography.t3.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.title,
                        style = ResponsiveTypography.t1,  // 20sp Inter SemiBold — track title
                        color = AmbientTheme.palette.textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.artist,
                        style = ResponsiveTypography.t2,  // 15sp Inter Regular — artist name
                        color = AmbientTheme.palette.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                // Visual signal for active state
                if (info.isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AmbientTheme.palette.accentHigh, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
