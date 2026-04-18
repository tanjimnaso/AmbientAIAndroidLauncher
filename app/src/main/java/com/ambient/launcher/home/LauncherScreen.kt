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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.*
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Packages excluded from the home-screen quick-app list
private val BLOCKED_QUICK_APP_PACKAGES = setOf(
    "com.google.android.googlequicksearchbox",
    "com.android.chrome",
    "com.google.android.apps.chrome"
)

/**
 * LauncherScreen — root composable.
 *
 * Three-page horizontal pager (swipe left/right to navigate):
 *   Page 0 — Left:   Masthead + AI summary + RSS headlines
 *   Page 1 — Middle: Masthead + 6 pinned app icons  ← DEFAULT
 *   Page 2 — Right:  S Pen note-taking canvas (no masthead)
 *
 * App Menu slides up only from the middle page (swipe-up gesture).
 */
@Composable
internal fun LauncherScreen(
    dashboardViewModel: DashboardViewModel,
    briefingViewModel: BriefingViewModel,
    todoViewModel: TodoViewModel
) {
    val context       = LocalContext.current
    val ambientPalette = AmbientTheme.palette

    // ── State collection ──────────────────────────────────────────────────────
    val feedItems          by dashboardViewModel.feedItems.collectAsStateWithLifecycle()
    val lastFeedRefreshTime by dashboardViewModel.lastFeedRefreshTime.collectAsStateWithLifecycle()
    val isFeedRefreshing   by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val weather            by dashboardViewModel.weather.collectAsStateWithLifecycle()
    val battery            by dashboardViewModel.batteryState.collectAsStateWithLifecycle()
    val briefing           by briefingViewModel.briefing.collectAsStateWithLifecycle()
    val analysis           by briefingViewModel.analysis.collectAsStateWithLifecycle()
    val isAnalysisLoading  by briefingViewModel.isAnalysisLoading.collectAsStateWithLifecycle()
    val isBriefingLoading  by briefingViewModel.isBriefingLoading.collectAsStateWithLifecycle()
    val analysisHasError   by briefingViewModel.analysisHasError.collectAsStateWithLifecycle()
    val installedApps      by dashboardViewModel.installedApps.collectAsStateWithLifecycle()

    // ── UI state ──────────────────────────────────────────────────────────────
    var editingApp      by remember { mutableStateOf<AppInfo?>(null) }
    var editingToday    by remember { mutableStateOf(false) }
    var showAppMenu     by remember { mutableStateOf(false) }
    var viewingArticle  by remember { mutableStateOf<RssFeedItem?>(null) }
    var viewingAnalysis by remember { mutableStateOf(false) }

    val isAnalysisReady = analysis != null && !isAnalysisLoading && !analysisHasError

    // Middle page is the default (index 1)
    val pagerState = rememberPagerState(initialPage = 1) { 3 }

    val (configuration, updateConfiguration) = rememberLauncherConfiguration(installedApps)

    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) dashboardViewModel.refreshWeather() }

    // ── Side effects ──────────────────────────────────────────────────────────
    LaunchedEffect(ambientPalette.mainBackground) {
        WallpaperHelper.setSolidColorWallpaper(context, ambientPalette.mainBackground)
    }
    LaunchedEffect(configuration.rssSources) {
        dashboardViewModel.setRssSources(configuration.rssSources)
    }
    LaunchedEffect(feedItems) {
        if (feedItems.isNotEmpty()) {
            // 30 headlines is sufficient signal for a one-liner briefing — no need to send all 66+
            briefingViewModel.generateBriefing(feedItems.take(30).map { "${it.source}: ${it.title}" })
        }
    }
    LaunchedEffect(Unit) {
        var permissionRequested = false
        while (isActive) {
            val hasLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasLocation) {
                dashboardViewModel.refreshWeather()
                delay(30 * 60 * 1000L)
            } else {
                if (!permissionRequested) {
                    permissionRequested = true
                    requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                delay(5_000L)
            }
        }
    }

    // ── App data derived state ────────────────────────────────────────────────
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
            bucket !in configuration.hiddenBuckets && bucketApps[bucket].orEmpty().isNotEmpty()
        }
    }

    val sevenDaysAgo = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val newApps = remember(installedApps) {
        installedApps
            .filter { it.firstInstallTime > sevenDaysAgo && it.packageName !in NEVER_SHOW_PACKAGES }
            .sortedBy { it.label.lowercase() }
    }
    val allApps = remember(installedApps) {
        installedApps
            .filter { it.packageName !in NEVER_SHOW_PACKAGES }
            .sortedBy { it.label.lowercase() }
    }

    val topApps = remember(installedApps, configuration) {
        installedApps
            .filter { it.packageName !in BLOCKED_QUICK_APP_PACKAGES }
            .take(9)
            .map { app ->
                val bucket = LauncherBucket.entries
                    .find { configuration.packagesFor(it).contains(app.packageName) }
                    ?: LauncherBucket.MISC
                app.copy(bucket = bucket)
            }
    }

    // ── Back stack ────────────────────────────────────────────────────────────
    BackHandler {
        when {
            viewingAnalysis       -> viewingAnalysis = false
            viewingArticle != null -> viewingArticle = null
            editingApp != null     -> editingApp = null
            showAppMenu            -> showAppMenu = false
        }
    }

    // ── Masthead height tracking (measured once rendered, used for page top-padding) ──
    val density = LocalDensity.current
    var mastheadHeightPx by remember { mutableIntStateOf(0) }
    val mastheadHeightDp: Dp = with(density) { mastheadHeightPx.toDp() }

    // S-curve fade: fully gone before the notes page is 30% into view.
    // Compressing the range to [1.0, 1.3] means the masthead clears before
    // "Right Handed" slides fully onto screen.
    val mastheadAlpha by remember(pagerState) {
        derivedStateOf {
            val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val t = ((pos - 1f) / 0.3f).coerceIn(0f, 1f) // complete by 30% of swipe
            val eased = t * t * (3f - 2f * t)              // smoothstep S-curve
            1f - eased
        }
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackplane()

        val scope = rememberCoroutineScope()

        // ── 3-page pager ─────────────────────────────────────────────────────
        HorizontalPager(
            state            = pagerState,
            modifier         = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                // ── Page 0: News / RSS / AI summary ──────────────────────────
                0 -> LeftNewsPage(
                    topPadding           = mastheadHeightDp,
                    briefing             = briefing,
                    isBriefingLoading    = isBriefingLoading,
                    isAnalysisLoading    = isAnalysisLoading,
                    isAnalysisReady      = isAnalysisReady,
                    analysisHasError     = analysisHasError,
                    feedItems            = feedItems,
                    isRefreshing         = isFeedRefreshing,
                    lastFeedRefreshTime  = lastFeedRefreshTime,
                    onRefresh            = {
                        // Pull-to-refresh only refreshes the feed — briefing stays on its
                        // morning cache (BRIEFING_SLOT_HOURS = [7]). clearBriefingCache()
                        // is intentionally NOT called here.
                        dashboardViewModel.refreshFeeds()
                    },
                    onOpenCloudNotifications = {
                        val pkg    = "com.google.android.apps.cloudconsole"
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                            ?: Intent(Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://console.cloud.google.com/billing"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    onFeedClick          = { viewingArticle = it },
                    onBriefingClick      = {
                        viewingAnalysis = true
                        val current = briefing
                        if (!current.isNullOrBlank() && !isAnalysisLoading && analysis == null) {
                            if (briefingViewModel.isAnalysisCached(current))
                                briefingViewModel.loadCachedAnalysis(current)
                            else
                                briefingViewModel.generateAnalysis(current)
                        }
                    }
                )

                // ── Page 1: Home apps (default) ───────────────────────────────
                1 -> MiddleHomeScreen(
                    topApps       = topApps,
                    appsByPackage = appsByPackage,
                    topPadding    = mastheadHeightDp,
                    onAppClick    = { app ->
                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(it)
                        }
                    },
                    onSwipeUp     = { showAppMenu = true }
                )

                // ── Page 2: S Pen notes ───────────────────────────────────────
                2 -> SPenNotesScreen()
            }
        }

        // ── Floating masthead — shared across pages 0 & 1, fades before page 2 ─
        // Rendered above the pager so it doesn't move during horizontal swipes.
        // Pages reserve top space equal to mastheadHeightDp via their topPadding param.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .onSizeChanged { mastheadHeightPx = it.height }
                .alpha(mastheadAlpha)
        ) {
            Masthead(
                weather  = weather,
                battery  = battery,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Overlays (sit above the pager) ────────────────────────────────────

        AnimatedVisibility(
            visible = viewingAnalysis,
            enter   = fadeIn(animationSpec = tween(240)),
            exit    = fadeOut(animationSpec = tween(180))
        ) {
            AnalysisScreen(
                briefingText = briefing ?: "",
                viewModel    = briefingViewModel,
                onDismiss    = { viewingAnalysis = false }
            )
        }

        AnimatedVisibility(
            visible = viewingArticle != null,
            enter   = fadeIn(animationSpec = tween(240)),
            exit    = fadeOut(animationSpec = tween(180))
        ) {
            val article = viewingArticle
            if (article != null) {
                ArticleViewerScreen(article = article, onDismiss = { viewingArticle = null })
            }
        }

        AppMenuCard(
            isOpen          = showAppMenu,
            visibleBuckets  = visibleBuckets,
            bucketApps      = bucketApps,
            configuration   = configuration,
            newApps         = newApps,
            allApps         = allApps,
            onDismiss       = { showAppMenu = false },
            onToggleCollapse = { bucket -> updateConfiguration(configuration.toggleCollapse(bucket)) },
            onAppClick      = { app ->
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                showAppMenu = false
            },
            onAppLongClick  = { editingApp = it }
        )

        if (editingApp != null) {
            AppEditDialog(
                app           = editingApp!!,
                configuration = configuration,
                onDismiss     = { editingApp = null },
                onSave        = { updateConfiguration(it) }
            )
        }
    }
}

// ── Page 0: Left news screen ──────────────────────────────────────────────────

@Composable
private fun LeftNewsPage(
    topPadding: Dp,
    briefing: String?,
    isBriefingLoading: Boolean,
    isAnalysisLoading: Boolean,
    isAnalysisReady: Boolean,
    analysisHasError: Boolean,
    feedItems: List<RssFeedItem>,
    isRefreshing: Boolean,
    lastFeedRefreshTime: Long,
    onRefresh: () -> Unit,
    onOpenCloudNotifications: () -> Unit,
    onFeedClick: (RssFeedItem) -> Unit,
    onBriefingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Reserve space for the floating masthead rendered at the root level
        Spacer(modifier = Modifier.height(topPadding))

        TodaysSignal(
            lastRefreshTime = lastFeedRefreshTime,
            modifier        = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )

        Text(
            text  = "AROUND THE WORLD",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily    = com.ambient.launcher.ui.theme.InterFontFamily,
                fontSize      = 10.sp,
                letterSpacing = 1.5.sp
            ),
            color    = AmbientTheme.palette.textSecondary.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 4.dp)
        )

        AiBriefingSection(
            briefing              = briefing,
            isBriefingLoading     = isBriefingLoading,
            isAnalysisLoading     = isAnalysisLoading,
            isAnalysisReady       = isAnalysisReady,
            analysisHasError      = analysisHasError,
            onClick               = onBriefingClick,
            onOpenCloudNotifications = onOpenCloudNotifications,
            modifier              = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HeadlinesSection(
                feedItems       = feedItems,
                isRefreshing    = isRefreshing,
                lastRefreshTime = lastFeedRefreshTime,
                onFeedClick     = onFeedClick,
                onRefresh       = onRefresh,
                modifier        = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Wallpaper backplane ───────────────────────────────────────────────────────

@Composable
private fun WallpaperBackplane() {
    val ambientPalette = AmbientTheme.palette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientPalette.mainBackground)
    )
}
