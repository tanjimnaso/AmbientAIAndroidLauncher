package com.ambient.launcher.ui.theme

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import java.time.LocalTime

// Ambient mode: pure lux + time-of-day automatic resolution
enum class AmbientMode {
    DAYLIGHT_LIGHT,      // 200+ lux, Light mode (white bg, black text)
    DAYLIGHT_READING,    // 200+ lux, Reading mode (off-blue bg, off-white text)
    INDOOR_LIGHT,        // 10-100 lux, Light mode (yellow bg, black text)
    INDOOR_READING,      // 10-100 lux, Reading mode (dark grey bg, off-white text)
    LOWGLARE             // 0-10 lux, forced (black bg, low-brightness off-white)
}

data class AmbientPalette(
    val wallpaperScrimTop: Color,
    val wallpaperScrimMid: Color,
    val wallpaperScrimBottom: Color,
    val wallpaperGlow: Color,
    val panel: Color,
    val elevatedPanel: Color,
    val searchPanel: Color,
    val drawerBackground: Color,
    val chipBackground: Color,
    val closeButtonContainer: Color,
    val accentHalo: Color,
    val accentHigh: Color,
    
    // Core UI
    val textPrimary: Color,
    val textSecondary: Color,
    val tileBackground: Color,
    
    // Backgrounds
    val mainBackground: Color,
    val errorAccent: Color,

    // Project Cards
    val cardToday: Color,
    val cardFocus: Color,
    val cardReading: Color,
    val cardWallet: Color,

    // App Clusters
    val clusterIntelligence: Color,
    val clusterUtility: Color,
    val clusterAction: Color,
    val clusterCommunication: Color,
    val clusterAssistant: Color,
    val clusterHealth: Color
)

private data class AmbientThemeTokens(
    val mode: AmbientMode,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val palette: AmbientPalette
)

// ── DAYLIGHT_LIGHT: White background, black text (high contrast daytime) ──
private val DaylightLightColorScheme = lightColorScheme(
    primary = Color(0xFF1F2937),      // Dark grey (high contrast)
    secondary = Color(0xFF4B5563),
    tertiary = Color(0xFF065F73),
    background = Color(0xFFFFFFFF),   // Pure white
    surface = Color(0xFFF3F4F6),       // Off-white
    onBackground = Color(0xFF111827),  // Nearly black
    onSurface = Color(0xFF111827)
)

// ── DAYLIGHT_READING: Off-blue background, off-white text (Gemini-like, reduced contrast) ──
private val DaylightReadingColorScheme = lightColorScheme(
    primary = Color(0xFF1E40AF),      // Deep blue
    secondary = Color(0xFF3B82F6),    // Medium blue
    tertiary = Color(0xFF7C3AED),     // Purple
    background = Color(0xFFF0F4F8),   // Off-blue (very light)
    surface = Color(0xFFE0E7FF),       // Light blue tint
    onBackground = Color(0xFF374151), // Slate grey (reduced contrast)
    onSurface = Color(0xFF4B5563)
)

// ── INDOOR_LIGHT: Warm yellow background, black text ──
private val IndoorLightColorScheme = lightColorScheme(
    primary = Color(0xFF92400E),      // Warm brown
    secondary = Color(0xFFA16207),
    tertiary = Color(0xFF7C2D12),
    background = Color(0xFFFFFBEB),   // Soft warm cream-yellow
    surface = Color(0xFFFEF3C7),       // Light yellow
    onBackground = Color(0xFF78350F),  // Dark brown
    onSurface = Color(0xFF78350F)
)

// ── INDOOR_READING: Dark grey background, off-white text ──
private val IndoorReadingColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),      // Light blue
    secondary = Color(0xFFA5B4FC),    // Light purple
    tertiary = Color(0xFF86EFAC),     // Light green
    background = Color(0xFF1F2937),   // Dark grey
    surface = Color(0xFF2D3748),       // Slightly lighter grey
    onBackground = Color(0xFFF3F4F6), // Off-white
    onSurface = Color(0xFFE5E7EB)
)

// ── LOWGLARE: Black background, low-brightness off-white (minimal eye strain, night reading) ──
private val LowGlareColorScheme = darkColorScheme(
    primary = Color(0xFF808080),      // Muted grey (de-saturated)
    secondary = Color(0xFF696969),
    tertiary = Color(0xFF606060),
    background = Color(0xFF000000),   // Pure black
    surface = Color(0xFF1A1A1A),       // Very dark grey
    onBackground = Color(0xFFB0B0B0), // Dim off-white (reduced brightness)
    onSurface = Color(0xFFA0A0A0)
)

// ── DAYLIGHT_LIGHT Palette (200+ lux, Light mode): White bg, black text, high contrast ──
private val DaylightLightPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x33FFFFFF),
    wallpaperScrimMid = Color(0x66FFFFFF),
    wallpaperScrimBottom = Color(0x99FFFFFF),
    wallpaperGlow = Color(0x08000000),
    panel = Color(0xFFF3F4F6),
    elevatedPanel = Color(0xFFE5E7EB),
    searchPanel = Color(0xFFFFFFFF),
    drawerBackground = Color(0xFFFFFFFF),
    chipBackground = Color(0xFFE5E7EB),
    closeButtonContainer = Color(0xFFE5E7EB),
    accentHalo = Color(0x19000000),
    accentHigh = Color(0xFF065F73),    // Teal (high contrast on white)
    textPrimary = Color(0xFF111827),   // Nearly black
    textSecondary = Color(0xFF6B7280), // Dark grey
    tileBackground = Color(0xFFF9FAFB),
    mainBackground = Color(0xFFFFFFFF), // Pure white
    errorAccent = Color(0xFFDC2626),
    cardToday = Color(0xFFF3F4F6),
    cardFocus = Color(0xFFE5E7EB),
    cardReading = Color(0xFFF3F4F6),
    cardWallet = Color(0xFFE0F2FE),
    clusterIntelligence = Color(0xFF0EA5E9),
    clusterUtility = Color(0xFF22C55E),
    clusterAction = Color(0xFFEA580C),
    clusterCommunication = Color(0xFF3B82F6),
    clusterAssistant = Color(0xFF8B5CF6),
    clusterHealth = Color(0xFFDC2626)
)

// ── DAYLIGHT_READING Palette (200+ lux, Reading mode): Off-blue bg, slate-grey text, reduced contrast ──
private val DaylightReadingPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x33EFF6FF),
    wallpaperScrimMid = Color(0x66EFF6FF),
    wallpaperScrimBottom = Color(0x99EFF6FF),
    wallpaperGlow = Color(0x081E40AF),
    panel = Color(0xFFEDE9FE),
    elevatedPanel = Color(0xFFDDD6FE),
    searchPanel = Color(0xFFF0F4F8),
    drawerBackground = Color(0xFFF0F4F8),
    chipBackground = Color(0xFFDDD6FE),
    closeButtonContainer = Color(0xFFDDD6FE),
    accentHalo = Color(0x151E40AF),
    accentHigh = Color(0xFF1E40AF),    // Deep blue
    textPrimary = Color(0xFF374151),   // Slate grey (reduced contrast on blue)
    textSecondary = Color(0xFF64748B), // Lighter slate
    tileBackground = Color(0xFFF3F4F8),
    mainBackground = Color(0xFFF0F4F8), // Off-blue (Gemini-like)
    errorAccent = Color(0xFF9333EA),   // Soft purple
    cardToday = Color(0xFFEDE9FE),
    cardFocus = Color(0xFFDDD6FE),
    cardReading = Color(0xFFEDE9FE),
    cardWallet = Color(0xFFE0E7FF),
    clusterIntelligence = Color(0xFF1E40AF),
    clusterUtility = Color(0xFF059669),
    clusterAction = Color(0xFFA16207),
    clusterCommunication = Color(0xFF0369A1),
    clusterAssistant = Color(0xFF6D28D9),
    clusterHealth = Color(0xFFA4161A)
)

// ── INDOOR_LIGHT Palette (10-100 lux, Light mode): Warm yellow bg, black text ──
private val IndoorLightPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x33FFFAEB),
    wallpaperScrimMid = Color(0x66FFFAEB),
    wallpaperScrimBottom = Color(0x99FFFAEB),
    wallpaperGlow = Color(0x1092400E),
    panel = Color(0xFFFEF3C7),
    elevatedPanel = Color(0xFFFCD34D),
    searchPanel = Color(0xFFFFFBEB),
    drawerBackground = Color(0xFFFFFBEB),
    chipBackground = Color(0xFFFCD34D),
    closeButtonContainer = Color(0xFFFCD34D),
    accentHalo = Color(0x1992400E),
    accentHigh = Color(0xFFA16207),    // Warm brown
    textPrimary = Color(0xFF78350F),   // Dark brown
    textSecondary = Color(0xFFB45309), // Medium brown
    tileBackground = Color(0xFFFEF08A),
    mainBackground = Color(0xFFFFFBEB), // Soft warm cream-yellow
    errorAccent = Color(0xFFB91C1C),
    cardToday = Color(0xFFFEF3C7),
    cardFocus = Color(0xFFFCD34D),
    cardReading = Color(0xFFFEF3C7),
    cardWallet = Color(0xFFFFECB3),
    clusterIntelligence = Color(0xFF92400E),
    clusterUtility = Color(0xFF654321),
    clusterAction = Color(0xFFA16207),
    clusterCommunication = Color(0xFF8B4513),
    clusterAssistant = Color(0xFF92400E),
    clusterHealth = Color(0xFFB91C1C)
)

// ── INDOOR_READING Palette (10-100 lux, Reading mode): Dark grey bg, off-white text ──
private val IndoorReadingPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x331F2937),
    wallpaperScrimMid = Color(0x661F2937),
    wallpaperScrimBottom = Color(0x991F2937),
    wallpaperGlow = Color(0x1093C5FD),
    panel = Color(0xFF374151),
    elevatedPanel = Color(0xFF4B5563),
    searchPanel = Color(0xFF2D3748),
    drawerBackground = Color(0xFF1F2937),
    chipBackground = Color(0xFF4B5563),
    closeButtonContainer = Color(0xFF374151),
    accentHalo = Color(0x1593C5FD),
    accentHigh = Color(0xFF93C5FD),    // Light blue
    textPrimary = Color(0xFFF3F4F6),   // Off-white
    textSecondary = Color(0xFFE5E7EB), // Light grey
    tileBackground = Color(0xFF2D3748),
    mainBackground = Color(0xFF1F2937), // Dark grey
    errorAccent = Color(0xFFF87171),
    cardToday = Color(0xFF2D3748),
    cardFocus = Color(0xFF374151),
    cardReading = Color(0xFF2D3748),
    cardWallet = Color(0xFF1E3A5F),
    clusterIntelligence = Color(0xFF93C5FD),
    clusterUtility = Color(0xFF86EFAC),
    clusterAction = Color(0xFFFCA5A5),
    clusterCommunication = Color(0xFFA5B4FC),
    clusterAssistant = Color(0xFFDDD6FE),
    clusterHealth = Color(0xFFFCA5A5)
)

// ── LOWGLARE Palette (0-10 lux, forced): Black bg, low-brightness off-white (minimal eye strain) ──
private val LowGlarePalette = AmbientPalette(
    wallpaperScrimTop = Color(0x33000000),
    wallpaperScrimMid = Color(0x66000000),
    wallpaperScrimBottom = Color(0x99000000),
    wallpaperGlow = Color(0x04808080),
    panel = Color(0xFF262626),
    elevatedPanel = Color(0xFF3A3A3A),
    searchPanel = Color(0xFF1A1A1A),
    drawerBackground = Color(0xFF000000),
    chipBackground = Color(0xFF3A3A3A),
    closeButtonContainer = Color(0xFF262626),
    accentHalo = Color(0x08808080),
    accentHigh = Color(0xFF808080),    // Muted grey (de-saturated)
    textPrimary = Color(0xFFB0B0B0),   // Dim off-white (low eye strain)
    textSecondary = Color(0xFF909090), // Even dimmer grey
    tileBackground = Color(0xFF1A1A1A),
    mainBackground = Color(0xFF000000), // Pure black
    errorAccent = Color(0xFF909090),   // Very muted (no bright reds)
    cardToday = Color(0xFF262626),
    cardFocus = Color(0xFF3A3A3A),
    cardReading = Color(0xFF262626),
    cardWallet = Color(0xFF1A2A3A),
    clusterIntelligence = Color(0xFF707070),
    clusterUtility = Color(0xFF707070),
    clusterAction = Color(0xFF707070),
    clusterCommunication = Color(0xFF707070),
    clusterAssistant = Color(0xFF707070),
    clusterHealth = Color(0xFF707070)
)

private val LocalAmbientPalette = staticCompositionLocalOf { LowGlarePalette }
private val LocalAmbientMode = staticCompositionLocalOf { AmbientMode.LOWGLARE }

object AmbientTheme {
    val palette: AmbientPalette
        @Composable get() = LocalAmbientPalette.current

    val mode: AmbientMode
        @Composable get() = LocalAmbientMode.current
}

@Composable
fun animatePaletteAsState(targetPalette: AmbientPalette): AmbientPalette {
    val animationSpec = tween<Color>(durationMillis = 1000) // Slow fade

    return AmbientPalette(
        wallpaperScrimTop = animateColorAsState(targetPalette.wallpaperScrimTop, animationSpec).value,
        wallpaperScrimMid = animateColorAsState(targetPalette.wallpaperScrimMid, animationSpec).value,
        wallpaperScrimBottom = animateColorAsState(targetPalette.wallpaperScrimBottom, animationSpec).value,
        wallpaperGlow = animateColorAsState(targetPalette.wallpaperGlow, animationSpec).value,
        panel = animateColorAsState(targetPalette.panel, animationSpec).value,
        elevatedPanel = animateColorAsState(targetPalette.elevatedPanel, animationSpec).value,
        searchPanel = animateColorAsState(targetPalette.searchPanel, animationSpec).value,
        drawerBackground = animateColorAsState(targetPalette.drawerBackground, animationSpec).value,
        chipBackground = animateColorAsState(targetPalette.chipBackground, animationSpec).value,
        closeButtonContainer = animateColorAsState(targetPalette.closeButtonContainer, animationSpec).value,
        accentHalo = animateColorAsState(targetPalette.accentHalo, animationSpec).value,
        accentHigh = animateColorAsState(targetPalette.accentHigh, animationSpec).value,
        textPrimary = animateColorAsState(targetPalette.textPrimary, animationSpec).value,
        textSecondary = animateColorAsState(targetPalette.textSecondary, animationSpec).value,
        tileBackground = animateColorAsState(targetPalette.tileBackground, animationSpec).value,
        mainBackground = animateColorAsState(targetPalette.mainBackground, animationSpec).value,
        errorAccent = animateColorAsState(targetPalette.errorAccent, animationSpec).value,
        cardToday = animateColorAsState(targetPalette.cardToday, animationSpec).value,
        cardFocus = animateColorAsState(targetPalette.cardFocus, animationSpec).value,
        cardReading = animateColorAsState(targetPalette.cardReading, animationSpec).value,
        cardWallet = animateColorAsState(targetPalette.cardWallet, animationSpec).value,
        clusterIntelligence = animateColorAsState(targetPalette.clusterIntelligence, animationSpec).value,
        clusterUtility = animateColorAsState(targetPalette.clusterUtility, animationSpec).value,
        clusterAction = animateColorAsState(targetPalette.clusterAction, animationSpec).value,
        clusterCommunication = animateColorAsState(targetPalette.clusterCommunication, animationSpec).value,
        clusterAssistant = animateColorAsState(targetPalette.clusterAssistant, animationSpec).value,
        clusterHealth = animateColorAsState(targetPalette.clusterHealth, animationSpec).value
    )
}

@Composable
fun AmbientLauncherTheme(
    mode: AmbientMode = rememberAmbientMode(),
    content: @Composable () -> Unit
) {
    val tokens = ambientThemeTokensFor(mode)
    val animatedPalette = animatePaletteAsState(tokens.palette)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !tokens.isDark
                isAppearanceLightNavigationBars = !tokens.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAmbientPalette provides animatedPalette,
        LocalAmbientMode provides tokens.mode
    ) {
        MaterialTheme(
            colorScheme = tokens.colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
private fun rememberAmbientMode(): AmbientMode {
    val context = LocalContext.current
    return produceState(initialValue = resolveAmbientMode(LocalTime.now(), 100f, AmbientMode.LOWGLARE)) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        var currentLux = 100f // Default to indoor brightness
        var lastMode = value

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.get(0)?.let { lux ->
                    currentLux = lux
                    val nextMode = resolveAmbientMode(LocalTime.now(), currentLux, lastMode)
                    if (nextMode != lastMode) {
                        lastMode = nextMode
                        value = nextMode
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }

        try {
            while (true) {
                val nextMode = resolveAmbientMode(LocalTime.now(), currentLux, lastMode)
                if (nextMode != lastMode) {
                    lastMode = nextMode
                    value = nextMode
                }
                delay(30_000)
            }
        } finally {
            sensorManager.unregisterListener(listener)
        }
    }.value
}

private fun ambientThemeTokensFor(mode: AmbientMode): AmbientThemeTokens {
    return when (mode) {
        AmbientMode.DAYLIGHT_LIGHT -> AmbientThemeTokens(mode, false, DaylightLightColorScheme, DaylightLightPalette)
        AmbientMode.DAYLIGHT_READING -> AmbientThemeTokens(mode, false, DaylightReadingColorScheme, DaylightReadingPalette)
        AmbientMode.INDOOR_LIGHT -> AmbientThemeTokens(mode, false, IndoorLightColorScheme, IndoorLightPalette)
        AmbientMode.INDOOR_READING -> AmbientThemeTokens(mode, true, IndoorReadingColorScheme, IndoorReadingPalette)
        AmbientMode.LOWGLARE -> AmbientThemeTokens(mode, true, LowGlareColorScheme, LowGlarePalette)
    }
}

private fun resolveAmbientMode(time: LocalTime, lux: Float, currentMode: AmbientMode): AmbientMode {
    val dayStart = LocalTime.of(7, 30)
    val nightStart = LocalTime.of(17, 30)
    val isDaytime = !time.isBefore(dayStart) && time.isBefore(nightStart)

    // 0–10 lux: Always low-glare (forced, no exceptions)
    // Hysteresis: enter at < 10, exit at > 15
    val isLowLight = if (currentMode == AmbientMode.LOWGLARE) lux < 15f else lux < 10f
    if (isLowLight) return AmbientMode.LOWGLARE

    // 10–100 lux: Indoor reading (reduced contrast)
    // Hysteresis: enter at < 100, exit at > 150
    val isMediumLight = if (currentMode in setOf(AmbientMode.DAYLIGHT_READING, AmbientMode.INDOOR_READING)) lux < 150f else lux < 100f
    if (isMediumLight) {
        return if (isDaytime) AmbientMode.DAYLIGHT_READING else AmbientMode.INDOOR_READING
    }

    // 200+ lux: Bright light (high contrast)
    // Hysteresis: enter at > 100, exit at < 50
    return if (isDaytime) AmbientMode.DAYLIGHT_LIGHT else AmbientMode.INDOOR_LIGHT
}
