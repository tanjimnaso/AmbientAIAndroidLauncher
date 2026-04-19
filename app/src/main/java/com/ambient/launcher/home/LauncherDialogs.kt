package com.ambient.launcher.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.ui.theme.AmbientSettings
import com.ambient.launcher.ui.theme.AmbientTheme

@Composable
internal fun TodaySettingsDialog(
    configuration: LauncherConfiguration,
    onDismiss: () -> Unit,
    onSave: (LauncherConfiguration) -> Unit
) {
    val rssSources = remember { mutableStateOf(configuration.rssSources) }
    val ambientPalette = AmbientTheme.palette
    val monochrome by AmbientSettings.monochrome.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                Text(text = "RSS Sources", style = ResponsiveTypography.t1)  // 20sp Inter SemiBold
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    itemsIndexed(rssSources.value) { index, source ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = source.first, style = ResponsiveTypography.t2, color = AmbientTheme.palette.textPrimary)  // 15sp
                                Text(text = source.second, style = ResponsiveTypography.t3, color = AmbientTheme.palette.textSecondary, maxLines = 1)  // 12sp
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
                        TextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Name", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
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

                HorizontalDivider(
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Monochrome ink", style = ResponsiveTypography.t2, color = AmbientTheme.palette.textPrimary)
                        Text(
                            text = "Flatten cluster colours to ink",
                            style = ResponsiveTypography.t3,
                            color = AmbientTheme.palette.textSecondary
                        )
                    }
                    Switch(
                        checked = monochrome,
                        onCheckedChange = { AmbientSettings.setMonochrome(it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(configuration.setRssSources(rssSources.value))
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun AppEditDialog(
    app: AppInfo,
    configuration: LauncherConfiguration,
    onDismiss: () -> Unit,
    onSave: (LauncherConfiguration) -> Unit
) {
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
    val ambientPalette = AmbientTheme.palette

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        containerColor = ambientPalette.drawerBackground,
        titleContentColor = AmbientTheme.palette.textPrimary,
        textContentColor = AmbientTheme.palette.textPrimary,
        title = { Text(app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Tile size",
                    style = MaterialTheme.typography.labelLarge
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
                                style = MaterialTheme.typography.labelMedium,
                                color = AmbientTheme.palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = "Section",
                    style = MaterialTheme.typography.labelLarge
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
                                color = AmbientTheme.palette.textPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show on home",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Switch(
                        checked = showOnHome,
                        onCheckedChange = { showOnHome = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var nextConfiguration = configuration.unassign(app.packageName)
                    selectedBucket?.let { bucket ->
                        nextConfiguration = nextConfiguration.assign(bucket, app.packageName)
                    }
                    nextConfiguration = nextConfiguration
                        .setHomeVisibility(app.packageName, showOnHome && selectedBucket != null)
                        .setTileSize(app.packageName, selectedTileSize)
                    onSave(nextConfiguration)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
