package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ambient.launcher.R
import com.ambient.launcher.BatteryUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val DMSerifDisplay = FontFamily(Font(R.font.dm_serif_display_regular))

/**
 * Masthead
 *
 * Reimagined as a Japandi editorial layout.
 * Uses serif typography for the primary time/day anchor to feel like a magazine header.
 * Reduces utility "noise" by removing labels (%, CHG) in favor of pure figures.
 */
@Composable
internal fun Masthead(
    battery: BatteryUiState,
    modifier: Modifier = Modifier,
    fullness: Float = 1f
) {
    val palette = AmbientTheme.palette
    val secondaryAlpha = (fullness - 0.2f).coerceIn(0f, 1f) * palette.secondaryOpacity
    val primaryAlpha = palette.primaryOpacity
    val today      = LocalDate.now()
    val dayName    = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val monthName  = today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val dateString = "${today.dayOfMonth} $monthName"
    
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }
    val timeString = currentTime.format(DateTimeFormatter.ofPattern("h:mm"))
    val amPm       = currentTime.format(DateTimeFormatter.ofPattern("a")).lowercase()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Primary Editorial Row ────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // THE TIME - Large Serif Anchor
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeString,
                    style = ResponsiveTypography.h1.copy(
                        fontFamily = DMSerifDisplay,
                        fontSize = 54.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = palette.textPrimary.copy(alpha = 0.85f * primaryAlpha)
                )
                Text(
                    text = amPm,
                    style = ResponsiveTypography.t3.copy(fontSize = 12.sp),
                    color = palette.textSecondary.copy(alpha = 0.4f * primaryAlpha),
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }

            // THE DAY - Vertical/Rotated aesthetic or thin caps
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dayName,
                    style = ResponsiveTypography.t3.copy(
                        letterSpacing = 4.sp,
                        fontSize = 10.sp
                    ),
                    color = palette.textSecondary.copy(alpha = 0.5f * primaryAlpha)
                )
                if (secondaryAlpha > 0.01f) {
                    Text(
                        text = dateString,
                        style = ResponsiveTypography.t3.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                        color = palette.textSecondary.copy(alpha = 0.3f * primaryAlpha),
                        modifier = Modifier.alpha(secondaryAlpha)
                    )
                }
            }
        }

        // ── Secondary Thin Line ────────────────
        if (secondaryAlpha > 0.1f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(secondaryAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery as a pure figure + charging dot
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${battery.percentage}",
                        style = ResponsiveTypography.t3.copy(fontSize = 9.sp),
                        color = palette.textSecondary.copy(alpha = 0.4f * primaryAlpha)
                    )
                    if (battery.isCharging) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(palette.accentHigh.copy(alpha = 0.6f * primaryAlpha), androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }

                // Minimal Weather / Season (simplified)
                Text(
                    text = getCurrentSeason().uppercase(),
                    style = ResponsiveTypography.t3.copy(fontSize = 8.sp, letterSpacing = 2.sp),
                    color = palette.textSecondary.copy(alpha = 0.3f * primaryAlpha)
                )
            }
        }
    }
}

// This was used by the old Masthead, keeping for reference if needed elsewhere or removing
// private fun SeasonChip...

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
