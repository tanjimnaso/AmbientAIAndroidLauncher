package com.ambient.launcher.ui.theme

import android.app.Activity
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import java.time.LocalTime

enum class AmbientMode {
    EARLY_MORNING,
    DAY,
    BLUE_HOUR,
    LATE_NIGHT
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
    
    // Project Cards
    val cardToday: Color,
    val cardFocus: Color,
    val cardReading: Color,
    val cardWallet: Color
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

private val EarlyMorningColorScheme = darkColorScheme(
    primary = Color(0xFFD4A574), // Warm rose-gold
    secondary = Color(0xFFE8E5DC),
    tertiary = Color(0xFFC89B6F),
    background = EarthyInk,
    surface = EarthySurface,
    onBackground = Color(0xFFE8E5DC),
    onSurface = Color(0xFFE8E5DC)
)

private val BlueHourColorScheme = darkColorScheme(
    primary = Color(0xFFE8A860), // Golden-tan
    secondary = Color(0xFFE8E5DC),
    tertiary = Color(0xFFF0B060),
    background = EarthyInk,
    surface = EarthySurface,
    onBackground = Color(0xFFE8E5DC),
    onSurface = Color(0xFFE8E5DC)
)

private val LateNightColorScheme = darkColorScheme(
    primary = Color(0xFF9DB5C9), // Cool steel-blue
    secondary = Color(0xFFE0E3E8),
    tertiary = Color(0xFFA8BFD9),
    background = Color(0xFF0F1218),
    surface = Color(0xFF1B1F27),
    onBackground = Color(0xFFE0E3E8),
    onSurface = Color(0xFFE0E3E8)
)

private val DayColorScheme = lightColorScheme(
    primary = MutedOchre,
    secondary = MutedClay,
    tertiary = MutedSage,
    background = Color(0xFFF5F4F0),
    surface = Color(0xFFEBEAE5),
    onBackground = Color(0xFF2C2A28),
    onSurface = Color(0xFF2C2A28)
)

// ── Early Morning Palette (5:00–7:30): Warm dawn awakening ──
private val EarlyMorningPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x66141312),
    wallpaperScrimMid = Color(0x99141312),
    wallpaperScrimBottom = Color(0xCC141312),
    wallpaperGlow = Color(0x10D4A574),
    panel = Color(0xFF3A3430),
    elevatedPanel = Color(0xFF4A4540),
    searchPanel = Color(0xFF252320),
    drawerBackground = EarthyInk,
    chipBackground = Color(0xFF4A4740),
    closeButtonContainer = Color(0xFF3D3A36),
    accentHalo = Color(0x15D4A574),
    accentHigh = Color(0xFFD4A574), // Warm rose-gold
    textPrimary = Color(0xFFE8E5DC),
    textSecondary = Color(0xFFB8B5AC),
    tileBackground = Color(0xFF3D3A36),
    cardToday = Color(0xFF2E2A27),
    cardFocus = Color(0xFF37342F),
    cardReading = Color(0xFF2E2A27),
    cardWallet = Color(0xFF282D22)
)

// ── Blue Hour Palette (17:30–20:00): Golden hour glow ──
private val BlueHourPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x66141312),
    wallpaperScrimMid = Color(0x99141312),
    wallpaperScrimBottom = Color(0xCC141312),
    wallpaperGlow = Color(0x12E8A860),
    panel = Color(0xFF3D3731),
    elevatedPanel = Color(0xFF4A4440),
    searchPanel = Color(0xFF252320),
    drawerBackground = EarthyInk,
    chipBackground = Color(0xFF4B4540),
    closeButtonContainer = Color(0xFF3D3731),
    accentHalo = Color(0x18E8A860),
    accentHigh = Color(0xFFE8A860), // Golden-tan, saturated
    textPrimary = Color(0xFFE8E5DC),
    textSecondary = Color(0xFFB8B5AC),
    tileBackground = Color(0xFF3D3A36),
    cardToday = Color(0xFF322D2A),
    cardFocus = Color(0xFF3B3632),
    cardReading = Color(0xFF322D2A),
    cardWallet = Color(0xFF2C3123)
)

// ── Late Night Palette (20:00–5:00): Cool, readable night ──
private val LateNightPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x66070605),
    wallpaperScrimMid = Color(0x99070605),
    wallpaperScrimBottom = Color(0xCC070605),
    wallpaperGlow = Color(0x0A9DB5C9),
    panel = Color(0xFF35383E),
    elevatedPanel = Color(0xFF42454B),
    searchPanel = Color(0xFF1F2228),
    drawerBackground = Color(0xFF0F1218),
    chipBackground = Color(0xFF424850),
    closeButtonContainer = Color(0xFF35383E),
    accentHalo = Color(0x149DB5C9),
    accentHigh = Color(0xFF9DB5C9), // Cool steel-blue, readable
    textPrimary = Color(0xFFE0E3E8),
    textSecondary = Color(0xFFB0B8C4),
    tileBackground = Color(0xFF35383E),
    cardToday = Color(0xFF272B32),
    cardFocus = Color(0xFF30343B),
    cardReading = Color(0xFF272B32),
    cardWallet = Color(0xFF24282F)
)

// Legacy earthy palette (fallback)
private val EarthyPalette = EarlyMorningPalette

private val DayPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x10EBEAE5),
    wallpaperScrimMid = Color(0x30EBEAE5),
    wallpaperScrimBottom = Color(0x60EBEAE5),
    wallpaperGlow = Color(0x10B8A690),
    panel = Color(0xFFDFDED9),
    elevatedPanel = Color(0xFFD4D3CE),
    searchPanel = Color(0xFFEBEAE5),
    drawerBackground = Color(0xFFF5F4F0),
    chipBackground = Color(0xFFD6D3CD),
    closeButtonContainer = Color(0xFFD6D3CD),
    accentHalo = Color(0x206B6458),
    accentHigh = MutedOchre,
    textPrimary = Color(0xFF1D1B16), // Darkened for readability
    textSecondary = Color(0xFF6D6860), // Darkened for contrast
    tileBackground = Color(0xFFE1E0DB),
    cardToday = Color(0xFFE6E5E0),
    cardFocus = Color(0xFFE0DFDA),
    cardReading = Color(0xFFE6E5E0),
    cardWallet = Color(0xFFDDE0D9)
)

private val LocalAmbientPalette = staticCompositionLocalOf { EarthyPalette }
private val LocalAmbientMode = staticCompositionLocalOf { AmbientMode.LATE_NIGHT }

object AmbientTheme {
    val palette: AmbientPalette
        @Composable get() = LocalAmbientPalette.current

    val mode: AmbientMode
        @Composable get() = LocalAmbientMode.current
}

@Composable
fun AmbientLauncherTheme(
    mode: AmbientMode = rememberAmbientMode(),
    content: @Composable () -> Unit
) {
    val tokens = ambientThemeTokensFor(mode)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            WindowCompat.getInsetsController(window, view).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isAppearanceLightStatusBars = !tokens.isDark
                isAppearanceLightNavigationBars = !tokens.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAmbientPalette provides tokens.palette,
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
    return produceState(initialValue = resolveAmbientMode(LocalTime.now())) {
        while (true) {
            value = resolveAmbientMode(LocalTime.now())
            delay(60_000)
        }
    }.value
}

private fun ambientThemeTokensFor(mode: AmbientMode): AmbientThemeTokens {
    return when (mode) {
        AmbientMode.EARLY_MORNING -> AmbientThemeTokens(mode, true, EarlyMorningColorScheme, EarlyMorningPalette)
        AmbientMode.DAY -> AmbientThemeTokens(mode, false, DayColorScheme, DayPalette)
        AmbientMode.BLUE_HOUR -> AmbientThemeTokens(mode, true, BlueHourColorScheme, BlueHourPalette)
        AmbientMode.LATE_NIGHT -> AmbientThemeTokens(mode, true, LateNightColorScheme, LateNightPalette)
    }
}

private fun resolveAmbientMode(time: LocalTime): AmbientMode {
    val earlyMorningStart = LocalTime.of(5, 0)
    val dayStart = LocalTime.of(7, 30)
    val blueHourStart = LocalTime.of(17, 30)
    val lateNightStart = LocalTime.of(20, 0)

    return when {
        !time.isBefore(earlyMorningStart) && time.isBefore(dayStart) -> AmbientMode.EARLY_MORNING
        !time.isBefore(dayStart) && time.isBefore(blueHourStart) -> AmbientMode.DAY
        !time.isBefore(blueHourStart) && time.isBefore(lateNightStart) -> AmbientMode.BLUE_HOUR
        else -> AmbientMode.LATE_NIGHT
    }
}
