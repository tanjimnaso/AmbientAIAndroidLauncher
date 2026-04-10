package com.ambient.launcher.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily

data class TodoItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isComplete: Boolean = false,
    val body: String = ""
)

@Composable
internal fun TodoPanel(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var todos by remember {
        mutableStateOf(
            listOf(
                TodoItem("1", "Register Syne font", true),
                TodoItem("2", "Update Today styling"),
                TodoItem("3", "Refactor ProjectCard")
            )
        )
    }
    var newTodoText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .animateContentSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = AmbientTheme.palette.drawerBackground,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Focus",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = AmbientTheme.palette.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AmbientTheme.palette.textSecondary
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.1f)
                )

                // Todo list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(todos) { index, item ->
                        TodoItemRow(
                            item = item,
                            onToggle = {
                                todos = todos.toMutableList().apply {
                                    this[index] = this[index].copy(isComplete = !this[index].isComplete)
                                }
                            }
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = AmbientTheme.palette.textPrimary.copy(alpha = 0.1f)
                )

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTodoText,
                        onValueChange = { newTodoText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Add a task…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AmbientTheme.palette.textPrimary,
                            unfocusedTextColor = AmbientTheme.palette.textPrimary,
                            focusedBorderColor = AmbientTheme.palette.accentHigh,
                            unfocusedBorderColor = AmbientTheme.palette.textPrimary.copy(alpha = 0.2f),
                            focusedPlaceholderColor = AmbientTheme.palette.textSecondary,
                            cursorColor = AmbientTheme.palette.accentHigh
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (newTodoText.isNotBlank()) {
                                todos = todos + TodoItem(title = newTodoText)
                                newTodoText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                AmbientTheme.palette.cardFocus.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = AmbientTheme.palette.accentHigh,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoItemRow(
    item: TodoItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (item.isComplete) AmbientTheme.palette.accentHigh
                        else AmbientTheme.palette.textPrimary.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isComplete) {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmbientTheme.palette.drawerBackground,
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal
                ),
                color = AmbientTheme.palette.textPrimary.copy(
                    alpha = if (item.isComplete) 0.5f else 1f
                )
            )
            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily
                    ),
                    color = AmbientTheme.palette.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
