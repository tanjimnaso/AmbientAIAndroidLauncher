package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily

/**
 * State Layer Dashboard (Level 1: The State)
 *
 * Bridges long-term context (TimeHorizonDashboard) and daily action (TodayInfoCard).
 * Shows trajectory, not snapshot. "Am I becoming more capable? More healthy? More leveraged?"
 *
 * Components:
 * - Health Gauge: Wellness metrics from fitness tracking
 * - Super/Leverage Gauge: Network, output, visibility, influence
 * - Skills Gauge: Competency growth and time investment
 * - Time Allocation: Weekly deep work vs. overhead breakdown
 *
 * Visual: Circular arc gauges (Material 3) with trend indicators.
 * Interaction: Tap gauge to expand detail view (phase 2).
 * Refresh: Daily (health), weekly (super/skills/time).
 */

data class GaugeMetric(
    val label: String,
    val percentage: Float,  // 0f..1f
    val trend: TrendDirection = TrendDirection.NEUTRAL,
    val trendPercent: Float = 0f  // weekly change, e.g., +3f for +3%
)

enum class TrendDirection {
    UP, NEUTRAL, DOWN
}

// ──────────────────────────────────────────────────────────────
// TODO: Phase 1 Implementation
// ──────────────────────────────────────────────────────────────
// 1. Create HealthGauge composable
//    - Integrate with MyFitnessPal API (via DashboardViewModel)
//    - Display: Cardio, Strength, Sleep, Recovery as sub-metrics
//    - Colors: Green (good), Yellow (monitor), Red (needs attention)
//    - Trend sparkline: 4-week rolling average
//
// 2. Create TimeAllocationGauge composable
//    - Parse Google Calendar for event types
//    - Display: Deep Work %, Meetings %, Learning %, Admin %
//    - Compare to user's baseline (needs storage in LauncherConfiguration)
//    - Trend indicator: this week vs. last week
//
// 3. Create StateLayerDashboard composable
//    - 2x2 grid of gauges (or stacked column on narrow screens)
//    - Health, SuperTrajectory, Skills, TimeAllocation
//    - Compact by default, tap to expand for detail sheet
//    - Use same collapse animation as industrial index buckets

@Composable
internal fun StateLayerDashboard(
    modifier: Modifier = Modifier,
    // Phase 1: Add parameters as data sources are integrated
    // healthMetric: GaugeMetric = GaugeMetric("Health", 0.76f, TrendDirection.UP, 3f),
    // timeMetric: GaugeMetric = GaugeMetric("Time", 0.62f, TrendDirection.NEUTRAL, 0f)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section label (ultra-subtle, T3 weight)
        Text(
            text = "Current State",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.3f)
        )

        // TODO: Implement grid of gauges
        // For now, render placeholder
        PlaceholderGaugeGrid()
    }
}

@Composable
private fun PlaceholderGaugeGrid() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder gauges (replace with actual HealthGauge, TimeAllocationGauge, etc.)
        repeat(2) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .background(
                        AmbientTheme.palette.textPrimary.copy(alpha = 0.05f),
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gauge",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Component: Arc Gauge
// ──────────────────────────────────────────────────────────────
// Reusable gauge component for all metrics.
// Usage: ArcGauge(label = "Health", percentage = 0.76f, trend = TrendDirection.UP, trendPercent = 3f)

@Composable
fun ArcGauge(
    label: String,
    percentage: Float,
    trend: TrendDirection = TrendDirection.NEUTRAL,
    trendPercent: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Circular progress arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            CircularProgressIndicator(
                progress = percentage,
                modifier = Modifier.size(80.dp),
                color = when (trend) {
                    TrendDirection.UP -> AmbientTheme.palette.accentHigh
                    TrendDirection.DOWN -> AmbientTheme.palette.accentHigh.copy(alpha = 0.6f)
                    TrendDirection.NEUTRAL -> AmbientTheme.palette.textPrimary.copy(alpha = 0.4f)
                },
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
                trackColor = AmbientTheme.palette.textPrimary.copy(alpha = 0.1f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = SyneFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 24.sp
                    ),
                    color = AmbientTheme.palette.textPrimary
                )

                // Trend indicator
                if (trendPercent != 0f) {
                    Text(
                        text = when {
                            trendPercent > 0 -> "↑ +${trendPercent.toInt()}%"
                            trendPercent < 0 -> "↓ ${trendPercent.toInt()}%"
                            else -> "→ No change"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 9.sp
                        ),
                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Gauge label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = AmbientTheme.palette.textPrimary
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Future: Detailed expansion sheets
// ──────────────────────────────────────────────────────────────
// TODO: Create HealthDetailSheet, SuperDetailSheet, SkillsDetailSheet
// These open as bottom sheets when user taps a gauge.
// Show breakdown (Cardio, Strength, Sleep, Recovery) + 4-week sparkline.
