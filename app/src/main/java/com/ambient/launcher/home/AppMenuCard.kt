package com.ambient.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

// System apps that must never appear in New Apps or All Apps view.
// Extend this list after testing.
internal val NEVER_SHOW_PACKAGES = setOf(
    // Android core & Google services
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.google.android.webview",
    "com.android.htmlviewer",
    "com.android.backupconfirm",
    "com.android.providers.downloads",
    "com.android.providers.media",
    "com.android.providers.media.module",
    "com.android.providers.contacts",
    "com.android.providers.calendar",
    "com.android.providers.telephony",
    "com.android.settings",
    "com.android.systemui",
    "com.android.phone",
    "com.android.server.telecom",
    "com.android.inputmethod.latin",
    "com.android.bluetooth",
    "com.android.nfc",
    "com.android.wallpaper",
    "com.android.permissioncontroller",
    "com.android.packageinstaller",
    "com.google.android.inputmethod.latin",
    "com.google.android.packageinstaller",
    "com.google.android.configupdater",
    "com.google.android.ext.services",
    "com.google.android.ext.shared",
    "com.google.android.permissioncontroller",
    "com.google.android.adservices.api",
    "com.google.android.federatedcompute",
    "com.google.android.ondevicepersonalization.services",
    // Samsung system
    "com.samsung.android.incallui",
    "com.samsung.android.app.telephonyui",
    "com.samsung.android.sm.devicesecurity",
    "com.samsung.android.sm",
    "com.samsung.android.providers.media",
    "com.samsung.android.providers.contacts",
    "com.samsung.android.providers.calendar",
    "com.samsung.android.knox.attestation",
    "com.samsung.android.samsungpass",
    "com.samsung.android.samsungpassautofill",
    "com.samsung.android.secsettings",
    "com.samsung.android.location",
    "com.samsung.android.networkstack",
    "com.samsung.android.networkstack.tethering",
    "com.samsung.android.biometrics.app.setting",
    "com.samsung.android.sdk.handwriting",
    "com.samsung.android.kgclient",
    "com.samsung.android.wifi.softap.resources",
    "com.sec.android.app.launcher",
    "com.sec.android.inputmethod",
    "com.samsung.android.honeyboard",
    "com.samsung.android.app.settings.bixby",
    "com.samsung.android.bixby.wakeup",
    "com.samsung.android.bixby.service",
    "com.samsung.android.bixby.agent",
    "com.samsung.android.bixby.voiceinput",
    "com.samsung.android.game.gamehome",
    "com.samsung.android.app.smartcapture",
    "com.samsung.android.kids",
    "com.samsung.android.app.spage",
    "com.samsung.android.themestore",
    "com.samsung.android.themecenter",
    "com.samsung.android.mobileservice",
    "com.samsung.android.app.updatecenter",
    "com.samsung.android.app.tips",
    "com.samsung.android.soundassistant",
    "com.sec.android.diagmonagent",
    "com.samsung.android.app.dressroom",
    "com.samsung.android.service.aircommand",
    "com.samsung.android.app.homestar",
    "com.samsung.android.app.goodcatch",
    "com.sec.android.easyMover",
    "com.samsung.android.smartswitch",
)

/**
 * AppMenuCard
 *
 * Slides up from the bottom. Contains:
 *   • Search bar at the top
 *   • "New Apps" special group (installed last 7 days) with an "all apps" button
 *   • Scrollable bucket list with tap-to-collapse titles and long-press to reassign
 *
 * Scroll-dismiss is two-gesture: the first gesture scrolls the list to the top;
 * only a second, separate gesture (swipe-down from top) dismisses the card.
 */
@Composable
internal fun AppMenuCard(
    isOpen: Boolean,
    visibleBuckets: List<LauncherBucket>,
    bucketApps: Map<LauncherBucket, List<AppInfo>>,
    configuration: LauncherConfiguration,
    newApps: List<AppInfo>,
    allApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onToggleCollapse: (LauncherBucket) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }

    // Reset state when card closes
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            searchQuery = ""
            showAllApps = false
        }
    }

    // Filter regular buckets and their apps
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

    // Filter special groups by search
    val filteredNewApps = remember(searchQuery, newApps) {
        if (searchQuery.isBlank()) newApps
        else newApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    val filteredAllApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

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
                // Two-gesture dismiss: a gesture that scrolled the list resets the dismiss
                // accumulator. Only a fresh gesture starting from the top can dismiss.
                val listState = rememberLazyListState()
                val dismissOnSwipeDown = remember(onDismiss) {
                    object : NestedScrollConnection {
                        private var accumulated = 0f
                        // True if the current touch gesture moved the list content at all.
                        // Resets in onPostFling (fires even after slow drags).
                        private var gestureScrolledList = false

                        override fun onPostScroll(
                            consumed: Offset, available: Offset, source: NestedScrollSource
                        ): Offset {
                            when {
                                consumed.y != 0f -> {
                                    // List scrolled this frame — block dismiss for this gesture
                                    gestureScrolledList = true
                                    accumulated = 0f
                                }
                                available.y > 0f && !gestureScrolledList -> {
                                    // List is at top, finger moving down — accumulate for dismiss
                                    accumulated += available.y
                                    if (accumulated > 120f) { onDismiss(); accumulated = 0f }
                                }
                                else -> accumulated = 0f
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            // Only fling-dismiss if the fling started from the top (list not scrolled this gesture)
                            if (!gestureScrolledList && available.y > 600f) onDismiss()
                            gestureScrolledList = false
                            accumulated = 0f
                            return Velocity.Zero
                        }
                    }
                }

                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxWidth().nestedScroll(dismissOnSwipeDown),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding      = PaddingValues(bottom = 80.dp)
                ) {
                    if (filteredBuckets.isEmpty() && filteredNewApps.isEmpty() && filteredAllApps.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text     = "No apps matching \"$searchQuery\"",
                                style    = ResponsiveTypography.t2,
                                color    = AmbientTheme.palette.textSecondary.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    // ── Special group: New Apps / All ─────────────────────────
                    item(key = "new_apps_special") {
                        NewAppsRow(
                            newApps        = filteredNewApps,
                            allApps        = filteredAllApps,
                            showAll        = showAllApps,
                            onShowAllToggle = { showAllApps = !showAllApps },
                            onAppClick     = { app -> onAppClick(app); onDismiss() },
                            onAppLongClick = onAppLongClick
                        )
                    }

                    // ── Regular bucket groups ─────────────────────────────────
                    itemsIndexed(filteredBuckets, key = { _, b -> b.name }) { index, bucket ->
                        if (bucket == LauncherBucket.SMART_HOME) {
                            AppMenuSmartHomeRow(
                                apps       = filteredApps[bucket].orEmpty(),
                                onAppClick = { app -> onAppClick(app); onDismiss() }
                            )
                        } else {
                            AppMenuBucketRow(
                                indexLabel      = (index + 1).toString().padStart(2, '0'),
                                title           = configuration.displayTitle(bucket),
                                apps            = filteredApps[bucket].orEmpty(),
                                isCollapsed     = searchQuery.isBlank() && configuration.isCollapsed(bucket),
                                onToggleCollapse = { onToggleCollapse(bucket) },
                                onAppClick      = { app -> onAppClick(app); onDismiss() },
                                onAppLongClick  = onAppLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── New Apps / All Apps special group ─────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewAppsRow(
    newApps: List<AppInfo>,
    allApps: List<AppInfo>,
    showAll: Boolean,
    onShowAllToggle: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val displayApps = if (showAll) allApps else newApps
    val title       = if (showAll) "All" else "New Apps"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "00",
                style    = ResponsiveTypography.t3,
                color    = AmbientTheme.palette.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp)
            )
            Text(
                text     = title,
                style    = ResponsiveTypography.t1,
                color    = AmbientTheme.palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text     = if (showAll) "↑ new apps" else "all apps →",
                style    = ResponsiveTypography.t3.copy(fontFamily = InterFontFamily),
                color    = AmbientTheme.palette.accentHigh,
                modifier = Modifier
                    .clickable { onShowAllToggle() }
                    .padding(start = 8.dp)
            )
        }

        AnimatedVisibility(visible = displayApps.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                displayApps.forEach { app ->
                    androidx.compose.material3.Surface(
                        shape    = RoundedCornerShape(50),
                        color    = AmbientTheme.palette.tileBackground,
                        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.combinedClickable(
                            onClick     = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
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

        if (displayApps.isEmpty()) {
            Text(
                text     = if (showAll) "No apps" else "No new apps this week",
                style    = ResponsiveTypography.t3,
                color    = AmbientTheme.palette.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 32.dp, top = 6.dp)
            )
        }
    }
}

// ── Bucket row ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppMenuBucketRow(
    indexLabel: String,
    title: String,
    apps: List<AppInfo>,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = indexLabel,
                style    = ResponsiveTypography.t3,
                color    = AmbientTheme.palette.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp)
            )
            Text(
                text     = title,
                style    = ResponsiveTypography.t1,
                color    = AmbientTheme.palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text  = if (isCollapsed) "▸" else "▾",
                style = ResponsiveTypography.t3,
                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = !isCollapsed,
            enter   = expandVertically(animationSpec = tween(200)),
            exit    = shrinkVertically(animationSpec = tween(180))
        ) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                apps.forEach { app ->
                    androidx.compose.material3.Surface(
                        shape    = RoundedCornerShape(50),
                        color    = AmbientTheme.palette.tileBackground,
                        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.combinedClickable(
                            onClick     = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
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
}

@Composable
private fun AppMenuSmartHomeRow(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Left margin: 30% from left edge
        Spacer(modifier = Modifier.weight(0.30f))

        // First app
        if (apps.isNotEmpty()) {
            val app = apps[0]
            val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
            AppIcon(
                packageName = app.packageName,
                colorFilter = getAmbientIconFilter(bucketColor),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAppClick(app) }
            )
        }

        // Center spacing (40% of width)
        Spacer(modifier = Modifier.weight(0.40f))

        // Second app
        if (apps.size > 1) {
            val app = apps[1]
            val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
            AppIcon(
                packageName = app.packageName,
                colorFilter = getAmbientIconFilter(bucketColor),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAppClick(app) }
            )
        }

        // Right margin: 30% from right edge
        Spacer(modifier = Modifier.weight(0.30f))
    }
}
