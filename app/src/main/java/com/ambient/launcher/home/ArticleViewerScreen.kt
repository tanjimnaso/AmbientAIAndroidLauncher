package com.ambient.launcher.home

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.tts.TtsIconButton
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import kotlin.math.max

@Composable
fun ArticleViewerScreen(
    article: RssFeedItem,
    onDismiss: () -> Unit
) {
    val palette = AmbientTheme.palette
    
    // Extract content using Readability4J
    var cleanedHtml by remember { mutableStateOf<String?>(null) }
    var plainText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Convert current palette colors to CSS-friendly hex strings
    val backgroundColorHex = String.format("#%06X", (0xFFFFFF and palette.mainBackground.toArgb()))
    val textColorHex = String.format("#%06X", (0xFFFFFF and palette.textPrimary.toArgb()))
    val textSecondaryColorHex = String.format("#%06X", (0xFFFFFF and palette.textSecondary.toArgb()))
    val accentColorHex = String.format("#%06X", (0xFFFFFF and palette.accentHigh.toArgb()))

    val customCss = """
        body { 
            background-color: $backgroundColorHex; 
            color: $textColorHex;
            font-family: 'Inter', -apple-system, sans-serif;
            line-height: 1.8;
            padding: 40px 24px 100px 24px;
            margin: 0;
            font-size: 16px;
        }
        h1 {
            color: $textColorHex;
            font-size: 26px;
            line-height: 1.2;
            margin-bottom: 8px;
            font-weight: 800;
        }
        .metadata {
            color: $textSecondaryColorHex;
            font-size: 14px;
            margin-bottom: 32px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        h2, h3 { color: $accentColorHex; margin-top: 1.8em; }
        a { color: $accentColorHex; text-decoration: none; border-bottom: 1px solid ${accentColorHex}44; }
        img { 
            max-width: 100%; 
            height: auto; 
            border-radius: 12px; 
            margin: 32px 0;
            display: block;
        }
        p { margin-bottom: 1.6em; }
        pre, code {
            background: ${textSecondaryColorHex}15;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.9em;
        }
        blockquote {
            border-left: 4px solid $accentColorHex;
            margin: 0;
            padding-left: 20px;
            font-style: italic;
            color: $textSecondaryColorHex;
        }
    """.trimIndent()

    LaunchedEffect(article.url) {
        isLoading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) {
                val doc = Jsoup.connect(article.url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()

                val readability = Readability4J(article.url, doc.html())
                val parsedArticle = readability.parse()

                val articleContent = parsedArticle.content ?: "Unable to extract article content."
                val articleTitle = parsedArticle.title ?: article.title
                val narration = "$articleTitle. ${parsedArticle.textContent.orEmpty()}"

                val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>$customCss</style>
                </head>
                <body>
                    <h1>$articleTitle</h1>
                    <div class="metadata">${article.source} • ${article.timestamp}</div>
                    $articleContent
                </body>
                </html>
                """.trimIndent()
                html to narration
            }
            cleanedHtml = result.first
            plainText = result.second
        } catch (e: Exception) {
            error = e.message ?: "Failed to load article"
        } finally {
            isLoading = false
        }
    }

    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { screenWidth.toPx() }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.mainBackground)
            .navigationBarsPadding()
            .statusBarsPadding()
            // Apply visual feedback: slide and fade
            .offset { IntOffset(dragOffset.value.toInt().coerceAtLeast(0), 0) }
            .alpha((1f - (dragOffset.value / screenWidthPx) * 0.5f).coerceIn(0f, 1f))
            .pointerInput(onDismiss) {
                val edgePx = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Only engage if the touch started near the left edge.
                    if (down.position.x > edgePx) return@awaitEachGesture

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    // Wait until horizontal slop is exceeded; vertical drags pass through to WebView.
                    val slopChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over > 0f) change.consume()
                    } ?: return@awaitEachGesture

                    velocityTracker.addPosition(slopChange.uptimeMillis, slopChange.position)
                    scope.launch { dragOffset.snapTo(max(0f, slopChange.position.x - down.position.x)) }

                    // Now we own the gesture — track until release.
                    var active = true
                    while (active) {
                        val event = awaitPointerEvent()
                        val drag = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!drag.pressed) { active = false; break }
                        velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                        val delta = drag.positionChange().x
                        val next = (dragOffset.value + delta).coerceAtLeast(0f)
                        scope.launch { dragOffset.snapTo(next) }
                        drag.consume()
                    }

                    val velocity = velocityTracker.calculateVelocity().x
                    if (dragOffset.value > screenWidthPx * 0.3f || velocity > 800f) {
                        scope.launch {
                            dragOffset.animateTo(screenWidthPx)
                            onDismiss()
                        }
                    } else {
                        scope.launch { dragOffset.animateTo(0f) }
                    }
                }
            }
    ) {
        Crossfade(targetState = isLoading to error, label = "ContentState") { (loading, err) ->
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.accentHigh)
                    }
                }
                err != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ambient Reader", fontSize = 12.sp, color = palette.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Unable to distill this article", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text(err, fontSize = 14.sp, color = palette.textSecondary)
                    }
                }
                cleanedHtml != null -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = false 
                                    domStorageEnabled = false
                                    loadsImagesAutomatically = true
                                    defaultFontSize = 16
                                }
                                // Prevent horizontal scrolling/bounce in the webview itself
                                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                                webViewClient = WebViewClient()
                                setBackgroundColor(palette.mainBackground.toArgb())
                            }
                        },
                        update = { webView ->
                            // Use a tag to prevent reloading the same content during drag-induced recompositions
                            val currentHtml = webView.tag as? String
                            if (currentHtml != cleanedHtml) {
                                cleanedHtml?.let {
                                    webView.loadDataWithBaseURL(article.url, it, "text/html", "UTF-8", null)
                                    webView.tag = it
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Dismiss handle / visual indicator at the top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(palette.textPrimary.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        )

        // Top-right TTS button
        plainText?.takeIf { it.isNotBlank() }?.let { narration ->
            TtsIconButton(
                sessionId = "article:${article.url.hashCode()}",
                title = article.title,
                textProvider = { narration },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 20.dp)
            )
        }
    }
}

