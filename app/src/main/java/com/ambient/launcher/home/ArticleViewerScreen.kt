package com.ambient.launcher.home

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.tts.ArticleTtsPrep
import com.ambient.launcher.tts.TtsChunk
import com.ambient.launcher.tts.TtsController
import com.ambient.launcher.tts.TtsMediaStrip
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import kotlin.math.max

/** Sentence-wrapped article content + the parallel chunk list used for TTS + highlight + tap-to-seek. */
private data class PreparedArticle(val html: String, val chunks: List<TtsChunk>)

/**
 * Binder-thread JS callback from the WebView. Hops to the main thread before talking
 * to TtsController so service binding + foreground starts happen on Looper.getMainLooper().
 */
private class TtsJsBridge(
    private val context: Context,
    private val sessionId: String,
    private val title: String,
    private val chunksProvider: () -> List<TtsChunk>
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSentenceTap(index: Int) {
        main.post {
            val s = TtsController.state.value
            if (s.sessionId != sessionId) {
                TtsController.toggle(context, sessionId, title, chunksProvider())
            }
            TtsController.seekTo(context, index)
        }
    }
}

@Composable
fun ArticleViewerScreen(
    article: RssFeedItem,
    onDismiss: () -> Unit
) {
    val palette = AmbientTheme.palette

    var cleanedHtml by remember { mutableStateOf<String?>(null) }
    var chunks by remember { mutableStateOf<List<TtsChunk>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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
            padding: 40px 24px 140px 24px;
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
        img { max-width: 100%; height: auto; border-radius: 12px; margin: 32px 0; display: block; }
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
        .tts-s { cursor: pointer; transition: background-color 120ms ease; }
        .tts-s.tts-active { background-color: ${accentColorHex}22; border-radius: 3px; }
    """.trimIndent()

    // JS that binds tap handlers and exposes window.highlightSentence(idx).
    val ttsJs = """
        (function() {
          function bind() {
            document.querySelectorAll('.tts-s').forEach(function(el) {
              if (el.dataset.bound === '1') return;
              el.dataset.bound = '1';
              el.addEventListener('click', function(e) {
                e.stopPropagation();
                var idx = parseInt(el.getAttribute('data-sidx'), 10);
                if (!isNaN(idx) && window.AmbientTts) window.AmbientTts.onSentenceTap(idx);
              });
            });
          }
          window.highlightSentence = function(idx) {
            var prev = document.querySelector('.tts-s.tts-active');
            if (prev) prev.classList.remove('tts-active');
            if (idx < 0) return;
            var el = document.querySelector('.tts-s[data-sidx="' + idx + '"]');
            if (!el) return;
            el.classList.add('tts-active');
            var r = el.getBoundingClientRect();
            if (r.top < 80 || r.bottom > window.innerHeight - 80) {
              el.scrollIntoView({block: 'center', behavior: 'smooth'});
            }
          };
          if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', bind);
          } else {
            bind();
          }
        })();
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

                // Compose a single body fragment: title + metadata + content, then wrap
                // every sentence inside block elements with seek-able spans.
                val bodyFragment = """
                    <h1>$articleTitle</h1>
                    <div class="metadata">${article.source} • ${article.timestamp}</div>
                    $articleContent
                """.trimIndent()
                val prepared = ArticleTtsPrep.wrap(bodyFragment)

                val fullHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>$customCss</style>
                </head>
                <body>
                    ${prepared.html}
                    <script>$ttsJs</script>
                </body>
                </html>
                """.trimIndent()
                PreparedArticle(fullHtml, prepared.chunks)
            }
            cleanedHtml = result.html
            chunks = result.chunks
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

    val context = LocalContext.current
    val sessionId = remember(article.url) { "article:${article.url.hashCode()}" }
    // Holder so the state-observer effect can reach into the WebView created by AndroidView.
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }
    val ttsState by TtsController.state.collectAsStateWithLifecycle()

    // Push highlight updates to the DOM whenever the active sentence or session changes.
    LaunchedEffect(ttsState.sessionId, ttsState.chunkIndex, cleanedHtml) {
        val wv = webViewHolder.value ?: return@LaunchedEffect
        if (cleanedHtml == null) return@LaunchedEffect
        val idx = if (ttsState.sessionId == sessionId) ttsState.chunkIndex else -1
        wv.post { wv.evaluateJavascript("window.highlightSentence && highlightSentence($idx);", null) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.mainBackground)
            .navigationBarsPadding()
            .statusBarsPadding()
            .offset { IntOffset(dragOffset.value.toInt().coerceAtLeast(0), 0) }
            .alpha((1f - (dragOffset.value / screenWidthPx) * 0.5f).coerceIn(0f, 1f))
            .pointerInput(onDismiss) {
                val edgePx = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgePx) return@awaitEachGesture

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    val slopChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over > 0f) change.consume()
                    } ?: return@awaitEachGesture

                    velocityTracker.addPosition(slopChange.uptimeMillis, slopChange.position)
                    scope.launch { dragOffset.snapTo(max(0f, slopChange.position.x - down.position.x)) }

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
                        Text("Unable to distill this article", fontSize = 18.sp, color = palette.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text(err, fontSize = 14.sp, color = palette.textSecondary)
                    }
                }
                cleanedHtml != null -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = false
                                    loadsImagesAutomatically = true
                                    defaultFontSize = 16
                                }
                                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                                webViewClient = WebViewClient()
                                setBackgroundColor(palette.mainBackground.toArgb())
                                addJavascriptInterface(
                                    TtsJsBridge(
                                        context = ctx.applicationContext,
                                        sessionId = sessionId,
                                        title = article.title,
                                        chunksProvider = { chunks }
                                    ),
                                    "AmbientTts"
                                )
                                webViewHolder.value = this
                            }
                        },
                        update = { webView ->
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

        // Bottom transport: play/pause center, prev/next sentence (tap) or paragraph (double-tap).
        if (chunks.isNotEmpty()) {
            TtsMediaStrip(
                sessionId = sessionId,
                title = article.title,
                chunks = chunks,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
