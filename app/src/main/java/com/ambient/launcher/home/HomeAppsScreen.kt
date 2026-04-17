package com.ambient.launcher.home

import android.content.Context
import androidx.compose.foundation.Image
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
import com.ambient.launcher.ui.theme.AmbientTheme
import org.json.JSONArray

private const val HOME_PREFS_NAME = "home_screen_prefs"
private const val HOME_PREFS_KEY  = "pinned_home_packages"

/**
 * MiddleHomeScreen — page 1 (default) of the 3-page launcher.
 *
 * Layout:
 *   • Masthead (day / time / battery / weather)
 *   • 3 × 2 grid of the 6 most-used apps, shown as icons
 *
 * Gestures:
 *   • Swipe up  → App Menu (only this page triggers it)
 *   • Long-press + drag on the grid → reorder pinned apps
 *   • Tap icon → launch app
 */
@Composable
internal fun MiddleHomeScreen(
    topApps: List<AppInfo>,
    appsByPackage: Map<String, AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onSwipeUp: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (pinnedApps, savePinnedApps) = rememberPinnedHomeApps(context, topApps, appsByPackage)

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
        // Reserve space for the floating masthead rendered at the root level
        Spacer(modifier = Modifier.height(topPadding))

        Spacer(modifier = Modifier.height(16.dp))

        HomeAppGrid(
            pinnedApps = pinnedApps,
            onAppClick = { if (pinnedApps.isNotEmpty()) onAppClick(it) },
            onReorder = savePinnedApps,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 24.dp)
        )
    }
}

// ── Pinned-apps persistence ───────────────────────────────────────────────────

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
            topApps.take(6).map { it.packageName }
        }
        mutableStateOf(initial)
    }

    // Seed / top-up when installed apps change
    LaunchedEffect(topApps) {
        if (pinnedPackages.isEmpty()) {
            pinnedPackages = topApps.take(6).map { it.packageName }
        }
    }

    val pinnedApps = remember(pinnedPackages, topApps) {
        val combined = appsByPackage + topApps.associateBy { it.packageName }
        val resolved = pinnedPackages.mapNotNull { combined[it] }
        val extras   = topApps.filter { it.packageName !in pinnedPackages }.take(6 - resolved.size)
        (resolved + extras).take(6)
    }

    val save: (List<AppInfo>) -> Unit = { apps ->
        val packages = apps.map { it.packageName }
        pinnedPackages = packages
        prefs.edit().putString(HOME_PREFS_KEY, JSONArray(packages).toString()).apply()
    }

    return Pair(pinnedApps, save)
}

// ── 3 × 2 icon grid with drag-to-reorder ─────────────────────────────────────

@Composable
private fun HomeAppGrid(
    pinnedApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onReorder: (List<AppInfo>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cols = 3
    val rows = 2

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset    by remember { mutableStateOf(Offset.Zero) }
    var dragStart     by remember { mutableStateOf(Offset.Zero) }
    var targetIndex   by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val cellW = wPx / cols
        val cellH = hPx / rows

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pinnedApps) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val col = (offset.x / cellW).toInt().coerceIn(0, cols - 1)
                            val row = (offset.y / cellH).toInt().coerceIn(0, rows - 1)
                            val idx = row * cols + col
                            if (idx < pinnedApps.size) {
                                draggedIndex = idx
                                dragStart    = offset
                                dragOffset   = Offset.Zero
                            }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            dragOffset += delta
                            val absX = dragStart.x + dragOffset.x
                            val absY = dragStart.y + dragOffset.y
                            val col = (absX / cellW).toInt().coerceIn(0, cols - 1)
                            val row = (absY / cellH).toInt().coerceIn(0, rows - 1)
                            targetIndex = (row * cols + col).coerceIn(0, pinnedApps.size - 1)
                        },
                        onDragEnd = {
                            val from = draggedIndex
                            val to   = targetIndex
                            if (from != null && to != null && from != to) {
                                val list = pinnedApps.toMutableList()
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
            pinnedApps.forEachIndexed { index, app ->
                val col       = index % cols
                val row       = index / cols
                val isDragged = index == draggedIndex
                val isTarget  = index == targetIndex && draggedIndex != null && targetIndex != draggedIndex

                val basePxX   = cellW * col + if (isDragged) dragOffset.x else 0f
                val basePxY   = cellH * row + if (isDragged) dragOffset.y else 0f

                Box(
                    modifier = Modifier
                        .offset { IntOffset(basePxX.toInt(), basePxY.toInt()) }
                        .size(with(density) { cellW.toDp() }, with(density) { cellH.toDp() })
                        .zIndex(if (isDragged) 10f else 0f)
                        .then(
                            if (isTarget) Modifier.background(
                                AmbientTheme.palette.textPrimary.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    HomeAppIcon(
                        app       = app,
                        isDragged = isDragged,
                        onClick   = { if (draggedIndex == null) onAppClick(app) }
                    )
                }
            }
        }
    }
}

// ── Single icon cell ──────────────────────────────────────────────────────────

@Composable
private fun HomeAppIcon(
    app: AppInfo,
    isDragged: Boolean,
    onClick: () -> Unit
) {
    val iconBitmap  = rememberAppIcon(app.packageName)
    val bucketColor = app.bucket.themeColor(AmbientTheme.palette)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .then(
                    if (isDragged) Modifier.background(
                        AmbientTheme.palette.tileBackground.copy(alpha = 0.55f),
                        RoundedCornerShape(14.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            iconBitmap?.let {
                Image(
                    bitmap           = it,
                    contentDescription = app.label,
                    modifier         = Modifier.size(44.dp),
                    colorFilter      = getAmbientIconFilter(bucketColor)
                )
            } ?: Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AmbientTheme.palette.tileBackground, RoundedCornerShape(12.dp))
            )
        }

        Text(
            text     = app.label,
            style    = ResponsiveTypography.t3.copy(fontSize = 11.sp),
            color    = AmbientTheme.palette.textPrimary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
