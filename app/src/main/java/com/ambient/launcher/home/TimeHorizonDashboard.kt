package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import java.time.LocalDate

/**
 * Reasoned Intention:
 * The user is creating a "Temporal Stack" footer. By ordering the bars from
 * Personal (shortest) to Cosmic (longest) and placing labels ABOVE the lines,
 * it creates a grounded, data-rich perspective that anchors the UI.
 * The 2000-3000 bar provides the "Millennial Horizon" bridging individual
 * life with cosmic time.
 */

@Composable
internal fun LifeTimelineBar(modifier: Modifier = Modifier) {
    val birthYear = 1993
    val expectedYear = 2085
    val currentYear = LocalDate.now().year
    val totalYears = expectedYear - birthYear
    val progress = (currentYear - birthYear).toFloat() / totalYears

    TimelineBar(
        labelStart = "$birthYear",
        labelEnd = "${expectedYear}+",
        progress = progress,
        modifier = modifier
    )
}

@Composable
internal fun MillennialTimelineBar(modifier: Modifier = Modifier) {
    val startYear = 2000
    val endYear = 3000
    val currentYear = LocalDate.now().year
    val totalYears = endYear - startYear
    val progress = (currentYear - startYear).toFloat() / totalYears

    TimelineBar(
        labelStart = "$startYear",
        labelEnd = "$endYear",
        progress = progress,
        modifier = modifier
    )
}

@Composable
internal fun UniversalScaleBar(modifier: Modifier = Modifier) {
    TimelineBar(
        labelStart = "Big Bang",
        labelEnd = "Heat Death",
        progress = 0.00001f,
        modifier = modifier
    )
}

@Composable
private fun TimelineBar(
    labelStart: String,
    labelEnd: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = labelStart,
                style = ResponsiveTypography.t3.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
            )
            Text(
                text = labelEnd,
                style = ResponsiveTypography.t3.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(AmbientTheme.palette.accentHigh.copy(alpha = 0.5f))
            )
        }
    }
}

data class ForecastItem(
    val source: String,
    val claim: String,
    val confidence: String,
    val year: String,
    val sortYear: Int
)

@Composable
internal fun SuperForecastersSection(
    modifier: Modifier = Modifier
) {
    val forecasts = remember {
        listOf(
            ForecastItem("Polymarket", "US Recession probability", "32%", "2026", 2026),
            ForecastItem("Metaculus", "AI automates >50% knowledge work", "72%", "2032", 2032),
            ForecastItem("Kurzweil", "Technological Singularity", "85%", "2045", 2045),
            ForecastItem("United Nations", "Global Human Population Peak", "90%", "2084", 2084)
        ).sortedBy { it.sortYear }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "FORECASTS",
            style = ResponsiveTypography.t3.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp)) // Tightened spacing

        forecasts.forEachIndexed { index, item ->
            ForecastRow(item)
            if (index < forecasts.size - 1) {
                Spacer(modifier = Modifier.height(12.dp)) // Tighter separation between forecasts for grouped appearance
            }
        }
    }
}

@Composable
private fun ForecastRow(item: ForecastItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = item.claim,
                style = ResponsiveTypography.t2,  // 15sp Inter Regular
                color = AmbientTheme.palette.textPrimary,
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            )

            // Aligned Metadata Block
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.confidence,
                    style = ResponsiveTypography.t3,  // 12sp Inter Regular
                    color = AmbientTheme.palette.textSecondary,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Text(
                    text = item.year,
                    style = ResponsiveTypography.t3,  // 12sp Inter Regular
                    color = AmbientTheme.palette.textSecondary,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
        Text(
            text = item.source.uppercase(),
            style = ResponsiveTypography.t3.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.accentHigh.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
