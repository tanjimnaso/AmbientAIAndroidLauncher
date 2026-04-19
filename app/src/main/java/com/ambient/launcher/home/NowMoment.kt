package com.ambient.launcher.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * "Now" moment — a single quiet figure on the main page.
 *
 * One slot, three sources rotated every ROTATE_MS:
 *   • A top headline (recent — refreshes via the briefing pipeline)
 *   • A weather sentence (present — refreshes hourly)
 *   • A century glance (deep time — a hand-curated forecast keyed to the day)
 *
 * The slot exists so the eye has one ambient thing to land on between the masthead
 * and the pinned apps. Crossfade is slow (800ms) so the change is noticed
 * peripherally, not as a jolt.
 */
private const val ROTATE_MS = 30_000L

@Composable
internal fun NowMoment(
    topHeadline: String?,
    weather: WeatherUiState,
    modifier: Modifier = Modifier
) {
    val centuryGlance = remember { centuryGlanceForToday() }

    val sources: List<Pair<String, String>> = remember(topHeadline, weather, centuryGlance) {
        buildList {
            topHeadline?.takeIf { it.isNotBlank() }?.let { add("HEADLINE" to it) }
            weatherSentence(weather)?.let { add("WEATHER" to it) }
            add("CENTURY" to centuryGlance)
        }
    }

    if (sources.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(sources) {
        index = 0
        while (sources.size > 1) {
            delay(ROTATE_MS)
            index = (index + 1) % sources.size
        }
    }

    val (kind, text) = sources[index.coerceIn(0, sources.lastIndex)]

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = labelFor(kind),
            style = ResponsiveTypography.t3.copy(fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Normal),
            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f),
        )
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = 800),
            label = "now-moment"
        ) { line ->
            Text(
                text = line,
                style = ResponsiveTypography.t1.copy(fontSize = 17.sp, lineHeight = 23.sp),
                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun labelFor(kind: String): String = when (kind) {
    "HEADLINE" -> "NOW"
    "WEATHER"  -> "TODAY"
    "CENTURY"  -> "DEEP TIME"
    else       -> ""
}

private fun weatherSentence(weather: WeatherUiState): String? {
    if (!weather.isAvailable) return null
    val temp = weather.temperatureText
    val summary = weather.summary.lowercase()
    if (temp.isBlank() && summary.isBlank()) return null
    return "$temp · $summary"
}

// ── Century glance — deep-time forecasts, day-keyed ──────────────────────────

private val centuryGlances: List<String> = listOf(
    "Halley's Comet returns in 2061",
    "By 2080, world population peaks near 10.4 billion",
    "At current pace, Earth warms ~2.4°C by 2100",
    "Voyager 1 reaches Gliese 445 in roughly 40,000 years",
    "Antarctic ice now thinning twice as fast as the 1990s",
    "The 23rd century begins in less than 175 years",
    "By 2050, more plastic in the ocean than fish, by mass",
    "A bristlecone pine seedling today may outlive the year 6000",
    "Sea level: 0.3–1.0 m higher by 2100, depending on us",
    "Greenland's last summer ice possibly within this century",
    "By 2075, half of all coral reefs may be gone",
    "Solar maximum 26 arrives around 2036",
    "Average human lifespan in 1900: 32 years. Today: 73.",
    "The Saturn V's last echo left Earth's vicinity in 1972",
    "Antarctica's last untouched ice shelf may collapse by 2070",
    "Mars sample return targeted for the 2030s",
    "Carbon levels: highest in roughly 3 million years",
    "By 2100, Arctic summers could be ice-free most years",
    "JWST's mirror will drift cold and quiet long after 2040",
    "The next total solar eclipse over the U.S.: August 2044",
    "Earth's day was 22 hours during the dinosaurs",
    "Andromeda will collide with the Milky Way in 4.5 billion years",
    "By 2150, Venice may be partly underwater",
    "Permafrost holds twice the carbon currently in our atmosphere",
    "Most lithium for the next century is already in the ground",
    "Pluto completes one orbit every 248 Earth years",
    "Moon drifts ~3.8 cm farther from Earth each year",
    "By 2100, more people over 80 than under 5",
    "Last known speaker of Yagán passed in 2022",
    "A photon from the Sun's core takes ~100,000 years to reach the surface",
    "Half the planet's fresh water sits frozen in Antarctica",
    "Bristlecones alive today were saplings before the pyramids"
)

private fun centuryGlanceForToday(): String {
    val day = LocalDate.now().toEpochDay().toInt()
    val idx = ((day % centuryGlances.size) + centuryGlances.size) % centuryGlances.size
    return centuryGlances[idx]
}
