package com.ambient.launcher.tts

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders a run of sentences as one paragraph with sentence + word-level TTS highlighting.
 * Taps on a sentence call [onSeek] with the sentence's absolute chunk index.
 *
 * Each element in [sentences] is (chunkIndex, text). Empty text entries are skipped.
 */
@Composable
internal fun HighlightableText(
    sentences: List<Pair<Int, String>>,
    ttsState: TtsState,
    style: TextStyle,
    baseColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onSeek: (Int) -> Unit
) {
    val nonEmpty = remember(sentences) { sentences.filter { it.second.isNotBlank() } }

    // Char ranges in the rendered string mapped to chunk indices — used for tap-to-seek and word overlay.
    val boundaries = remember(nonEmpty) {
        val list = mutableListOf<Triple<Int, Int, Int>>() // chunkIdx, startChar, endChar
        var cursor = 0
        nonEmpty.forEachIndexed { i, (chunkIdx, text) ->
            val start = cursor
            cursor += text.length
            list += Triple(chunkIdx, start, cursor)
            if (i < nonEmpty.lastIndex) cursor += 1 // the separating space
        }
        list
    }

    val annotated = remember(nonEmpty, ttsState) {
        buildAnnotatedString {
            nonEmpty.forEachIndexed { i, (chunkIdx, text) ->
                val isActiveSentence = ttsState.isActive() && ttsState.chunkIndex == chunkIdx
                if (isActiveSentence) {
                    val ws = ttsState.wordStart.coerceIn(0, text.length)
                    val we = ttsState.wordEnd.coerceIn(ws, text.length)
                    if (we > ws) {
                        withStyle(SpanStyle(color = activeColor.copy(alpha = 0.65f))) {
                            append(text.substring(0, ws))
                        }
                        withStyle(SpanStyle(color = activeColor, fontWeight = FontWeight.Bold)) {
                            append(text.substring(ws, we))
                        }
                        withStyle(SpanStyle(color = activeColor.copy(alpha = 0.65f))) {
                            append(text.substring(we))
                        }
                    } else {
                        withStyle(SpanStyle(color = activeColor)) { append(text) }
                    }
                } else {
                    withStyle(SpanStyle(color = baseColor)) { append(text) }
                }
                if (i < nonEmpty.lastIndex) append(" ")
            }
        }
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(boundaries) {
            detectTapGestures { pos ->
                val l = layout ?: return@detectTapGestures
                val charOffset = l.getOffsetForPosition(pos)
                boundaries.firstOrNull { (_, s, e) -> charOffset in s until e }
                    ?.let { (chunkIdx, _, _) -> onSeek(chunkIdx) }
            }
        }
    )
}

internal fun TtsState.isActive(): Boolean = isPlaying || isPaused
