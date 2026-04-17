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
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import java.time.LocalTime

enum class AmbientMode {
    EARLY_MORNING,
    DAY,
    BLUE_HOUR,
    READING
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

// Earthy, Muted Palettes
private val EarthySurface = Color(0xFF1C1B1A)
private val EarthyInk = Color(0xFF141312)
private val MutedClay = Color(0xFF3D3A36)
private val MutedSage = Color(0xFF4A4D45)
private val MutedOchre = Color(0xFF6B6458)
private val MutedText = Color(0xFFD6D3CD)
private val MutedAccent = Color(0xFFB8A690)

// Atom One Dark (bright light at night / early dawn): crisp, IDE-calm navy
private val EarlyMorningColorScheme = darkColorScheme(
    primary = Color(0xFF61AFEF),   // Atom blue
    secondary = Color(0xFFABB2BF),
    tertiary = Color(0xFF98C379),  // Atom green
    background = Color(0xFF21252B),
    surface = Color(0xFF282C34),
    onBackground = Color(0xFFABB2BF),
    onSurface = Color(0xFFABB2BF)
)

// Atom One Dark (IDE-inspired): deep navy, cool-tinted, high readability
private val BlueHourColorScheme = darkColorScheme(
    primary = Color(0xFF61AFEF),   // Atom blue
    secondary = Color(0xFFABB2BF), // Atom text grey
    tertiary = Color(0xFF98C379),  // Atom green
    background = Color(0xFF21252B),
    surface = Color(0xFF282C34),
    onBackground = Color(0xFFABB2BF),
    onSurface = Color(0xFFABB2BF)
)

// Warm Earthy Night (dark ambient, any time): cozy, warm dark for reading in the dark
private val ReadingColorScheme = darkColorScheme(
    primary = Color(0xFFD4A574),   // Warm rose-gold
    secondary = Color(0xFFE8E5DC),
    tertiary = Color(0xFFC89B6F),
    background = EarthyInk,
    surface = EarthySurface,
    onBackground = Color(0xFFE8E5DC),
    onSurface = Color(0xFFE8E5DC)
)

// Quiet Light (VSCode-inspired): warm cream, low contrast, easy on eyes
private val DayColorScheme = lightColorScheme(
    primary = Color(0xFF7A5C3E),   // Warm brown
    secondary = Color(0xFF5C6E5B), // Muted sage green
    tertiary = Color(0xFF7D5A7A),  // Dusty mauve
    background = Color(0xFFF5F0E8),
    surface = Color(0xFFEDE8DC),
    onBackground = Color(0xFF3D3530),
    onSurface = Color(0xFF3D3530)
)

// ── Atom One Dark Palette (bright light before 7:30 / late night lamp): IDE navy ──
private val EarlyMorningPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x9921252B),
    wallpaperScrimMid = Color(0xCC21252B),
    wallpaperScrimBottom = Color(0xF221252B),
    wallpaperGlow = Color(0x1261AFEF),
    panel = Color(0xFF2C313A),
    elevatedPanel = Color(0xFF333841),
    searchPanel = Color(0xFF1C2026),
    drawerBackground = Color(0xFF21252B),
    chipBackground = Color(0xFF353B45),
    closeButtonContainer = Color(0xFF2C313A),
    accentHalo = Color(0x1561AFEF),
    accentHigh = Color(0xFF61AFEF),    // Atom blue
    textPrimary = Color(0xFFCDD3DE),
    textSecondary = Color(0xFF7F8A99),
    tileBackground = Color(0xFF2C313A),
    mainBackground = Color(0xFF21252B),
    errorAccent = Color(0xFFE06C75),
    cardToday = Color(0xFF292E36),
    cardFocus = Color(0xFF2F3440),
    cardReading = Color(0xFF292E36),
    cardWallet = Color(0xFF263040),
    clusterIntelligence = Color(0xFF61AFEF),
    clusterUtility = Color(0xFF98C379),
    clusterAction = Color(0xFFE5C07B),
    clusterCommunication = Color(0xFF56B6C2),
    clusterAssistant = Color(0xFFC678DD),
    clusterHealth = Color(0xFFE06C75)
)

// ── Atom One Dark Palette (17:30+, bright ambient): Deep navy, IDE-calm ──
private val BlueHourPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x9921252B),
    wallpaperScrimMid = Color(0xCC21252B),
    wallpaperScrimBottom = Color(0xF221252B),
    wallpaperGlow = Color(0x1261AFEF),
    panel = Color(0xFF2C313A),
    elevatedPanel = Color(0xFF333841),
    searchPanel = Color(0xFF1C2026),
    drawerBackground = Color(0xFF21252B),
    chipBackground = Color(0xFF353B45),
    closeButtonContainer = Color(0xFF2C313A),
    accentHalo = Color(0x1561AFEF),
    accentHigh = Color(0xFF61AFEF),    // Atom blue
    textPrimary = Color(0xFFCDD3DE),   // Atom light text
    textSecondary = Color(0xFF7F8A99), // Atom muted
    tileBackground = Color(0xFF2C313A),
    mainBackground = Color(0xFF21252B), // Atom One Dark base
    errorAccent = Color(0xFFE06C75),   // Atom red
    cardToday = Color(0xFF292E36),
    cardFocus = Color(0xFF2F3440),
    cardReading = Color(0xFF292E36),
    cardWallet = Color(0xFF263040),
    clusterIntelligence = Color(0xFF61AFEF),
    clusterUtility = Color(0xFF98C379),
    clusterAction = Color(0xFFE5C07B),
    clusterCommunication = Color(0xFF56B6C2),
    clusterAssistant = Color(0xFFC678DD),
    clusterHealth = Color(0xFFE06C75)
)

// ── Warm Paper Night Palette (dark ambient, any time): Deep warm paper, off-white ink ──
private val ReadingPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x99141312),
    wallpaperScrimMid = Color(0xCC141312),
    wallpaperScrimBottom = Color(0xF2141312),
    wallpaperGlow = Color(0x10D4A574),
    panel = Color(0xFF2A2520),
    elevatedPanel = Color(0xFF332E28),
    searchPanel = Color(0xFF1E1A16),
    drawerBackground = Color(0xFF1A1612),
    chipBackground = Color(0xFF352F28),
    closeButtonContainer = Color(0xFF2A2520),
    accentHalo = Color(0x15D4A574),
    accentHigh = Color(0xFFD4A574),    // Warm rose-gold
    textPrimary = Color(0xFFEDE8DF),   // Off-white: warm parchment
    textSecondary = Color(0xFFF5F2EC), // Slightly lighter off-white for content
    tileBackground = Color(0xFF211D18),
    mainBackground = Color(0xFF1A1612), // Warm paper dark — deep warm brown-black
    errorAccent = Color(0xFFCF6679),
    cardToday = Color(0xFF252017),
    cardFocus = Color(0xFF2E2920),
    cardReading = Color(0xFF252017),
    cardWallet = Color(0xFF20251A),
    clusterIntelligence = Color(0xFF4A5568),
    clusterUtility = Color(0xFF48BB78),
    clusterAction = Color(0xFFED8936),
    clusterCommunication = Color(0xFFA0AEC0),
    clusterAssistant = Color(0xFF9F7AEA),
    clusterHealth = Color(0xFFF56565)
)

// Legacy earthy palette (fallback)
private val EarthyPalette = ReadingPalette

// ── Quiet Light Palette (7:30–17:30, bright ambient): Warm cream, editorial calm ──
private val DayPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x10EDE8DC),
    wallpaperScrimMid = Color(0x35EDE8DC),
    wallpaperScrimBottom = Color(0x70EDE8DC),
    wallpaperGlow = Color(0x157A5C3E),
    panel = Color(0xFFE5DFCF),
    elevatedPanel = Color(0xFFD9D3C3),
    searchPanel = Color(0xFFEDE8DC),
    drawerBackground = Color(0xFFF5F0E8),
    chipBackground = Color(0xFFD9D3C3),
    closeButtonContainer = Color(0xFFD9D3C3),
    accentHalo = Color(0x207A5C3E),
    accentHigh = Color(0xFF7A5C3E),    // Warm brown
    textPrimary = Color(0xFF3D3530),   // Warm dark brown
    textSecondary = Color(0xFF6B6157), // Muted warm grey
    tileBackground = Color(0xFFDDD8C8),
    mainBackground = Color(0xFFF5F0E8), // Quiet Light cream
    errorAccent = Color(0xFFA32929),
    cardToday = Color(0xFFE8E2D2),
    cardFocus = Color(0xFFE2DBCB),
    cardReading = Color(0xFFE8E2D2),
    cardWallet = Color(0xFFE0E5D0),
    clusterIntelligence = Color(0xFF2B6CB0),
    clusterUtility = Color(0xFF5C6E5B),
    clusterAction = Color(0xFF7A5C3E),
    clusterCommunication = Color(0xFF718096),
    clusterAssistant = Color(0xFF7D5A7A),
    clusterHealth = Color(0xFFA32929)
)

private val LocalAmbientPalette = staticCompositionLocalOf { EarthyPalette }
private val LocalAmbientMode = staticCompositionLocalOf { AmbientMode.READING }

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
    return produceState(initialValue = resolveAmbientMode(LocalTime.now(), 100f, AmbientMode.READING)) {
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
        AmbientMode.EARLY_MORNING -> AmbientThemeTokens(mode, true, EarlyMorningColorScheme, EarlyMorningPalette)
        AmbientMode.DAY -> AmbientThemeTokens(mode, false, DayColorScheme, DayPalette)
        AmbientMode.BLUE_HOUR -> AmbientThemeTokens(mode, true, BlueHourColorScheme, BlueHourPalette)
        AmbientMode.READING -> AmbientThemeTokens(mode, true, ReadingColorScheme, ReadingPalette)
    }
}

private fun resolveAmbientMode(time: LocalTime, lux: Float, currentMode: AmbientMode): AmbientMode {
    // 1. Ambient light takes priority — any dark environment → Reading mode
    // Hysteresis: enter READING at lux < 8, exit at lux > 15
    val isDark = if (currentMode == AmbientMode.READING) lux < 15f else lux < 8f
    if (isDark) return AmbientMode.READING

    // Dim indoor light → Early Morning feel
    // Hysteresis: enter EARLY_MORNING at lux < 120, exit at lux > 180
    val isDim = if (currentMode == AmbientMode.EARLY_MORNING) lux < 180f else lux < 120f
    if (isDim) return AmbientMode.EARLY_MORNING

    // 2. Bright ambient light → Time-of-Day determines mode
    val dayStart = LocalTime.of(7, 30)
    val blueHourStart = LocalTime.of(17, 30)

    return when {
        !time.isBefore(dayStart) && time.isBefore(blueHourStart) -> AmbientMode.DAY
        !time.isBefore(blueHourStart) -> AmbientMode.BLUE_HOUR
        else -> AmbientMode.EARLY_MORNING
    }
}
