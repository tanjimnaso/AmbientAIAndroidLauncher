package com.ambient.launcher.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

/**
 * Wabi-sabi grain overlay.
 *
 * Renders a tiled, deterministic monochrome noise pattern across the full screen
 * at very low intensity. Two purposes:
 *   1. Adds subtle organic texture (Japanese wabi-sabi — perfection's imperfection).
 *   2. Disrupts uniform color regions to suppress OLED moire visible at oblique angles.
 *
 * The bitmap is built once and tiled via a [BitmapShader], so the per-frame cost
 * is a single textured rect — negligible compared to recomposition or scrolling.
 */
@Composable
internal fun GrainOverlay(
    alpha: Float = 0.04f,
    tileSize: Int = 192,
    seed: Int = 0xA1B1E47.toInt()
) {
    val grainBitmap = remember(tileSize, seed) { buildGrainBitmap(tileSize, seed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val paint = Paint().apply {
                    this.shader = shader
                    this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    // Overlay preserves palette identity: darkens darks, lightens lights,
                    // unlike plain alpha which flattens to a uniform haze.
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
                }
                onDrawWithContent {
                    drawContent()
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                    }
                }
            }
    )
}

private fun buildGrainBitmap(size: Int, seed: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rng = Random(seed)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        // Greys clustered around mid-grey so OVERLAY blend modulates ~symmetrically.
        val v = (96 + rng.nextInt(64)).coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
}
