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
    val accentHigh: Color
)

private data class AmbientThemeTokens(
    val mode: AmbientMode,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val palette: AmbientPalette
)

private val EarlyMorningColorScheme = darkColorScheme(
    primary = Color(0xFF7DDAE8),
    secondary = IcyWhite,
    tertiary = Color(0xFFBDF4FF),
    background = DawnInk,
    surface = DawnSurface,
    onBackground = Color(0xFFF3FAFF),
    onSurface = Color(0xFFF3FAFF),
    onSurfaceVariant = Color(0xFFA8C0CF)
)

private val DayColorScheme = lightColorScheme(
    primary = OceanTeal,
    secondary = IcyWhite,
    tertiary = Color(0xFF38CBE8),
    background = OceanMist,
    surface = Color(0xFFF8FBFD),
    onBackground = Color(0xFF13222D),
    onSurface = Color(0xFF13222D),
    onSurfaceVariant = OceanSlate
)

private val BlueHourColorScheme = darkColorScheme(
    primary = ElectricCyan,
    secondary = IcyWhite,
    tertiary = Color(0xFFC9FBFF),
    background = BlueHourInk,
    surface = BlueHourSurface,
    onBackground = Color(0xFFF4FAFF),
    onSurface = Color(0xFFF4FAFF),
    onSurfaceVariant = Color(0xFFB3C9D8)
)

private val LateNightColorScheme = darkColorScheme(
    primary = Color(0xFF82F6FF),
    secondary = IcyWhite,
    tertiary = Color(0xFFDDF8FF),
    background = MidnightOcean,
    surface = MidnightSurface,
    onBackground = IcyWhite,
    onSurface = IcyWhite,
    onSurfaceVariant = Color(0xFFB7CDDC)
)

private val EarlyMorningPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x44142A36),
    wallpaperScrimMid = Color(0x7A10222E),
    wallpaperScrimBottom = Color(0xB8162731),
    wallpaperGlow = Color(0x242EACFF),
    panel = Color(0x7A18313E),
    elevatedPanel = Color(0x8F203A47),
    searchPanel = Color(0xA1142631),
    drawerBackground = Color(0xE913212B),
    chipBackground = Color(0x2637CBE8),
    closeButtonContainer = Color(0x332D4756),
    accentHalo = Color(0x2446D9F3),
    accentHigh = Color(0xFFBDF4FF)
)

private val DayPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x1CE6F3F9),
    wallpaperScrimMid = Color(0x52D6E7F0),
    wallpaperScrimBottom = Color(0x99BFD5E0),
    wallpaperGlow = Color(0x1434BDEB),
    panel = Color(0xB8F5FAFD),
    elevatedPanel = Color(0xDDFBFEFF),
    searchPanel = Color(0xD7EFF6FA),
    drawerBackground = Color(0xEEF2F8FB),
    chipBackground = Color(0x1F1B728E),
    closeButtonContainer = Color(0x120E2530),
    accentHalo = Color(0x1A25CBEA),
    accentHigh = Color(0xFF1F8EAF)
)

private val BlueHourPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x2A071320),
    wallpaperScrimMid = Color(0x70102437),
    wallpaperScrimBottom = Color(0xBC081623),
    wallpaperGlow = Color(0x2F2DAFFF),
    panel = Color(0x73101F31),
    elevatedPanel = Color(0x8A13283B),
    searchPanel = Color(0x99111F2E),
    drawerBackground = Color(0xF1091623),
    chipBackground = Color(0x222DE8FF),
    closeButtonContainer = Color(0x2E173146),
    accentHalo = Color(0x2670F0FF),
    accentHigh = ElectricCyan
)

private val LateNightPalette = AmbientPalette(
    wallpaperScrimTop = Color(0x3806121D),
    wallpaperScrimMid = Color(0x7C081726),
    wallpaperScrimBottom = Color(0xC20A1520),
    wallpaperGlow = Color(0x3040C8FF),
    panel = Color(0x7F0F2232),
    elevatedPanel = Color(0x96132739),
    searchPanel = Color(0xA5101E2C),
    drawerBackground = Color(0xF2081622),
    chipBackground = Color(0x2448DCFF),
    closeButtonContainer = Color(0x33172D40),
    accentHalo = Color(0x2D76F4FF),
    accentHigh = Color(0xFF89F8FF)
)

private val LocalAmbientPalette = staticCompositionLocalOf { LateNightPalette }
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
    val time by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }

    return resolveAmbientMode(time)
}

private fun ambientThemeTokensFor(mode: AmbientMode): AmbientThemeTokens {
    return when (mode) {
        AmbientMode.EARLY_MORNING -> AmbientThemeTokens(
            mode = mode,
            isDark = true,
            colorScheme = EarlyMorningColorScheme,
            palette = EarlyMorningPalette
        )

        AmbientMode.DAY -> AmbientThemeTokens(
            mode = mode,
            isDark = false,
            colorScheme = DayColorScheme,
            palette = DayPalette
        )

        AmbientMode.BLUE_HOUR -> AmbientThemeTokens(
            mode = mode,
            isDark = true,
            colorScheme = BlueHourColorScheme,
            palette = BlueHourPalette
        )

        AmbientMode.LATE_NIGHT -> AmbientThemeTokens(
            mode = mode,
            isDark = true,
            colorScheme = LateNightColorScheme,
            palette = LateNightPalette
        )
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
