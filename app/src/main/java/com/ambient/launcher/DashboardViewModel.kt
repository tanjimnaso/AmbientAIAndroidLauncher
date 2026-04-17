package com.ambient.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.app.usage.UsageStatsManager
import android.app.AppOpsManager
import android.os.BatteryManager
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.ambient.launcher.home.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RssFeedItem(
    val title: String,
    val source: String,
    val timestamp: String,
    val url: String,
    val publishedAtEpochMillis: Long
)

data class ForecastDay(
    val dayName: String,
    val maxTemp: Int,
    val minTemp: Int,
    val weatherCode: Int
)

data class WeatherUiState(
    val temperatureText: String = "",
    val rangeText: String = "",
    val summary: String = "",
    val locationName: String = "",
    val isAvailable: Boolean = false,
    val currentCode: Int = 0,
    val forecast: List<ForecastDay> = emptyList()
)

data class BatteryUiState(
    val percentage: Int = 0,
    val remainingHours: Int = 0,
    val remainingMinutes: Int = 0,
    val isCharging: Boolean = false
)

internal data class FeedSource(
    val name: String,
    val url: String
)

private fun getFeedSources(context: Context): List<FeedSource> {
    val prefs = context.getSharedPreferences("ambient_launcher_sections", Context.MODE_PRIVATE)
    val json = prefs.getString("rss_sources_v2", null)
    return if (json != null) {
        runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(FeedSource(obj.getString("name"), obj.getString("url").trim()))
                }
            }
        }.getOrDefault(defaultFeedSources)
    } else {
        defaultFeedSources
    }
}

internal val defaultFeedSources = listOf(
    // Wire services — authoritative, global, event-driven
    FeedSource("NYT World", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
    FeedSource("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
    FeedSource("AP Top News", "http://associated-press.s3-website-us-east-1.amazonaws.com/topnews.xml"), //
    FeedSource("Reuters World", "https://fivefilters.org/reuters-world.xml"), //
    // Global Sources
    FeedSource("Global Voices Full site feed", "https://globalvoices.org/feed/" ),
    FeedSource("The Straits Times All", "https://www.straitstimes.com/RSS-Feeds" ),
    FeedSource("The Times of India Top Stories", "https://timesofindia.indiatimes.com/rss.cms" ),
    FeedSource("Daily Maverick (South African Independent)", "https://www.dailymaverick.co.za/dmrss/" ),
    FeedSource("El Pais English", "https://english.elpais.com/rss/" ),
    FeedSource("The Age (Australia)", "https://www.theage.com.au/rssheadlines" ),
    FeedSource("Nikkei Asia Business", "https://asia.nikkei.com/rss/Business"), // [15]
    FeedSource("South China Morning Post", "https://www.scmp.com/rss/4/feed"), //
    FeedSource("AllAfrica English", "https://allafrica.com/tools/headlines/rdf/latest/headlines.rdf"), //
    FeedSource("MercoPress LatAm", "https://en.mercopress.com/rss/latin-america"), //
    FeedSource("NYT World", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
    FeedSource("France 24", "https://www.france24.com/en/rss"),
    FeedSource("Deutsche Welle", "https://rss.dw.com/rdf/rss-en-all"),
    FeedSource("NHK World", "https://www3.nhk.or.jp/nhkworld/en/news/"),
    FeedSource("CBC World", "https://www.cbc.ca/cmlink/rss-world"),
    // Politics
    FeedSource("NPR News", "https://feeds.npr.org/1001/rss.xml"),
    FeedSource("Politico", "https://rss.politico.com/politics-news.xml"),
    FeedSource("The Economist", "https://www.economist.com/latest/rss.xml"),
    FeedSource("Foreign Policy", "https://foreignpolicy.com/feed/"),
    FeedSource("CSIS Analysis", "https://www.csis.org/analysis/rss.xml"),
    // === BUSINESS & ECONOMICS ===
    FeedSource("Bloomberg", "https://www.bloomberg.com/feeds/markets"),
    FeedSource("Project Syndicate", "https://www.project-syndicate.org/rss"),
    // Tech & science
    FeedSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
    FeedSource("Hacker News", "https://hnrss.org/frontpage"),
    FeedSource("MIT Technology Review", "https://www.technologyreview.com/feed/"), //
    FeedSource("IEEE Spectrum Top", "https://spectrum.ieee.org/feeds/feed.rss"), //
    FeedSource("Science Magazine", "https://www.science.org/rss/news_current.xml"), //
    FeedSource("Quanta Magazine", "https://www.quantamagazine.org/feed/"),
    FeedSource("The Conversation", "https://theconversation.com/articles.atom"),
    FeedSource("Rest of World", "https://restofworld.org/feed/"),
    // === ENVIRONMENT & CLIMATE ===
    FeedSource("Carbon Brief", "https://www.carbonbrief.org/feed/"),
    FeedSource("Mongabay", "https://news.mongabay.com/feed/"),
    FeedSource("Yale Environment 360", "https://e360.yale.edu/feed.xml"),
    // === CULTURE, IDEAS & LONGFORM ===
    FeedSource("Aeon", "https://aeon.co/feed.rss"),
    FeedSource("Psyche", "https://psyche.co/feed"),
    FeedSource("Longreads", "https://longreads.com/feed/"),
    FeedSource("Smithsonian", "https://www.smithsonianmag.com/rss/articles/"),
    FeedSource("Open Culture", "https://www.openculture.com/feed"),
    // === HUMANITIES & SOCIAL SCIENCE ===
    FeedSource("Lapham's Quarterly", "https://www.laphamsquarterly.org/rss.xml"),
    FeedSource("Public Domain Review", "https://publicdomainreview.org/feed/"),
    // Health, & Wellness (Evidence-Based)
    FeedSource("Mayo Clinic Research", "https://newsnetwork.mayoclinic.org/rss/research.xml"), // [36]
    FeedSource("Harvard Health Blog", "https://www.health.harvard.edu/blog/feed"), // [53]
    FeedSource("Stronger by Science", "https://sbspod.com/feed/"), // [41]
    FeedSource("Science of Running", "http://feeds.feedburner.com/stevemagness"), //
    FeedSource("iRunFar Trail", "https://www.irunfar.com/feed"), //
    FeedSource("STAT News", "https://www.statnews.com/feed/"),
    FeedSource("BMJ News", "https://www.bmj.com/rss.xml"),
    FeedSource("Runner's World", "https://www.runnersworld.com/rss/all.xml"),
    FeedSource("Greatist", "https://greatist.com/feed"),
    FeedSource("Outside Magazine", "https://www.outsideonline.com/feed/"),
    // Culinary Science & Precision Cooking
    FeedSource("Serious Eats Latest", "https://www.seriouseats.com/atom.xml"), // [52]
    FeedSource("ATK In the Test Kitchen", "https://megaphone.fm/feeds/americas-test-kitchen"), // [51]
    FeedSource("Anova Culinary Blog", "https://anovaculinary.com/blogs/blog.atom") //
)

// ---------------------------------------------------------------------------
// Feed quality filters — pure functions, no network, no LLM
// ---------------------------------------------------------------------------

private object FeedFilter {
    // Unambiguous sports-only terminology — deliberately narrow to avoid false positives
    private val SPORTS_TERMS = setOf(
        "premier league", "champions league", "europa league", "conference league",
        "la liga", "serie a", "bundesliga", "ligue 1", "mls season",
        "super bowl", "stanley cup", "world series", "nba finals", "nfl draft",
        "grand slam", "wimbledon", "french open", "us open tennis",
        "formula 1", "motogp race", "nascar",
        "cricket test", "ashes series", " odi ", " t20 ",
        "rugby union", "rugby league", "six nations",
        "transfer window", "transfer fee", "signed for £", "signed for $",
        "hat-trick", "hat trick", "own goal", "penalty shootout",
        "quarterback", "touchdown", "linebacker", "slam dunk", "home run",
        " ufc ", "heavyweight title fight", "weigh-in"
    )

    // Opinion/personal column markers — check as title prefix or full match
    private val OPINION_PREFIXES = listOf(
        "opinion:", "column:", "commentary:", "my view:", "perspective:",
        "letters:", "letter to the editor", "your letters",
        "why i ", "my ", "i think ", "what i "
    )

    // Formulaic clickbait constructions
    private val CLICKBAIT_PATTERNS = listOf(
        Regex("""^\d{1,2} (things|ways|reasons|tips|signs|facts)""", RegexOption.IGNORE_CASE),
        Regex("""^here'?s (why|what|how|everything)""", RegexOption.IGNORE_CASE),
        Regex("""you (won'?t believe|need to know|should know)""", RegexOption.IGNORE_CASE),
        Regex("""everything you need to know""", RegexOption.IGNORE_CASE),
        Regex("""^(watch|photos|video|gallery):""", RegexOption.IGNORE_CASE)
    )

    fun shouldInclude(item: RssFeedItem): Boolean {
        val lower = item.title.lowercase()
        return SPORTS_TERMS.none { lower.contains(it) }
            && OPINION_PREFIXES.none { lower.startsWith(it) }
            && CLICKBAIT_PATTERNS.none { it.containsMatchIn(item.title) }
    }
}

// ---------------------------------------------------------------------------
// Cross-source deduplication via Jaccard title similarity — no LLM required
// ---------------------------------------------------------------------------

private object FeedDeduplicator {
    private val STOP_WORDS = setOf(
        "the", "a", "an", "in", "on", "at", "to", "of", "for", "with",
        "is", "are", "was", "were", "be", "been", "has", "have", "had",
        "will", "would", "could", "should", "that", "this", "it", "its",
        "and", "or", "but", "not", "as", "by", "from", "after", "over",
        "new", "says", "said", "up", "down", "out", "than", "more", "than"
    )

    private fun fingerprint(title: String): Set<String> =
        title.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 2 && it !in STOP_WORDS }
            .toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        return a.intersect(b).size.toFloat() / (a + b).size
    }

    // Input should already be sorted newest-first; the first item in each cluster wins.
    fun deduplicate(items: List<RssFeedItem>): List<RssFeedItem> {
        val clusters = mutableListOf<Pair<RssFeedItem, Set<String>>>()
        for (item in items) {
            val fp = fingerprint(item.title)
            val isDuplicate = clusters.any { (_, clusterFp) -> jaccard(fp, clusterFp) >= 0.40f }
            if (!isDuplicate) clusters.add(item to fp)
        }
        return clusters.map { it.first }
    }
}

internal class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sharedPreferences =
        appContext.getSharedPreferences("ambient_dashboard_cache", Context.MODE_PRIVATE)
    private val client = HttpClient.instance

    private val _feedItems = MutableStateFlow<List<RssFeedItem>>(emptyList())
    val feedItems: StateFlow<List<RssFeedItem>> = _feedItems.asStateFlow()

    private val _lastFeedRefreshTime = MutableStateFlow(0L)
    val lastFeedRefreshTime: StateFlow<Long> = _lastFeedRefreshTime.asStateFlow()

    // Dead feed URLs → replacement mappings.  Applied once on startup to migrate saved prefs.
    private val deadFeedReplacements = mapOf(
        "feeds.ap.org" to ("NYT World" to "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
        "reutersagency.com" to ("Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml"),
        "feeds.reuters.com" to ("Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml")
    )

    // Seed from SharedPreferences so the first refreshFeeds() on init uses real sources.
    private var currentSources: List<Pair<String, String>> = run {
        val prefs = appContext.getSharedPreferences("ambient_launcher_sections", Context.MODE_PRIVATE)
        val json = prefs.getString("rss_sources_v2", null)
        if (json != null) {
            val parsed = runCatching {
                val array = JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(obj.getString("name") to obj.getString("url").trim())
                    }
                }
            }.getOrNull()

            // Migrate dead feeds in saved prefs
            if (parsed != null) {
                val migrated = parsed.map { (name, url) ->
                    val replacement = deadFeedReplacements.entries.find { url.contains(it.key) }
                    if (replacement != null) replacement.value else (name to url)
                }
                if (migrated != parsed) {
                    // Persist the migration
                    val array = JSONArray()
                    migrated.forEach { (n, u) -> array.put(JSONObject().put("name", n).put("url", u)) }
                    prefs.edit().putString("rss_sources_v2", array.toString()).apply()
                }
                migrated
            } else null
        } else null
    } ?: defaultFeedSources.map { it.name to it.url }

    fun setRssSources(sources: List<Pair<String, String>>) {
        // Prevent refresh if sources are identical (by content, not reference)
        val sourcesChanged = sources.size != currentSources.size ||
                sources.zip(currentSources).any { (a, b) -> a != b }

        if (!sourcesChanged) return
        currentSources = sources
        refreshFeeds()
    }

    private val _weather = MutableStateFlow(WeatherUiState())
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryUiState())
    val batteryState: StateFlow<BatteryUiState> = _batteryState.asStateFlow()

    // Tracks the last known charging state to detect the moment the charger is removed.
    // When a transition from charging→discharging is detected, we snapshot the current
    // percentage as the "reset point" so the countdown always starts from the actual
    // level you unplugged at, not an assumed 100%.
    private var lastWasCharging = false
    private var batteryReceiver: BroadcastReceiver? = null
    private val feedFetchSemaphore = Semaphore(8)

    companion object {
        // Measured discharge duration for this device: 29 hours 35 minutes.
        private const val TOTAL_DISCHARGE_HOURS = 29.583
        private const val PREFS_BATTERY = "battery_prefs"
        private const val KEY_RESET_PCT = "reset_pct"
        // Max articles any single source can contribute to the final 30-item feed.
        // Prevents high-frequency publishers (ABC, FT) from flooding the list.
        private const val MAX_PER_SOURCE = 3
    }

    init {
        loadCachedFeeds()
        loadCachedWeather()
        loadInstalledApps()
        initBatteryMonitoring()
        refreshFeeds()
    }

    private fun initBatteryMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleBatteryIntent(intent)
            }
        }
        appContext.registerReceiver(batteryReceiver, filter)

        // Seed the initial state immediately from the sticky broadcast.
        appContext.registerReceiver(null, filter)?.let { handleBatteryIntent(it) }
    }

    private fun handleBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level == -1 || scale == -1) return

        val pct = (level * 100 / scale.toFloat()).toInt()
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // Charger just removed → record this percentage (if we need it later)
        if (lastWasCharging && !isCharging) {
            appContext.getSharedPreferences(PREFS_BATTERY, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_RESET_PCT, pct)
                .apply()
        }
        lastWasCharging = isCharging

        val remainingHours = if (isCharging) 0.0
        else TOTAL_DISCHARGE_HOURS * (pct.toDouble() / 100.0) // Bug fix: scale off absolute 100%

        _batteryState.value = BatteryUiState(
            percentage = pct,
            remainingHours = remainingHours.toInt(),
            remainingMinutes = ((remainingHours - remainingHours.toInt()) * 60).toInt(),
            isCharging = isCharging
        )
    }

    override fun onCleared() {
        super.onCleared()
        batteryReceiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) { }
        }
        batteryReceiver = null
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val profiles = launcherApps.profiles
            
            val apps = mutableListOf<AppInfo>()
            for (profile in profiles) {
                launcherApps.getActivityList(null, profile).forEach { info ->
                    if (info.componentName.packageName != appContext.packageName) {
                        apps.add(
                            AppInfo(
                                label = info.label.toString(),
                                packageName = info.componentName.packageName
                            )
                        )
                    }
                }
            }
            
            // Sort by usage stats if permission is granted, otherwise alphabetical
            val sortedApps = sortAppsByUsage(apps)
            
            _installedApps.value = sortedApps.distinctBy { it.packageName }
        }
    }

    private fun sortAppsByUsage(apps: List<AppInfo>): List<AppInfo> {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        }

        if (mode != AppOpsManager.MODE_ALLOWED) {
            return apps.sortedBy { it.label.lowercase() }
        }

        val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - java.util.concurrent.TimeUnit.DAYS.toMillis(3) // Last 3 days

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        return apps.sortedByDescending { app ->
            stats[app.packageName]?.totalTimeInForeground ?: 0L
        }
    }

    fun refreshWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            val location = getLastKnownLocation()
            val (latitude, longitude, locationName) = if (location != null) {
                Triple(location.latitude, location.longitude, resolveLocationName(location))
            } else {
                Triple(-33.8688, 151.2093, "Sydney")
            }
            val weather = fetchWeather(latitude, longitude, locationName)
            if (weather != null) {
                saveCachedWeather(weather)
                _weather.value = weather
            }
        }
    }

    fun refreshFeeds() {
        viewModelScope.launch(Dispatchers.IO) {
            val cutoffMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(48)
            val channel = Channel<List<RssFeedItem>>(capacity = Channel.UNLIMITED)

            val jobs = currentSources.map { (name, url) ->
                async {
                    val items = feedFetchSemaphore.withPermit {
                        fetchFeedItems(FeedSource(name, url.trim()))
                    }
                    channel.send(items)
                }
            }
            // Close channel once every source has responded (or timed out)
            launch { jobs.awaitAll(); channel.close() }

            // Stream: update the feed as each source arrives rather than waiting for all 55
            val accumulator = mutableListOf<RssFeedItem>()
            for (incoming in channel) {
                if (incoming.isEmpty()) continue
                accumulator.addAll(incoming)
                val processed = buildFreshFeed(accumulator, cutoffMillis)
                if (processed.isNotEmpty()) {
                    _feedItems.value = processed
                    _lastFeedRefreshTime.value = System.currentTimeMillis()
                }
            }

            // Persist the final settled feed to cache
            val final = _feedItems.value
            if (final.isNotEmpty()) saveCachedFeeds(final)
        }
    }

    private fun buildFreshFeed(raw: List<RssFeedItem>, cutoffMillis: Long): List<RssFeedItem> {
        val filtered = raw
            .filter { it.publishedAtEpochMillis > 0L }
            .filter { it.publishedAtEpochMillis >= cutoffMillis }
            .filter { FeedFilter.shouldInclude(it) }
            .sortedByDescending { it.publishedAtEpochMillis }
        val deduped = FeedDeduplicator.deduplicate(filtered).distinctBy { it.url }
        val counts = mutableMapOf<String, Int>()
        return deduped.filter { item ->
            val n = counts.getOrDefault(item.source, 0)
            (n < MAX_PER_SOURCE).also { if (it) counts[item.source] = n + 1 }
        }.take(30)
    }

    fun addFeedSource(name: String, url: String) {
        val prefs = appContext.getSharedPreferences("ambient_launcher_sections", Context.MODE_PRIVATE)
        val sources = getFeedSources(appContext).toMutableList()
        sources.add(FeedSource(name, url.trim()))
        
        val array = JSONArray()
        sources.forEach { 
            array.put(JSONObject().put("name", it.name).put("url", it.url))
        }
        prefs.edit().putString("rss_sources_v2", array.toString()).apply()
        refreshFeeds()
    }

    private fun fetchFeedItems(source: FeedSource): List<RssFeedItem> {
        val request = Request.Builder()
            .url(source.url)
            // Browser-like UA required — Google News RSS silently returns empty/403 for custom agents
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return emptyList()
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
                                        // Atom: preferred rel="alternate", or no rel
                                        if (rel == null || rel == "alternate") {
                                            link = href
                                        }
                                    } else {
                                        // RSS: simple link body
                                        link = safeNextText(parser)
                                    }
                                }
                                localName.equals("pubDate", ignoreCase = true) || 
                                localName.equals("published", ignoreCase = true) || 
                                localName.equals("updated", ignoreCase = true) ||
                                localName.equals("date", ignoreCase = true) -> {
                                    pubDate = safeNextText(parser)
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
                                        publishedAtEpochMillis = parseEpochMillis(pubDate)
                                    )
                                )
                            }
                            inItem = false
                            title = ""
                            link = ""
                            pubDate = ""
                            
                            if (items.size >= 8) break  // 8/source; quality over volume
                        }
                    }
                }
                eventType = parser.next()
            }
            items
        }.getOrDefault(emptyList())
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return runCatching { parser.nextText() }.getOrDefault("")
    }

    private fun parseEpochMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val trimmed = dateStr.trim()
        return runCatching {
            // RSS standard
            ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.recoverCatching {
            // Atom standard / ISO
            OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
        }.recoverCatching {
            // Common variation: "2024-05-23 12:00:00"
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.of("UTC"))
            ZonedDateTime.parse(trimmed, fmt).toInstant().toEpochMilli()
        }.getOrDefault(0L)  // 0L = unknown date; excluded by freshness gate in refreshFeeds
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

    private fun fetchWeather(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): WeatherUiState? {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=$latitude")
            append("&longitude=$longitude")
            append("&current=temperature_2m,weather_code")
            append("&daily=temperature_2m_max,temperature_2m_min,weather_code")
            append("&forecast_days=4")
            append("&timezone=auto")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AmbientLauncher/1.0")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return null

                val root = JSONObject(body)
                val current = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")

                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                
                val maxTemps = daily.getJSONArray("temperature_2m_max")
                val minTemps = daily.getJSONArray("temperature_2m_min")
                val weatherCodes = daily.getJSONArray("weather_code")
                val timeArray = daily.getJSONArray("time")

                val forecast = mutableListOf<ForecastDay>()
                // Today is index 0, so next 3 days are 1, 2, 3
                for (i in 1 until 4) {
                    val date = java.time.LocalDate.parse(timeArray.getString(i))
                    forecast.add(
                        ForecastDay(
                            dayName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                            maxTemp = maxTemps.getDouble(i).toInt(),
                            minTemp = minTemps.getDouble(i).toInt(),
                            weatherCode = weatherCodes.getInt(i)
                        )
                    )
                }

                WeatherUiState(
                    temperatureText = "$temp°",
                    rangeText = "H:${maxTemps.getDouble(0).toInt()}° L:${minTemps.getDouble(0).toInt()}°",
                    summary = weatherCodeToSummary(code),
                    locationName = locationName,
                    isAvailable = true,
                    currentCode = code,
                    forecast = forecast
                )
            }
        }.getOrNull()
    }

    private fun loadCachedWeather() {
        val json = sharedPreferences.getString("weather_v2", null).orEmpty()
        if (json.isBlank()) return

        runCatching {
            val obj = JSONObject(json)
            val forecastArray = obj.optJSONArray("forecast")
            val forecast = mutableListOf<ForecastDay>()
            if (forecastArray != null) {
                for (i in 0 until forecastArray.length()) {
                    val f = forecastArray.getJSONObject(i)
                    forecast.add(
                        ForecastDay(
                            f.getString("dayName"),
                            f.getInt("maxTemp"),
                            f.getInt("minTemp"),
                            f.getInt("weatherCode")
                        )
                    )
                }
            }
            _weather.value = WeatherUiState(
                temperatureText = obj.getString("temperatureText"),
                rangeText = obj.getString("rangeText"),
                summary = obj.getString("summary"),
                locationName = obj.getString("locationName"),
                isAvailable = obj.getBoolean("isAvailable"),
                currentCode = obj.optInt("currentCode", 0),
                forecast = forecast
            )
        }
    }

    private fun saveCachedWeather(weather: WeatherUiState) {
        val json = JSONObject().apply {
            put("temperatureText", weather.temperatureText)
            put("rangeText", weather.rangeText)
            put("summary", weather.summary)
            put("locationName", weather.locationName)
            put("isAvailable", weather.isAvailable)
            put("currentCode", weather.currentCode)
            
            val forecastArray = JSONArray()
            weather.forecast.forEach { f ->
                forecastArray.put(JSONObject().apply {
                    put("dayName", f.dayName)
                    put("maxTemp", f.maxTemp)
                    put("minTemp", f.minTemp)
                    put("weatherCode", f.weatherCode)
                })
            }
            put("forecast", forecastArray)
        }.toString()
        sharedPreferences.edit().putString("weather_v2", json).apply()
    }

    private fun loadCachedFeeds() {
        val json = sharedPreferences.getString("feed_items_v1", null).orEmpty()
        if (json.isBlank()) return

        val cachedItems = runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        RssFeedItem(
                            title = item.getString("title"),
                            source = item.getString("source"),
                            timestamp = item.getString("timestamp"),
                            url = item.getString("url"),
                            publishedAtEpochMillis = item.optLong("publishedAtEpochMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        if (cachedItems.isNotEmpty()) {
            _feedItems.value = cachedItems
        }
    }

    private fun saveCachedFeeds(items: List<RssFeedItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("source", item.source)
                    put("timestamp", item.timestamp)
                    put("url", item.url)
                    put("publishedAtEpochMillis", item.publishedAtEpochMillis)
                }
            )
        }
        sharedPreferences.edit().putString("feed_items_v1", array.toString()).apply()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarse && !hasFine) return null

        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // Try getting an active current location if possible (API 30+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    val cancellationSignal = android.os.CancellationSignal()
                    cont.invokeOnCancellation { cancellationSignal.cancel() }
                    
                    locationManager.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        cancellationSignal,
                        appContext.mainExecutor
                    ) { location ->
                        if (location != null) {
                            cont.resume(location, null)
                        } else {
                            cont.resume(null, null)
                        }
                    }
                } ?: getBestLastKnown(locationManager)
            } catch (e: Exception) {
                return getBestLastKnown(locationManager)
            }
        }

        return getBestLastKnown(locationManager)
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnown(locationManager: LocationManager): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun resolveLocationName(location: Location): String {
        val geocoder = Geocoder(appContext, Locale.getDefault())
        return runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.countryName
                }
        }.getOrNull().orEmpty()
    }

    private fun weatherCodeToSummary(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2 -> "Mostly clear"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}
