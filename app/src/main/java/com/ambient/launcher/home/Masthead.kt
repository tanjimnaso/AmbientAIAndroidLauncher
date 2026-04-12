package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.Settings
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.BatteryUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Masthead
 *
 * Full-width date/time/weather header — news-first hierarchy.
 * Uses ResponsiveTypography for consistency.
 *
 * Left:  DAY NAME (H1 / Syne Light 40sp)
 *        Full Date (T2 / Inter 15sp, secondary)
 * Right: Temp (H1 / Syne Light 40sp)
 *        Time (T2 / Inter 15sp, secondary)
 * Right: Season Chip (T3 / Inter 12sp, metadata)
 */
@Composable
internal fun Masthead(
    weather: WeatherUiState,
    battery: BatteryUiState,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val dateString = "${today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${today.dayOfMonth}, ${today.year}"
    val weekFields = WeekFields.of(Locale.getDefault())
    val weekOfYear = today.get(weekFields.weekOfYear())
    val weeksInYear = 52
    val weekString = "WEEK $weekOfYear/$weeksInYear"
    val season = remember { getCurrentSeason() }
    val context = LocalContext.current

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }
    val timeString = currentTime.format(DateTimeFormatter.ofPattern("h:mma")).lowercase()

    // Pure typographic layout: three lines, left-right justified
    val batteryText = if (battery.isCharging) {
        "${battery.percentage}% • Charging"
    } else {
        "${battery.percentage}% • ${battery.remainingHours}h ${battery.remainingMinutes}m"
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Line 1: SUNDAY _ 6:40pm
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = dayName,
                style = ResponsiveTypography.d1,
                color = AmbientTheme.palette.textPrimary
            )
            Text(
                text = timeString,
                style = ResponsiveTypography.d1,
                color = AmbientTheme.palette.textPrimary
            )
        }

        // Line 2: April 12, 2026 AUTUMN _ 15° CLOUDY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Date + Season inline
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = ResponsiveTypography.t2,
                    color = AmbientTheme.palette.textSecondary
                )
                SeasonChip(season = season)
            }

            // Right: Temp + Weather
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (weather.temperatureText.isNotBlank()) weather.temperatureText else "—",
                    style = ResponsiveTypography.t2,
                    color = if (weather.temperatureText.isNotBlank()) AmbientTheme.palette.textPrimary else AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                )
                Text(
                    text = weather.summary.uppercase(),
                    style = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Light),
                    color = AmbientTheme.palette.textSecondary
                )
            }
        }

        // Line 3: Week 15/52 _ battery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weekString,
                style = ResponsiveTypography.t3.copy(fontWeight = FontWeight.Light),
                color = AmbientTheme.palette.textSecondary
            )
            Text(
                text = batteryText,
                style = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Light),
                color = AmbientTheme.palette.textSecondary,
                modifier = Modifier.clickable {
                    try {
                        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )
        }
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

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = season.uppercase(),
            style = ResponsiveTypography.t3.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
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

