package com.ambient.launcher

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ambient.launcher.ui.theme.AmbientLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmbientLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen()
                }
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }

    // Fetch Apps
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
            val parsedApps = apps.map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm)
                )
            }.sortedBy { it.label.lowercase() }
            installedApps = parsedApps
        }
    }

    // Fast filtering
    val filteredApps by remember(searchQuery, installedApps) {
        derivedStateOf {
            if (searchQuery.isEmpty()) installedApps
            else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Main Dashboard Content (Minimalist)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Time & Date for now
            Column(modifier = Modifier.weight(1f)) {
                val currentTime = remember { LocalDateTime.now() }
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
                
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = currentTime.format(timeFormatter), 
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily
                    )
                )
                Text(
                    text = currentTime.format(dateFormatter), 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Bottom Section: App Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.isNotEmpty()) {
                        isDrawerOpen = true
                    }
                },
                placeholder = { Text("Search apps...") },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "Search", 
                        modifier = Modifier.clickable { isDrawerOpen = !isDrawerOpen }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // App Drawer Overlay
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                    .padding(16.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().imePadding()
                    ) {
                        items(filteredApps) { app ->
                            AppRow(app = app, onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                                isDrawerOpen = false
                                searchQuery = ""
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
    }
}
