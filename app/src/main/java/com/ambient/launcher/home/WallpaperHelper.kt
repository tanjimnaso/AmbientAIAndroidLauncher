package com.ambient.launcher.home

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperHelper {
    suspend fun setSolidColorWallpaper(context: Context, color: Color) {
        withContext(Dispatchers.Default) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            try {
                val width = if (wallpaperManager.desiredMinimumWidth > 0) wallpaperManager.desiredMinimumWidth else 1080
                val height = if (wallpaperManager.desiredMinimumHeight > 0) wallpaperManager.desiredMinimumHeight else 2400

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    this.color = color.toArgb()
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
                bitmap.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
