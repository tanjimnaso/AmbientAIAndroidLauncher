package com.ambient.launcher.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    // Icon Overlay Opacity
    val iconOverlayOpacity: Float,

    // Derived/Optional (with defaults)
    val drawerBackground: Color = mainBackground,
    val chipBackground: Color = elevatedPanel,
    val cardFocus: Color = elevatedPanel
)

// ── Unified Palette Definitions (The "6 Blocks") ──

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
    iconOverlayOpacity = 0.0f
)

// Daytime-dim: used only within the day window when the room is genuinely dark
// (dim office, basement, heavy cloud cover). Chromatically NEUTRAL — cool slate,
// not warm — so it reads as "still daytime, just dim" rather than "sunset."
// Warm palettes are reserved for DUSK/TWILIGHT.
private val DayInteriorDimPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF1F2328),  // Cool neutral near-black slate
    panel          = Color(0xFF2A2F36),
    elevatedPanel  = Color(0xFF353B44),
    accentHigh     = Color(0xFFD4DBE3),  // Cool gray highlight
    textPrimary    = Color(0xFFF5F7FA),  // Crisp bright near-white
    textSecondary  = Color(0xFFB4B8BD),
    tileBackground = Color(0xFF2A2F36),
    errorAccent    = Color(0xFFE06C75),
    inkColor       = Color(0xFFF5F7FA),
    clusterIntelligence = HueIntelligence.copy(alpha = 0.75f),
    clusterUtility = HueUtility.copy(alpha = 0.75f),
    clusterCommunication = HueCommunication.copy(alpha = 0.75f),
    clusterAssistant = HueAssistant.copy(alpha = 0.75f),
    clusterHealth = HueHealth.copy(alpha = 0.75f),
    iconOverlayOpacity = 0.20f
)

private val TwilightPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF2B1710),  // Deep burnt umber (twilight earth)
    panel          = Color(0xFF3A2218),  // Lifted warm brown
    elevatedPanel  = Color(0xFF48291D),  // Toasted cocoa
    accentHigh     = Color(0xFFE8A066),  // Warm sienna-amber (lifted for dark)
    textPrimary    = Color(0xFFC2B299),  // Dimmed warm cream (was 0xFFF4E2C4)
    textSecondary  = Color(0xFF8C7A5E),  // Muted honey (was 0xFFBFA077)
    tileBackground = Color(0xFF3E2519),  // Just above panel
    errorAccent    = Color(0xFFD86B5A),  // Lifted rust
    inkColor       = Color(0xFFC2B299),  // Matches textPrimary
    clusterIntelligence  = Color(0xFF8AB0D9),  // Lifted inkwell blue
    clusterUtility       = Color(0xFF9FC088),  // Lifted olive
    clusterCommunication = Color(0xFF8AB0D9),
    clusterAssistant     = Color(0xFFC79FD1),  // Lifted aged plum
    clusterHealth        = Color(0xFFD88676),  // Lifted rust
    iconOverlayOpacity = 0.30f
)

private val DuskPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF2D2B33),  // Dusty plum-smoke ("sky 20m after sunset")
    panel          = Color(0xFF3A3742),  // Lifted plum
    elevatedPanel  = Color(0xFF46424F),  // Smoked iris
    accentHigh     = Color(0xFF9A8FB8),  // Muted iris
    textPrimary    = Color(0xFFE0DEF4),  // Soft lavender-white
    textSecondary  = Color(0xFF908EAA),  // Muted iris-grey
    tileBackground = Color(0xFF34323C),  // Near-bg tile
    errorAccent    = Color(0xFFC97A8A),  // Muted wine
    inkColor       = Color(0xFFE0DEF4),  // Matches textPrimary
    clusterIntelligence  = Color(0xFF7B91C4),  // Cool twilight blue
    clusterUtility       = Color(0xFF8AA397),  // Sage-grey
    clusterCommunication = Color(0xFF7B91C4),
    clusterAssistant     = Color(0xFFA98FB8),  // Velvet plum
    clusterHealth        = Color(0xFFC97A8A),
    iconOverlayOpacity = 0.30f
)

private val LocalAmbientPalette = staticCompositionLocalOf { DaylightOutdoorPalette }
private val LocalAmbientMode = staticCompositionLocalOf { AmbientMode.DAYLIGHT_OUTDOOR }

object AmbientTheme {
    val palette: AmbientPalette @Composable get() = LocalAmbientPalette.current
    val mode: AmbientMode @Composable get() = LocalAmbientMode.current
}

/**
 * Collapses all cluster hues to [inkColor] for a Kindle/Muji monochrome read.
 * Keeps the daily palette (background, text, accent) so transitions still feel seasonal.
 */
private fun AmbientPalette.toMonochrome(): AmbientPalette = copy(
    clusterIntelligence = inkColor,
    clusterUtility = inkColor,
    clusterCommunication = inkColor,
    clusterAssistant = inkColor,
    clusterHealth = inkColor
)

@Composable
fun animatePaletteAsState(target: AmbientPalette): AmbientPalette {
    val anim = tween<Color>(durationMillis = 1000)
    val floatAnim = tween<Float>(durationMillis = 1000)
    return AmbientPalette(
        isDark = target.isDark,
        mainBackground = animateColorAsState(target.mainBackground, anim).value,
        panel = animateColorAsState(target.panel, anim).value,
        elevatedPanel = animateColorAsState(target.elevatedPanel, anim).value,
        accentHigh = animateColorAsState(target.accentHigh, anim).value,
        textPrimary = animateColorAsState(target.textPrimary, anim).value,
        textSecondary = animateColorAsState(target.textSecondary, anim).value,
        tileBackground = animateColorAsState(target.tileBackground, anim).value,
        errorAccent = animateColorAsState(target.errorAccent, anim).value,
        inkColor = animateColorAsState(target.inkColor, anim).value,
        clusterIntelligence = animateColorAsState(target.clusterIntelligence, anim).value,
        clusterUtility = animateColorAsState(target.clusterUtility, anim).value,
        clusterCommunication = animateColorAsState(target.clusterCommunication, anim).value,
        clusterAssistant = animateColorAsState(target.clusterAssistant, anim).value,
        clusterHealth = animateColorAsState(target.clusterHealth, anim).value,
        iconOverlayOpacity = animateFloatAsState(target.iconOverlayOpacity, floatAnim).value,
        drawerBackground = animateColorAsState(target.drawerBackground, anim).value,
        chipBackground = animateColorAsState(target.chipBackground, anim).value,
        cardFocus = animateColorAsState(target.cardFocus, anim).value
    )
}

@Composable
fun AmbientLauncherTheme(content: @Composable () -> Unit) {
    val mode = rememberAmbientMode()
    val targetPalette = when(mode) {
        AmbientMode.DAYLIGHT_OUTDOOR -> DaylightOutdoorPalette
        AmbientMode.DAY_INTERIOR_DIM -> DayInteriorDimPalette
        AmbientMode.DUSK -> DuskPalette
        AmbientMode.TWILIGHT -> TwilightPalette
    }
    val monochrome by AmbientSettings.monochrome.collectAsStateWithLifecycle()
    val effectivePalette = if (monochrome) targetPalette.toMonochrome() else targetPalette
    val animatedPalette = animatePaletteAsState(effectivePalette)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
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

/**
 * Dynamically generates a Material3 [ColorScheme] based on the current [AmbientPalette].
 * This ensures standard components align with the ambient color state.
 */
private fun AmbientPalette.toColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = accentHigh,
        onPrimary = mainBackground,
        primaryContainer = elevatedPanel,
        onPrimaryContainer = textPrimary,
        secondary = accentHigh,
        onSecondary = mainBackground,
        secondaryContainer = panel,
        onSecondaryContainer = textPrimary,
        tertiary = clusterIntelligence,
        onTertiary = mainBackground,
        tertiaryContainer = panel,
        onTertiaryContainer = textPrimary,
        background = mainBackground,
        onBackground = textPrimary,
        surface = panel,
        onSurface = textPrimary,
        surfaceVariant = elevatedPanel,
        onSurfaceVariant = textSecondary,
        error = errorAccent,
        onError = mainBackground,
        outline = textSecondary.copy(alpha = 0.5f)
    )
} else {
    lightColorScheme(
        primary = accentHigh,
        onPrimary = Color.White,
        primaryContainer = elevatedPanel,
        onPrimaryContainer = textPrimary,
        secondary = accentHigh,
        onSecondary = Color.White,
        secondaryContainer = panel,
        onSecondaryContainer = textPrimary,
        tertiary = clusterIntelligence,
        onTertiary = Color.White,
        tertiaryContainer = panel,
        onTertiaryContainer = textPrimary,
        background = mainBackground,
        onBackground = textPrimary,
        surface = panel,
        onSurface = textPrimary,
        surfaceVariant = elevatedPanel,
        onSurfaceVariant = textSecondary,
        error = errorAccent,
        onError = Color.White,
        outline = textSecondary.copy(alpha = 0.5f)
    )
}

@Composable
private fun rememberAmbientMode(): AmbientMode {
    val context = LocalContext.current
    val signals = remember(context) { LightingSignals(context.applicationContext) }

    DisposableEffect(signals) {
        signals.start()
        onDispose { signals.stop() }
    }

    val mode by signals.mode.collectAsStateWithLifecycle()
    return mode
}
