package com.ambient.launcher.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.*
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.AmbientMode
import kotlinx.coroutines.delay

// Packages excluded from the home-screen quick-app list
private val BLOCKED_QUICK_APP_PACKAGES = setOf(
    "com.google.android.googlequicksearchbox",
    "com.android.chrome",
    "com.google.android.apps.chrome"
)

// Masthead-fade tuning
private val RSS_SCROLL_FADE_DISTANCE = 100.dp   // full → transparent as the RSS list scrolls up
private const val PAGE_EXIT_FRACTION  = 0.3f    // masthead is gone 30% into the news→notes swipe

/** Smoothstep easing — 3t² − 2t³. Produces a soft S-curve on [0, 1]. */
private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

/**
 * UI State for the Launcher screen to manage navigation and overlays.
 */
@Stable
internal class LauncherUiState {
    var editingApp by mutableStateOf<AppInfo?>(null)
    var showAppMenu by mutableStateOf(false)
    var viewingArticle by mutableStateOf<RssFeedItem?>(null)
    var viewingAnalysis by mutableStateOf(false)

    fun isOverlayVisible() = viewingAnalysis || viewingArticle != null || editingApp != null || showAppMenu

    fun handleBack(): Boolean {
        return when {
            viewingAnalysis -> { viewingAnalysis = false; true }
            viewingArticle != null -> { viewingArticle = null; true }
            editingApp != null -> { editingApp = null; true }
            showAppMenu -> { showAppMenu = false; true }
            else -> false
        }
    }
}

@Composable
internal fun rememberLauncherUiState() = remember { LauncherUiState() }

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
    briefingViewModel: BriefingViewModel
) {
    val context       = LocalContext.current
    val ambientPalette = AmbientTheme.palette

    // ── State collection ──────────────────────────────────────────────────────
    val feedItems          by dashboardViewModel.feedItems.collectAsStateWithLifecycle()
    val lastFeedRefreshTime by dashboardViewModel.lastFeedRefreshTime.collectAsStateWithLifecycle()
    val isFeedRefreshing   by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val battery            by dashboardViewModel.batteryState.collectAsStateWithLifecycle()
    val briefing           by briefingViewModel.briefing.collectAsStateWithLifecycle()
    val analysis           by briefingViewModel.analysis.collectAsStateWithLifecycle()
    val isAnalysisLoading  by briefingViewModel.isAnalysisLoading.collectAsStateWithLifecycle()
    val isBriefingLoading  by briefingViewModel.isBriefingLoading.collectAsStateWithLifecycle()
    val briefingHasError   by briefingViewModel.briefingHasError.collectAsStateWithLifecycle()
    val analysisHasError   by briefingViewModel.analysisHasError.collectAsStateWithLifecycle()
    val installedApps      by dashboardViewModel.installedApps.collectAsStateWithLifecycle()

    // ── UI state ──────────────────────────────────────────────────────────────
    val uiState = rememberLauncherUiState()

    val isAnalysisReady = analysis != null && !isAnalysisLoading && !analysisHasError

    // Middle page is the default (index 1)
    val pagerState = rememberPagerState(initialPage = 1) { 3 }

    val (configuration, updateConfiguration) = rememberLauncherConfiguration(installedApps)

    // ── Side effects ──────────────────────────────────────────────────────────
    LaunchedEffect(ambientPalette.mainBackground) {
        // Debounce wallpaper updates. During a palette transition (1s), we wait for 
        // the color to settle before pushing the expensive bitmap update to the system.
        delay(1100L)
        WallpaperHelper.setSolidColorWallpaper(context, ambientPalette.mainBackground)
    }
    LaunchedEffect(configuration.rssSources) {
        dashboardViewModel.setRssSources(configuration.rssSources)
    }
    LaunchedEffect(feedItems) {
        if (feedItems.isNotEmpty()) {
            // Top 20 items — ViewModel splits 5 primary (with description) + 15 ambient (title only)
            briefingViewModel.generateBriefing(feedItems.take(20))
        }
    }

    // ── App data derived state ────────────────────────────────────────────────
    val appsByPackage = remember(installedApps) { installedApps.associateBy { it.packageName } }

    val bucketApps = remember(configuration.assignments, appsByPackage) {
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
    val allApps = remember(installedApps, configuration) {
        installedApps
            .filter { it.packageName !in NEVER_SHOW_PACKAGES }
            .map { app ->
                val bucket = LauncherBucket.entries
                    .find { configuration.packagesFor(it).contains(app.packageName) }
                    ?: LauncherBucket.MISC
                app.copy(bucket = bucket)
            }
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
    BackHandler(enabled = uiState.isOverlayVisible()) {
        uiState.handleBack()
    }

    // ── Masthead height tracking (measured once rendered, used for page top-padding) ──
    val density = LocalDensity.current
    var mastheadHeightPx by remember { mutableIntStateOf(0) }
    val mastheadHeightDp: Dp = with(density) { mastheadHeightPx.toDp() }

    // ── Masthead alpha ─────────────────────────────────────────────────────
    // The masthead fades for two independent reasons, multiplied together:
    //   1. Vertical scroll on the news page — keeps the masthead from layering
    //      on top of RSS headlines as the list moves up.
    //   2. Horizontal swipe toward the notes page — notes has no masthead, so
    //      we clear it before page 2 is fully on-screen.

    val rssListState = rememberLazyListState()

    // (1) News page: fully visible at the top of the list, fully hidden after
    //     the user has scrolled RSS_SCROLL_FADE_DISTANCE or moved past the header.
    val rssScrollAlpha by remember(rssListState, density) {
        derivedStateOf {
            val pastHeader = rssListState.firstVisibleItemIndex > 0
            when {
                pastHeader -> 0f
                else -> {
                    val fadePx = with(density) { RSS_SCROLL_FADE_DISTANCE.toPx() }
                    val progress = (rssListState.firstVisibleItemScrollOffset / fadePx)
                        .coerceIn(0f, 1f)
                    1f - progress
                }
            }
        }
    }

    // (2) Pager swipe toward notes: starts fading as page 1 → 2 begins, fully
    //     clear by 30% of the swipe (so the masthead exits before "Right Handed"
    //     slides into view). Uses smoothstep for eased S-curve motion.
    val pagerSwipeAlpha by remember(pagerState) {
        derivedStateOf {
            val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val progress = ((position - 1f) / PAGE_EXIT_FRACTION).coerceIn(0f, 1f)
            1f - smoothstep(progress)
        }
    }

    // Combined: the scroll fade only applies while we're on or near the news page.
    val mastheadAlpha by remember(pagerState, rssScrollAlpha, pagerSwipeAlpha) {
        derivedStateOf {
            val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val onNewsPage = position < 0.5f
            if (onNewsPage) pagerSwipeAlpha * rssScrollAlpha else pagerSwipeAlpha
        }
    }

    // Masthead variant morph: news (page 0) = minimal day+time only,
    // main (page 1) = full date/season/weather. Smooth crossfade between.
    val mastheadFullness by remember(pagerState) {
        derivedStateOf {
            val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
            pos.coerceIn(0f, 1f) // 0 at news, 1 at main
        }
    }

    // Ambient reveal: on the main page the masthead rests at a low alpha so the
    // eye isn't constantly pulled to it. A tap anywhere pulses it to full for
    // 3 seconds, then it fades back. On the news page it stays fully visible.
    var mastheadRevealed by remember { mutableStateOf(false) }
    val onMainPage by remember(pagerState) {
        derivedStateOf { mastheadFullness > 0.8f }
    }
    val ambientMastheadAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            !onMainPage -> 1f
            mastheadRevealed -> 1f
            else -> 0.30f
        },
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (mastheadRevealed) 200 else 400
        ),
        label = "masthead-ambient-reveal"
    )
    LaunchedEffect(mastheadRevealed) {
        if (mastheadRevealed) {
            delay(3000L)
            mastheadRevealed = false
        }
    }

    // Idle blackout: 10s after the chrome recedes on the main page, the palette
    // background drifts to true black over 3s. Any tap reverses it in 400ms.
    // Skipped in light themes (DAYLIGHT_OUTDOOR, TWILIGHT) — fading a light screen
    // to black reads as a glitch, and makes dark text illegible.
    val ambientMode = AmbientTheme.mode
    val shouldBlackout by remember(mastheadRevealed, onMainPage, ambientMode, ambientPalette.isDark) {
        derivedStateOf {
            onMainPage &&
                !mastheadRevealed &&
                ambientPalette.isDark
        }
    }
    var idleElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(shouldBlackout) {
        idleElapsed = false
        if (shouldBlackout) {
            delay(10_000L)
            idleElapsed = true
        }
    }
    val idleBlackout by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (idleElapsed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (idleElapsed) 3000 else 400
        ),
        label = "idle-blackout"
    )

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Peek taps in the Initial pass — triggers the reveal without
            // consuming the event, so app-icon launches still work.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && onMainPage) {
                            mastheadRevealed = true
                        }
                    }
                }
            }
    ) {
        WallpaperBackplane(idleBlackout = idleBlackout)

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
                    topPadding           = 90.dp,
                    briefing             = briefing,
                    isBriefingLoading    = isBriefingLoading,
                    briefingHasError     = briefingHasError,
                    isAnalysisLoading    = isAnalysisLoading,
                    isAnalysisReady      = isAnalysisReady,
                    analysisHasError     = analysisHasError,
                    feedItems            = feedItems,
                    isRefreshing         = isFeedRefreshing,
                    lastRefreshTime      = lastFeedRefreshTime,
                    rssListState         = rssListState,
                    onRefresh            = {
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
                    onFeedClick          = { uiState.viewingArticle = it },
                    onToggleStar         = { dashboardViewModel.toggleStar(it) },
                    onDelete             = { dashboardViewModel.deleteFeedItem(it) },
                    onBriefingClick      = {
                        if (briefingHasError) {
                            briefingViewModel.generateBriefing(
                                items = feedItems.take(20),
                                force = true
                            )
                        } else {
                            uiState.viewingAnalysis = true
                            val current = briefing
                            if (!current.isNullOrBlank() && !isAnalysisLoading && analysis == null) {
                                if (briefingViewModel.isAnalysisCached(current))
                                    briefingViewModel.loadCachedAnalysis(current)
                                else
                                    briefingViewModel.generateAnalysis(current)
                            }
                        }
                    },
                    onRefreshBriefing = {
                        briefingViewModel.clearBriefingCache()
                        briefingViewModel.generateBriefing(
                            items = feedItems.take(20),
                            force = true
                        )
                    }
                )

                // ── Page 1: Home apps (default) ───────────────────────────────
                1 -> MiddleHomeScreen(
                    topApps       = topApps,
                    allApps       = allApps,
                    appsByPackage = appsByPackage,
                    topPadding    = 140.dp,
                    topHeadline   = feedItems.firstOrNull(),
                    onHeadlineClick = { uiState.viewingArticle = it },
                    onAppClick    = { app ->
                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(it)
                        }
                    },
                    onSwipeUp     = { uiState.showAppMenu = true },
                    ambientAlpha  = ambientMastheadAlpha
                )

                // ── Page 2: S Pen notes ───────────────────────────────────────
                2 -> SPenNotesScreen()
            }
        }

        // Wabi-sabi grain — drawn above pager content, below masthead and overlays.
        // Subtle (~4% alpha) texture that combats OLED moire at oblique viewing angles.
        com.ambient.launcher.ui.theme.GrainOverlay(idleBlackout = idleBlackout)

        // ── Floating masthead — shared across pages 0 & 1, fades before page 2 ─
        // Rendered above the pager so it doesn't move during horizontal swipes.
        // Pages reserve top space equal to mastheadHeightDp via their topPadding param.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp) // Pinned height to prevent layout jumps during swipe
                .align(Alignment.TopStart)
                .alpha(mastheadAlpha * ambientMastheadAlpha)
        ) {
            Masthead(
                battery  = battery,
                modifier = Modifier.fillMaxWidth(),
                fullness = mastheadFullness
            )
        }

        // ── Overlays (sit above the pager) ────────────────────────────────────

        AnimatedVisibility(
            visible = uiState.viewingAnalysis,
            enter   = fadeIn(animationSpec = tween(240)),
            exit    = fadeOut(animationSpec = tween(180))
        ) {
            AnalysisScreen(
                briefingText = briefing ?: "",
                viewModel    = briefingViewModel,
                onDismiss    = { uiState.viewingAnalysis = false }
            )
        }

        AnimatedVisibility(
            visible = uiState.viewingArticle != null,
            enter   = fadeIn(animationSpec = tween(240)),
            exit    = fadeOut(animationSpec = tween(180))
        ) {
            val article = uiState.viewingArticle
            if (article != null) {
                ArticleViewerScreen(article = article, onDismiss = { uiState.viewingArticle = null })
            }
        }

        AppMenuCard(
            isOpen          = uiState.showAppMenu,
            visibleBuckets  = visibleBuckets,
            bucketApps      = bucketApps,
            configuration   = configuration,
            newApps         = newApps,
            allApps         = allApps,
            onDismiss       = { uiState.showAppMenu = false },
            onToggleCollapse = { bucket -> updateConfiguration(configuration.toggleCollapse(bucket)) },
            onAppClick      = { app ->
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                uiState.showAppMenu = false
            },
            onAppLongClick  = { uiState.editingApp = it }
        )

        if (uiState.editingApp != null) {
            AppEditDialog(
                app           = uiState.editingApp!!,
                configuration = configuration,
                onDismiss     = { uiState.editingApp = null },
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
    briefingHasError: Boolean,
    isAnalysisLoading: Boolean,
    isAnalysisReady: Boolean,
    analysisHasError: Boolean,
    feedItems: List<RssFeedItem>,
    isRefreshing: Boolean,
    lastRefreshTime: Long,
    rssListState: LazyListState,
    onRefresh: () -> Unit,
    onOpenCloudNotifications: () -> Unit,
    onFeedClick: (RssFeedItem) -> Unit,
    onToggleStar: (RssFeedItem) -> Unit,
    onDelete: (RssFeedItem) -> Unit,
    onBriefingClick: () -> Unit,
    onRefreshBriefing: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        HeadlinesSection(
            feedItems       = feedItems,
            isRefreshing    = isRefreshing,
            lastRefreshTime = lastRefreshTime,
            onFeedClick     = onFeedClick,
            onToggleStar    = onToggleStar,
            onDelete        = onDelete,
            onRefresh       = onRefresh,
            listState       = rssListState,
            modifier        = Modifier.fillMaxSize(),
            headerContent   = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Spacer(modifier = Modifier.height(topPadding))

                    TodaysSignal(
                        lastRefreshTime = lastRefreshTime,
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
                        briefingHasError      = briefingHasError,
                        isAnalysisLoading     = isAnalysisLoading,
                        isAnalysisReady       = isAnalysisReady,
                        analysisHasError      = analysisHasError,
                        onClick               = onBriefingClick,
                        onOpenCloudNotifications = onOpenCloudNotifications,
                        onRefreshBriefing     = onRefreshBriefing,
                        modifier              = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

// ── Wallpaper backplane ───────────────────────────────────────────────────────

@Composable
private fun WallpaperBackplane(idleBlackout: Float = 0f) {
    val ambientPalette = AmbientTheme.palette
    val bgColor = androidx.compose.ui.graphics.lerp(
        ambientPalette.mainBackground,
        Color.Black,
        idleBlackout.coerceIn(0f, 1f)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    )
}


