package com.ambient.launcher.home

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

internal data class AppInfo(
    val label: String,
    val packageName: String,
    val bucket: LauncherBucket = LauncherBucket.MISC,
    val firstInstallTime: Long = 0L
)

private object IconCache {
    val cache = LruCache<String, ImageBitmap>(150)
}

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    
    // By using remember(packageName), we ensure that when the app changes,
    // we immediately check the cache and reset the state, preventing "ghost" icons.
    var state by remember(packageName) { 
        mutableStateOf<ImageBitmap?>(IconCache.cache.get(packageName)) 
    }
    
    LaunchedEffect(packageName) {
        if (state == null) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bmp = drawable.toBitmap().asImageBitmap()
                    IconCache.cache.put(packageName, bmp)
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
            state = bitmap
        }
    }
    
    return state
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val icon = rememberAppIcon(packageName)
    
    Box(modifier = modifier) {
        Crossfade(targetState = icon, label = "icon-fade") { state ->
            if (state != null) {
                Image(
                    bitmap = state,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = colorFilter
                )
            }
        }
    }
}

/** Icon filter: tint opacity varies by light mode for visibility. */
@Composable
internal fun getAmbientIconFilter(bucketColor: Color): ColorFilter {
    val opacity = com.ambient.launcher.ui.theme.AmbientTheme.palette.iconOverlayOpacity
    return getAmbientIconFilterNonComposable(bucketColor, opacity)
}

internal fun getAmbientIconFilterNonComposable(bucketColor: Color, opacity: Float): ColorFilter {
    // To achieve the Japandi "Ink" effect, we need to:
    // 1. Completely desaturate the icon (convert to grayscale).
    // 2. Tint it with the bucket's thematic color.
    // 3. Modulate the alpha so it feels like it's printed on the surface.
    
    val matrix = ColorMatrix().apply {
        setToSaturation(0f) // Grayscale
        
        // Adjust the matrix to apply the tint color. 
        // We multiply the grayscale values by the tint color components.
        val r = bucketColor.red
        val g = bucketColor.green
        val b = bucketColor.blue
        val a = opacity.coerceAtLeast(0.5f) // Stronger presence in dark modes
        
        val m = floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, a, 0f
        )
        timesAssign(ColorMatrix(m))
    }
    
    return ColorFilter.colorMatrix(matrix)
}

/**
 * LauncherLayout: Design System for Slab Phones (360–480dp)
 *
 * Baseline: 480p power-user narrow mode. All measurements are DPI-aware (dp).
 * Typography: See ResponsiveTypography.kt (5-tier hierarchy: D1, H1, D2, T1, T2, T3).
 * Spacing: 8dp base grid. All multiples follow: 4, 8, 16, 32, 48, 64.
 *
 * This system adapts to user accessibility settings (Configuration.fontScale) automatically
 * via Compose's sp unit behavior. Components should use ResponsiveTypography for all text.
 */
internal object LauncherLayout {

    // ── Content Inset & Padding (480p baseline) ─────────────────────────────
    val edgeBleed = 18.dp           // Horizontal bleed for full-width elements
    val contentPadding = 0.dp        // Standard inner padding (adjust per section)
    val topPadding = 0.dp
    val sectionHorizontalPadding = 24.dp  // Horizontal padding for content sections

    // ── Gap System (8dp base grid: 8, 16, 24, 32, 48, 64) ──────────────────
    // Use these for spacing between elements, NOT within text
    val smallGap = 10.dp             // Minor spacing between items
    val mediumGap = 16.dp            // Standard spacing between sections
    val largeGap = 24.dp             // Generous spacing, section boundaries
    val spacingXS = 4.dp
    val spacingS = 8.dp
    val spacingM = 16.dp
    val spacingL = 32.dp             // Breathing room between major sections
    val spacingXL = 48.dp            // Large vertical separator (between Level 0 & Level 2)
    val spacingXXL = 64.dp           // Maximum breathing room
    val tileGap = 8.dp

    // ── Shape & Radius ───────────────────────────────────────────────────────
    val cardRadius = 0.dp            // Flat cards (no border radius)
    val sectionCardRadius = 0.dp
    val tileRadius = 16.dp           // App tile corner radius
    val heroTileRadius = 24.dp       // Large tile corner radius
    val chipRadius = 0.dp

    // ── Component Sizing (for 480p baseline) ──────────────────────────────────
    val pinnedItemWidth = 72.dp
    val pinnedIconSize = 61.dp
    val browserCardMinSize = 232.dp
    val appTileSize = 45.dp
    val appTileWidth = 122.dp
    val appTileHeight = 122.dp
    val wideTileWidth = 293.dp
    val wideTileHeight = 144.dp
    val largeTileWidth = 293.dp
    val largeTileHeight = 293.dp
    val compactTileWidth = 144.dp
    val compactTileHeight = 144.dp
    val iconBadgeSize = 44.dp

    // ── Industrial Index (left-swipe page) ────────────────────────────────────
    val industrialInset = 26.dp
    val industrialLineWidth = 1.dp

    // ── Typography Reference ──────────────────────────────────────────────────
    // Import ResponsiveTypography for all text styles. This is a design system reference.
    // Do NOT hardcode font sizes in components — always use ResponsiveTypography.
    // Example: Text("Headlines", style = ResponsiveTypography.d1, color = ...)
}

internal fun Modifier.horizontalBleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val bleedPx = amount.roundToPx()
    val expandedConstraints = constraints.copy(
        minWidth = (constraints.minWidth + bleedPx * 2).coerceAtLeast(0),
        maxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth + bleedPx * 2).coerceAtLeast(0)
        } else {
            constraints.maxWidth
        }
    )

    val placeable = measurable.measure(expandedConstraints)
    val layoutWidth = if (constraints.hasBoundedWidth) {
        constraints.maxWidth
    } else {
        (placeable.width - bleedPx * 2).coerceAtLeast(0)
    }

    layout(width = layoutWidth, height = placeable.height) {
        placeable.placeRelative(x = -bleedPx, y = 0)
    }
}
