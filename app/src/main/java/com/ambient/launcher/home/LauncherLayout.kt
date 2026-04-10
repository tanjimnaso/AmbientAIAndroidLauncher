package com.ambient.launcher.home

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.produceState
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class AppInfo(
    val label: String,
    val packageName: String
)

private val iconCache = LruCache<String, ImageBitmap>(150)

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = iconCache.get(packageName), packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val drawable = pm.getApplicationIcon(packageName)
                    val bitmap = drawable.toBitmap().asImageBitmap()
                    iconCache.put(packageName, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }.value
}

internal object LauncherLayout {
    val edgeBleed = 18.dp
    val contentPadding = 0.dp
    val topPadding = 0.dp
    val largeGap = 24.dp
    val mediumGap = 16.dp
    val smallGap = 10.dp
    val tileGap = 2.dp
    val cardRadius = 0.dp
    val sectionCardRadius = 0.dp
    val tileRadius = 0.dp
    val heroTileRadius = 0.dp
    val chipRadius = 0.dp
    val pinnedItemWidth = 72.dp
    val pinnedIconSize = 46.dp
    val browserCardMinSize = 232.dp
    val appTileSize = 34.dp
    val appTileWidth = 92.dp
    val appTileHeight = 92.dp
    val wideTileWidth = 220.dp
    val wideTileHeight = 108.dp
    val largeTileWidth = 220.dp
    val largeTileHeight = 220.dp
    val compactTileWidth = 108.dp
    val compactTileHeight = 108.dp
    val iconBadgeSize = 44.dp
    val industrialInset = 26.dp
    val industrialLineWidth = 1.dp
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
