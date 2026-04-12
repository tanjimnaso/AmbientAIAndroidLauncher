package com.ambient.launcher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily


// UI components for the Launcher Home screen

/**
 * RecentAppsStrip
 *
 * Compact horizontal row of up to 6 most-used apps, displayed directly under the masthead.
 * No tile backgrounds — just icon + label, evenly spaced across the full width.
 */
@Composable
internal fun RecentAppsStrip(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        apps.take(6).forEach { app ->
            val iconBitmap = rememberAppIcon(app.packageName)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAppClick(app) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
                Text(
                    text = app.label,
                    style = ResponsiveTypography.t3,
                    color = AmbientTheme.palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
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
                    style = ResponsiveTypography.t3.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
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
    isAnalysisLoading: Boolean = false,
    isAnalysisReady: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (briefing.isNullOrBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = briefing,
            style = ResponsiveTypography.d3,
            color = AmbientTheme.palette.textPrimary.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        )
        if (onClick != null) {
            val label = when {
                isAnalysisLoading -> "AWAITING..."
                isAnalysisReady -> "READ ANALYSIS"
                else -> "ANALYSE →"
            }
            Text(
                text = label,
                style = ResponsiveTypography.t3.copy(fontWeight = FontWeight.SemiBold),
                color = AmbientTheme.palette.accentHigh.copy(
                    alpha = if (isAnalysisLoading) 0.4f else 0.7f
                )
            )
        }
    }
}

@Composable
internal fun HeadlinesSection(
    feedItems: List<RssFeedItem>,
    onFeedClick: (RssFeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (feedItems.isEmpty()) {
        HeadlineItem(
            modifier = modifier,
            source = "",
            title = "Aggregating latest intelligence...",
            onClick = null
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp),  // Aggressive utilitarian width constraint
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (item in feedItems) {
                HeadlineItem(
                    source = item.source,
                    title = item.title,
                    onClick = { onFeedClick(item) }
                )
            }
        }
    }
}

@Composable
private fun HeadlineItem(
    source: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Source as colored functional label
        if (source.isNotBlank()) {
            Text(
                text = source.uppercase(),
                style = ResponsiveTypography.t3.copy(fontWeight = FontWeight.SemiBold),
                color = getSourceColor(source),
                letterSpacing = 0.5.sp
            )
        }

        // Title bold and prominent
        Text(
            text = title,
            style = ResponsiveTypography.t1.copy(fontWeight = FontWeight.Bold),
            color = AmbientTheme.palette.textPrimary,
            maxLines = 3,
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
                        colorFilter = ColorFilter.tint(bucketColor.copy(alpha = 0.55f), BlendMode.SrcAtop)
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
