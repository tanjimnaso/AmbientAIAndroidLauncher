package com.ambient.launcher.ui.theme

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.LocalTime

// Ambient mode: pure lux + time-of-day automatic resolution
enum class AmbientMode {
    DAYLIGHT_OUTDOOR, DAY_INTERIOR_HI, DUSK, TWILIGHT
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

private val DayInteriorHiPalette = AmbientPalette(
    isDark = true,
    mainBackground = Color(0xFF4B5158),  // Darker Slate Background
    panel = Color(0xFF565D65),          // Lifted surface, darker slate
    elevatedPanel = Color(0xFF636A73),  // Elevated slate
    accentHigh = Color(0xFFCED4DA),     // Pale gray highlight (replaces ink-accent)
    textPrimary = Color(0xFFF8F9FA),    // Off-white text
    textSecondary = Color(0xFFDEE2E6),  // Muted light gray text
    tileBackground = Color(0xFF565D65), // Matches panel
    errorAccent = Color(0xFFE06C75),    // Softened red
    inkColor = Color(0xFFF8F9FA),       // White/Off-white ink
    clusterIntelligence = HueIntelligence.copy(alpha = 0.7f),
    clusterUtility = HueUtility.copy(alpha = 0.7f),
    clusterCommunication = HueCommunication.copy(alpha = 0.7f),
    clusterAssistant = HueAssistant.copy(alpha = 0.7f),
    clusterHealth = HueHealth.copy(alpha = 0.7f),
    iconOverlayOpacity = 0.25f
)

private val TwilightPalette = AmbientPalette(
    isDark = false,
    mainBackground = Color(0xFFE8C896),  // Honeyed parchment (less saturated than current)
    panel          = Color(0xFFF4E2C4),  // Warm cream
    elevatedPanel  = Color(0xFFEACBA0),  // Toasted oat
    accentHigh     = Color(0xFF7C3F1D),  // Burnt sienna
    textPrimary    = Color(0xFF2E1A0B),  // Deep cocoa (NOT black)
    textSecondary  = Color(0xFF6B4423),  // Walnut
    tileBackground = Color(0xFFFAEED6),  // Pale parchment
    errorAccent    = Color(0xFF8B2E1F),  // Rust red
    inkColor       = Color(0xFF3A1F0D),
    clusterIntelligence  = Color(0xFF1F4E79),  // Inkwell blue
    clusterUtility       = Color(0xFF3D6B2E),  // Olive
    clusterCommunication = Color(0xFF1F4E79),
    clusterAssistant     = Color(0xFF6B3A7A),  // Aged plum
    clusterHealth        = Color(0xFF8B2E1F),
    iconOverlayOpacity = 0.40f
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
        AmbientMode.DAY_INTERIOR_HI -> DayInteriorHiPalette
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
    val modeState = remember { mutableStateOf(resolveAmbientMode(LocalTime.now(), 100f, AmbientMode.DAY_INTERIOR_HI)) }
    
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        
        val luxHistory = mutableListOf<Float>()
        val maxHistory = 8 
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent?) {
                e?.values?.get(0)?.let { lux ->
                    luxHistory.add(lux)
                    if (luxHistory.size > maxHistory) luxHistory.removeAt(0)
                    val smoothedLux = luxHistory.average().toFloat()
                    
                    val next = resolveAmbientMode(LocalTime.now(), smoothedLux, modeState.value)
                    if (next != modeState.value) {
                        modeState.value = next
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000) // Periodic time-of-day check
            val next = resolveAmbientMode(LocalTime.now(), 100f, modeState.value)
            if (next != modeState.value) {
                modeState.value = next
            }
        }
    }

    return modeState.value
}

private object LuxConfig {
    const val OUTDOOR_THRESHOLD = 2500f  // Actual sunlight/bright window
    const val OFFICE_THRESHOLD  = 500f   // Well-lit workspace
    const val DIM_THRESHOLD     = 80f    // Moody interior/evening lamps
    
    // Hysteresis buffer to prevent bouncing at the edges
    const val HYSTERESIS = 0.15f 
}

private fun resolveAmbientMode(time: LocalTime, lux: Float, current: AmbientMode): AmbientMode {
    val dayStart      = LocalTime.of(6, 30)
    val duskStart     = LocalTime.of(16, 30)
    val twilightStart = LocalTime.of(18, 30)

    // 1. High-Lux Sunlight (always takes priority)
    if (lux > LuxConfig.OUTDOOR_THRESHOLD) return AmbientMode.DAYLIGHT_OUTDOOR

    val isDay      = !time.isBefore(dayStart) && time.isBefore(duskStart)
    val isDusk     = !time.isBefore(duskStart) && time.isBefore(twilightStart)

    return when {
        isDay -> AmbientMode.DAY_INTERIOR_HI
        isDusk -> AmbientMode.DUSK
        else -> AmbientMode.TWILIGHT
    }
}
