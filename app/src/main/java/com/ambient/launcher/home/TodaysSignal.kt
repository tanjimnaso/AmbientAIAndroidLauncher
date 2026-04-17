package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * TodaysSignal
 *
 * Section divider above the headlines feed.
 * Layout (S22 Ultra Aligned):
 *  - "AROUND THE WORLD" (Centered)
 *  - Day, Date, Season, Week (Centered)
 *  - Progress bar (Full width)
 */
@Composable
internal fun TodaysSignal(
    lastRefreshTime: Long = 0L,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val weekNumber = java.time.temporal.WeekFields.ISO.weekOfYear().getFrom(today)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(weekNumber / 52f)
                    .height(1.dp)
                    .background(AmbientTheme.palette.accentHigh.copy(alpha = 0.5f))
            )
        }
    }
}

/** "Xh Ym ago", "Xm ago", or "just now" */
internal fun formatElapsed(epochMillis: Long): String {
    val diffMs = System.currentTimeMillis() - epochMillis
    val mins = (diffMs / 60_000).toInt().coerceAtLeast(0)
    val hours = mins / 60
    return when {
        mins < 1 -> "just now"
        hours < 1 -> "${mins}m ago"
        mins % 60 == 0 -> "${hours}h ago"
        else -> "${hours}h ${mins % 60}m ago"
    }
}

@Composable
private fun SeasonChip(
    season: String,
    modifier: Modifier = Modifier
) {
    val color = when (season) {
        "Spring" -> Color(0xFF4CAF50)
        "Summer" -> Color(0xFFFFC107)
        "Autumn" -> Color(0xFFFF9800)
        "Winter" -> Color(0xFF2196F3)
        else -> AmbientTheme.palette.accentHigh
    }

    Box(modifier = modifier.background(color.copy(alpha = 0.18f))) {
        Text(
            text = season.uppercase(),
            style = ResponsiveTypography.t3.copy(letterSpacing = 0.5.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getCurrentSeason(): String {
    val today = LocalDate.now()
    val month = today.monthValue
    val day = today.dayOfMonth
    val isSouthernHemisphere = java.util.TimeZone.getDefault().id.contains("Australia", ignoreCase = true) ||
                               java.util.TimeZone.getDefault().id.contains("Pacific", ignoreCase = true) ||
                               java.util.TimeZone.getDefault().id.startsWith("America/Argentina", ignoreCase = true) ||
                               java.util.TimeZone.getDefault().id.startsWith("America/Sao_Paulo", ignoreCase = true) ||
                               java.util.TimeZone.getDefault().id.startsWith("Africa/Johannesburg", ignoreCase = true)
    
    return if (isSouthernHemisphere) {
        when (month) {
            3, 4, 5 -> "Autumn"
            6, 7, 8 -> "Winter"
            9, 10, 11 -> "Spring"
            12, 1, 2 -> "Summer"
            else -> "Summer"
        }
    } else {
        when (month) {
            3 -> if (day >= 20) "Spring" else "Winter"
            4, 5 -> "Spring"
            6 -> if (day >= 21) "Summer" else "Spring"
            7, 8 -> "Summer"
            9 -> if (day >= 22) "Autumn" else "Summer"
            10, 11 -> "Autumn"
            12 -> if (day >= 21) "Winter" else "Autumn"
            else -> "Winter"
        }
    }
}
