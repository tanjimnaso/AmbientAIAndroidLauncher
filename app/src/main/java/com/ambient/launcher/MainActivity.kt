package com.ambient.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ambient.launcher.home.LauncherScreen
import com.ambient.launcher.ui.theme.AmbientLauncherTheme

class MainActivity : ComponentActivity() {
    private val dashboardViewModel by viewModels<DashboardViewModel>()
    private val agenticAIViewModel by viewModels<AgenticAIViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AmbientLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherScreen(
                        dashboardViewModel = dashboardViewModel,
                        agenticAIViewModel = agenticAIViewModel
                    )
                }
            }
        }
    }
}
