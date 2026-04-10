package com.ambient.launcher.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.ambient.launcher.AgenticAIViewModel
import com.ambient.launcher.BatteryUiState
import com.ambient.launcher.BriefingViewModel
import com.ambient.launcher.ChatMessage
import com.ambient.launcher.DashboardViewModel
import com.ambient.launcher.MediaViewModel
import com.ambient.launcher.ReadingViewModel
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.WeatherUiState
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.AmbientPalette
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import androidx.compose.runtime.LaunchedEffect

@Composable
fun LauncherScreen(
    dashboardViewModel: DashboardViewModel,
    agenticAIViewModel: AgenticAIViewModel,
    mediaViewModel: MediaViewModel,
    readingViewModel: ReadingViewModel,
    briefingViewModel: BriefingViewModel
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val ambientPalette = AmbientTheme.palette
    val feedItems by dashboardViewModel.feedItems.collectAsStateWithLifecycle()
    val weather by dashboardViewModel.weather.collectAsStateWithLifecycle()
    val battery by dashboardViewModel.batteryState.collectAsStateWithLifecycle()
    val messages by agenticAIViewModel.messages.collectAsStateWithLifecycle()
    val isAiLoading by agenticAIViewModel.isLoading.collectAsStateWithLifecycle()
    val briefing by briefingViewModel.briefing.collectAsStateWithLifecycle()
    var lastBriefingTime by remember { mutableStateOf(0L) }
    val installedApps by dashboardViewModel.installedApps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var isTodoOpen by remember { mutableStateOf(false) }
    var editingApp: AppInfo? by remember { mutableStateOf(null) }
    var editingToday by remember { mutableStateOf(false) }
    var renamingBucket: LauncherBucket? by remember { mutableStateOf(null) }
    var addingSection by remember { mutableStateOf(false) }
    var removingBucket: LauncherBucket? by remember { mutableStateOf(null) }
    var renameValue by remember { mutableStateOf("") }
    var calendarGestureActive by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1) { 2 }
    val homeListState = rememberLazyListState()
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dashboardViewModel.refreshWeather()
        }
    }

    val (configuration, updateConfiguration) = rememberLauncherConfiguration(installedApps)

    LaunchedEffect(configuration.rssSources) {
        dashboardViewModel.setRssSources(configuration.rssSources)
    }

    LaunchedEffect(feedItems, configuration.briefingInstruction) {
        if (feedItems.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val fourHoursInMillis = 4 * 60 * 60 * 1000L
            if (briefing == null || (now - lastBriefingTime) > fourHoursInMillis) {
                val headlines = feedItems.map { "${it.source}: ${it.title}" }
                briefingViewModel.generateBriefing(headlines, configuration.briefingInstruction)
                lastBriefingTime = now
            }
        }
    }

    // Safety: reset calendarGestureActive if it gets stuck (prevents pager lock)
    LaunchedEffect(calendarGestureActive) {
        if (calendarGestureActive) {
            delay(2000)
            calendarGestureActive = false
        }
    }

    val appsByPackage = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val bucketApps = remember(configuration, appsByPackage) {
        LauncherBucket.entries.associateWith { bucket ->
            configuration.packagesFor(bucket).mapNotNull(appsByPackage::get)
        }
    }

    val visibleBuckets = remember(configuration, bucketApps) {
        configuration.bucketOrder.filter { bucket ->
            !configuration.hiddenBuckets.contains(bucket) && bucketApps[bucket].orEmpty().isNotEmpty()
        }
    }

    // Only score-≥6 apps (isOnHome=true) appear in the home tile grid.
    val homeBucketApps = remember(configuration, bucketApps) {
        LauncherBucket.entries.associateWith { bucket ->
            bucketApps[bucket].orEmpty().filter { app -> configuration.isOnHome(app.packageName) }
        }
    }

    val walletApps = remember(bucketApps) {
        bucketApps[LauncherBucket.WALLET].orEmpty()
    }

    val homeVisibleBuckets = remember(configuration, homeBucketApps) {
        configuration.bucketOrder.filter { bucket ->
            bucket in LauncherBucket.homeBuckets &&          // WALLET excluded from tile grid
            !configuration.hiddenBuckets.contains(bucket) &&
            homeBucketApps[bucket].orEmpty().isNotEmpty()
        }
    }

    val sectionScrollMap = remember(homeVisibleBuckets) {
        homeVisibleBuckets.mapIndexed { index, bucket ->
            bucket to (2 + index)
        }.toMap()
    }

    val unassignedApps = remember(installedApps, configuration) {
        installedApps.filter { !configuration.isAssigned(it.packageName) }
    }

    BackHandler {
        if (editingApp != null) {
            editingApp = null
        } else if (pagerState.currentPage == 0) {
            scope.launch { pagerState.animateScrollToPage(1) }
        }
    }

    LaunchedEffect(Unit) {
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCoarseLocation) {
            dashboardViewModel.refreshWeather()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackplane()

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

        // Todo panel overlay
        if (isTodoOpen) {
            TodoPanel(
                isOpen = isTodoOpen,
                onDismiss = { isTodoOpen = false }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !calendarGestureActive
        ) { page ->
            when (page) {
                0 -> IndustrialIndexPage(ambientPalette, 
                    configuration = configuration,
                    bucketApps = bucketApps,
                    unassignedApps = unassignedApps,
                    onMenuTap = { bucket ->
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                            homeListState.animateScrollToItem(sectionScrollMap[bucket] ?: 0)
                        }
                    },
                    onOpenTodaySettings = { editingToday = true },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivity(intent)
                    },
                    onAppClick = { app ->
                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                            ?.let(context::startActivity)
                    },
                    onToggleVisibility = { bucket ->
                        updateConfiguration(configuration.toggleVisibility(bucket))
                    },
                    onToggleCollapse = { bucket ->
                        updateConfiguration(configuration.toggleCollapse(bucket))
                    },
                    onReorderBuckets = { fromIndex, toIndex ->
                        updateConfiguration(configuration.moveBucket(fromIndex, toIndex))
                    },
                    onAddSection = { addingSection = true },
                    onRemoveBucket = { bucket ->
                        val appCount = configuration.packagesFor(bucket).size
                        if (appCount >= 2) {
                            removingBucket = bucket
                        } else {
                            updateConfiguration(configuration.removeBucket(bucket))
                        }
                    },
                    onRenameBucket = { bucket ->
                        renamingBucket = bucket
                        renameValue = configuration.displayTitle(bucket)
                    },
                    onAppLongClick = { app ->
                        editingApp = app
                    }
                )

                else -> HomePage(
                    context = context,
                    listState = homeListState,
                    weather = weather,
                    battery = battery,
                    messages = messages,
                    isAiLoading = isAiLoading,
                    briefing = briefing,
                    feedItems = feedItems,
                    visibleBuckets = homeVisibleBuckets,
                    bucketApps = homeBucketApps,
                    tileSizes = configuration.tileSizes,
                    walletApps = walletApps,
                    mediaViewModel = mediaViewModel,
                    readingViewModel = readingViewModel,
                    onFeedClick = { item ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        activity.startActivity(intent)
                    },
                    onWalletClick = {
                        val walletIntent = context.packageManager
                            .getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://pay.google.com"))
                        walletIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivity(walletIntent)
                    },
                    onCalendarGestureChanged = { calendarGestureActive = it },
                    onAiSend = { query ->
                        agenticAIViewModel.sendMessage(query)
                    },
                    onAppClick = { app ->
                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                            ?.let(context::startActivity)
                    },
                    onAppLongClick = { app -> editingApp = app },
                    onOpenTodo = { isTodoOpen = true }
                )
            }
        }

        if (editingToday) {
            var instruction by remember { mutableStateOf(configuration.briefingInstruction) }
            val rssSources = remember { mutableStateOf(configuration.rssSources) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { editingToday = false },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 700.dp),
                shape = RoundedCornerShape(28.dp),
                containerColor = ambientPalette.drawerBackground,
                titleContentColor = AmbientTheme.palette.textPrimary,
                textContentColor = AmbientTheme.palette.textPrimary,
                title = { Text("Today Settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(text = "AI Briefing Instruction", style = MaterialTheme.typography.labelLarge)
                        androidx.compose.material3.OutlinedTextField(
                            value = instruction,
                            onValueChange = { instruction = it },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmbientTheme.palette.accentHigh,
                                unfocusedBorderColor = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f),
                                focusedTextColor = AmbientTheme.palette.textPrimary,
                                unfocusedTextColor = AmbientTheme.palette.textPrimary
                            )
                        )

                        Text(text = "RSS Sources", style = MaterialTheme.typography.labelLarge)
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            itemsIndexed(rssSources.value) { index, source ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = source.first, style = MaterialTheme.typography.bodySmall, color = AmbientTheme.palette.textPrimary)
                                        Text(text = source.second, style = MaterialTheme.typography.labelSmall, color = AmbientTheme.palette.textSecondary, maxLines = 1)
                                    }
                                    IconButton(onClick = { 
                                        rssSources.value = rssSources.value.toMutableList().apply { removeAt(index) } 
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = AmbientTheme.palette.textSecondary)
                                    }
                                }
                            }
                        }
                        
                        var newName by remember { mutableStateOf("") }
                        var newUrl by remember { mutableStateOf("") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                androidx.compose.material3.TextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    placeholder = { Text("Name", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                androidx.compose.material3.TextField(
                                    value = newUrl,
                                    onValueChange = { newUrl = it },
                                    placeholder = { Text("URL", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(onClick = {
                                if (newName.isNotBlank() && newUrl.isNotBlank()) {
                                    rssSources.value = rssSources.value + (newName to newUrl)
                                    newName = ""
                                    newUrl = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = AmbientTheme.palette.accentHigh)
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        updateConfiguration(
                            configuration
                                .setBriefingInstruction(instruction)
                                .setRssSources(rssSources.value)
                        )
                        editingToday = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { editingToday = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (editingApp != null) {
            val app = editingApp!!
            var selectedBucket by remember(app.packageName) {
                mutableStateOf(
                    configuration.bucketOrder.firstOrNull { configuration.isSelected(it, app.packageName) }
                )
            }
            var showOnHome by remember(app.packageName) {
                mutableStateOf(configuration.isOnHome(app.packageName))
            }
            var selectedTileSize by remember(app.packageName) {
                mutableStateOf(configuration.tileSize(app.packageName))
            }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { editingApp = null },
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                containerColor = ambientPalette.drawerBackground,
                titleContentColor = AmbientTheme.palette.textPrimary,
                textContentColor = AmbientTheme.palette.textPrimary,
                title = { Text(app.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // ── Tile size picker ─────────────────────────────────
                        Text(
                            text = "Tile size",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TileSize.entries.forEach { size ->
                                val isSelected = selectedTileSize == size
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTileSize = size },
                                    color = if (isSelected) ambientPalette.chipBackground else ambientPalette.panel,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = size.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                        color = AmbientTheme.palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // ── Section picker ───────────────────────────────────
                        Text(
                            text = "Section",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )

                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBucket = null },
                                    color = if (selectedBucket == null) ambientPalette.chipBackground else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "App menu only",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            items(configuration.bucketOrder) { bucket ->
                                val isCurrent = selectedBucket == bucket
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBucket = bucket },
                                    color = if (isCurrent) ambientPalette.chipBackground else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = configuration.displayTitle(bucket),
                                        modifier = Modifier.padding(16.dp),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ── Home visibility toggle ───────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show on home",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                            )
                            androidx.compose.material3.Switch(
                                checked = showOnHome,
                                onCheckedChange = { showOnHome = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            var nextConfiguration = configuration.unassign(app.packageName)
                            selectedBucket?.let { bucket ->
                                nextConfiguration = nextConfiguration.assign(bucket, app.packageName)
                            }
                            nextConfiguration = nextConfiguration
                                .setHomeVisibility(app.packageName, showOnHome && selectedBucket != null)
                                .setTileSize(app.packageName, selectedTileSize)
                            updateConfiguration(nextConfiguration)
                            editingApp = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { editingApp = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomePage(
    context: android.content.Context,
    listState: LazyListState,
    weather: WeatherUiState,
    battery: BatteryUiState,
    messages: List<ChatMessage>,
    isAiLoading: Boolean,
    briefing: String?,
    feedItems: List<RssFeedItem>,
    visibleBuckets: List<LauncherBucket>,
    bucketApps: Map<LauncherBucket, List<AppInfo>>,
    tileSizes: Map<String, TileSize>,
    walletApps: List<AppInfo>,
    mediaViewModel: MediaViewModel,
    readingViewModel: ReadingViewModel,
    onFeedClick: (RssFeedItem) -> Unit,
    onWalletClick: () -> Unit,
    onCalendarGestureChanged: (Boolean) -> Unit,
    onAiSend: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onOpenTodo: () -> Unit
) {
    var aiQuery by remember { mutableStateOf("") }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = LauncherLayout.contentPadding,
            end = LauncherLayout.contentPadding,
            top = LauncherLayout.topPadding,
            bottom = LauncherLayout.mediumGap
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            HomeHeader(
                weather = weather
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            TimeHorizonDashboard()
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }

        item {
            StateLayerDashboard()
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }

        item {
            TodayInfoCard(
                feedItems = feedItems,
                briefing = briefing,
                onFeedClick = onFeedClick
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            ProjectCard(
                kicker = "FOCUS",
                title = "Finish Launcher Redesign",
                body = "3 tasks remaining",
                color = AmbientTheme.palette.cardFocus,
                modifier = Modifier.fillMaxWidth(),
                isActive = true,
                progress = 0.75f,
                tasks = listOf(
                    "Register Syne font",
                    "Update Today styling",
                    "Refactor ProjectCard"
                ),
                onClick = onOpenTodo
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            val readingState by readingViewModel.readingState.collectAsStateWithLifecycle()
            ProjectCard(
                kicker = "READING",
                title = readingState?.title ?: "The Design of Everyday Things",
                body = readingState?.author ?: "Chapter 4",
                color = AmbientTheme.palette.cardReading,
                modifier = Modifier.fillMaxWidth(),
                progress = readingState?.progress ?: 0.33f,
                tasks = readingState?.let { emptyList() } ?: listOf("Chapter 4: Knowing What to Do")
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            MediaStrip(viewModel = mediaViewModel)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            CalendarPreviewStrip(onCalendarGestureChanged = onCalendarGestureChanged)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            val allHomeApps = visibleBuckets.flatMap { bucket -> bucketApps[bucket].orEmpty() }
            if (allHomeApps.isNotEmpty()) {
                BentoTileGrid(
                    apps = allHomeApps,
                    tileSizes = tileSizes,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(LauncherLayout.sectionCardRadius),
                color = Color.Transparent // Keep it flat but respect the grid/padding
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Battery ${battery.percentage}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontFamily = SyneFontFamily,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (battery.isCharging) "Charging" else "${battery.remainingHours}h ${battery.remainingMinutes}m left",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            lineHeight = 22.sp,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
        /* Composer card removed */
        }
    }
    
    // Floating Gemini Chat
    var showAiChat by remember { mutableStateOf(false) }

    if (showAiChat) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Message history
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages) { message ->
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (message.isUser) Color.White else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .then(if (message.isUser) Modifier else Modifier.padding(start = 12.dp))
                        )
                    }
                    if (isAiLoading) {
                        item { Text("Thinking...", color = Color.Gray, modifier = Modifier.padding(vertical = 6.dp)) }
                    }
                }

                // Input row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = aiQuery,
                        onValueChange = { aiQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Ambient AI…", color = Color.Gray) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    androidx.compose.material3.IconButton(
                        onClick = {
                            val q = aiQuery.trim()
                            if (q.isNotEmpty()) {
                                onAiSend(q)
                                aiQuery = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }

                // Footer controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app")).apply {
                            setPackage("com.google.android.apps.bard")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Launch Gemini")
                    }
                    androidx.compose.material3.TextButton(onClick = { showAiChat = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }

    // Floating Gemini Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        GeminiFloatingButton(
            onClick = { showAiChat = true }
        )
    }
}

@Composable
private fun IndustrialIndexPage(
    ambientPalette: AmbientPalette,
    configuration: LauncherConfiguration,
    bucketApps: Map<LauncherBucket, List<AppInfo>>,
    unassignedApps: List<AppInfo>,
    onMenuTap: (LauncherBucket) -> Unit,
    onOpenTodaySettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onToggleVisibility: (LauncherBucket) -> Unit,
    onToggleCollapse: (LauncherBucket) -> Unit,
    onReorderBuckets: (Int, Int) -> Unit,
    onAddSection: () -> Unit,
    onRemoveBucket: (LauncherBucket) -> Unit,
    onRenameBucket: (LauncherBucket) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    var isReorderingMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientPalette.drawerBackground)
            .navigationBarsPadding()
            .padding(
                horizontal = LauncherLayout.industrialInset,
                vertical = LauncherLayout.industrialInset
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = onOpenTodaySettings) {
                        Text("Today Settings", color = AmbientTheme.palette.accentHigh, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { isReorderingMode = !isReorderingMode }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = if (isReorderingMode) "Exit reordering" else "Reorder sections",
                            tint = if (isReorderingMode) AmbientTheme.palette.accentHigh else AmbientTheme.palette.textPrimary
                        )
                    }
                    IconButton(onClick = onAddSection) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add section",
                            tint = AmbientTheme.palette.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(configuration.bucketOrder, key = { _, bucket -> bucket.key }) { index, bucket ->
                IndustrialMenuRow(
                    bucket = bucket,
                    indexLabel = (index + 1).toString().padStart(2, '0'),
                    title = configuration.displayTitle(bucket),
                    apps = bucketApps[bucket].orEmpty(),
                    isHidden = configuration.hiddenBuckets.contains(bucket),
                    isCollapsed = configuration.isCollapsed(bucket),
                    onToggleVisibility = { onToggleVisibility(bucket) },
                    onToggleCollapse = { onToggleCollapse(bucket) },
                    onClick = { onMenuTap(bucket) },
                    onRemove = { onRemoveBucket(bucket) },
                    onRename = { onRenameBucket(bucket) },
                    bucketIndex = index,
                    bucketCount = configuration.bucketOrder.size,
                    isReorderingMode = isReorderingMode,
                    onReorder = onReorderBuckets,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick
                )
            }

            item {
                IndustrialStandaloneRow(
                    indexLabel = (configuration.bucketOrder.size + 1).toString().padStart(2, '0'),
                    title = "Unassigned Apps",
                    detail = "",
                    isHidden = false,
                    onToggleVisibility = null,
                    onClick = {}
                )
            }

            if (unassignedApps.isNotEmpty()) {
                items(unassignedApps.chunked(30)) { chunk ->
                    @OptIn(ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, top = 6.dp, bottom = 14.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chunk.forEach { app ->
                            androidx.compose.runtime.key(app.packageName) {
                                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmbientTheme.palette.textPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAppClick(app) },
                                        onLongClick = { onAppLongClick(app) }
                                    )
                                ) {
                                    Text(
                                        text = app.label,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                                        color = AmbientTheme.palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IndustrialGearButton(onClick = onOpenSettings)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IndustrialMenuRow(
    bucket: LauncherBucket,
    indexLabel: String,
    title: String,
    apps: List<AppInfo>,
    isHidden: Boolean,
    isCollapsed: Boolean,
    onToggleVisibility: () -> Unit,
    onToggleCollapse: () -> Unit,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onRename: () -> Unit,
    bucketIndex: Int,
    bucketCount: Int,
    isReorderingMode: Boolean,
    onReorder: (Int, Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val density = LocalDensity.current
    val dragThreshold = with(density) { 44.dp.toPx() }
    val dragOffset = remember(bucket.key) { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column {
        IndustrialStandaloneRow(
            indexLabel = indexLabel,
            title = title,
            detail = if (isCollapsed) "(${apps.size})" else apps.size.toString(),
            isHidden = isHidden,
            isCollapsed = isCollapsed,
            onToggleVisibility = onToggleVisibility,
            onToggleCollapse = onToggleCollapse,
            onClick = onClick,
            onRemove = onRemove,
            onRename = onRename,
            showReorderHandle = isReorderingMode,
            modifier = Modifier
                .graphicsLayer {
                    translationY = if (isReorderingMode) dragOffset.value * 0.2f else 0f
                }
                .pointerInput(bucket.key, bucketIndex, bucketCount, isReorderingMode) {
                    if (isReorderingMode) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                scope.launch { dragOffset.snapTo(0f) }
                            },
                            onDragCancel = {
                                scope.launch { dragOffset.animateTo(0f) }
                            },
                            onDragEnd = {
                                scope.launch { dragOffset.animateTo(0f) }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = dragOffset.value + dragAmount.y
                                dragOffset.snapTo(newOffset)

                                if (newOffset > dragThreshold && bucketIndex < bucketCount - 1) {
                                    onReorder(bucketIndex, bucketIndex + 1)
                                    dragOffset.snapTo(newOffset - dragThreshold)
                                } else if (newOffset < -dragThreshold && bucketIndex > 0) {
                                    onReorder(bucketIndex, bucketIndex - 1)
                                    dragOffset.snapTo(newOffset + dragThreshold)
                                }
                            }
                        }
                    }
                }
        )
        val expandedHeight = remember { androidx.compose.animation.core.Animatable(if (isCollapsed) 0f else 1f) }

        LaunchedEffect(isCollapsed) {
            expandedHeight.animateTo(if (isCollapsed) 0f else 1f, animationSpec = tween(300))
        }

        if (apps.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = expandedHeight.value
                        scaleY = expandedHeight.value * 0.8f + 0.2f
                    }
                    .animateContentSize()
            ) {
                if (!isCollapsed) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, top = 6.dp, bottom = 14.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        apps.forEach { app ->
                            androidx.compose.runtime.key(app.packageName) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmbientTheme.palette.accentHalo.copy(alpha = 0.3f)),
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAppClick(app) },
                                        onLongClick = { onAppLongClick(app) }
                                    )
                                ) {
                                    Text(
                                        text = app.label,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                                        color = AmbientTheme.palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndustrialStandaloneRow(
    indexLabel: String,
    title: String,
    detail: String,
    isHidden: Boolean,
    onToggleVisibility: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    showReorderHandle: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showReorderHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = AmbientTheme.palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = indexLabel,
            style = MaterialTheme.typography.bodySmall,
            color = AmbientTheme.palette.textPrimary.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SyneFontFamily, fontWeight = FontWeight.Normal),
            color = AmbientTheme.palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = AmbientTheme.palette.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (onToggleCollapse != null) {
            IconButton(onClick = onToggleCollapse, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = AmbientTheme.palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (onToggleVisibility != null) {
            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isHidden) "Show" else "Hide",
                    tint = AmbientTheme.palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WallpaperBackplane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    )
}
