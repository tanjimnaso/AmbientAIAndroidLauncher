package com.ambient.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * CompactTodayHeader
 *
 * Consolidates date, time, and weather into a single compact row,
 * positioned directly under TimeHorizonDashboard for visual continuity.
 *
 * Visual:
 * "72°F Clear | April 11, 2026"
 *
 * Rationale:
 * - Eliminates scattered weather/date/time from HomeHeader
 * - Creates narrative flow: cosmic temporal scale → today's actual conditions
 * - Minimal visual weight (T3/T2 typography)
 * - Supports news-first reading flow
 */

@Composable
internal fun CompactTodayHeader(
    weather: WeatherUiState,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val dateString = "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()}, ${today.year}"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        // Weather + Date on single compact line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather (left side): "72°F Clear"
            Text(
                text = "${weather.temperatureText} ${weather.summary}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = AmbientTheme.palette.textPrimary
            )

            // Separator: minimal divider
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
            )

            // Date (right side): "April 11, 2026"
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = AmbientTheme.palette.textPrimary
            )
        }

        // Optional: Spacer if you want breathing room (minimal)
        // Spacer(modifier = Modifier.height(4.dp))
    }
}
