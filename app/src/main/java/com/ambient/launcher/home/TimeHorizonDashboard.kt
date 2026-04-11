package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
internal fun TimeHorizonDashboard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Universal Scale (Universe start -> Now -> Heat Death)
        TimeBar(label = "Universal Scale", progress = 0.0001f, start = "Big Bang", end = "Heat Death")
        
        // 2. Industrial/AGI Scale (Ind. Rev -> Now -> Post-AGI)
        TimeBar(label = "Industrial / AGI Scale", progress = 0.65f, start = "1760", end = "AGI")
        
        // 3. Year/Week Bar & Counter
        YearContextBar()

        // 4. Today's Date/Weather/Season (Handled by HomeHeader, this is supplementary)
    }
}

@Composable
private fun TimeBar(label: String, progress: Float, start: String, end: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.textSecondary
        )
        Row(modifier = Modifier.fillMaxWidth().height(4.dp).background(AmbientTheme.palette.textPrimary.copy(alpha = 0.1f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(AmbientTheme.palette.accentHigh))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = start, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AmbientTheme.palette.textSecondary)
            Text(text = end, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AmbientTheme.palette.textSecondary)
        }
    }
}

@Composable
private fun YearContextBar() {
    val today = LocalDate.now()
    val dayOfYear = today.dayOfYear
    val daysInYear = if (today.isLeapYear) 366 else 365
    val progress = dayOfYear.toFloat() / daysInYear

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "2026", style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SyneFontFamily, fontSize = 24.sp), color = AmbientTheme.palette.textPrimary)
            Text(text = "Week ${java.time.temporal.WeekFields.ISO.weekOfYear().getFrom(today)}/52", style = MaterialTheme.typography.bodySmall, color = AmbientTheme.palette.textSecondary)
        }
        Row(modifier = Modifier.fillMaxWidth().height(4.dp).background(AmbientTheme.palette.textPrimary.copy(alpha = 0.1f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(AmbientTheme.palette.accentHigh))
        }
    }
}
