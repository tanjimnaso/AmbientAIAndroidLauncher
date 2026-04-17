package com.ambient.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ambient.launcher.ui.theme.AmbientTheme

internal enum class LibraryMode {
    EDIT
}

internal data class LibraryOverlayState(
    val visible: Boolean = false,
    val mode: LibraryMode = LibraryMode.EDIT,
    val bucket: LauncherBucket = LauncherBucket.UTILITIES
)

@Composable
internal fun LibraryOverlay(
    state: LibraryOverlayState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    apps: List<AppInfo>,
    configuration: LauncherConfiguration,
    onToggleAssignment: (LauncherBucket, String) -> Unit,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onStateChange: (LibraryOverlayState) -> Unit
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        val palette = AmbientTheme.palette
        val ink = palette.textPrimary
        val border = palette.textPrimary.copy(alpha = 0.2f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.drawerBackground)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unassigned Apps",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Assign one home bucket per app, or hide it.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                            color = ink.copy(alpha = 0.78f)
                        )
                    }
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = ink
                        ),
                        modifier = Modifier.border(1.dp, border, RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close library"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search unassigned apps"
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = border,
                        unfocusedBorderColor = border.copy(alpha = 0.65f),
                        focusedTextColor = ink,
                        unfocusedTextColor = ink,
                        cursorColor = ink,
                        focusedPlaceholderColor = ink.copy(alpha = 0.48f),
                        unfocusedPlaceholderColor = ink.copy(alpha = 0.48f),
                        focusedLeadingIconColor = ink,
                        unfocusedLeadingIconColor = ink.copy(alpha = 0.7f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ink
                        )
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LauncherBucket.entries.forEach { bucket ->
                        OutlineFilterChip(
                            label = configuration.displayTitle(bucket),
                            selected = state.bucket == bucket,
                            onClick = { onStateChange(state.copy(bucket = bucket)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        EditAssignmentRow(
                            app = app,
                            bucket = state.bucket,
                            bucketTitle = configuration.displayTitle(state.bucket),
                            selected = configuration.isSelected(state.bucket, app.packageName),
                            onToggle = { onToggleAssignment(state.bucket, app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = AmbientTheme.palette
    Surface(
        color = if (selected) palette.accentHigh.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.textPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = palette.textPrimary
        )
    }
}

@Composable
private fun EditAssignmentRow(
    app: AppInfo,
    bucket: LauncherBucket,
    bucketTitle: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    AppLibraryRowShell(
        app = app,
        onClick = onToggle
    ) {
        OutlineFilterChip(
            label = if (selected) "In $bucketTitle" else "Assign",
            selected = selected,
            onClick = onToggle
        )
    }
}

@Composable
private fun AppLibraryRowShell(
    app: AppInfo,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    val palette = AmbientTheme.palette
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.textPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val iconBitmap = rememberAppIcon(packageName = app.packageName)
            val bucketColor = app.bucket.themeColor(AmbientTheme.palette)
            Box(modifier = Modifier.size(64.dp)) {
                iconBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colorFilter = getAmbientIconFilter(bucketColor)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing()
        }
    }
}
