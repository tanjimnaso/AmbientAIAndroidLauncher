package com.ambient.launcher.data.repository

import android.content.Context
import com.ambient.launcher.FeedSource
import com.ambient.launcher.HttpClient
import com.ambient.launcher.RssFeedItem
import com.ambient.launcher.defaultFeedSources
import com.ambient.launcher.data.repository.FeedFilter
import com.ambient.launcher.data.repository.FeedDeduplicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

internal class FeedRepository(private val context: Context) {
    private val client = HttpClient.instance
    private val feedFetchSemaphore = Semaphore(8)

    private val deadFeedReplacements = mapOf(
        "feeds.ap.org" to ("NYT World" to "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
        "reutersagency.com" to ("Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml"),
        "feeds.reuters.com" to ("Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml")
    )

    // Sources are no longer persisted to prefs. `defaultFeedSources` is the source of truth —
    // edit the code list to manage feeds. The settings dialog may still call `saveSources` but
    // its changes are session-scoped and reset on launch.
    fun getSources(): List<Pair<String, String>> {
        // One-shot cleanup: drop the legacy rss_sources_v3 key if it's lingering.
        val prefs = context.getSharedPreferences("ambient_launcher_sections", Context.MODE_PRIVATE)
        if (prefs.contains("rss_sources_v3")) {
            prefs.edit().remove("rss_sources_v3").apply()
        }
        return defaultFeedSources
            .map { it.name to it.url }
            .map { (name, url) ->
                val replacement = deadFeedReplacements.entries.find { url.contains(it.key) }
                replacement?.value ?: (name to url)
            }
    }

    // No-op: kept so the in-session dialog save path doesn't need to change shape.
    // If persistent user customization is reintroduced, wire it back here.
    @Suppress("UNUSED_PARAMETER")
    fun saveSources(sources: List<Pair<String, String>>) = Unit

    suspend fun fetchFeeds(sources: List<Pair<String, String>>, onItemsFetched: (List<RssFeedItem>) -> Unit) = coroutineScope {
        val channel = Channel<List<RssFeedItem>>(capacity = Channel.UNLIMITED)

        val jobs = sources.map { (name, url) ->
            async {
                val items = feedFetchSemaphore.withPermit {
                    fetchFeedItems(FeedSource(name, url.trim()))
                }
                channel.send(items)
            }
        }

        launch { jobs.awaitAll(); channel.close() }

        for (incoming in channel) {
            if (incoming.isNotEmpty()) {
                onItemsFetched(incoming)
            }
        }
    }

    private suspend fun fetchFeedItems(source: FeedSource): List<RssFeedItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching emptyList()
                parseFeedItems(xml = body, source = source.name)
            }
        }.getOrDefault(emptyList())
    }

    private fun parseFeedItems(xml: String, source: String): List<RssFeedItem> {
        val items = mutableListOf<RssFeedItem>()
        return runCatching {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var title = ""
            var link = ""
            var pubDate = ""
            var description = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (localName.equals("item", ignoreCase = true) || localName.equals("entry", ignoreCase = true)) {
                            inItem = true
                        } else if (inItem) {
                            when {
                                localName.equals("title", ignoreCase = true) -> title = safeNextText(parser)
                                localName.equals("link", ignoreCase = true) -> {
                                    val rel = parser.getAttributeValue(null, "rel")
                                    val href = parser.getAttributeValue(null, "href")
                                    if (href != null) {
                                        if (rel == null || rel == "alternate") link = href
                                    } else {
                                        link = safeNextText(parser)
                                    }
                                }
                                localName.equals("pubDate", ignoreCase = true) ||
                                localName.equals("published", ignoreCase = true) ||
                                localName.equals("updated", ignoreCase = true) ||
                                localName.equals("date", ignoreCase = true) -> {
                                    pubDate = safeNextText(parser)
                                }
                                // Description sources, in preference order: RSS <description>,
                                // Atom <summary>, Atom <content>, and <content:encoded>. The last
                                // non-blank wins so richer fields override poorer ones within the item.
                                localName.equals("description", ignoreCase = true) ||
                                localName.equals("summary", ignoreCase = true) ||
                                localName.equals("content", ignoreCase = true) ||
                                localName.equals("encoded", ignoreCase = true) -> {
                                    val text = safeNextText(parser).cleanDescription()
                                    if (text.isNotBlank()) description = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (localName.equals("item", ignoreCase = true) || localName.equals("entry", ignoreCase = true)) {
                            if (title.isNotBlank() && link.isNotBlank()) {
                                items.add(
                                    RssFeedItem(
                                        title = title.trim(),
                                        source = source,
                                        timestamp = parseTimeAgo(pubDate),
                                        url = link.trim(),
                                        publishedAtEpochMillis = parseEpochMillis(pubDate),
                                        description = description
                                    )
                                )
                            }
                            inItem = false
                            title = ""
                            link = ""
                            pubDate = ""
                            description = ""
                            if (items.size >= 8) break
                        }
                    }
                }
                eventType = parser.next()
            }
            items
        }.getOrDefault(emptyList())
    }

    private fun safeNextText(parser: XmlPullParser): String = runCatching { parser.nextText() }.getOrDefault("")

    /** Strip HTML tags + common entities, collapse whitespace, cap at 300 chars. */
    private fun String.cleanDescription(): String {
        if (isBlank()) return ""
        var s = replace(Regex("<[^>]+>"), " ")
        s = s
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("&#\\d+;"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        return if (s.length > 300) s.take(297) + "..." else s
    }

    private fun parseEpochMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val trimmed = dateStr.trim()
        return runCatching {
            ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.recoverCatching {
            OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
        }.recoverCatching {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.of("UTC"))
            ZonedDateTime.parse(trimmed, fmt).toInstant().toEpochMilli()
        }.getOrDefault(0L)
    }

    private fun parseTimeAgo(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return runCatching {
            val instant = Instant.ofEpochMilli(parseEpochMillis(dateStr))
            val now = Instant.now()
            val duration = Duration.between(instant, now)
            when {
                duration.toDays() > 0 -> "${duration.toDays()}d"
                duration.toHours() > 0 -> "${duration.toHours()}h"
                duration.toMinutes() > 0 -> "${duration.toMinutes()}m"
                else -> "now"
            }
        }.getOrDefault("")
    }

    fun buildFreshFeed(raw: List<RssFeedItem>): List<RssFeedItem> {
        val now = System.currentTimeMillis()
        val strictCutoff = now - TimeUnit.HOURS.toMillis(48)
        val relaxedCutoff = now - TimeUnit.DAYS.toMillis(14)

        val filtered = raw
            .filter { it.publishedAtEpochMillis > 0L }
            .filter { it.publishedAtEpochMillis >= relaxedCutoff }
            .filter { FeedFilter.shouldInclude(it) }
            .sortedByDescending { it.publishedAtEpochMillis }
            
        val deduped = FeedDeduplicator.deduplicate(filtered).distinctBy { it.url }
        val bySource = deduped.groupBy { it.source }

        val finalSelection = mutableListOf<RssFeedItem>()
        var depth = 0
        val sourceFrequency = bySource.mapValues { (_, items) -> items.size }
        val feedCap = (bySource.size * 2).coerceIn(30, 80)

        while (finalSelection.size < feedCap && depth < 3 && bySource.values.any { it.size > depth }) {
            val candidates = bySource.values
                .mapNotNull { it.getOrNull(depth) }
                .filter { depth == 0 || it.publishedAtEpochMillis >= strictCutoff }
                .sortedWith(compareBy(
                    { sourceFrequency[it.source] ?: 0 },
                    { -it.publishedAtEpochMillis }
                ))

            for (item in candidates) {
                if (finalSelection.size >= feedCap) break
                finalSelection.add(item)
            }
            depth++
        }
        return finalSelection.sortedByDescending { it.publishedAtEpochMillis }
    }
}
