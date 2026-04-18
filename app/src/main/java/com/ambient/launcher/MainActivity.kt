package com.ambient.launcher

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ambient.launcher.home.LauncherScreen
import com.ambient.launcher.tts.TtsController
import com.ambient.launcher.ui.theme.AmbientLauncherTheme
import com.ambient.launcher.ui.theme.AmbientMode

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val briefingViewModel: BriefingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TtsController.ensureBound(applicationContext)
        
        // ALWAYS mode (API 30+) lets us render content behind the punch-hole,
        // enabling the CutoutRingOverlay to draw decorative rings around it.
        window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Make system bars completely transparent so they blend with the wallpaper.
        // We DO NOT hide them. Keeping them visible but transparent ensures that a single 
        // swipe from the top pulls down the notification shade, instead of requiring a 
        // first swipe to reveal the status bar and a second swipe for notifications.
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            AmbientLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherScreen(
                        dashboardViewModel = dashboardViewModel,
                        briefingViewModel = briefingViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
