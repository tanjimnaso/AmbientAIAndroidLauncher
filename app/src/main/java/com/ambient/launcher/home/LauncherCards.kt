package com.ambient.launcher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.BatteryUiState
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily


// UI components for the Launcher Home screen

/**
 * HomeAppBar
 *
 * The top-most fixed status bar showing global context and battery life.
 */
@Composable
internal fun HomeAppBar(
    battery: BatteryUiState,
    modifier: Modifier = Modifier
) {
    val batteryStatusText = if (battery.isCharging) {
        "${battery.percentage}% • CHARGING"
    } else {
        "${battery.percentage}% • ${battery.remainingHours}H ${battery.remainingMinutes}M"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = batteryStatusText,
            style = ResponsiveTypography.t3.copy(letterSpacing = 0.5.sp),
            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f)
        )
    }
}

/**
 * TextQuickApps
 *
 * Purely textual, icon-less quick app launcher grid.
 * Styled like an editorial index or forecast table.
 */
@Composable
internal fun TextQuickApps(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEach { app ->
            Text(
                text = app.label,
                style = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Normal),
                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.9f),
                maxLines = 1,
                modifier = Modifier.clickable { onAppClick(app) }
            )
        }
    }
}

@Composable
internal fun ProjectCard(
    kicker: String,
    title: String,
    body: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    progress: Float? = null,
    isActive: Boolean = false,
    tasks: List<String> = emptyList()
) {
    Surface(
        modifier = modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(LauncherLayout.sectionCardRadius),
        color = if (isActive) color.copy(alpha = 0.95f) else color.copy(alpha = 0.7f),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .heightIn(min = 120.dp)
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Text(
                    text = kicker,
                    style = ResponsiveTypography.t3.copy(letterSpacing = 0.5.sp),
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = ResponsiveTypography.t1,  // 20sp Inter SemiBold
                    color = AmbientTheme.palette.textPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    for (task in tasks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.4f), RoundedCornerShape(50))
                            )
                            Text(
                                text = task,
                                style = ResponsiveTypography.t2,  // 15sp Inter Regular
                                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (body.isNotBlank()) {
                    Text(
                        text = body,
                        style = ResponsiveTypography.t2,  // 15sp Inter Regular
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (progress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopStart)
                        .background(AmbientTheme.palette.textPrimary.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AmbientTheme.palette.accentHigh.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

@Composable
internal fun AiBriefingSection(
    briefing: String?,
    isBriefingLoading: Boolean = false,
    isAnalysisLoading: Boolean = false,
    isAnalysisReady: Boolean = false,
    analysisHasError: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onOpenCloudNotifications: (() -> Unit)? = null
) {
    val displayText = when {
        !briefing.isNullOrBlank() -> briefing
        isBriefingLoading -> "Refreshing..."
        else -> "No data"
    }
    val textAlpha = if (briefing.isNullOrBlank()) 0.35f else 0.95f

    val label = when {
        isAnalysisLoading -> "AWAITING..."
        analysisHasError  -> "ERROR"
        isAnalysisReady   -> "READ ANALYSIS"
        else              -> "ANALYSE →"
    }
    val labelColor = when {
        analysisHasError  -> AmbientTheme.palette.errorAccent
        isAnalysisLoading -> AmbientTheme.palette.accentHigh.copy(alpha = 0.35f)
        else              -> AmbientTheme.palette.accentHigh.copy(alpha = 0.7f)
    }
    val labelAction: (() -> Unit)? = when {
        isAnalysisLoading -> null
        analysisHasError  -> onOpenCloudNotifications
        else              -> onClick
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmbientTheme.palette.panel.copy(alpha = 0.3f))
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(
                text = displayText,
                style = ResponsiveTypography.t2,
                color = AmbientTheme.palette.textPrimary.copy(alpha = textAlpha),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            )
        }

        // Action label — sits below the summary panel, flush right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 28.dp, top = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = label,
                style = ResponsiveTypography.t3,
                color = labelColor,
                modifier = if (labelAction != null) Modifier.clickable(onClick = labelAction) else Modifier
            )
        }
    }
}

@Composable
internal fun HeadlinesSection(
    feedItems: List<RssFeedItem>,
    lastRefreshTime: Long = 0L,
    onFeedClick: (RssFeedItem) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    // Pull-to-refresh: track how far the user has over-pulled past the top.
    // We use a NestedScrollConnection on the outer Column — the inner verticalScroll
    // can't consume downward drag when already at position 0, so the leftover delta
    // flows back up to us via onPostScroll (available.y > 0).
    var pullOffsetPx by remember { mutableStateOf(0f) }
    val triggerPx    = with(density) { 64.dp.toPx() }

    val pullRefreshConnection = remember(onRefresh) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Collapse the pull indicator when scrolling back up
                if (pullOffsetPx > 0f && available.y < 0f) {
                    val consumed = maxOf(available.y, -pullOffsetPx)
                    pullOffsetPx += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && onRefresh != null) {
                    pullOffsetPx = (pullOffsetPx + available.y).coerceAtMost(triggerPx * 1.5f)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffsetPx >= triggerPx) onRefresh?.invoke()
                pullOffsetPx = 0f
                return Velocity.Zero
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().nestedScroll(pullRefreshConnection)) {
        // Pull indicator — a subtle arrow/text that fades in as you pull
        if (pullOffsetPx > 0f) {
            val indicatorAlpha = (pullOffsetPx / triggerPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (pullOffsetPx / 2f).toDp().coerceAtMost(32.dp) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = if (pullOffsetPx >= triggerPx) "↑ Release to refresh" else "↓ Pull to refresh",
                    style = ResponsiveTypography.t3,
                    color = AmbientTheme.palette.textSecondary.copy(alpha = indicatorAlpha * 0.6f)
                )
            }
        }

        if (feedItems.isEmpty()) {
            HeadlineItem(
                source = "",
                title  = "Aggregating latest intelligence...",
                onClick = null
            )
            TimestampFooter(lastRefreshTime)
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            items(feedItems, key = { it.url }) { item ->
                HeadlineItem(
                    source = item.source,
                    title = item.title,
                    publishedAtEpochMillis = item.publishedAtEpochMillis,
                    onClick = { onFeedClick(item) }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        TimestampFooter(lastRefreshTime)
    }
}

@Composable
private fun TimestampFooter(lastRefreshTime: Long) {
    if (lastRefreshTime <= 0L) return
    Text(
        text = "updated ${formatElapsed(lastRefreshTime)}",
        style = ResponsiveTypography.t3,
        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.35f),
        textAlign = TextAlign.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun HeadlineItem(
    source: String,
    title: String,
    publishedAtEpochMillis: Long = 0L,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Kicker row: source (left) + timestamp (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (source.isNotBlank()) {
                Text(
                    text = source.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = getSourceColor(source).copy(alpha = 0.85f)
                )
            }
            if (publishedAtEpochMillis > 0L) {
                Text(
                    text = formatElapsed(publishedAtEpochMillis),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.4f)
                )
            }
        }

        // Title — flush left, ragged right
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.sp,
            color = AmbientTheme.palette.textPrimary,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun getSourceColor(source: String): Color {
    return when {
        source.contains("BBC", ignoreCase = true) -> Color(0xFF6B8CFF)  // Blue
        source.contains("Guardian", ignoreCase = true) -> Color(0xFF999999)  // Grey
        source.contains("Reuters", ignoreCase = true) -> Color(0xFFFFA500)  // Orange
        source.contains("Associated Press", ignoreCase = true) -> Color(0xFF00A4EF)  // AP Blue
        source.contains("NPR", ignoreCase = true) -> Color(0xFFCC0000)  // NPR Red
        source.contains("Politico", ignoreCase = true) -> Color(0xFFE81B23)  // Politico Red
        source.contains("Ars Technica", ignoreCase = true) -> Color(0xFFFF6633)  // Rust
        source.contains("Hacker News", ignoreCase = true) -> Color(0xFFFF6633)  // Rust
        else -> AmbientTheme.palette.textSecondary
    }
}

@Composable
internal fun BentoTileGrid(
    apps: List<AppInfo>,
    tileSizes: Map<String, TileSize>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val gap = LauncherLayout.tileGap
    val rows = remember(apps, tileSizes) { packIntoRows(apps, tileSizes) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalBleed(LauncherLayout.edgeBleed)
    ) {
        val tileUnit = (maxWidth - gap * 2) / 3

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    for (app in row) {
                        val size = tileSizes[app.packageName] ?: TileSize.REGULAR
                        val tileWidth = if (size == TileSize.WIDE) tileUnit * 2 + gap else tileUnit
                        
                        BentoAppTile(
                            app = app,
                            size = size,
                            modifier = Modifier
                                .width(tileWidth)
                                .height(tileUnit),
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }
                    val usedSlots = row.sumOf { app ->
                        if ((tileSizes[app.packageName] ?: TileSize.REGULAR) == TileSize.WIDE) 2 else 1
                    }
                    val remaining = 3 - usedSlots
                    if (remaining > 0) {
                        Spacer(
                            modifier = Modifier
                                .width(tileUnit * (remaining.toFloat()) + gap * (remaining - 1))
                                .height(tileUnit)
                        )
                    }
                }
            }
        }
    }
}

private fun packIntoRows(
    apps: List<AppInfo>,
    tileSizes: Map<String, TileSize>
): List<List<AppInfo>> {
    val rows = mutableListOf<List<AppInfo>>()
    val queue = ArrayDeque(apps)

    while (queue.isNotEmpty()) {
        val row = mutableListOf<AppInfo>()
        var slots = 0
        val overflow = ArrayDeque<AppInfo>()

        val snapshot = queue.toList()
        queue.clear()
        for (app in snapshot) {
            val needed = when(tileSizes[app.packageName] ?: TileSize.REGULAR) {
                TileSize.WIDE -> 2
                TileSize.SMALL -> 1 // Currently SMALL still takes 1 slot in the 3-column grid
                else -> 1
            }
            if (slots + needed <= 3) {
                row.add(app)
                slots += needed
            } else {
                overflow.add(app)
            }
        }

        if (row.isEmpty() && overflow.isNotEmpty()) {
            row.add(overflow.removeFirst())
        }

        queue.addAll(overflow)
        if (row.isNotEmpty()) rows.add(row)
    }

    return rows
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BentoAppTile(
    app: AppInfo,
    size: TileSize,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(LauncherLayout.tileRadius),
        color = AmbientTheme.palette.tileBackground,
        tonalElevation = 0.dp
    ) {
        val fontSize = when {
            app.label.length <= 6 -> 13.sp
            app.label.length <= 11 -> 12.sp
            else -> 11.sp
        }

        val iconSize = if (size == TileSize.SMALL) 18.dp else 26.dp 
        val padding = if (size == TileSize.SMALL) 8.dp else 12.dp
        val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding, vertical = padding)
        ) {
            val iconBitmap = rememberAppIcon(packageName = app.packageName)
            Box(
                modifier = Modifier.align(Alignment.TopStart).size(iconSize),
                contentAlignment = Alignment.Center
            ) {
                iconBitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(4.dp)),
                        colorFilter = getAmbientIconFilter(bucketColor)
                    )
                }
            }

            Text(
                text = app.label,
                style = ResponsiveTypography.t3,  // 12sp Inter Regular
                color = AmbientTheme.palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
