package com.ambient.launcher.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * "Now" moment — a single quiet figure on the main page.
 *
 * One slot, two sources rotated every ROTATE_MS:
 *   • A top headline (recent — refreshes via the briefing pipeline)
 *   • A century glance (deep time — hand-curated forecast keyed to the day)
 *
 * Tapping a headline opens the article viewer. The century glance is not
 * tappable — it's context, not content.
 *
 * The slot exists so the eye has one ambient thing to land on between the
 * masthead and the pinned apps. Crossfade is slow (800ms) so the change is
 * noticed peripherally, not as a jolt.
 */
private const val ROTATE_MS = 30_000L

private data class NowSource(val kind: String, val text: String, val item: RssFeedItem? = null)

@Composable
internal fun NowMoment(
    topHeadline: RssFeedItem?,
    onHeadlineClick: (RssFeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Session-stable shuffled glances so the sequence is different each time you launch
    val shuffledGlances = remember { centuryGlances.shuffled() }
    
    var rotationTick by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(topHeadline) {
        while (true) {
            delay(ROTATE_MS)
            rotationTick++
        }
    }

    // Alternating logic: Even ticks show Headline (if exists), Odd ticks show a Glance.
    // This ensures the headline stays "Now" while the Deep Time background shifts.
    val current = remember(topHeadline, rotationTick) {
        val hasHeadline = topHeadline != null && topHeadline.title.isNotBlank()
        
        if (hasHeadline && rotationTick % 2 == 0) {
            NowSource("HEADLINE", topHeadline.title, topHeadline)
        } else {
            // Pick a glance based on the tick count
            val gIdx = (rotationTick / (if (hasHeadline) 2 else 1)) % shuffledGlances.size
            NowSource("CENTURY", shuffledGlances[gIdx])
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = labelFor(current.kind),
            style = ResponsiveTypography.t3.copy(fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Normal),
            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f),
        )
        Crossfade(
            targetState = current,
            animationSpec = tween(durationMillis = 800),
            label = "now-moment"
        ) { snap ->
            val tapModifier = if (snap.item != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onHeadlineClick(snap.item) }
                )
            } else Modifier
            Text(
                text = snap.text,
                style = ResponsiveTypography.t1.copy(fontSize = 17.sp, lineHeight = 23.sp),
                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = tapModifier
            )
        }
    }
}

private fun labelFor(kind: String): String = when (kind) {
    "HEADLINE" -> "NOW"
    "CENTURY"  -> "DEEP TIME"
    else       -> ""
}

// ── Century glance — deep-time forecasts, day-keyed ──────────────────────────

private val centuryGlances: List<String> = listOf(
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
    "The Saturn V's last echo left Earth's vicinity in 1972",
    "Antarctica's last untouched ice shelf may collapse by 2070",
    "Mars sample return targeted for the 2030s",
    "Carbon levels: highest in roughly 3 million years",
    "By 2100, Arctic summers could be ice-free most years",
    "JWST's mirror will drift cold and quiet long after 2040",
    "Andromeda will collide with the Milky Way in 4.5 billion years",
    "By 2150, Venice may be partly underwater",
    "Permafrost holds twice the carbon currently in our atmosphere",
    "Most lithium for the next century is already in the ground",
    "By 2100, more people over 80 than under 5",
    "Last known speaker of Yagán passed in 2022",
    "Bristlecones alive today were saplings before the pyramids"
)

private fun centuryGlanceForToday(): String {
    val day = LocalDate.now().toEpochDay().toInt()
    val idx = ((day % centuryGlances.size) + centuryGlances.size) % centuryGlances.size
    return centuryGlances[idx]
}
