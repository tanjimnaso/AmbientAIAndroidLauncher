package com.ambient.launcher.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.util.Calendar

private const val HOME_PREFS_NAME = "home_screen_prefs"
private const val HOME_PREFS_KEY  = "pinned_home_packages"
private const val PINNED_ROW_SIZE = 4

/**
 * MiddleHomeScreen — page 1 (default) of the 3-page launcher.
 *
 * Asymmetric layout (Wacom interaction-shelf inspired):
 *   • Masthead reserves top space (rendered at root level)
 *   • A loose row of 4 secondary pinned apps, left-aligned
 *   • A contextual "hero" tile bottom-right — large, swaps by time-of-day
 *
 * The hero is computed from `topApps` filtered by time-appropriate buckets
 * (e.g., NEWS in the morning, ENTERTAINMENT in the evening). Falls back to
 * the most-used app overall if no contextual match exists.
 *
 * Gestures:
 *   • Swipe up      → App Menu (only this page triggers it)
 *   • Long-press + drag on the row → reorder pinned apps
 *   • Tap icon      → launch app
 */
@Composable
internal fun MiddleHomeScreen(
    topApps: List<AppInfo>,
    appsByPackage: Map<String, AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onSwipeUp: () -> Unit,
    topHeadline: String? = null,
    weather: WeatherUiState = WeatherUiState(),
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (pinnedRow, savePinnedRow) = rememberPinnedHomeApps(context, topApps, appsByPackage)

    // Recompute hero every 5 minutes so the slot can drift across time-of-day.
    var hourTick by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60 * 1000L)
            hourTick = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val heroApp = remember(topApps, pinnedRow, hourTick) {
        pickContextualHero(topApps, pinnedRow, hourTick)
    }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val bottomLift = screenHeight * 0.10f

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30f) onSwipeUp()
                }
            }
    ) {
        Spacer(modifier = Modifier.height(topPadding))
        Spacer(modifier = Modifier.weight(0.4f))

        NowMoment(topHeadline = topHeadline, weather = weather)

        Spacer(modifier = Modifier.weight(0.6f))

        // Row of 4 secondary apps — left-aligned, loose spacing.
        SecondaryAppRow(
            apps      = pinnedRow,
            onAppClick = onAppClick,
            onReorder  = savePinnedRow,
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero tile — bottom-right, large, contextual.
        if (heroApp != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                HeroAppTile(
                    app = heroApp,
                    onClick = { onAppClick(heroApp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomLift))
    }
}

// ── Contextual hero selection ─────────────────────────────────────────────────

/**
 * Picks the hero app based on current hour. Maps time-of-day to a preference
 * order over [LauncherBucket]s, then picks the most-used app from `topApps`
 * matching the first non-empty bucket. Excludes apps already in the secondary row.
 */
private fun pickContextualHero(
    topApps: List<AppInfo>,
    pinnedRow: List<AppInfo>,
    hour: Int
): AppInfo? {
    val rowPackages = pinnedRow.map { it.packageName }.toSet()
    val pool = topApps.filter { it.packageName !in rowPackages }
    if (pool.isEmpty()) return topApps.firstOrNull()

    val preferredBuckets = when (hour) {
        in 5..10  -> listOf(LauncherBucket.NEWS, LauncherBucket.AI, LauncherBucket.UTILITIES)
        in 11..16 -> listOf(LauncherBucket.SOCIAL, LauncherBucket.UTILITIES, LauncherBucket.AI)
        in 17..21 -> listOf(LauncherBucket.ENTERTAINMENT, LauncherBucket.NEWS, LauncherBucket.SOCIAL)
        else      -> listOf(LauncherBucket.TOOLS, LauncherBucket.UTILITIES, LauncherBucket.NEWS)
    }
    return preferredBuckets.firstNotNullOfOrNull { bucket ->
        pool.firstOrNull { it.bucket == bucket }
    } ?: pool.first()
}

// ── Pinned-row persistence ────────────────────────────────────────────────────

@Composable
private fun rememberPinnedHomeApps(
    context: Context,
    topApps: List<AppInfo>,
    appsByPackage: Map<String, AppInfo>
): Pair<List<AppInfo>, (List<AppInfo>) -> Unit> {
    val prefs = remember { context.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE) }

    var pinnedPackages by remember {
        val saved = prefs.getString(HOME_PREFS_KEY, null)
        val initial: List<String> = if (saved != null) {
            JSONArray(saved).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        } else {
            topApps.take(PINNED_ROW_SIZE).map { it.packageName }
        }
        mutableStateOf(initial)
    }

    LaunchedEffect(topApps) {
        if (pinnedPackages.isEmpty()) {
            pinnedPackages = topApps.take(PINNED_ROW_SIZE).map { it.packageName }
        }
    }

    val pinnedApps = remember(pinnedPackages, topApps) {
        val combined = appsByPackage + topApps.associateBy { it.packageName }
        val resolved = pinnedPackages.mapNotNull { combined[it] }.take(PINNED_ROW_SIZE)
        val extras   = topApps.filter { it.packageName !in resolved.map { a -> a.packageName } }
            .take(PINNED_ROW_SIZE - resolved.size)
        (resolved + extras).take(PINNED_ROW_SIZE)
    }

    val save: (List<AppInfo>) -> Unit = { apps ->
        val packages = apps.map { it.packageName }.take(PINNED_ROW_SIZE)
        pinnedPackages = packages
        prefs.edit().putString(HOME_PREFS_KEY, JSONArray(packages).toString()).apply()
    }

    return Pair(pinnedApps, save)
}

// ── Secondary row: 4 apps with drag-reorder ──────────────────────────────────

@Composable
private fun SecondaryAppRow(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onReorder: (List<AppInfo>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset    by remember { mutableStateOf(Offset.Zero) }
    var dragStart     by remember { mutableStateOf(Offset.Zero) }
    var targetIndex   by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier.height(96.dp)) {
        val wPx = with(density) { maxWidth.toPx() }
        val cellW = wPx / PINNED_ROW_SIZE.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(apps) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val idx = (offset.x / cellW).toInt().coerceIn(0, PINNED_ROW_SIZE - 1)
                            if (idx < apps.size) {
                                draggedIndex = idx
                                dragStart    = offset
                                dragOffset   = Offset.Zero
                            }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            dragOffset += delta
                            val absX = dragStart.x + dragOffset.x
                            targetIndex = (absX / cellW).toInt().coerceIn(0, apps.size - 1)
                        },
                        onDragEnd = {
                            val from = draggedIndex
                            val to   = targetIndex
                            if (from != null && to != null && from != to) {
                                val list = apps.toMutableList()
                                val item = list.removeAt(from)
                                list.add(to.coerceIn(0, list.size), item)
                                onReorder(list)
                            }
                            draggedIndex = null; targetIndex = null; dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            draggedIndex = null; targetIndex = null; dragOffset = Offset.Zero
                        }
                    )
                }
        ) {
            apps.forEachIndexed { index, app ->
                val isDragged = index == draggedIndex
                val isTarget  = index == targetIndex && draggedIndex != null && targetIndex != draggedIndex
                val basePxX   = cellW * index + if (isDragged) dragOffset.x else 0f
                val basePxY   = if (isDragged) dragOffset.y else 0f

                Box(
                    modifier = Modifier
                        .offset { IntOffset(basePxX.toInt(), basePxY.toInt()) }
                        .size(with(density) { cellW.toDp() }, 96.dp)
                        .zIndex(if (isDragged) 10f else 0f)
                        .then(
                            if (isTarget) Modifier.background(
                                AmbientTheme.palette.textPrimary.copy(alpha = 0.06f),
                                RoundedCornerShape(14.dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryAppIcon(
                        app       = app,
                        isDragged = isDragged,
                        onClick   = { if (draggedIndex == null) onAppClick(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryAppIcon(
    app: AppInfo,
    isDragged: Boolean,
    onClick: () -> Unit
) {
    val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (isDragged) Modifier.background(
                        AmbientTheme.palette.tileBackground.copy(alpha = 0.55f),
                        RoundedCornerShape(12.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier    = Modifier.size(34.dp),
                colorFilter = getAmbientIconFilter(bucketColor)
            )
        }
        Text(
            text     = app.label,
            style    = ResponsiveTypography.t3.copy(fontSize = 10.sp),
            color    = AmbientTheme.palette.textPrimary.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Hero tile: large, contextual ─────────────────────────────────────────────

@Composable
private fun HeroAppTile(
    app: AppInfo,
    onClick: () -> Unit
) {
    val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier    = Modifier.size(80.dp),
                colorFilter = getAmbientIconFilter(bucketColor)
            )
        }
        Text(
            text     = app.label,
            style    = ResponsiveTypography.t2,
            color    = AmbientTheme.palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
