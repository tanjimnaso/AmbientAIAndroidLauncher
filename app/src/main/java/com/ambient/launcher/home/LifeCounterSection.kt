// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  ARCHIVED GRAPHICS — not currently rendered in the launcher UI           ║
// ║                                                                          ║
// ║  These components were part of the original "life arc" design layer:     ║
// ║    • LifeCounterSection  — seconds/days countdown ticker                 ║
// ║    • AnnualLifeGrid      — seasonal month progress bar                   ║
// ║    • MonthChip           — individual month indicator chip                ║
// ║                                                                          ║
// ║  They may return in a future screen or widget. Keep them here intact.    ║
// ╚══════════════════════════════════════════════════════════════════════════╝
package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.SyneFontFamily
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val BIRTH_DATE: LocalDate = LocalDate.of(1993, 8, 24)
private val TARGET_DATE = java.time.LocalDateTime.of(2085, 8, 24, 3, 0)

/**
 * LifeCounterSection
 *
 * Zone 5 of the 5-zone grid. 
 *  • Annual life grid — top-right anchor
 *  • Time — right-aligned under grid
 *  • Days left — huge Syne Light number, top-left anchor + vertical offset
 *  • Days lived — top-right anchor + vertical offset
 */
@Composable
internal fun LifeCounterSection(
    modifier: Modifier = Modifier
) {
    var currentDateTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentDateTime = java.time.LocalDateTime.now()
        }
    }

    val today         = LocalDate.now()
    val daysLived     = ChronoUnit.DAYS.between(BIRTH_DATE, today).toInt()
    val secondsLeft   = ChronoUnit.SECONDS.between(currentDateTime, TARGET_DATE).coerceAtLeast(0)
    val daysRemaining = (secondsLeft / 86400).toInt()

    // Space-separated thousands: 1 862 448 000
    val secondsFormatted = "%,d".format(secondsLeft).replace(",", " ")

    Box(modifier = modifier.fillMaxSize()) {

        // ── CENTERED: seconds ticker ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp), // Moving it UP away from bottom elements
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = secondsFormatted,
                style = TextStyle(
                    fontFamily = SyneFontFamily,
                    fontSize = 32.sp, // 5 steps larger than previous metadata size
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp
                ),
                color = AmbientTheme.palette.textPrimary
            )
            Text(
                text = "seconds left",
                style = ResponsiveTypography.t3.copy(fontSize = 11.sp),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.End) // Right justified to the ticker above
            )
        }

        // ── BOTTOM-LEFT: days left ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 4.dp)
        ) {
            Text(
                text = "%,d".format(daysRemaining).replace(",", " "),
                style = TextStyle(
                    fontFamily = SyneFontFamily,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 54.sp,
                    letterSpacing = (-1.5).sp
                ),
                color = AmbientTheme.palette.textPrimary
            )
            Text(
                text = "days left",
                style = ResponsiveTypography.t3,
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.45f)
            )
        }

        // ── BOTTOM-RIGHT: days lived ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "%,d".format(daysLived).replace(",", " "),
                style = TextStyle(
                    fontFamily = SyneFontFamily,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 54.sp,
                    letterSpacing = (-1.5).sp
                ),
                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.45f)
            )
            Text(
                text = "days lived",
                style = ResponsiveTypography.t3,
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
internal fun AnnualLifeGrid(
    currentMonth: Int,
    modifier: Modifier = Modifier
) {
    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEachIndexed { index, label ->
                MonthChip(
                    label = label,
                    monthIndex = index + 1,
                    currentMonth = currentMonth,
                    modifier = Modifier.weight(1f)
                )
                if (index < months.size - 1) Spacer(Modifier.width(1.dp))
            }
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    monthIndex: Int,
    currentMonth: Int,
    modifier: Modifier = Modifier
) {
    val isCurrent = monthIndex == currentMonth
    val isPast = monthIndex < currentMonth

    // Seasonal Colors
    val spring = Color(0xFF81C784)
    val summer = Color(0xFFFFD54F)
    val autumn = Color(0xFFFF8A65)
    val winter = Color(0xFF64B5F6)

    // Desaturated versions for past/future
    val springMuted = Color(0xFF96AC96)
    val summerMuted = Color(0xFFB9B096)
    val autumnMuted = Color(0xFFB9A296)
    val winterMuted = Color(0xFF96A6B9)

    val tz = java.util.TimeZone.getDefault().id
    val isSouth = tz.contains("Australia", true) || tz.contains("Pacific", true) ||
            tz.startsWith("America/Argentina", true) ||
            tz.startsWith("America/Sao_Paulo", true) ||
            tz.startsWith("Africa/Johannesburg", true)

    val baseColor = if (isSouth) {
        when (monthIndex) {
            9, 10, 11 -> if (isCurrent) spring else springMuted
            12, 1, 2 -> if (isCurrent) summer else summerMuted
            3, 4, 5 -> if (isCurrent) autumn else autumnMuted
            else -> if (isCurrent) winter else winterMuted
        }
    } else {
        when (monthIndex) {
            3, 4, 5 -> if (isCurrent) spring else springMuted
            6, 7, 8 -> if (isCurrent) summer else summerMuted
            9, 10, 11 -> if (isCurrent) autumn else autumnMuted
            else -> if (isCurrent) winter else winterMuted
        }
    }

    val bgColor = when {
        isCurrent -> baseColor.copy(alpha = 0.45f) 
        isPast    -> baseColor.copy(alpha = 0.20f)
        else      -> baseColor.copy(alpha = 0.10f)
    }
    val borderColor = when {
        isCurrent -> baseColor.copy(alpha = 0.75f)
        isPast    -> baseColor.copy(alpha = 0.35f)
        else      -> baseColor.copy(alpha = 0.20f)
    }

    Box(
        modifier = modifier
            .height(16.dp)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.take(1),
            style = ResponsiveTypography.t3.copy(fontSize = 8.sp),
            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}
