package com.ambient.launcher

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ambient.launcher.home.LauncherScreen
import com.ambient.launcher.ui.theme.AmbientLauncherTheme

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()
    private val readingViewModel: ReadingViewModel by viewModels()
    private val briefingViewModel: BriefingViewModel by viewModels()
    private val todoViewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReadingServiceBus.setListener { readingViewModel.updateReadingState(it) }
        
        // ALWAYS mode (API 30+) lets us render content behind the punch-hole,
        // enabling the CutoutRingOverlay to draw decorative rings around it.
        // Fall back to SHORT_EDGES on API 28–29 for safe edge-cutout handling.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system status bar to let custom HomeAppBar own the top edge
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            AmbientLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherScreen(
                        dashboardViewModel = dashboardViewModel,
                        briefingViewModel = briefingViewModel,
                        todoViewModel = todoViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadingServiceBus.setListener(null)
    }
}
