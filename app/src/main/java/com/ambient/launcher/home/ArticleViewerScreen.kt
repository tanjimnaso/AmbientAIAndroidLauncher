package com.ambient.launcher.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.ui.theme.AmbientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

// Sites that require JS + cookies for login — use WebView instead of the reader parser.
private val WEBVIEW_DOMAINS = setOf(
    "ft.com", "financialtimes.com",
    "wsj.com", "economist.com",
    "bloomberg.com", "thetimes.co.uk",
    "telegraph.co.uk", "newyorker.com"
)

private fun requiresWebView(url: String): Boolean =
    WEBVIEW_DOMAINS.any { domain -> url.contains(domain, ignoreCase = true) }

data class ArticleContent(
    val headline: String,
    val author: String,
    val publishedDate: String,
    val bodyParagraphs: List<String>
)

object ReadingModeParser {
    fun parse(html: String): ArticleContent {
        val doc = Jsoup.parse(html)

        val headline = doc.select("h1").firstOrNull()?.text()
            ?: doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
            ?: doc.select("title").text()

        val author = doc.select("meta[name=author]").attr("content").takeIf { it.isNotBlank() }
            ?: doc.select("[class*=author], [rel=author]").firstOrNull()?.text()
            ?: doc.select("meta[name=creator]").attr("content").takeIf { it.isNotBlank() }
            ?: ""

        val publishedDate = doc.select("meta[property=article:published_time]").attr("content").takeIf { it.isNotBlank() }
            ?: doc.select("meta[name=publish_date]").attr("content").takeIf { it.isNotBlank() }
            ?: doc.select("time").attr("datetime").takeIf { it.isNotBlank() }
            ?: doc.select("[class*=date], [class*=published]").firstOrNull()?.text()
            ?: ""

        val body = doc.select("article").firstOrNull()
            ?: doc.select("main").firstOrNull()
            ?: doc.select("[class*=article-body], [class*=article__body]").firstOrNull()
            ?: doc.select("[class*=essay-body], [class*=essay__body]").firstOrNull()
            ?: doc.select("[class*=post-content], [class*=entry-content]").firstOrNull()
            ?: doc.select("[class*=story-body], [class*=page-content]").firstOrNull()
            ?: doc.select("[class*=content]").firstOrNull()
            ?: doc.select("body").first()

        val paragraphs = body?.select("p")
            ?.map { it.text() }
            ?.filter { text ->
                text.length > 30 &&
                !text.contains("Cookie", ignoreCase = true) &&
                !text.contains("Subscribe", ignoreCase = true) &&
                !text.contains("JavaScript", ignoreCase = true) &&
                !text.contains("sign up", ignoreCase = true) &&
                !text.contains("newsletter", ignoreCase = true)
            }
            ?.take(60)
            ?: emptyList()

        return ArticleContent(
            headline = (headline ?: "Article").trim(),
            author = author.trim(),
            publishedDate = publishedDate.trim(),
            bodyParagraphs = paragraphs
        )
    }
}

@Composable
fun ArticleViewerScreen(
    article: RssFeedItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    if (requiresWebView(article.url)) {
        // Chrome Custom Tabs: shares Chrome's cookie jar, Google OAuth allowed.
        // Launch immediately then dismiss back to the launcher.
        LaunchedEffect(article.url) {
            val backgroundColor = AmbientTheme  // can't read palette outside composition
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(article.url))
            onDismiss()
        }
        return
    }

    var content by remember { mutableStateOf<ArticleContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(article.url) {
        isLoading = true
        error = null
        content = null
        try {
            val html = withContext(Dispatchers.IO) { fetchArticleHtml(article.url) }
            if (html.isNotBlank()) {
                content = withContext(Dispatchers.Default) { ReadingModeParser.parse(html) }
            } else {
                error = "Could not load article"
            }
        } catch (e: Exception) {
            error = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    BackHandler { onDismiss() }

    // Background fills the full screen including camera cutout and system bars
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbientTheme.palette.drawerBackground)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 100f) onDismiss()
                }
            }
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmbientTheme.palette.accentHigh)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            style = ResponsiveTypography.t2,
                            color = AmbientTheme.palette.errorAccent
                        )
                        Text(
                            text = "Swipe right to return",
                            style = ResponsiveTypography.t3,
                            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            content != null -> {
                // Content column respects status bar + cutout, but background already fills everything
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .displayCutoutPadding()
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    item {
                        Text(
                            text = content!!.headline,
                            style = ResponsiveTypography.d2,
                            color = AmbientTheme.palette.textPrimary,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = article.source.uppercase(),
                                style = ResponsiveTypography.t3.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = AmbientTheme.palette.accentHigh.copy(alpha = 0.7f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (content!!.author.isNotBlank()) {
                                    Text(
                                        text = content!!.author,
                                        style = ResponsiveTypography.t3.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = AmbientTheme.palette.textPrimary.copy(alpha = 0.6f)
                                    )
                                }
                                if (content!!.publishedDate.isNotBlank()) {
                                    if (content!!.author.isNotBlank()) {
                                        Text(
                                            text = "·",
                                            style = ResponsiveTypography.t3,
                                            color = AmbientTheme.palette.textSecondary.copy(alpha = 0.3f)
                                        )
                                    }
                                    Text(
                                        text = content!!.publishedDate.take(10),
                                        style = ResponsiveTypography.t3,
                                        color = AmbientTheme.palette.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    items(content!!.bodyParagraphs) { paragraph ->
                        Text(
                            text = paragraph,
                            style = ResponsiveTypography.t2.copy(
                                fontSize = 16.sp,
                                lineHeight = 26.sp
                            ),
                            color = AmbientTheme.palette.textPrimary.copy(alpha = 0.92f),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}


private fun fetchArticleHtml(url: String): String {
    return runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .build()

        com.ambient.launcher.HttpClient.instance.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string().orEmpty() else ""
        }
    }.getOrNull().orEmpty()
}
