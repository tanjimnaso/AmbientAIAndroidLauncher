package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily
import java.time.LocalDate
import java.time.Month

/**
 * Time Horizon Dashboard
 *
 * Displays temporal awareness and life progress metrics:
 * - Year progress (% through 2026)
 * - Current season (Southern Hemisphere)
 * - Week/day context
 * - 12-month activity heatmap
 * - Life progress arc (1993-2076, adjusted for 95+ lifespan)
 * - Future milestones (AGI, retirement, genetic horizon)
 */

@Composable
internal fun TimeHorizonDashboard(
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val yearProgress = calculateYearProgress(today)
    val currentSeason = calculateSeason(today.month)
    val lifeProgress = calculateLifeProgress()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 1. Year Progress
        YearProgressSection(yearProgress = yearProgress)

        // 2. Season and Current Context
        CurrentContextSection(season = currentSeason, today = today)

        // 3. 12-Month Heatmap
        MonthHeatmapSection()

        // 4. Life Progress Arc
        LifeProgressSection(lifeProgress = lifeProgress)

        // 5. Future Milestones
        MilestonesSection()
    }
}

@Composable
private fun YearProgressSection(yearProgress: Float) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "2026",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp
                ),
                color = AmbientTheme.palette.textPrimary
            )
            Text(
                text = String.format("%.1f%%", yearProgress * 100),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily
                ),
                color = AmbientTheme.palette.textSecondary
            )
        }

        LinearProgressIndicator(
            progress = yearProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = AmbientTheme.palette.accentHigh,
            trackColor = AmbientTheme.palette.textPrimary.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun CurrentContextSection(season: String, today: LocalDate) {
    val dayName = today.dayOfWeek.name.take(3).uppercase()
    val weekOfYear = getWeekOfYear(today)
    val dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 32.sp,
                letterSpacing = (-0.5).sp
            ),
            color = AmbientTheme.palette.textPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateStr.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = AmbientTheme.palette.textSecondary
                )
                Text(
                    text = "Week $weekOfYear of 52",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f)
                )
            }

            // Season indicator
            Box(
                modifier = Modifier
                    .background(
                        color = getSeasonColor(season),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = season,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MonthHeatmapSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "12-Month Activity",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.textSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val months = (1..12).map { it.toFloat() / 12f }
            val today = LocalDate.now()
            val currentMonth = today.monthValue

            months.forEachIndexed { index, _ ->
                val height = if (index < currentMonth) {
                    0.3f + (index / 12f) * 0.7f // Varying heights for visual interest
                } else {
                    0.2f // Lighter for future months
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(height)
                        .background(
                            color = if (index + 1 == currentMonth)
                                AmbientTheme.palette.accentHigh
                            else if (index < currentMonth)
                                AmbientTheme.palette.textPrimary.copy(alpha = 0.6f)
                            else
                                AmbientTheme.palette.textPrimary.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun LifeProgressSection(lifeProgress: LifeProgress) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Life Progress",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.textSecondary
        )

        // Life arc
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Past (1993-2026): Solid white
                Box(
                    modifier = Modifier
                        .weight(lifeProgress.pastPercent)
                        .fillMaxHeight()
                        .background(AmbientTheme.palette.textPrimary)
                )

                // Prime years (2026-2053): Focus gradient
                Box(
                    modifier = Modifier
                        .weight(lifeProgress.primePercent)
                        .fillMaxHeight()
                        .background(AmbientTheme.palette.accentHigh)
                )

                // Preservation (2053-2064): Muted
                Box(
                    modifier = Modifier
                        .weight(lifeProgress.preservationPercent)
                        .fillMaxHeight()
                        .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.4f))
                )

                // Bonus years (2064-2076+): Fading
                Box(
                    modifier = Modifier
                        .weight(lifeProgress.bonusPercent)
                        .fillMaxHeight()
                        .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.15f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1993",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 9.sp
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                )

                Text(
                    text = "2076+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 9.sp
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                )
            }
        }

        // Current status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Age 33 (40% through estimated lifespan)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily
                    ),
                    color = AmbientTheme.palette.textPrimary
                )
                Text(
                    text = "Super access at 60 | Pension at 67 | Est. 95+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MilestonesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Future Milestones",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.textSecondary
        )

        val milestones = listOf(
            MilestoneItem("2029", "Age 36", "AGI Milestone (Kurzweil)"),
            MilestoneItem("2035", "Age 42", "Living Intelligence Era (Webb)"),
            MilestoneItem("2053", "Age 60", "Super Drawdown + Singularity"),
            MilestoneItem("2088", "Age 95", "Genetic Horizon (100-year goal)")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            milestones.forEach { milestone ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.width(50.dp)) {
                        Text(
                            text = milestone.year,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SyneFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = AmbientTheme.palette.accentHigh
                        )
                        Text(
                            text = milestone.age,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontSize = 9.sp
                            ),
                            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = milestone.event,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp
                        ),
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// Helper data classes
data class LifeProgress(
    val pastPercent: Float = 0.26f,           // 1993-2026
    val primePercent: Float = 0.41f,          // 2026-2053 (career/action years)
    val preservationPercent: Float = 0.13f,   // 2053-2064 (preservation age)
    val bonusPercent: Float = 0.20f           // 2064-2076+ (bonus years)
)

data class MilestoneItem(
    val year: String,
    val age: String,
    val event: String
)

// Utility functions
private fun calculateYearProgress(today: LocalDate): Float {
    val dayOfYear = today.dayOfYear
    val daysInYear = if (today.year % 4 == 0 && (today.year % 100 != 0 || today.year % 400 == 0)) 366 else 365
    return dayOfYear.toFloat() / daysInYear.toFloat()
}

private fun calculateSeason(month: Month): String {
    return when (month) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> "Summer"
        Month.MARCH, Month.APRIL, Month.MAY -> "Autumn"
        Month.JUNE, Month.JULY, Month.AUGUST -> "Winter"
        Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "Spring"
    }
}

private fun getSeasonColor(season: String): Color {
    return when (season) {
        "Summer" -> Color(0xFFE8A860)   // Warm orange
        "Autumn" -> Color(0xFFC9834E)   // Rust
        "Winter" -> Color(0xFF5A8FB5)   // Cool slate blue
        "Spring" -> Color(0xFF6BAE7F)   // Green
        else -> Color(0xFF9DB5C9)
    }
}

private fun getWeekOfYear(date: LocalDate): Int {
    return java.time.temporal.WeekFields.ISO.weekOfYear.getFrom(date)
}

private fun calculateLifeProgress(): LifeProgress {
    // Born 1993, estimated lifespan 95+ years
    // Current year 2026 (age 33)
    return LifeProgress(
        pastPercent = (2026 - 1993).toFloat() / (2088 - 1993).toFloat(),        // Past
        primePercent = (2053 - 2026).toFloat() / (2088 - 1993).toFloat(),        // Prime
        preservationPercent = (2064 - 2053).toFloat() / (2088 - 1993).toFloat(), // Preservation
        bonusPercent = (2088 - 2064).toFloat() / (2088 - 1993).toFloat()         // Bonus
    )
}
