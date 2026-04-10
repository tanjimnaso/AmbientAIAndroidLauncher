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
import android.os.BatteryManager
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.ambient.launcher.home.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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

data class WeatherUiState(
    val temperatureText: String = "",
    val rangeText: String = "",
    val summary: String = "",
    val locationName: String = "",
    val isAvailable: Boolean = false
)

data class BatteryUiState(
    val percentage: Int = 0,
    val remainingHours: Int = 0,
    val remainingMinutes: Int = 0,
    val isCharging: Boolean = false
)

private data class FeedSource(
    val name: String,
    val url: String
)

private fun getFeedSources(context: Context): List<FeedSource> {
    val prefs = context.getSharedPreferences("rss_sources", Context.MODE_PRIVATE)
    val json = prefs.getString("sources", null)
    return if (json != null) {
        runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(FeedSource(obj.getString("name"), obj.getString("url")))
                }
            }
        }.getOrDefault(defaultFeedSources)
    } else {
        defaultFeedSources
    }
}

private val defaultFeedSources = listOf(
    FeedSource("AAP FactCheck", "https://www.aap.com.au/feed/"),
    FeedSource("Associated Press", "https://feeds.ap.org/api/v2/feed/topics/headline.rss?site=apnews"),
    FeedSource("Reuters", "https://www.reutersagency.com/feed/?feed-type=RSS&taxonomy=taxonomy&value=top-news"),
    FeedSource("Financial Times: Markets", "https://www.ft.com/markets?format=rss"),
    FeedSource("Financial Times: Comment", "https://www.ft.com/comment?format=rss"),
    FeedSource("The Guardian", "https://www.theguardian.com/world/rss"),
    FeedSource("New Scientist", "https://www.newscientist.com/feed/"),
    FeedSource("ArchDaily", "https://www.archdaily.com/rss.xml"),
    FeedSource("Designboom", "https://www.designboom.com/feed/"),
    FeedSource("Dezeen", "https://www.dezeen.com/feed/"),
    FeedSource("ABC News", "https://www.abc.net.au/news/feed/51120/rss.xml"),
    FeedSource("Scientific American", "https://www.scientificamerican.com/feed/"),
    FeedSource("Nature", "https://www.nature.com/nature.rss"),
    FeedSource("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
    FeedSource("TIME", "https://time.com/feed/"),
    FeedSource("WSJ: World News", "https://feeds.a.dj.com/rss/RSSWorldNews.xml")
)

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val sharedPreferences =
        appContext.getSharedPreferences("ambient_dashboard_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _feedItems = MutableStateFlow<List<RssFeedItem>>(emptyList())
    val feedItems: StateFlow<List<RssFeedItem>> = _feedItems.asStateFlow()

    // Seed from SharedPreferences so the first refreshFeeds() on init uses real sources.
    private var currentSources: List<Pair<String, String>> = run {
        val prefs = appContext.getSharedPreferences("rss_sources", Context.MODE_PRIVATE)
        val json = prefs.getString("sources", null)
        if (json != null) {
            runCatching {
                val array = org.json.JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(obj.getString("name") to obj.getString("url"))
                    }
                }
            }.getOrNull()
        } else null
    } ?: listOf(
        "Financial Times: Markets" to "https://www.ft.com/markets?format=rss",
        "Financial Times: Comment" to "https://www.ft.com/comment?format=rss",
        "ABC News" to "https://www.abc.net.au/news/feed/51120/rss.xml"
    )

    fun setRssSources(sources: List<Pair<String, String>>) {
        if (sources == currentSources) return
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

    companion object {
        // Measured discharge duration for this device: 29 hours 35 minutes.
        private const val TOTAL_DISCHARGE_HOURS = 29.583
        private const val PREFS_BATTERY = "battery_prefs"
        private const val KEY_RESET_PCT = "reset_pct"
    }

    init {
        loadCachedFeeds()
        loadCachedWeather()
        refreshFeeds()
        loadInstalledApps()
        initBatteryMonitoring()
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
        batteryReceiver?.let { appContext.unregisterReceiver(it) }
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
            
            _installedApps.value = apps.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
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
            val fetchedItems = currentSources.mapNotNull { (name, url) -> 
                fetchLeadItem(FeedSource(name, url))
            }.sortedByDescending { it.publishedAtEpochMillis }

            if (fetchedItems.isNotEmpty()) {
                val limitedItems = fetchedItems.take(3) // Show only 2-3 long reads
                saveCachedFeeds(limitedItems)
                _feedItems.value = limitedItems
            }
        }
    }

    fun addFeedSource(name: String, url: String) {
        val prefs = appContext.getSharedPreferences("rss_sources", Context.MODE_PRIVATE)
        val sources = getFeedSources(appContext).toMutableList()
        sources.add(FeedSource(name, url))
        
        val array = JSONArray()
        sources.forEach { 
            array.put(JSONObject().put("name", it.name).put("url", it.url))
        }
        prefs.edit().putString("sources", array.toString()).apply()
        refreshFeeds()
    }

    private fun fetchLeadItem(source: FeedSource): RssFeedItem? {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "AmbientLauncher/1.0")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return null
                parseLeadItem(xml = body, source = source.name)
            }
        }.getOrNull()
    }

    private fun parseLeadItem(xml: String, source: String): RssFeedItem? {
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
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            inItem = true
                        } else if (inItem) {
                            when {
                                name.equals("title", ignoreCase = true) -> title = parser.nextText()
                                name.equals("link", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    link = href ?: parser.nextText()
                                }
                                name.equals("pubDate", ignoreCase = true) || name.equals("published", ignoreCase = true) || name.equals("updated", ignoreCase = true) -> pubDate = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            if (title.isNotBlank() && link.isNotBlank()) {
                                return RssFeedItem(
                                    title = title.trim(),
                                    source = source,
                                    timestamp = parseTimeAgo(pubDate),
                                    url = link.trim(),
                                    publishedAtEpochMillis = parseEpochMillis(pubDate)
                                )
                            }
                            inItem = false
                            title = ""
                            link = ""
                            pubDate = ""
                        }
                    }
                }
                eventType = parser.next()
            }
            null
        }.getOrNull()
    }

    private fun parseEpochMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return runCatching {
            ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.recoverCatching {
            OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
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

    private fun fetchWeather(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): WeatherUiState? {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=$latitude")
            append("&longitude=$longitude")
            append("¤t=temperature_2m,weather_code")
            append("&daily=temperature_2m_max,temperature_2m_min")
            append("&forecast_days=1")
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
                
                val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt()
                val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt()

                WeatherUiState(
                    temperatureText = "$temp°",
                    rangeText = "H:$maxTemp° L:$minTemp°",
                    summary = weatherCodeToSummary(code),
                    locationName = locationName,
                    isAvailable = true
                )
            }
        }.getOrNull()
    }

    private fun loadCachedWeather() {
        val json = sharedPreferences.getString("weather_v1", null).orEmpty()
        if (json.isBlank()) return

        runCatching {
            val obj = JSONObject(json)
            _weather.value = WeatherUiState(
                temperatureText = obj.getString("temperatureText"),
                rangeText = obj.getString("rangeText"),
                summary = obj.getString("summary"),
                locationName = obj.getString("locationName"),
                isAvailable = obj.getBoolean("isAvailable")
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
        }.toString()
        sharedPreferences.edit().putString("weather_v1", json).apply()
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
