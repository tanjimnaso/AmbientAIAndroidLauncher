package com.ambient.launcher.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ── Models & State ────────────────────────────────────────────────────────────

// Ambient mode: time-of-day gates the window, lux refines day-hour split,
// CameraLuxSampler disambiguates when the front lux sensor is suspect.
enum class AmbientMode {
    DAYLIGHT_OUTDOOR, DAY_INTERIOR_DIM, DUSK, TWILIGHT
}

data class AmbientPalette(
    val isDark: Boolean,
    val mainBackground: Color,
    val panel: Color,
    val elevatedPanel: Color,
    val accentHigh: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val tileBackground: Color,
    val errorAccent: Color,
    val inkColor: Color,

    // App Clusters
    val clusterIntelligence: Color,
    val clusterUtility: Color,
    val clusterCommunication: Color,
    val clusterAssistant: Color,
    val clusterHealth: Color,

    // Secondary Chrome / Ambient Reveal
    val iconOverlayOpacity: Float,
    val primaryOpacity: Float,
    val secondaryOpacity: Float,
    val actionBackgroundOpacity: Float,

    // Derived/Optional (with defaults)
    val drawerBackground: Color = mainBackground,
    val chipBackground: Color = elevatedPanel,
    val cardFocus: Color = elevatedPanel
)

// ── Theme Public API ──────────────────────────────────────────────────────────

private val LocalAmbientPalette = staticCompositionLocalOf { DaylightOutdoorPalette }
private val LocalAmbientMode = staticCompositionLocalOf { AmbientMode.DAYLIGHT_OUTDOOR }

object AmbientTheme {
    val palette: AmbientPalette @Composable get() = LocalAmbientPalette.current
    val mode: AmbientMode @Composable get() = LocalAmbientMode.current
}

@Composable
fun AmbientLauncherTheme(content: @Composable () -> Unit) {
    val (mode, lux) = rememberAmbientSignals()
    val basePalette = when (mode) {
        AmbientMode.DAYLIGHT_OUTDOOR -> DaylightOutdoorPalette
        AmbientMode.DAY_INTERIOR_DIM -> DayInteriorDimPalette
        AmbientMode.DUSK -> DuskPalette
        AmbientMode.TWILIGHT -> TwilightPalette
    }
    
    val targetPalette = basePalette.modulateForLux(lux, mode)
    // val monochrome by AmbientSettings.monochrome.collectAsStateWithLifecycle()
    // val effectivePalette = if (monochrome) targetPalette.toMonochrome() else targetPalette
    val effectivePalette = targetPalette
    val animatedPalette = animatePaletteAsState(effectivePalette)
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !animatedPalette.isDark
                isAppearanceLightNavigationBars = !animatedPalette.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAmbientPalette provides animatedPalette,
        LocalAmbientMode provides mode
    ) {
        MaterialTheme(
            colorScheme = animatedPalette.toColorScheme(),
            typography = Typography,
            content = content
        )
    }
}

// ── Helper Extensions (Tinting) ────────────────────────────────────────────────

/** RSS source tint: looks up the base hue, applies the mode transform. */
fun AmbientPalette.sourceTint(source: String): Color {
    val hue = SourceHueTable
        .firstOrNull { source.contains(it.first, ignoreCase = true) }
        ?.second
        ?: return textSecondary
    return tintForChannel(hue)
}

/** Season tint: same pattern as [sourceTint], keyed by season name. */
fun AmbientPalette.seasonTint(season: String): Color {
    val hue = SeasonHueTable[season] ?: return accentHigh
    return tintForChannel(hue)
}

/**
 * Per-mode hue transform. Light backgrounds get a darkened variant so the
 * source color holds contrast; dark backgrounds keep the hue vibrant. Warm
 * palettes (dusk/twilight) also get a touch of desaturation so a bright
 * electric blue doesn't fight the amber ambience.
 */
private fun AmbientPalette.tintForChannel(hue: Color): Color {
    if (!isDark) {
        val bgLuminance = mainBackground.red 
        val shift = lerpFloat(-0.6f, 0f, ((bgLuminance - 0.5f) / 0.5f).coerceIn(0f, 1f))
        return hue.shiftLightness(shift)
    }
    // Differentiate between cool-dark (Dusk Purple) and warm-dark (Twilight Amber)
    val isCool = (mainBackground.blue > mainBackground.red) || (textPrimary.blue > textPrimary.red)
    return if (isCool) hue else hue.shiftSaturation(-0.15f)
}

// ── Color Constants & Palettes ────────────────────────────────────────────────

// Core Hues (The "Syntax" of the Information)
private val HueIntelligence  = Color(0xFF0EA5E9) // Sky
private val HueUtility       = Color(0xFF22C55E) // Green
private val HueCommunication = Color(0xFF3B82F6) // Blue
private val HueAssistant     = Color(0xFF8B5CF6) // Violet
private val HueHealth        = Color(0xFFDC2626) // Red

private val DaylightOutdoorPalette = AmbientPalette(
    isDark = false,
    mainBackground = Color(0xFFFFFFFF), // High-visibility White
    panel = Color(0xFFF3F4F6),
    elevatedPanel = Color(0xFFE5E7EB),
    accentHigh = Color(0xFF065F73),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    tileBackground = Color(0xFFF9FAFB),
    errorAccent = Color(0xFFDC2626),
    inkColor = Color(0xFF333333), // Charcoal
    clusterIntelligence = HueIntelligence,
    clusterUtility = HueUtility,
    clusterCommunication = HueCommunication,
    clusterAssistant = HueAssistant,
    clusterHealth = HueHealth,
    iconOverlayOpacity = 0.0f,
    primaryOpacity = 1.0f,
    secondaryOpacity = 1.0f,
    actionBackgroundOpacity = 0.0f
)

// Daytime-dim: used only within the day window when the room is genuinely dark
private val DayInteriorDimPalette = AmbientPalette(
    isDark = true, // Though mid-tone, it acts as a dark theme for system bars
    mainBackground = Color(0xFF555B63), // Slate Gray base
    panel          = Color(0xFF4C5158), // Slightly darker for depth without shadows
    elevatedPanel  = Color(0xFF626972), // Lighter slate for cards/chips
    accentHigh     = Color(0xFFEAEAEA), // Crisp off-white for high legibility
    textPrimary    = Color(0xFFF4F5F7), // Very bright text for contrast against slate
    textSecondary  = Color(0xFFAAB0B7), // Muted stone gray
    tileBackground = Color(0xFF4C5158),
    errorAccent    = Color(0xFFE06C75),
    inkColor       = Color(0xFFF4F5F7),
    clusterIntelligence = HueIntelligence.shiftLightness(0.1f),
    clusterUtility      = HueUtility.shiftLightness(0.1f),
    clusterCommunication= HueCommunication.shiftLightness(0.1f),
    clusterAssistant    = HueAssistant.shiftLightness(0.1f),
    clusterHealth       = HueHealth.shiftLightness(0.1f),
    iconOverlayOpacity = 0.10f,
    primaryOpacity = 1.0f,
    secondaryOpacity = 0.85f,
    actionBackgroundOpacity = 0.08f
)

private val DuskPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF22242B), // Deep charcoal with a hint of blue/purple
    panel          = Color(0xFF1A1C23), // Darker well for grouped content
    elevatedPanel  = Color(0xFF2C2F38), // Soft elevation
    accentHigh     = Color(0xFFB589D6), // Soft IDE pastel purple
    textPrimary    = Color(0xFFE2E4E9), // Soft white/gray, easy on the eyes
    textSecondary  = Color(0xFF8B92A5), // Muted blue-gray comments
    tileBackground = Color(0xFF1A1C23),
    errorAccent    = Color(0xFFE06C75),
    inkColor       = Color(0xFFE2E4E9),
    clusterIntelligence  = Color(0xFF7CB7FF), // Soft IDE blue
    clusterUtility       = Color(0xFF8CD8A7), // Soft IDE green
    clusterCommunication = Color(0xFF7CB7FF),
    clusterAssistant     = Color(0xFFCBA6F7), // Soft IDE violet
    clusterHealth        = Color(0xFFF38BA8), // Soft IDE red
    iconOverlayOpacity = 0.12f,
    primaryOpacity = 0.95f,
    secondaryOpacity = 0.80f,
    actionBackgroundOpacity = 0.10f
)


private val TwilightPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF161412), // Very dark warm charcoal
    panel          = Color(0xFF221F1C), // Slight lift for depth
    elevatedPanel  = Color(0xFF2D2925), // Brightest surface
    accentHigh     = Color(0xFFD4A373), // Muted amber for primary accents
    textPrimary    = Color(0xFFE6CCB2), // Soft sand for readable text
    textSecondary  = Color(0xFF9C8A7C), // Dimmed earth tone
    tileBackground = Color(0xFF221F1C),
    errorAccent    = Color(0xFFD9534F),
    inkColor       = Color(0xFFE6CCB2),
    clusterIntelligence  = Color(0xFF8B7355),
    clusterUtility       = Color(0xFF7A6B53),
    clusterCommunication = Color(0xFF8B7355),
    clusterAssistant     = Color(0xFF967D69),
    clusterHealth        = Color(0xFFA67B71),
    iconOverlayOpacity = 0.15f,
    primaryOpacity = 0.90f,
    secondaryOpacity = 0.75f,
    actionBackgroundOpacity = 0.10f
)


// ── Modulation Logic ─────────────────────────────────────────────────────────

private const val OUTDOOR_LUX_FLOOR = 800f
private const val OUTDOOR_LUX_CEILING = 2500f
private val OUTDOOR_BG_DIM = Color(0xFFE2E8F0)
private val OUTDOOR_BG_BRIGHT = Color(0xFFFFFFFF)
private val OUTDOOR_PANEL_DIM = Color(0xFFF1F5F9)
private val OUTDOOR_PANEL_BRIGHT = Color(0xFFF3F4F6)
private val OUTDOOR_ELEVATED_DIM = Color(0xFFE2E8F0)
private val OUTDOOR_ELEVATED_BRIGHT = Color(0xFFE5E7EB)
private const val OUTDOOR_ICON_OPACITY_DIM = 0.15f
private const val OUTDOOR_ICON_OPACITY_BRIGHT = 0.0f

private fun AmbientPalette.modulateForLux(lux: Float, mode: AmbientMode): AmbientPalette {
    return when (mode) {
        AmbientMode.DAYLIGHT_OUTDOOR -> {
            val range = OUTDOOR_LUX_CEILING - OUTDOOR_LUX_FLOOR
            val t = ((lux - OUTDOOR_LUX_FLOOR) / range).coerceIn(0f, 1f)
            copy(
                mainBackground     = lerp(OUTDOOR_BG_DIM,       OUTDOOR_BG_BRIGHT,       t),
                drawerBackground   = lerp(OUTDOOR_BG_DIM,       OUTDOOR_BG_BRIGHT,       t),
                panel              = lerp(OUTDOOR_PANEL_DIM,    OUTDOOR_PANEL_BRIGHT,    t),
                elevatedPanel      = lerp(OUTDOOR_ELEVATED_DIM, OUTDOOR_ELEVATED_BRIGHT, t),
                tileBackground     = lerp(OUTDOOR_ELEVATED_DIM, OUTDOOR_ELEVATED_BRIGHT, t),
                chipBackground     = lerp(OUTDOOR_ELEVATED_DIM, OUTDOOR_ELEVATED_BRIGHT, t),
                cardFocus          = lerp(OUTDOOR_ELEVATED_DIM, OUTDOOR_ELEVATED_BRIGHT, t),
                // Keep text stark dark for high visibility
                textSecondary      = lerp(Color(0xFF202429), Color(0xFF4B5563), t),
                iconOverlayOpacity = lerpFloat(OUTDOOR_ICON_OPACITY_DIM, OUTDOOR_ICON_OPACITY_BRIGHT, t)
            )
        }
        AmbientMode.DAY_INTERIOR_DIM -> {
            // Very subtle background tuning, no text dimming.
            // The flat stone look relies on exact hex values, so we barely touch it.
            val t = (lux / 200f).coerceIn(0f, 1f)
            val overlayBoost = lerpFloat(0.05f, 0f, t)

            copy(
                // Max dimming is now only 3% instead of 12%, preventing washout
                mainBackground     = mainBackground.shiftLightness(lerpFloat(-0.03f, 0f, t)),
                iconOverlayOpacity = (iconOverlayOpacity + overlayBoost).coerceIn(0f, 0.3f)
            )
        }
        AmbientMode.DUSK -> {
            val t = (lux / 200f).coerceIn(0f, 1f)
            val overlayBoost = lerpFloat(0.08f, 0f, t)

            copy(
                // Drop the background dimming entirely to keep the dark IDE look crisp.
                // We only shift opacities so UI elements settle smoothly.
                iconOverlayOpacity = (iconOverlayOpacity + overlayBoost).coerceIn(0f, 0.3f),
                actionBackgroundOpacity = (actionBackgroundOpacity + lerpFloat(-0.04f, 0f, t)).coerceIn(0f, 1f)
            )
        }
        AmbientMode.TWILIGHT -> {
            // Twilight is the "Sand" theme.
            // 0-10 lux: Keep it pure Sand (no dimming) to match the Theme Park look.
            // Beyond 10 lux: Transition towards DayInteriorDim (Slate) for a crisper indoor look.
            if (lux <= 10f) return this

            val t = ((lux - 10f) / 70f).coerceIn(0f, 1f) // Fully Slate by 80 lux
            lerpPalette(this, DayInteriorDimPalette, t)
        }
    }
}

private fun lerpPalette(a: AmbientPalette, b: AmbientPalette, t: Float): AmbientPalette {
    return a.copy(
        isDark = if (t > 0.5f) b.isDark else a.isDark,
        mainBackground = lerp(a.mainBackground, b.mainBackground, t),
        panel = lerp(a.panel, b.panel, t),
        elevatedPanel = lerp(a.elevatedPanel, b.elevatedPanel, t),
        accentHigh = lerp(a.accentHigh, b.accentHigh, t),
        textPrimary = lerp(a.textPrimary, b.textPrimary, t),
        textSecondary = lerp(a.textSecondary, b.textSecondary, t),
        tileBackground = lerp(a.tileBackground, b.tileBackground, t),
        errorAccent = lerp(a.errorAccent, b.errorAccent, t),
        inkColor = lerp(a.inkColor, b.inkColor, t),
        clusterIntelligence = lerp(a.clusterIntelligence, b.clusterIntelligence, t),
        clusterUtility = lerp(a.clusterUtility, b.clusterUtility, t),
        clusterCommunication = lerp(a.clusterCommunication, b.clusterCommunication, t),
        clusterAssistant = lerp(a.clusterAssistant, b.clusterAssistant, t),
        clusterHealth = lerp(a.clusterHealth, b.clusterHealth, t),
        iconOverlayOpacity = lerpFloat(a.iconOverlayOpacity, b.iconOverlayOpacity, t),
        primaryOpacity = lerpFloat(a.primaryOpacity, b.primaryOpacity, t),
        secondaryOpacity = lerpFloat(a.secondaryOpacity, b.secondaryOpacity, t),
        actionBackgroundOpacity = lerpFloat(a.actionBackgroundOpacity, b.actionBackgroundOpacity, t),
        drawerBackground = lerp(a.drawerBackground, b.drawerBackground, t),
        chipBackground = lerp(a.chipBackground, b.chipBackground, t),
        cardFocus = lerp(a.cardFocus, b.cardFocus, t)
    )
}


@Composable
private fun rememberAmbientSignals(): Pair<AmbientMode, Float> {
    val context = LocalContext.current
    val signals = remember(context) { LightingSignals(context.applicationContext) }

    DisposableEffect(signals) {
        signals.start()
        onDispose { signals.stop() }
    }

    val mode by signals.mode.collectAsStateWithLifecycle()
    val lux  by signals.lux.collectAsStateWithLifecycle()
    return mode to lux
}

// ── Lookup Tables ─────────────────────────────────────────────────────────────

private val SourceHueTable: List<Pair<String, Color>> = listOf(
    "Daring Fireball"      to Color(0xFF6B8CFF),
    "Ars Technica"         to Color(0xFFFF6633),
    "Hacker News"          to Color(0xFFFF6633),
    "Quanta"               to Color(0xFF00A4EF),
    "STAT"                 to Color(0xFFE06C75),
    "Arts & Letters Daily" to Color(0xFFD4AF37),
    "Noema"                to Color(0xFF98C379),
    "3 Quarks"             to Color(0xFFC678DD),
    "Harper"               to Color(0xFF56B6C2),
    "Marginalian"          to Color(0xFFE5C07B),
    "Marginal Revolution"  to Color(0xFF98C379),
    "BBC"                  to Color(0xFF6B8CFF),
    "Guardian"             to Color(0xFF999999),
    "Reuters"              to Color(0xFFFFA500),
    "Associated Press"     to Color(0xFF00A4EF),
    "NPR"                  to Color(0xFFCC0000),
    "Politico"             to Color(0xFFE81B23)
)

private val SeasonHueTable: Map<String, Color> = mapOf(
    "Spring" to Color(0xFF4CAF50),
    "Summer" to Color(0xFFFFC107),
    "Autumn" to Color(0xFFFF9800),
    "Winter" to Color(0xFF2196F3)
)

// ── Color Math & Animation ────────────────────────────────────────────────────

private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun Color.shiftLightness(delta: Float): Color {
    val factor = 1f + delta
    return if (delta < 0f) Color(
        red   = (red   * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue  = (blue  * factor).coerceIn(0f, 1f),
        alpha = alpha
    ) else Color(
        red   = (red   + (1f - red)   * delta).coerceIn(0f, 1f),
        green = (green + (1f - green) * delta).coerceIn(0f, 1f),
        blue  = (blue  + (1f - blue)  * delta).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.shiftSaturation(delta: Float): Color {
    val gray = 0.299f * red + 0.587f * green + 0.114f * blue
    val k = (-delta).coerceIn(0f, 1f)
    return Color(
        red   = (red   + (gray - red)   * k).coerceIn(0f, 1f),
        green = (green + (gray - green) * k).coerceIn(0f, 1f),
        blue  = (blue  + (gray - blue)  * k).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun AmbientPalette.toMonochrome(): AmbientPalette = copy(
    clusterIntelligence = inkColor,
    clusterUtility = inkColor,
    clusterCommunication = inkColor,
    clusterAssistant = inkColor,
    clusterHealth = inkColor
)

private fun AmbientPalette.toColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = accentHigh,
            onPrimary = mainBackground,
            secondary = clusterIntelligence,
            background = mainBackground,
            surface = panel,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            error = errorAccent
        )
    } else {
        lightColorScheme(
            primary = accentHigh,
            onPrimary = Color.White,
            secondary = clusterIntelligence,
            background = mainBackground,
            surface = panel,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            error = errorAccent
        )
    }
}

@Composable
private fun animatePaletteAsState(target: AmbientPalette): AmbientPalette {
    val paletteAnim = tween<Color>(durationMillis = 1000)
    val mainBackground = animateColorAsState(target.mainBackground, paletteAnim, label="bg").value
    val panel = animateColorAsState(target.panel, paletteAnim, label="panel").value
    val elevatedPanel = animateColorAsState(target.elevatedPanel, paletteAnim, label="elevated").value
    val tileBackground = animateColorAsState(target.tileBackground, paletteAnim, label="tileBg").value
    val drawerBackground = animateColorAsState(target.drawerBackground, paletteAnim, label="drawerBg").value
    val chipBackground = animateColorAsState(target.chipBackground, paletteAnim, label="chipBg").value
    val cardFocus = animateColorAsState(target.cardFocus, paletteAnim, label="cardFocus").value

    val fgAnim = tween<Color>(durationMillis = 800)
    val floatAnim = tween<Float>(durationMillis = 1000)

    val textPrimary = animateColorAsState(target.textPrimary, fgAnim, label="txt1").value
    val textSecondary = animateColorAsState(target.textSecondary, fgAnim, label="txt2").value
    val accentHigh = animateColorAsState(target.accentHigh, fgAnim, label="accent").value
    val errorAccent = animateColorAsState(target.errorAccent, fgAnim, label="error").value
    val inkColor = animateColorAsState(target.inkColor, fgAnim, label="ink").value

    val clusterIntelligence = animateColorAsState(target.clusterIntelligence, fgAnim, label="c1").value
    val clusterUtility = animateColorAsState(target.clusterUtility, fgAnim, label="c2").value
    val clusterCommunication = animateColorAsState(target.clusterCommunication, fgAnim, label="c3").value
    val clusterAssistant = animateColorAsState(target.clusterAssistant, fgAnim, label="c4").value
    val clusterHealth = animateColorAsState(target.clusterHealth, fgAnim, label="c5").value

    val iconOverlayOpacity by animateFloatAsState(target.iconOverlayOpacity, floatAnim, label="op1")
    val primaryOpacity by animateFloatAsState(target.primaryOpacity, floatAnim, label="op2")
    val secondaryOpacity by animateFloatAsState(target.secondaryOpacity, floatAnim, label="op3")
    val actionBackgroundOpacity by animateFloatAsState(target.actionBackgroundOpacity, floatAnim, label="op4")

    return AmbientPalette(
        isDark = target.isDark,
        mainBackground = mainBackground,
        panel = panel,
        elevatedPanel = elevatedPanel,
        tileBackground = tileBackground,
        drawerBackground = drawerBackground,
        chipBackground = chipBackground,
        cardFocus = cardFocus,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        accentHigh = accentHigh,
        errorAccent = errorAccent,
        inkColor = inkColor,
        clusterIntelligence = clusterIntelligence,
        clusterUtility = clusterUtility,
        clusterCommunication = clusterCommunication,
        clusterAssistant = clusterAssistant,
        clusterHealth = clusterHealth,
        iconOverlayOpacity = iconOverlayOpacity,
        primaryOpacity = primaryOpacity,
        secondaryOpacity = secondaryOpacity,
        actionBackgroundOpacity = actionBackgroundOpacity
    )
}
