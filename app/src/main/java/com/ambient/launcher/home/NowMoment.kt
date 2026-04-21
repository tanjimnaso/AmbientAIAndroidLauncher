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
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * "Now" moment — a single quiet figure on the main page.
 *
 * One slot, two sources rotated. Headline (recent) alternates with a glance
 * drawn from an axis-aware composed sequence. Occasionally the slot resolves
 * to silence — empty body, no label — to give the eye a rest.
 *
 * Axes (PAST_DEEP … COSMIC, PERSONAL_ARC, SILENCE) are internal metadata
 * that shape the rotation rhythm; they never surface in the UI.
 */
private const val ROTATE_MS = 30_000L
private const val SILENCE_HOLD_MS = 45_000L
private const val SILENCE_EVERY = 7   // one silence slot per N glances

// User DOB — used to personalize PERSONAL_ARC glances.
// TODO: move to SharedPreferences when a settings surface exists.
private val USER_DOB: LocalDate = LocalDate.of(1993, 8, 24)

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

// ── Model ────────────────────────────────────────────────────────────────────

private enum class GlanceAxis {
    PAST_DEEP,     // geologic / pre-history
    PAST_RECENT,   // within a human lifetime or two
    NEAR_FUTURE,   // this century
    DEEP_FUTURE,   // next century onwards
    COSMIC,        // stellar / galactic scale
    PERSONAL_ARC,  // anchored to the user's age at an event
    SILENCE        // intentional empty slot
}

private data class Glance(
    val axis: GlanceAxis,
    val text: String? = null,
    val eventDate: LocalDate? = null,
    val template: ((age: Int, eventDate: LocalDate) -> String)? = null
) {
    fun render(dob: LocalDate): String? = when {
        axis == GlanceAxis.SILENCE -> null
        template != null && eventDate != null -> {
            val age = Period.between(dob, eventDate).years
            template.invoke(age, eventDate)
        }
        else -> text
    }
}

private sealed class NowSource {
    data class Headline(val item: RssFeedItem) : NowSource()
    data class GlanceText(val text: String) : NowSource()
    object Silence : NowSource()
}

// ── Glance catalogue ─────────────────────────────────────────────────────────

private val glanceCatalogue: List<Glance> = listOf(
    // PAST_DEEP — before recorded history
    Glance(GlanceAxis.PAST_DEEP, "Bristlecones alive today were saplings before the pyramids"),
    Glance(GlanceAxis.PAST_DEEP, "Carbon levels: highest in roughly 3 million years"),
    Glance(GlanceAxis.PAST_DEEP, "Permafrost holds twice the carbon currently in our atmosphere"),

    // PAST_RECENT — within living or near-living memory
    Glance(GlanceAxis.PAST_RECENT, "The Saturn V's last echo left Earth's vicinity in 1972"),
    Glance(GlanceAxis.PAST_RECENT, "Antarctic ice now thinning twice as fast as the 1990s"),

    // NEAR_FUTURE — this century
    Glance(GlanceAxis.NEAR_FUTURE, "By 2080, world population peaks near 10.4 billion"),
    Glance(GlanceAxis.NEAR_FUTURE, "At current pace, Earth warms ~2.4°C by 2100"),
    Glance(GlanceAxis.NEAR_FUTURE, "Sea level: 0.3–1.0 m higher by 2100, depending on us"),
    Glance(GlanceAxis.NEAR_FUTURE, "Greenland's last summer ice possibly within this century"),
    Glance(GlanceAxis.NEAR_FUTURE, "By 2100, Arctic summers could be ice-free most years"),
    Glance(GlanceAxis.NEAR_FUTURE, "Antarctica's last untouched ice shelf may collapse by 2070"),
    Glance(GlanceAxis.NEAR_FUTURE, "By 2100, more people over 80 than under 5"),
    Glance(GlanceAxis.NEAR_FUTURE, "Most lithium for the next century is already in the ground"),

    // DEEP_FUTURE — beyond this century
    Glance(GlanceAxis.DEEP_FUTURE, "The 23rd century begins in less than 175 years"),
    Glance(GlanceAxis.DEEP_FUTURE, "By 2150, Venice may be partly underwater"),
    Glance(GlanceAxis.DEEP_FUTURE, "A bristlecone pine seedling today may outlive the year 6000"),

    // COSMIC — stellar / galactic
    Glance(GlanceAxis.COSMIC, "Voyager 1 reaches Gliese 445 in roughly 40,000 years"),
    Glance(GlanceAxis.COSMIC, "Andromeda will collide with the Milky Way in 4.5 billion years"),
    Glance(GlanceAxis.COSMIC, "JWST's mirror will drift cold and quiet long after 2040"),

    // PERSONAL_ARC — age computed from USER_DOB
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2061, 7, 28),
        template = { age, date -> "Halley's Comet returns — you'll be $age (${date.format(MONTH_YEAR)})" }
    ),
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2033, 6, 1),
        template = { age, _ -> "Mars samples arrive on Earth — you'll be $age" }
    ),
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2036, 7, 1),
        template = { age, _ -> "Solar maximum 26 arrives — you'll be $age" }
    ),
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2050, 6, 1),
        template = { age, _ -> "More plastic than fish in the ocean by 2050 — you'll be $age" }
    ),
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2075, 6, 1),
        template = { age, _ -> "Half of coral reefs possibly gone by 2075 — you'll be $age" }
    ),
    Glance(
        axis = GlanceAxis.PERSONAL_ARC,
        eventDate = LocalDate.of(2022, 2, 14),
        template = { age, _ -> "The last speaker of Yagán passed when you were $age" }
    )
)

// ── Sequence composition ─────────────────────────────────────────────────────

// Axes that should not sit next to each other (whiplash too strong).
private val ADJACENCY_BAN: Set<Pair<GlanceAxis, GlanceAxis>> = setOf(
    GlanceAxis.PERSONAL_ARC to GlanceAxis.COSMIC,
    GlanceAxis.COSMIC to GlanceAxis.PERSONAL_ARC
)

private fun axesAreAdjacentlyBanned(a: GlanceAxis?, b: GlanceAxis): Boolean {
    if (a == null) return false
    if (a == b) return true
    return (a to b) in ADJACENCY_BAN
}

/**
 * Shuffle the catalogue and greedily re-order so no two adjacent entries share
 * an axis and PERSONAL_ARC never sits next to COSMIC. Then splice in a
 * SILENCE glance every [SILENCE_EVERY] positions. Falls back to a forced
 * insertion if no valid candidate remains.
 */
private fun composeSequence(
    source: List<Glance>,
    silenceEvery: Int = SILENCE_EVERY
): List<Glance> {
    val pool = source.shuffled().toMutableList()
    val out = mutableListOf<Glance>()
    while (pool.isNotEmpty()) {
        val last = out.lastOrNull()?.axis
        val pickIdx = pool.indexOfFirst { !axesAreAdjacentlyBanned(last, it.axis) }
        val next = if (pickIdx >= 0) pool.removeAt(pickIdx) else pool.removeAt(0)
        out += next
        if (out.size % silenceEvery == 0) {
            out += Glance(axis = GlanceAxis.SILENCE)
        }
    }
    return out
}

// ── Composable ───────────────────────────────────────────────────────────────

@Composable
internal fun NowMoment(
    topHeadline: RssFeedItem?,
    onHeadlineClick: (RssFeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Session-stable composed sequence (shuffled + axis-ordered + silence-spliced)
    val sequence = remember { composeSequence(glanceCatalogue) }

    var tick by remember { mutableIntStateOf(0) }

    val hasHeadline = topHeadline != null && topHeadline.title.isNotBlank()

    // Alternating logic: even ticks → headline (when available), odd ticks → next glance.
    val current: NowSource = remember(topHeadline, tick, sequence) {
        if (hasHeadline && tick % 2 == 0) {
            NowSource.Headline(topHeadline!!)
        } else {
            val gIdx = (if (hasHeadline) tick / 2 else tick) % sequence.size
            val g = sequence[gIdx]
            val rendered = g.render(USER_DOB)
            if (g.axis == GlanceAxis.SILENCE || rendered.isNullOrBlank()) NowSource.Silence
            else NowSource.GlanceText(rendered)
        }
    }

    // Note: LaunchedEffect coroutine outlives individual recompositions, so we can't
    // close over `current` here (captured stale). Re-derive from state each iteration.
    LaunchedEffect(topHeadline, sequence) {
        while (true) {
            val currentIsSilence = run {
                if (hasHeadline && tick % 2 == 0) false
                else {
                    val gIdx = (if (hasHeadline) tick / 2 else tick) % sequence.size
                    sequence[gIdx].axis == GlanceAxis.SILENCE
                }
            }
            delay(if (currentIsSilence) SILENCE_HOLD_MS else ROTATE_MS)
            tick++
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label: hidden on silence so the empty slot reads as intentional, not broken.
        val label = labelFor(current)
        if (label != null) {
            Text(
                text = label,
                style = ResponsiveTypography.t3.copy(fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Normal),
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f),
            )
        } else {
            // Reserve vertical space so the layout doesn't jump when silence lands.
            Spacer(Modifier.height(13.dp))
        }
        Crossfade(
            targetState = current,
            animationSpec = tween(durationMillis = 800),
            label = "now-moment"
        ) { snap ->
            when (snap) {
                is NowSource.Headline -> {
                    Text(
                        text = snap.item.title,
                        style = ResponsiveTypography.t1.copy(fontSize = 17.sp, lineHeight = 23.sp),
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onHeadlineClick(snap.item) }
                        )
                    )
                }
                is NowSource.GlanceText -> {
                    Text(
                        text = snap.text,
                        style = ResponsiveTypography.t1.copy(fontSize = 17.sp, lineHeight = 23.sp),
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                is NowSource.Silence -> {
                    // Two lines of breathing room matching the glance text height.
                    Spacer(Modifier.height(46.dp))
                }
            }
        }
    }
}

private fun labelFor(source: NowSource): String? = when (source) {
    is NowSource.Headline   -> "NOW"
    is NowSource.GlanceText -> "DEEP TIME"
    is NowSource.Silence    -> null
}
