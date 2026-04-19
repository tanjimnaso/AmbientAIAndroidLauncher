package com.ambient.launcher.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

/**
 * Wabi-sabi grain overlay.
 *
 * Renders a tiled, deterministic monochrome noise pattern across the full screen
 * at very low intensity. Two purposes:
 *   1. Subtle organic texture (wabi-sabi — perfection's imperfection).
 *   2. Disrupts uniform color regions to suppress OLED moire at oblique angles.
 *
 * Parameters vary per ambient mode — white backgrounds carry finer, stronger grain
 * (reads as paper tooth); dark backgrounds need coarser, softer grain that doesn't
 * lift the deep shadows (SOFT_LIGHT preserves near-blacks better than OVERLAY).
 */
private data class GrainParams(
    val alpha: Float,
    val tileSize: Int,
    val blend: BlendMode,
)

private fun grainParamsFor(mode: AmbientMode, idleBlackout: Float): GrainParams = when (mode) {
    AmbientMode.DAYLIGHT_OUTDOOR -> GrainParams(0.07f, 128, BlendMode.OVERLAY)
    AmbientMode.DAY_INTERIOR_HI  -> GrainParams(0.05f, 192, BlendMode.OVERLAY)
    AmbientMode.DUSK             -> GrainParams(0.04f, 192, BlendMode.SOFT_LIGHT)
    AmbientMode.TWILIGHT -> {
        // TWILIGHT fades background to black during idle blackout; grain follows
        // (invisible on true black anyway, and stops any faint banding).
        val a = 0.03f * (1f - idleBlackout).coerceIn(0f, 1f)
        GrainParams(a, 256, BlendMode.SOFT_LIGHT)
    }
}

@Composable
internal fun GrainOverlay(
    idleBlackout: Float = 0f,
    seed: Int = 0xA1B1E47.toInt()
) {
    val mode = AmbientTheme.mode
    val params = grainParamsFor(mode, idleBlackout)
    val grainBitmap = remember(params.tileSize, seed) { buildGrainBitmap(params.tileSize, seed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val paint = Paint().apply {
                    this.shader = shader
                    this.alpha = (params.alpha * 255).toInt().coerceIn(0, 255)
                    blendMode = params.blend
                }
                onDrawWithContent {
                    drawContent()
                    if (params.alpha > 0.001f) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                        }
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
        val v = (96 + rng.nextInt(64)).coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
}
