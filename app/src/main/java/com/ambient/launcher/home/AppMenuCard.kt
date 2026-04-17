package com.ambient.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.Image
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily

/**
 * AppMenuCard
 *
 * Slides up from the bottom. Contains:
 *   • Search bar at the top (filters bucket rows and app chips in real time)
 *   • Scrollable bucket list (no pinned app tiles — managed on the home screen instead)
 *
 * Signature change from previous version: [topApps] and [tileSizes] parameters removed.
 */
@Composable
internal fun AppMenuCard(
    isOpen: Boolean,
    visibleBuckets: List<LauncherBucket>,
    bucketApps: Map<LauncherBucket, List<AppInfo>>,
    configuration: LauncherConfiguration,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter buckets and apps by search query
    val filteredBuckets = remember(searchQuery, visibleBuckets, bucketApps) {
        if (searchQuery.isBlank()) visibleBuckets
        else LauncherBucket.entries.filter { bucket ->
            bucket != LauncherBucket.SYSTEM_BLOAT && bucketApps[bucket].orEmpty().any {
                it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val filteredApps = remember(searchQuery, bucketApps) {
        if (searchQuery.isBlank()) bucketApps
        else bucketApps.mapValues { (_, apps) ->
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Clear search when closed
    LaunchedEffect(isOpen) { if (!isOpen) searchQuery = "" }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrim ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isOpen,
            enter   = fadeIn(animationSpec = tween(220)),
            exit    = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            )
        }

        // ── Sheet panel ───────────────────────────────────────────────────────
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible  = isOpen,
            enter    = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = tween(380, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(180)),
            exit     = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(260, easing = FastOutLinearInEasing)
            ) + fadeOut(tween(160))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.93f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(AmbientTheme.palette.drawerBackground)
                    .pointerInput(Unit) { detectTapGestures { /* consume */ } }
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                // ── Drag handle ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 14.dp)
                        .width(80.dp)
                        .height(20.dp)
                        .pointerInput(onDismiss) {
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragEnd    = { if (totalDrag > 60f) onDismiss(); totalDrag = 0f },
                                onDragCancel = { totalDrag = 0f },
                                onVerticalDrag = { change, delta ->
                                    change.consume(); totalDrag += delta
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                AmbientTheme.palette.textSecondary.copy(alpha = 0.2f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                // ── Search bar ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AmbientTheme.palette.textPrimary.copy(alpha = 0.07f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text  = "Search apps…",
                            style = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Normal),
                            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.4f)
                        )
                    }
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine    = true,
                        textStyle     = ResponsiveTypography.t2.copy(
                            color      = AmbientTheme.palette.textPrimary,
                            fontFamily = InterFontFamily
                        ),
                        cursorBrush = SolidColor(AmbientTheme.palette.accentHigh),
                        modifier    = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── App list ──────────────────────────────────────────────────
                // nestedScrollConnection: when list is at top and user swipes down,
                // the unconsumed downward scroll (available.y > 0 in onPostScroll)
                // accumulates. Once past 120px or a fast fling, the sheet dismisses.
                val listState = rememberLazyListState()
                val dismissOnSwipeDown = remember(onDismiss) {
                    object : NestedScrollConnection {
                        private var accumulated = 0f
                        override fun onPostScroll(
                            consumed: Offset, available: Offset, source: NestedScrollSource
                        ): Offset {
                            if (available.y > 0f) {
                                accumulated += available.y
                                if (accumulated > 120f) { onDismiss(); accumulated = 0f }
                            } else { accumulated = 0f }
                            return Offset.Zero
                        }
                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            if (available.y > 600f) onDismiss()
                            accumulated = 0f
                            return Velocity.Zero
                        }
                    }
                }

                LazyColumn(
                    state                 = listState,
                    modifier              = Modifier.fillMaxWidth().nestedScroll(dismissOnSwipeDown),
                    verticalArrangement   = Arrangement.spacedBy(24.dp),
                    contentPadding        = PaddingValues(bottom = 80.dp)
                ) {
                    if (filteredBuckets.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text     = "No apps matching \"$searchQuery\"",
                                style    = ResponsiveTypography.t2,
                                color    = AmbientTheme.palette.textSecondary.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    itemsIndexed(filteredBuckets, key = { _, b -> b.name }) { index, bucket ->
                        if (bucket == LauncherBucket.SMART_HOME) {
                            AppMenuSmartHomeRow(
                                apps         = filteredApps[bucket].orEmpty(),
                                onAppClick   = { app -> onAppClick(app); onDismiss() }
                            )
                        } else {
                            AppMenuBucketRow(
                                indexLabel   = (index + 1).toString().padStart(2, '0'),
                                title        = configuration.displayTitle(bucket),
                                apps         = filteredApps[bucket].orEmpty(),
                                onAppClick   = { app -> onAppClick(app); onDismiss() },
                                onAppLongClick = onAppLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Bucket row ────────────────────────────────────────────────────────────────

@Composable
private fun AppMenuBucketRow(
    indexLabel: String,
    title: String,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = indexLabel,
                style    = ResponsiveTypography.t3,
                color    = AmbientTheme.palette.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp)
            )
            Text(
                text  = title,
                style = ResponsiveTypography.t1,
                color = AmbientTheme.palette.textPrimary
            )
        }

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier                = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 8.dp),
            horizontalArrangement   = Arrangement.spacedBy(8.dp),
            verticalArrangement     = Arrangement.spacedBy(8.dp)
        ) {
            apps.forEach { app ->
                androidx.compose.material3.Surface(
                    shape    = RoundedCornerShape(50),
                    color    = AmbientTheme.palette.tileBackground,
                    border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.clickable { onAppClick(app) }
                ) {
                    Text(
                        text     = app.label,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = AmbientTheme.palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppMenuSmartHomeRow(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier                = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 8.dp),
        horizontalArrangement   = Arrangement.SpaceBetween,
        verticalArrangement     = Arrangement.spacedBy(16.dp)
    ) {
        apps.forEach { app ->
            val iconBitmap  = rememberAppIcon(packageName = app.packageName)
            val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
            if (iconBitmap != null) {
                Image(
                    bitmap             = iconBitmap,
                    contentDescription = app.label,
                    colorFilter        = getAmbientIconFilter(bucketColor),
                    modifier           = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAppClick(app) }
                )
            } else {
                androidx.compose.material3.Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = AmbientTheme.palette.tileBackground,
                    border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.size(48.dp).clickable { onAppClick(app) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text     = app.label.take(1),
                            style    = MaterialTheme.typography.bodyLarge,
                            color    = AmbientTheme.palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}
