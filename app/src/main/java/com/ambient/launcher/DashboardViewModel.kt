package com.ambient.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ambient.launcher.data.repository.AppRepository
import com.ambient.launcher.data.repository.FeedRepository
import com.ambient.launcher.data.repository.WeatherRepository
import com.ambient.launcher.home.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class RssFeedItem(
    val title: String,
    val source: String,
    val timestamp: String,
    val url: String,
    val publishedAtEpochMillis: Long,
    val isStarred: Boolean = false
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

internal val defaultFeedSources = listOf(
    // ── Global wire services ──────────────────────────────────────────────────
    FeedSource("BBC World", "http://feeds.bbci.co.uk/news/world/rss.xml"),
    FeedSource("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
    FeedSource("NYT World", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
    FeedSource("The Guardian", "https://www.theguardian.com/world/rss"),
    FeedSource("NPR World", "https://feeds.npr.org/1004/rss.xml"),
    FeedSource("CBC World", "https://www.cbc.ca/cmlink/rss-world"),
    FeedSource("Deutsche Welle", "https://rss.dw.com/rdf/rss-en-all"),
    FeedSource("France 24", "https://www.france24.com/en/rss"),
    // ── Asia-Pacific ─────────────────────────────────────────────────────────
    FeedSource("Nikkei Asia", "https://asia.nikkei.com/rss/feed/nar"),
    FeedSource("South China Morning Post", "https://www.scmp.com/rss/91/feed"),
    FeedSource("Global Voices", "https://globalvoices.org/feed/"),
    FeedSource("NHK World", "https://www3.nhk.or.jp/nhkworld/en/news/rss.xml"),
    // ── Latin America & Africa ────────────────────────────────────────────────
    FeedSource("MercoPress", "https://en.mercopress.com/rss/latin-america"),
    FeedSource("AllAfrica", "https://allafrica.com/tools/headlines/rdf/latest/headlines.rdf"),
    // ── Politics & analysis ───────────────────────────────────────────────────
    FeedSource("The Economist", "https://www.economist.com/latest/rss.xml"),
    FeedSource("Foreign Policy", "https://foreignpolicy.com/feed/"),
    FeedSource("Project Syndicate", "https://www.project-syndicate.org/rss"),
    // ── Tech & science ────────────────────────────────────────────────────────
    FeedSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
    FeedSource("Hacker News", "https://hnrss.org/frontpage"),
    FeedSource("Quanta Magazine", "https://www.quantamagazine.org/feed/"),
    FeedSource("The Conversation", "https://theconversation.com/articles.atom"),
    FeedSource("Rest of World", "https://restofworld.org/feed/"),
    FeedSource("STAT News", "https://www.statnews.com/feed/"),
    FeedSource("Daring Fireball", "https://daringfireball.net/feeds/main"),
    // ── Environment & climate ─────────────────────────────────────────────────
    FeedSource("Carbon Brief", "https://www.carbonbrief.org/feed/"),
    FeedSource("Mongabay", "https://news.mongabay.com/feed/"),
    FeedSource("Yale E360", "https://e360.yale.edu/feed.xml"),
    // ── Ideas & culture ───────────────────────────────────────────────────────
    FeedSource("Aeon", "https://aeon.co/feed.rss"),
    FeedSource("Longreads", "https://longreads.com/feed/"),
    FeedSource("Smithsonian", "https://www.smithsonianmag.com/rss/articles/"),
    FeedSource("Public Domain Review", "https://publicdomainreview.org/feed/"),
    FeedSource("Arts & Letters Daily", "https://www.aldaily.com/feed"),
    FeedSource("Noema Magazine", "https://www.noemamag.com/feed/"),
    FeedSource("3 Quarks Daily", "https://3quarksdaily.com/feed"),
    FeedSource("Harper’s Magazine", "https://harpers.org/feed"),
    FeedSource("The Marginalian", "https://feeds.feedburner.com/brainpickings/rss"),
    FeedSource("Marginal Revolution", "https://feeds.feedblitz.com/marginalrevolution"),
)

internal class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sharedPreferences =
        appContext.getSharedPreferences("ambient_dashboard_cache", Context.MODE_PRIVATE)

    private val appRepository = AppRepository(appContext)
    private val weatherRepository = WeatherRepository(appContext)
    private val feedRepository = FeedRepository(appContext)

    private val _feedItems = MutableStateFlow<List<RssFeedItem>>(emptyList())
    val feedItems: StateFlow<List<RssFeedItem>> = _feedItems.asStateFlow()

    private val _lastFeedRefreshTime = MutableStateFlow(0L)
    val lastFeedRefreshTime: StateFlow<Long> = _lastFeedRefreshTime.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentSources: List<Pair<String, String>> = feedRepository.getSources()

    private val _weather = MutableStateFlow(WeatherUiState())
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _starredUrls = MutableStateFlow<Set<String>>(emptySet())
    private val _deletedUrls = MutableStateFlow<Set<String>>(emptySet())

    private fun loadRssUserStates() {
        sharedPreferences?.let { prefs ->
            val starred = prefs.getStringSet("rss_starred_urls", emptySet()) ?: emptySet()
            val deleted = prefs.getStringSet("rss_deleted_urls", emptySet()) ?: emptySet()
            _starredUrls.value = starred
            _deletedUrls.value = deleted
        }
    }

    private fun saveStarredUrls(urls: Set<String>) {
        sharedPreferences?.edit()?.putStringSet("rss_starred_urls", urls)?.apply()
    }

    private fun saveDeletedUrls(urls: Set<String>) {
        sharedPreferences?.edit()?.putStringSet("rss_deleted_urls", urls)?.apply()
    }

    fun toggleStar(item: RssFeedItem) {
        val current = _starredUrls.value.toMutableSet()
        if (current.contains(item.url)) current.remove(item.url) else current.add(item.url)
        _starredUrls.value = current
        saveStarredUrls(current)
        applyUserStatesToFeeds()
    }

    fun deleteFeedItem(item: RssFeedItem) {
        val current = _deletedUrls.value.toMutableSet()
        current.add(item.url)
        _deletedUrls.value = current
        saveDeletedUrls(current)
        applyUserStatesToFeeds()
    }

    private fun applyUserStatesToFeeds() {
        val starred = _starredUrls.value
        val deleted = _deletedUrls.value
        val currentItems = _feedItems.value.map { it.copy(isStarred = starred.contains(it.url)) }
            .filter { !deleted.contains(it.url) }
            .sortedWith(compareByDescending<RssFeedItem> { it.isStarred }.thenByDescending { it.publishedAtEpochMillis })
        _feedItems.value = currentItems
    }

    private val _batteryState = MutableStateFlow(BatteryUiState())
    val batteryState: StateFlow<BatteryUiState> = _batteryState.asStateFlow()

    private var lastWasCharging = false
    private var batteryReceiver: BroadcastReceiver? = null

    companion object {
        private const val TOTAL_DISCHARGE_HOURS = 29.583
        private const val PREFS_BATTERY = "battery_prefs"
        private const val KEY_RESET_PCT = "reset_pct"
    }

    init {
        loadRssUserStates()
        loadCachedFeeds()
        loadCachedWeather()
        loadInstalledApps()
        initBatteryMonitoring()
        refreshFeeds()
        refreshWeather()
    }

    private fun initBatteryMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleBatteryIntent(intent)
            }
        }
        appContext.registerReceiver(batteryReceiver, filter)
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

        if (lastWasCharging && !isCharging) {
            appContext.getSharedPreferences(PREFS_BATTERY, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_RESET_PCT, pct)
                .apply()
        }
        lastWasCharging = isCharging

        val remainingHours = if (isCharging) 0.0
        else TOTAL_DISCHARGE_HOURS * (pct.toDouble() / 100.0)

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
        viewModelScope.launch {
            _installedApps.value = appRepository.getInstalledApps()
        }
    }

    fun setRssSources(sources: List<Pair<String, String>>) {
        val sourcesChanged = sources.size != currentSources.size ||
                sources.zip(currentSources).any { (a, b) -> a != b }

        if (!sourcesChanged) return
        currentSources = sources
        feedRepository.saveSources(sources)
        refreshFeeds()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val weather = weatherRepository.fetchWeather()
            if (weather != null) {
                saveCachedWeather(weather)
                _weather.value = weather
            }
        }
    }

    fun refreshFeeds() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                feedRepository.fetchFeeds(currentSources) { incoming ->
                    val currentList = _feedItems.value
                    val combined = currentList + incoming
                    val processed = feedRepository.buildFreshFeed(combined)
                    
                    if (processed.isNotEmpty()) {
                        val starred = _starredUrls.value
                        val deleted = _deletedUrls.value
                        val withStates = processed.map { it.copy(isStarred = starred.contains(it.url)) }
                            .filter { !deleted.contains(it.url) }
                            .sortedWith(compareByDescending<RssFeedItem> { it.isStarred }.thenByDescending { it.publishedAtEpochMillis })

                        _feedItems.value = withStates
                        _lastFeedRefreshTime.value = System.currentTimeMillis()
                    }
                }

                val final = _feedItems.value
                if (final.isNotEmpty()) saveCachedFeeds(final)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addFeedSource(name: String, url: String) {
        val sources = feedRepository.getSources().toMutableList()
        sources.add(name to url.trim())
        feedRepository.saveSources(sources)
        currentSources = sources
        refreshFeeds()
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
        val expiry = 15 * 60 * 1000L
        val cacheTime = sharedPreferences.getLong("feed_items_timestamp_v1", 0L)
        if (System.currentTimeMillis() - cacheTime > expiry) return

        val json = sharedPreferences.getString("feed_items_v1", null).orEmpty()
        if (json.isBlank()) return

        val starred = _starredUrls.value
        val deleted = _deletedUrls.value

        val cachedItems = runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val url = item.getString("url")
                    if (!deleted.contains(url)) {
                        add(
                            RssFeedItem(
                                title = item.getString("title"),
                                source = item.getString("source"),
                                timestamp = item.getString("timestamp"),
                                url = url,
                                publishedAtEpochMillis = item.optLong("publishedAtEpochMillis", 0L),
                                isStarred = starred.contains(url)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())

        if (cachedItems.isNotEmpty()) {
            _feedItems.value = cachedItems.sortedWith(compareByDescending<RssFeedItem> { it.isStarred }.thenByDescending { it.publishedAtEpochMillis })
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
        sharedPreferences.edit()
            .putString("feed_items_v1", array.toString())
            .putLong("feed_items_timestamp_v1", System.currentTimeMillis())
            .apply()
    }
}
