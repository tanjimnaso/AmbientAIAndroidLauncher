package com.ambient.launcher.ui.theme

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos

/**
 * Approximate solar transitions based on calendar date.
 */
internal object SolarCalculator {

    data class SolarWindows(
        val dayStart: LocalTime,
        val duskStart: LocalTime,
        val twilightStart: LocalTime
    )

    fun getWindowsForDate(date: LocalDate): SolarWindows {
        val dayOfYear = date.dayOfYear
        
        // theta is roughly 0 at winter solstice (~Dec 21).
        val theta = 2.0 * Math.PI * (dayOfYear + 10) / 365.25
        
        // Variance of ~1.5 hours depending on the season.
        val variance = 1.5 * cos(theta)
        
        // Twilight (Night) centered at 18.5 (6:30 PM).
        val twilightStartHour = 18.5 - variance
        
        // Dusk starts 2 hours before Twilight (centered at 16.5 / 4:30 PM).
        val duskStartHour = twilightStartHour - 2.0
        
        // Day starts around 05:30. Sunrise varies opposite to sunset.
        val dayStartHour = 5.5 + variance
        
        return SolarWindows(
            dayStart = hourToTime(dayStartHour),
            duskStart = hourToTime(duskStartHour),
            twilightStart = hourToTime(twilightStartHour)
        )
    }

    private fun hourToTime(hour: Double): LocalTime {
        // Handle negative hours or hours >= 24 if they occur due to extreme variance
        val normalizedHour = (hour + 24) % 24
        val h = normalizedHour.toInt().coerceIn(0, 23)
        val m = ((normalizedHour % 1.0) * 60).toInt().coerceIn(0, 59)
        return LocalTime.of(h, m)
    }
}
