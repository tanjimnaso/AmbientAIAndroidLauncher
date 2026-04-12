package com.ambient.launcher.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.*
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun LauncherScreen(
    dashboardViewModel: DashboardViewModel,
    briefingViewModel: BriefingViewModel,
    todoViewModel: TodoViewModel
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val ambientPalette = AmbientTheme.palette
    
    // State collection
    val feedItems by dashboardViewModel.feedItems.collectAsStateWithLifecycle()
    val weather by dashboardViewModel.weather.collectAsStateWithLifecycle()
    val battery by dashboardViewModel.batteryState.collectAsStateWithLifecycle()
    val briefing by briefingViewModel.briefing.collectAsStateWithLifecycle()
    val analysis by briefingViewModel.analysis.collectAsStateWithLifecycle()
    val isAnalysisLoading by briefingViewModel.isAnalysisLoading.collectAsStateWithLifecycle()
    val installedApps by dashboardViewModel.installedApps.collectAsStateWithLifecycle()
    
    // todoViewModel is now passed in from MainActivity (proper lifecycle scoping)

    // UI State
    var isTodoOpen by remember { mutableStateOf(false) }
    var editingApp: AppInfo? by remember { mutableStateOf(null) }
    var editingToday by remember { mutableStateOf(false) }
    var showAppMenu by remember { mutableStateOf(false) }
    var viewingArticle: RssFeedItem? by remember { mutableStateOf(null) }
    var viewingAnalysis by remember { mutableStateOf(false) }
    
    val isAnalysisReady = analysis != null && !isAnalysisLoading &&
        analysis?.startsWith("Timed out") == false &&
        analysis?.startsWith("Analysis unavailable") == false

    val pagerState = rememberPagerState(initialPage = 0) { 1 }

    // Configuration
    val (configuration, updateConfiguration) = rememberLauncherConfiguration(installedApps)

    // Permissions
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) dashboardViewModel.refreshWeather()
    }

    // Side Effects
    LaunchedEffect(ambientPalette.mainBackground) {
        WallpaperHelper.setSolidColorWallpaper(context, ambientPalette.mainBackground)
    }

    LaunchedEffect(configuration.rssSources) {
        dashboardViewModel.setRssSources(configuration.rssSources)
    }

    LaunchedEffect(feedItems, configuration.briefingInstruction) {
        if (feedItems.isNotEmpty()) {
            // TTL and caching are now owned by BriefingViewModel
            val headlines = feedItems.map { "${it.source}: ${it.title}" }
            briefingViewModel.generateBriefing(headlines, configuration.briefingInstruction)
        }
    }

    LaunchedEffect(Unit) {
        var permissionRequested = false
        while (isActive) {
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCoarseLocation) {
                dashboardViewModel.refreshWeather()
                delay(30 * 60 * 1000L) // 30 minutes
            } else {
                if (!permissionRequested) {
                    permissionRequested = true
                    requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                delay(5_000L) // Wait 5s then re-check if permission was granted
            }
        }
    }

    // App Bucketing Logic
    val appsByPackage = remember(installedApps) { installedApps.associateBy { it.packageName } }
    
    val bucketApps = remember(configuration, appsByPackage) {
        LauncherBucket.entries.associateWith { bucket ->
            configuration.packagesFor(bucket).mapNotNull { pkg ->
                appsByPackage[pkg]?.copy(bucket = bucket)
            }
        }
    }

    val visibleBuckets = remember(configuration, bucketApps) {
        configuration.bucketOrder.filter { bucket ->
            !configuration.hiddenBuckets.contains(bucket) && bucketApps[bucket].orEmpty().isNotEmpty()
        }
    }

    val topApps = remember(installedApps, configuration) {
        installedApps.take(9).map { app ->
            val bucket = LauncherBucket.entries.find { configuration.packagesFor(it).contains(app.packageName) } ?: LauncherBucket.MISC
            app.copy(bucket = bucket)
        }
    }

    BackHandler {
        when {
            viewingAnalysis -> viewingAnalysis = false
            viewingArticle != null -> viewingArticle = null
            editingApp != null -> editingApp = null
            showAppMenu -> showAppMenu = false
            isTodoOpen -> isTodoOpen = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackplane()

        // Background Scrims
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ambientPalette.wallpaperScrimTop,
                            ambientPalette.wallpaperScrimMid,
                            ambientPalette.wallpaperScrimBottom
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientPalette.wallpaperGlow,
                            Color.Transparent
                        )
                    )
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) {
            HomePage(
                context = context,
                weather = weather,
                battery = battery,
                briefing = briefing,
                isAnalysisLoading = isAnalysisLoading,
                isAnalysisReady = isAnalysisReady,
                feedItems = feedItems,
                recentApps = topApps.take(6),
                onFeedClick = { item ->
                    viewingArticle = item
                },
                onRecentAppClick = { app ->
                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                onAppMenuClick = { showAppMenu = true },
                onOpenTodo = { isTodoOpen = true },
                onBriefingClick = {
                    val currentBriefing = briefing ?: return@HomePage
                    if (briefingViewModel.isAnalysisCached(currentBriefing) || analysis != null) {
                        // Already have content — load into state if needed and open immediately
                        if (analysis == null) briefingViewModel.loadCachedAnalysis(currentBriefing)
                        viewingAnalysis = true
                    } else if (!isAnalysisLoading) {
                        // Not cached, not in-flight — start background generation
                        briefingViewModel.generateAnalysis(currentBriefing)
                    }
                    // If already loading, tap does nothing (label shows "AWAITING...")
                },
                bucketApps = bucketApps,
                tileSizes = configuration.tileSizes,
                onAppClick = { app ->
                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                onAppLongClick = { editingApp = it }
            )
        }

        // Overlays
        if (isTodoOpen) {
            TodoScreen(viewModel = todoViewModel, onDismiss = { isTodoOpen = false })
        }

        // Analysis screen — only opens when content is ready
        AnimatedVisibility(
            visible = viewingAnalysis && analysis != null,
            enter = fadeIn(animationSpec = tween(240)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            val currentBriefing = briefing
            if (currentBriefing != null && analysis != null) {
                AnalysisScreen(
                    briefingText = currentBriefing,
                    viewModel = briefingViewModel,
                    onDismiss = { viewingAnalysis = false }
                )
            }
        }

        // Article viewer fades in/out
        AnimatedVisibility(
            visible = viewingArticle != null,
            enter = fadeIn(animationSpec = tween(240)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            val article = viewingArticle
            if (article != null) {
                ArticleViewerScreen(
                    article = article,
                    onDismiss = { viewingArticle = null }
                )
            }
        }

        AppMenuCard(
            isOpen = showAppMenu,
            visibleBuckets = visibleBuckets,
            bucketApps = bucketApps,
            topApps = topApps,
            tileSizes = configuration.tileSizes,
            configuration = configuration,
            onDismiss = { showAppMenu = false },
            onAppClick = { app ->
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                showAppMenu = false
            },
            onAppLongClick = { editingApp = it }
        )

        // Dialogs
        if (editingToday) {
            TodaySettingsDialog(
                configuration = configuration,
                onDismiss = { editingToday = false },
                onSave = { updateConfiguration(it) }
            )
        }

        if (editingApp != null) {
            AppEditDialog(
                app = editingApp!!,
                configuration = configuration,
                onDismiss = { editingApp = null },
                onSave = { updateConfiguration(it) }
            )
        }
    }
}

@Composable
private fun HomePage(
    context: android.content.Context,
    weather: WeatherUiState,
    battery: BatteryUiState,
    briefing: String?,
    isAnalysisLoading: Boolean = false,
    isAnalysisReady: Boolean = false,
    feedItems: List<RssFeedItem>,
    recentApps: List<AppInfo>,
    onFeedClick: (RssFeedItem) -> Unit,
    onRecentAppClick: (AppInfo) -> Unit,
    onAppMenuClick: () -> Unit,
    onOpenTodo: () -> Unit,
    onBriefingClick: () -> Unit,
    bucketApps: Map<LauncherBucket, List<AppInfo>> = emptyMap(),
    tileSizes: Map<String, TileSize> = emptyMap(),
    onAppClick: (AppInfo) -> Unit = {},
    onAppLongClick: (AppInfo) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Root gesture detector for the App Menu (Drawer)
                    // If a child (like the RSS feed) consumes the gesture, this won't trigger.
                    if (dragAmount < -20f) onAppMenuClick()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .offset(y = (-8).dp) // Total ~50px shift up (removing 12dp padding + 8dp offset)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -100f) onOpenTodo()
                    }
                },
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Masthead ──────────────────────────────────────────────────────
            Masthead(weather = weather, battery = battery)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section Divider: AROUND THE WORLD ──────────────────────────────
            TodaysSignal()

            Spacer(modifier = Modifier.height(16.dp))

            // ── AI Briefing (editorial headline) ─────────────────────────────
            AiBriefingSection(
                briefing = briefing,
                isAnalysisLoading = isAnalysisLoading,
                isAnalysisReady = isAnalysisReady,
                onClick = onBriefingClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Apps (above feed) ──────────────────────────────────────
            RecentAppsStrip(
                apps = recentApps,
                onAppClick = onAppClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Headlines/RSS (fills remaining space) ─────────────────────────
            HeadlinesSection(
                feedItems = feedItems,
                onFeedClick = onFeedClick,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WallpaperBackplane() {
    val ambientPalette = AmbientTheme.palette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ambientPalette.mainBackground,
                        ambientPalette.mainBackground.copy(alpha = 0.9f) // Slight depth
                    )
                )
            )
    )
}
