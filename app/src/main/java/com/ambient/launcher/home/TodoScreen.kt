package com.ambient.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.SaveStatus
import com.ambient.launcher.TodoItem
import com.ambient.launcher.TodoViewModel
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()

    var inputHeader by remember { mutableStateOf("") }
    var inputBody by remember { mutableStateOf("") }

    // Back button handling
    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbientTheme.palette.drawerBackground)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Delete) {
                    selectedIndex?.let { viewModel.removeTodo(it) }
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    // Swipe right to dismiss
                    if (dragAmount > 100f) {
                        onDismiss()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ──── Header + Save Status ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = ResponsiveTypography.d2,  // 32sp Syne Regular — section header
                    color = AmbientTheme.palette.textPrimary
                )

                Text(
                    text = when (saveStatus) {
                        SaveStatus.SAVING -> "Saving..."
                        SaveStatus.SAVED -> "Saved"
                        SaveStatus.ERROR -> "Error"
                    },
                    style = ResponsiveTypography.t3,  // 12sp Inter Regular — metadata badge
                    color = when (saveStatus) {
                        SaveStatus.SAVING -> AmbientTheme.palette.textSecondary.copy(alpha = 0.7f)
                        SaveStatus.SAVED -> AmbientTheme.palette.textSecondary
                        SaveStatus.ERROR -> AmbientTheme.palette.errorAccent
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ──── Text Input ────
            OutlinedTextField(
                value = inputHeader + if (inputBody.isNotBlank()) "\n$inputBody" else "",
                onValueChange = { text ->
                    val lines = text.split("\n")
                    inputHeader = lines.getOrNull(0) ?: ""
                    inputBody = lines.drop(1).joinToString("\n")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(
                        "Add a task...",
                        style = ResponsiveTypography.t2,  // 15sp Inter Regular
                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                    )
                },
                textStyle = ResponsiveTypography.t2,  // 15sp Inter Regular

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmbientTheme.palette.accentHigh.copy(alpha = 0.5f),
                    unfocusedBorderColor = AmbientTheme.palette.textSecondary.copy(alpha = 0.2f),
                    focusedTextColor = AmbientTheme.palette.textPrimary,
                    unfocusedTextColor = AmbientTheme.palette.textPrimary,
                    cursorColor = AmbientTheme.palette.accentHigh
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputHeader.isNotBlank()) {
                            viewModel.addTodo(inputHeader, inputBody)
                            inputHeader = ""
                            inputBody = ""
                        }
                    }
                ),
                singleLine = false,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ──── Todo List ────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(todos, key = { _, item -> item.id }) { index, item ->
                    val isSelected = selectedIndex == index

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected)
                                    AmbientTheme.palette.panel.copy(alpha = 0.5f)
                                else
                                    Color.Transparent
                            )
                            .clickable {
                                viewModel.setSelectedIndex(if (isSelected) null else index)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmbientTheme.palette.accentHigh.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                if (item.header.isNotBlank()) {
                                    Text(
                                        text = item.header,
                                        style = ResponsiveTypography.t1,  // 20sp Inter SemiBold — task header
                                        color = AmbientTheme.palette.textPrimary
                                    )
                                }

                                if (item.body.isNotBlank()) {
                                    Text(
                                        text = item.body,
                                        style = ResponsiveTypography.t2,  // 15sp Inter Regular — task body
                                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            Text(
                                text = "Press Delete to remove",
                                style = ResponsiveTypography.t3,  // 12sp Inter Regular — hint
                                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
