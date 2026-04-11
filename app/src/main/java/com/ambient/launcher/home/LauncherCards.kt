package com.ambient.launcher.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.view.MotionEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



// Removed hardcoded inks, now using AmbientTheme.palette in HomeHeader


@Composable
internal fun HomeHeader(
    weather: WeatherUiState
) {
    val now by rememberCurrentDateTime()
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mma") }
    
    val headerInk = AmbientTheme.palette.textPrimary
    val headerSubtleInk = AmbientTheme.palette.textSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = now.format(dayFormatter),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        lineHeight = 44.sp,
                        letterSpacing = (-1.0).sp
                    ),
                    color = headerInk
                )
                Text(
                    text = now.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = headerSubtleInk
                    )
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = weather.temperatureText.ifBlank { "--°" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                    color = headerInk
                )
                Text(
                    text = now.format(timeFormatter).lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = headerSubtleInk
                )
            }
        }
    }
}

@Composable
internal fun ProjectCard(
    title: String,
    kicker: String,
    body: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    color = AmbientTheme.palette.textPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    tasks.forEach { task ->
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
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = AmbientTheme.palette.textPrimary.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (body.isNotBlank()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Add padding at bottom if progress bar exists
                if (progress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Tufte/Moggridge: Progress bar along the TOP edge
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
internal fun ProjectCardRow(
    latestMessage: String,
    isLoading: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalBleed(LauncherLayout.contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProjectCard(
                kicker = "FOCUS",
                title = "Finish Launcher Redesign",
                body = "3 tasks remaining",
                color = AmbientTheme.palette.cardFocus,
                modifier = Modifier.width(180.dp)
            )
        }
        item {
            ProjectCard(
                kicker = "READING",
                title = "The Design of Everyday Things",
                body = "Chapter 4",
                color = AmbientTheme.palette.cardReading,
                modifier = Modifier.width(180.dp)
            )
        }
    }
}

@Composable
internal fun ActionCard(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = AmbientTheme.palette.cardWallet,
    onClick: () -> Unit,
    subTitle: String? = null
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LauncherLayout.cardRadius),
        color = color,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = com.ambient.launcher.ui.theme.SyneFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = AmbientTheme.palette.textPrimary
                )
                if (subTitle != null) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun TodayInfoCard(
    feedItems: List<RssFeedItem>,
    briefing: String? = null,
    onFeedClick: (RssFeedItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Normal,
                fontFamily = SyneFontFamily,
                fontSize = 32.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!briefing.isNullOrBlank()) {
            Text(
                text = briefing,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (feedItems.isEmpty()) {
                TodayInfoItem(
                    text = "Sydney Morning Herald: Markets open higher on tech rally ->",
                    fontWeight = FontWeight.Light,
                    onClick = null
                )
            } else {
                feedItems.take(5).forEach { item ->
                    TodayInfoItem(
                        text = "${item.source}: ${item.title} ->",
                        fontWeight = FontWeight.Light,
                        onClick = { onFeedClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayInfoItem(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = fontWeight,
                fontFamily = InterFontFamily,
                lineHeight = 24.sp,
                fontSize = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun CalendarPreviewStrip(
    onCalendarGestureChanged: (Boolean) -> Unit
) {
    val today = LocalDate.now()
    val days = (-6..10).map { today.plusDays(it.toLong()) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd") }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalBleed(LauncherLayout.contentPadding)
            .padding(vertical = 10.dp)
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        onCalendarGestureChanged(true)
                        true // Consume to prevent double-processing
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        onCalendarGestureChanged(false)
                        true
                    }
                    else -> false
                }
            },
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(days) { day ->
            val isToday = day == today
            val label = "${day.dayOfWeek.name.take(2)} ${day.format(dateFmt)}"
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = if (isToday) AmbientTheme.palette.textPrimary else AmbientTheme.palette.textSecondary
            )
        }
    }
}

@Composable
internal fun BucketHomeSection(
    apps: List<AppInfo>,
    tileSizes: Map<String, TileSize> = emptyMap(),
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    if (apps.isEmpty()) return
    BentoTileGrid(
        apps = apps,
        tileSizes = tileSizes,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick
    )
}

/**
 * Bento-style tile grid.
 *
 * Packs apps into 3-slot rows using a greedy algorithm:
 *   WIDE tiles = 2 slots, REGULAR = 1 slot.
 * Apps that don't fit in the current row overflow to the next.
 * Remaining empty slots are padded with an invisible [Spacer].
 *
 * Uses [BoxWithConstraints] (non-lazy) to compute exact [Dp] widths from
 * available screen width, avoiding the nested-LazyColumn scroll conflict.
 */
@Composable
internal fun BentoTileGrid(
    apps: List<AppInfo>,
    tileSizes: Map<String, TileSize>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    bucketForApp: ((AppInfo) -> LauncherBucket?)? = null
) {
    val gap = LauncherLayout.tileGap
    val rows = remember(apps, tileSizes) { packIntoRows(apps, tileSizes) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalBleed(LauncherLayout.contentPadding)
    ) {
        // 3 equal columns with 2 gaps between them
        val tileUnit = (maxWidth - gap * 2) / 3

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    row.forEach { app ->
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
                    // Pad remaining slots so the row fills the full width
                    val usedSlots = row.sumOf { app ->
                        if ((tileSizes[app.packageName] ?: TileSize.REGULAR) == TileSize.WIDE) 2 else 1
                    }
                    val remaining = 3 - usedSlots
                    if (remaining > 0) {
                        Spacer(
                            modifier = Modifier
                                .width(tileUnit * remaining + gap * (remaining - 1))
                                .height(tileUnit)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Greedy row packer: scans the queue and fills each 3-slot row with whatever
 * apps fit next. WIDE (2-slot) + REGULAR (1-slot) tiles compose naturally.
 */
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

        // Pull apps from queue; defer ones that don't fit to overflow
        val snapshot = queue.toList()
        queue.clear()
        snapshot.forEach { app ->
            val needed = if ((tileSizes[app.packageName] ?: TileSize.REGULAR) == TileSize.WIDE) 2 else 1
            if (slots + needed <= 3) {
                row.add(app)
                slots += needed
            } else {
                overflow.add(app)
            }
        }

        // Safety: force-add if nothing fit (prevents infinite loop on a WIDE-only tail)
        if (row.isEmpty() && overflow.isNotEmpty()) {
            row.add(overflow.removeFirst())
        }

        queue.addAll(overflow)
        if (row.isNotEmpty()) rows.add(row)
    }

    return rows
}

@Composable
internal fun GeminiFloatingButton(
    onClick: () -> Unit
) {
    val ambientPalette = AmbientTheme.palette
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick),
        color = ambientPalette.chipBackground,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gemini",
                tint = ambientPalette.accentHigh,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun IndustrialGearButton(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = AmbientTheme.palette.textPrimary
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = AmbientTheme.palette.textPrimary
            )
        }
    }
}

/**
 * Single bento tile. WIDE tiles use a horizontal layout (icon left, label right);
 * REGULAR tiles use the standard vertical layout (icon top-left, label bottom-left).
 */
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
        // Vertical: icon top-left, label bottom-left for all sizes
        val fontSize = when {
            app.label.length <= 6 -> 13.sp
            app.label.length <= 11 -> 12.sp
            else -> 11.sp
        }
        val iconSize = if (size == TileSize.SMALL) 28.dp else 40.dp
        val padding = if (size == TileSize.SMALL) 6.dp else 10.dp
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding, vertical = padding)
        ) {
            LauncherIconBadge(
                app = app,
                modifier = Modifier.align(Alignment.TopStart),
                iconSize = iconSize
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value + 3).sp
                ),
                color = AmbientTheme.palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun LauncherIconBadge(
    app: AppInfo,
    modifier: Modifier = Modifier,
    iconSize: Dp = LauncherLayout.appTileSize
) {
    val iconBitmap = rememberAppIcon(packageName = app.packageName)
    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        iconBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = app.label,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun AmbientCardSurface(
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LauncherLayout.cardRadius),
        color = color,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            content = content
        )
    }
}

private fun weatherIconFor(summary: String): ImageVector {
    val normalized = summary.lowercase()
    return when {
        "storm" in normalized || "thunder" in normalized -> Icons.Default.Thunderstorm
        "rain" in normalized || "drizzle" in normalized || "showers" in normalized -> Icons.Default.WaterDrop
        "cloud" in normalized || "fog" in normalized || "overcast" in normalized -> Icons.Default.Cloud
        else -> Icons.Default.WbSunny
    }
}

@Composable
private fun rememberCurrentDateTime() = androidx.compose.runtime.produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        kotlinx.coroutines.delay(60_000)
    }
}
