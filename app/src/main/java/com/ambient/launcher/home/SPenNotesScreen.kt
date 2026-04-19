package com.ambient.launcher.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.AmbientTheme
import com.ambient.launcher.ui.theme.AmbientMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// ── Data model ────────────────────────────────────────────────────────────────

private data class NotePoint(val x: Float, val y: Float, val pressure: Float)
private data class NoteStroke(val points: List<NotePoint>)

private class NotePage(val id: Long) {
    val strokes = mutableStateListOf<NoteStroke>()
}

// ── Persistence ───────────────────────────────────────────────────────────────

private fun notesDir(context: Context): File =
    File(context.filesDir, "spen_notes").also { it.mkdirs() }

private fun saveNote(context: Context, id: Long, strokes: List<NoteStroke>) {
    val arr = JSONArray()
    strokes.forEach { stroke ->
        val strokeArr = JSONArray()
        stroke.points.forEach { pt ->
            strokeArr.put(JSONObject().apply {
                put("x", pt.x); put("y", pt.y); put("p", pt.pressure)
            })
        }
        arr.put(strokeArr)
    }
    File(notesDir(context), "note_$id.json").writeText(arr.toString())
}

private fun loadCurrentNote(context: Context): NotePage? =
    notesDir(context)
        .listFiles { f -> f.extension == "json" }
        .orEmpty()
        .maxByOrNull { it.nameWithoutExtension.removePrefix("note_").toLongOrNull() ?: 0L }
        ?.let { file ->
            runCatching {
                val id  = file.nameWithoutExtension.removePrefix("note_").toLong()
                val arr = JSONArray(file.readText())
                NotePage(id).also { page ->
                    for (i in 0 until arr.length()) {
                        val strokeArr = arr.getJSONArray(i)
                        page.strokes.add(NoteStroke(
                            (0 until strokeArr.length()).map { j ->
                                val pt = strokeArr.getJSONObject(j)
                                NotePoint(pt.getDouble("x").toFloat(), pt.getDouble("y").toFloat(), pt.getDouble("p").toFloat())
                            }
                        ))
                    }
                }
            }.getOrNull()
        }

// ── Export ────────────────────────────────────────────────────────────────────

private fun saveToGallery(context: Context, id: Long, strokes: List<NoteStroke>, paletteInk: Color) {
    val displayWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
    var maxY = screenHeight
    strokes.forEach { s -> s.points.forEach { if (it.y > maxY) maxY = it.y } }
    val height = (maxY + 400f).toInt()

    val bitmap = Bitmap.createBitmap(displayWidth, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    // For gallery export, we might still want a background, but for consistency we'll use white-ish ink on dark
    // or dark ink on light based on the palette's intent.
    // However, the requested inkColor is now in the palette.
    val ink = android.graphics.Color.rgb(
        (paletteInk.red * 255).toInt(),
        (paletteInk.green * 255).toInt(),
        (paletteInk.blue * 255).toInt()
    )
    val paint = Paint().apply { color = ink; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
    strokes.forEach { stroke ->
        for (i in 0 until stroke.points.size - 1) {
            val a = stroke.points[i]; val b = stroke.points[i + 1]
            paint.strokeWidth = 2f + ((a.pressure + b.pressure) / 2f).coerceIn(0f, 1f) * 12f
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        }
    }

    val cv = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Note_$id.png")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
            android.os.Environment.DIRECTORY_PICTURES + "/AmbientNotes")
    }
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv) ?: return
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * SPenNotesScreen — page 2 of the 3-page launcher.
 *
 * Infinite ink canvas optimised for S Pen and third-party styluses:
 *   • PointerType.Stylus  → draw with pressure sensitivity
 *   • PointerType.Eraser  → erase strokes (Staedtler eraser tip, S Pen flip eraser)
 *   • Stylus secondary button (S Pen side button) → erase mode while held
 *   • Touch on pen side  → blocked so palm doesn't scroll (Right/Left Handed toggle)
 *   • Touch on safe side → drives vertical scroll
 *   • Canvas height auto-grows 800 px below the lowest drawn stroke
 *   • Undo / Redo per gesture
 *   • "+ New Note" → saves current to Gallery, opens blank canvas
 *
 * The palm blocker `pointerInput` sits BEFORE `verticalScroll` in the modifier chain.
 * In Compose's Initial pass, the first modifier fires first — it consumes touch on the
 * pen side before `verticalScroll` ever sees it, while stylus events pass through unblocked.
 */
@Composable
internal fun SPenNotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current
    val prefs   = remember { context.getSharedPreferences("spen_prefs", Context.MODE_PRIVATE) }

    var isRightHanded  by remember { mutableStateOf(prefs.getBoolean("right_handed", true)) }
    var isEraserMode   by remember { mutableStateOf(false) }
    var currentNote    by remember { mutableStateOf<NotePage?>(null) }
    var isLoaded       by remember { mutableStateOf(false) }
    var activeStroke   by remember { mutableStateOf<List<NotePoint>>(emptyList()) }

    LaunchedEffect(Unit) {
        val note = withContext(Dispatchers.IO) {
            loadCurrentNote(context) ?: NotePage(System.currentTimeMillis()).also {
                saveNote(context, it.id, emptyList())
            }
        }
        currentNote = note
        isLoaded    = true
    }

    if (!isLoaded || currentNote == null) return
    val note = currentNote!!

    // Undo / Redo stacks — reset when active note changes
    val undoStack = remember(note.id) { ArrayDeque<List<NoteStroke>>() }
    val redoStack = remember(note.id) { ArrayDeque<List<NoteStroke>>() }

    LaunchedEffect(note.id) {
        activeStroke = emptyList()
    }

    val hasStrokes = note.strokes.isNotEmpty()

    val palette       = AmbientTheme.palette
    val mode          = AmbientTheme.mode
    val isOutdoor     = mode == AmbientMode.DAYLIGHT_OUTDOOR

    val inkColor      = palette.inkColor
    val textPrimary   = palette.textPrimary
    val textSecondary = palette.textSecondary
    val accentHigh    = palette.accentHigh

    // Dynamic contrast: Toolbar is "closer" to the user than the canvas.
    // Outdoor mode stays "flat" (0% overlay effect) for maximum clarity.
    val toolbarAlpha   = if (isOutdoor) 1f else 0.95f
    val secondaryAlpha = if (isOutdoor) 1f else 0.82f
    val disabledAlpha  = if (isOutdoor) 0.5f else 0.38f
    val actionBgAlpha  = if (isOutdoor) 0f else 0.16f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val scrollState    = rememberScrollState()
        val eraserRadius   = with(density) { 40.dp.toPx() }

        val canvasHeightPx by remember(activeStroke) {
            derivedStateOf {
                var maxY = screenHeightPx
                note.strokes.forEach { s -> s.points.forEach { if (it.y > maxY) maxY = it.y } }
                activeStroke.forEach { if (it.y > maxY) maxY = it.y }
                // Always keep a full screen height of blank space below the lowest stroke
                maxY + screenHeightPx
            }
        }
        val canvasHeightDp = with(density) { canvasHeightPx.toDp() }

        // ── Scrollable ink surface ────────────────────────────────────────────
        // The palm-blocking pointerInput is the FIRST modifier here — it runs in
        // the Initial pass before verticalScroll, so it can silently consume touch
        // events on the pen side without verticalScroll ever seeing them.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isRightHanded) {
                    awaitPointerEventScope {
                        while (true) {
                            val event    = awaitPointerEvent(PointerEventPass.Initial)
                            val halfWidth = size.width / 2f
                            event.changes
                                .filter { ch ->
                                    ch.type == PointerType.Touch &&
                                    if (isRightHanded) ch.position.x > halfWidth
                                    else ch.position.x < halfWidth
                                }
                                .forEach { it.consume() }
                        }
                    }
                }
                .verticalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeightDp)
                    .pointerInput(note.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event  = awaitPointerEvent(PointerEventPass.Initial)
                                val down   = event.changes.firstOrNull {
                                    (it.type == PointerType.Stylus || it.type == PointerType.Eraser) && it.pressed
                                } ?: continue

                                down.consume()

                                // Eraser when: toolbar toggle, physical eraser tip, OR S Pen side button held
                                val isErasing = isEraserMode ||
                                    down.type == PointerType.Eraser ||
                                    event.buttons.isSecondaryPressed

                                val beforeGesture  = note.strokes.toList()
                                var gestureChanged = false

                                if (!isErasing) {
                                    // ── Draw mode ─────────────────────────────
                                    val pts = mutableListOf(
                                        NotePoint(down.position.x, down.position.y,
                                            down.pressure.coerceIn(0.01f, 1f))
                                    )
                                    activeStroke = pts.toList()

                                    while (true) {
                                        val drag   = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = drag.changes.firstOrNull { it.id == down.id } ?: break
                                        change.consume()
                                        pts.add(NotePoint(change.position.x, change.position.y,
                                            change.pressure.coerceIn(0.01f, 1f)))
                                        activeStroke = pts.toList()
                                        if (!change.pressed) break
                                    }

                                    if (pts.size > 1) {
                                        gestureChanged = true
                                        note.strokes.add(NoteStroke(pts.toList()))
                                    }
                                    activeStroke = emptyList()
                                } else {
                                    // ── Erase mode ────────────────────────────
                                    fun eraseAt(x: Float, y: Float) {
                                        val r2 = eraserRadius * eraserRadius
                                        val before = note.strokes.size
                                        note.strokes.removeAll { stroke ->
                                            stroke.points.any { pt ->
                                                val dx = pt.x - x; val dy = pt.y - y
                                                dx * dx + dy * dy < r2
                                            }
                                        }
                                        if (note.strokes.size != before) gestureChanged = true
                                    }

                                    eraseAt(down.position.x, down.position.y)

                                    while (true) {
                                        val drag   = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = drag.changes.firstOrNull { it.id == down.id } ?: break
                                        change.consume()
                                        eraseAt(change.position.x, change.position.y)
                                        if (!change.pressed) break
                                    }
                                }

                                if (gestureChanged) {
                                    val before = beforeGesture
                                    val current = note.strokes.toList()
                                    undoStack.addLast(before)
                                    redoStack.clear()
                                    scope.launch(Dispatchers.IO) { saveNote(context, note.id, current) }
                                }
                            }
                        }
                    }
            ) {
                note.strokes.forEach { drawPressureStroke(it.points, inkColor.copy(alpha = 0.88f)) }
                if (activeStroke.size > 1) drawPressureStroke(activeStroke, inkColor)
            }
        }

        // ── Toolbar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Left: handedness toggle + editing actions
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                    Text(
                        text     = if (isRightHanded) "Right Handed" else "Left Handed",
                        style    = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp),
                        color    = textSecondary.copy(alpha = secondaryAlpha),
                        modifier = Modifier.clickable {
                            isRightHanded = !isRightHanded
                            prefs.edit().putBoolean("right_handed", isRightHanded).apply()
                        }
                    )

                    Text(
                        text     = if (isEraserMode) "Eraser" else "Pen",
                        style    = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp),
                        color    = if (isEraserMode) accentHigh else textSecondary.copy(alpha = secondaryAlpha),
                        modifier = Modifier.clickable { isEraserMode = !isEraserMode }
                    )

                if (hasStrokes || undoStack.isNotEmpty()) {
                    Text(
                        text     = "Clear",
                        style    = ResponsiveTypography.t2.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp),
                        color    = textSecondary.copy(alpha = secondaryAlpha * 0.9f),
                        modifier = Modifier.clickable {
                            if (note.strokes.isNotEmpty()) {
                                undoStack.addLast(note.strokes.toList())
                                redoStack.clear()
                            }
                            note.strokes.clear()
                            scope.launch(Dispatchers.IO) { saveNote(context, note.id, emptyList()) }
                        }
                    )

                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint               = if (undoStack.isNotEmpty())
                            textSecondary.copy(alpha = toolbarAlpha)
                        else
                            textSecondary.copy(alpha = disabledAlpha),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(enabled = undoStack.isNotEmpty()) {
                                val prev = undoStack.removeLastOrNull() ?: return@clickable
                                redoStack.addLast(note.strokes.toList())
                                note.strokes.clear()
                                note.strokes.addAll(prev)
                                val snapshot = note.strokes.toList()
                                scope.launch(Dispatchers.IO) { saveNote(context, note.id, snapshot) }
                            }
                    )

                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint               = if (redoStack.isNotEmpty())
                            textSecondary.copy(alpha = toolbarAlpha)
                        else
                            textSecondary.copy(alpha = disabledAlpha),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(enabled = redoStack.isNotEmpty()) {
                                val next = redoStack.removeLastOrNull() ?: return@clickable
                                undoStack.addLast(note.strokes.toList())
                                note.strokes.clear()
                                note.strokes.addAll(next)
                                val snapshot = note.strokes.toList()
                                scope.launch(Dispatchers.IO) { saveNote(context, note.id, snapshot) }
                            }
                    )
                }
            }

            // Right: save current and start fresh
            Box(
                modifier = Modifier
                    .clickable {
                        if (note.strokes.isEmpty()) return@clickable
                        val snapshot = note.strokes.toList()
                        val oldId    = note.id
                        scope.launch(Dispatchers.IO) {
                            runCatching { saveToGallery(context, oldId, snapshot, inkColor) }
                            File(notesDir(context), "note_$oldId.json").delete()
                        }
                        val newNote = NotePage(System.currentTimeMillis())
                        currentNote  = newNote
                        activeStroke = emptyList()
                        undoStack.clear()
                        redoStack.clear()
                        scope.launch(Dispatchers.IO) { saveNote(context, newNote.id, emptyList()) }
                    }
                    .background(
                        textPrimary.copy(alpha = actionBgAlpha),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text  = "+ New Note",
                    style = ResponsiveTypography.t3.copy(fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
                    color = accentHigh.copy(alpha = toolbarAlpha)
                )
            }
        }
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawPressureStroke(points: List<NotePoint>, color: Color) {
    if (points.size < 2) return
    for (i in 0 until points.size - 1) {
        val a    = points[i]; val b = points[i + 1]
        val avgP = ((a.pressure + b.pressure) / 2f).coerceIn(0f, 1f)
        drawLine(
            color       = color,
            start       = Offset(a.x, a.y),
            end         = Offset(b.x, b.y),
            strokeWidth = 2f + avgP * 10f,
            cap         = StrokeCap.Round
        )
    }
}
