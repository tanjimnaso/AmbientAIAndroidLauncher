package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.BatteryUiState
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Masthead
 *
 * Only designed for S22 Ultra (center-cutout display).
 */
@Composable
internal fun Masthead(
    weather: WeatherUiState,
    battery: BatteryUiState,
    modifier: Modifier = Modifier
) {
    val today      = LocalDate.now()
    val dayName    = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val dateString = "${today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${today.dayOfMonth}, ${today.year}"
    val season     = remember { getCurrentSeason() }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }
    val timeString = currentTime.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Row 1: DAY (Start) | TIME & BATTERY (End) ────────────────
        // Using a Row ensures the elements cannot overlap. 
        // We push the time/battery to the end, respecting the cutout/system insets.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left side: Day Name
            Text(
                text = dayName,
                style = ResponsiveTypography.h1,
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f),
                // Weight allows it to take up space without pushing the right side off screen.
                // We don't want it to squish the time, so we let the time dictate its width.
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1, // Prevent it from pushing layout down if text scales up
            )

            // Right side: Time and Battery
            Row(
                modifier = Modifier.offset(x = with(density) { 60.toDp() }),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeString,
                    style = ResponsiveTypography.h1,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                )

                // Battery: percentage and time left stacked
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End // Align right since it's on the right edge
                ) {
                    Text(
                        text = "${battery.percentage}%",
                        style = ResponsiveTypography.t3,
                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                    )
                    val batterySub = if (battery.isCharging) "CHG"
                    else "${battery.remainingHours}h ${battery.remainingMinutes}m"
                    Text(
                        text = batterySub,
                        style = ResponsiveTypography.t3.copy(fontSize = 9.sp),
                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }

        // ── Row 2: DATE/SEASON (Start) | TEMP (End) ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateString,
                    style = ResponsiveTypography.t2,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                )
                SeasonChip(season = season)
            }
            if (weather.isAvailable) {
                Text(
                    text = "${weather.temperatureText} ${weather.summary.lowercase()}",
                    style = ResponsiveTypography.t2,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(season: String, modifier: Modifier = Modifier) {
    val color = when (season) {
        "Spring" -> Color(0xFF4CAF50)
        "Summer" -> Color(0xFFFFC107)
        "Autumn" -> Color(0xFFFF9800)
        "Winter" -> Color(0xFF2196F3)
        else     -> AmbientTheme.palette.accentHigh
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
    val day   = today.dayOfMonth
    val tz    = java.util.TimeZone.getDefault().id
    val isSouth = tz.contains("Australia", true) || tz.contains("Pacific", true) ||
                  tz.startsWith("America/Argentina", true) ||
                  tz.startsWith("America/Sao_Paulo", true) ||
                  tz.startsWith("Africa/Johannesburg", true)
    return if (isSouth) {
        when (month) { 3, 4, 5 -> "Autumn"; 6, 7, 8 -> "Winter"; 9, 10, 11 -> "Spring"; else -> "Summer" }
    } else {
        when (month) {
            3  -> if (day >= 20) "Spring" else "Winter"
            4, 5 -> "Spring"
            6  -> if (day >= 21) "Summer" else "Spring"
            7, 8 -> "Summer"
            9  -> if (day >= 22) "Autumn" else "Summer"
            10, 11 -> "Autumn"
            12 -> if (day >= 21) "Winter" else "Autumn"
            else -> "Winter"
        }
    }
}
