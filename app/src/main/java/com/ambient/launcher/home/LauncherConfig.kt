package com.ambient.launcher.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import com.ambient.launcher.defaultFeedSources
import com.ambient.launcher.ui.theme.AmbientPalette
import org.json.JSONArray
import org.json.JSONObject

// ── Tile size ────────────────────────────────────────────────────────────────

/**
 * Controls how wide an app tile is on the bento home grid.
 *
 *   SMALL   → 1 col span (0.5×0.5 of regular) — always grouped 2×2, never mixed with REGULAR
 *   REGULAR → 2 col span, square (1×1 baseline)
 *   WIDE    → 4 col span, landscape (2×1, same height as REGULAR)
 *
 * Default is set by [AppValueScorer.defaultTileSize] at seeding time.
 * Users can override via long-press → size picker.
 */
internal enum class TileSize { SMALL, REGULAR, WIDE }

// ── Buckets ──────────────────────────────────────────────────────────────────

internal enum class LauncherBucket(
    val key: String,
    val title: String,
    val hint: String
) {
    NEWS("news", "News", "Readers, feeds, newspapers"),
    BROWSERS("browsers", "Browsers", "One browser, one square"),
    AI("ai", "AI", "Chat and assistant apps"),
    SOCIAL("social", "Social", "Messaging, photos, social surfaces"),
    HEALTH("health", "Health", "Health, fitness, and medical apps"),
    SECURITY("security", "Banking & Security", "Banking, authenticators, identity apps"),
    WALLET("wallet", "Wallet", "Wallets and payment apps"),
    UTILITIES("utilities", "Utilities", "Phone, messages, navigation, productivity"),
    TOOLS("tools", "Tools", "Calculator, files, notes, quick productivity"),
    SMART_HOME("smart_home", "Smart Home", "IoT and device control"),
    HARDWARE("hardware", "Hardware", "Device utilities and performance tools"),
    TRANSPORT_DELIVERY("transport", "Transport & Delivery", "Navigation, rideshare, food delivery"),
    DATING("dating", "Dating", "Dating and social discovery apps"),
    ENTERTAINMENT("entertainment", "Entertainment", "Music, video, and streaming apps"),
    DEV("dev", "Dev", "GitHub, Cloud, and tracking tools"),
    SYSTEM_BLOAT("bloat", "System & Bloat", "OS clutter and rarely-opened apps"),
    MISC("misc", "Misc", "Unknown purpose or single-use apps");

    companion object {
        val indexBuckets = listOf(
            NEWS, SECURITY, SOCIAL, UTILITIES, AI, BROWSERS, HEALTH,
            ENTERTAINMENT, TOOLS, WALLET, TRANSPORT_DELIVERY, HARDWARE, DATING,
            DEV, SMART_HOME
        )
        /** Buckets that render as tile grids on home. WALLET is excluded — it's an action card. */
        val homeBuckets = indexBuckets.filterNot { it == WALLET }
    }
}

internal fun LauncherBucket.themeColor(palette: AmbientPalette): Color {
    return when (this) {
        LauncherBucket.NEWS, LauncherBucket.BROWSERS -> palette.clusterIntelligence
        LauncherBucket.AI -> palette.clusterAssistant
        LauncherBucket.SOCIAL, LauncherBucket.DATING -> palette.clusterCommunication
        LauncherBucket.ENTERTAINMENT -> palette.clusterCommunication
        LauncherBucket.HEALTH -> palette.clusterHealth
        LauncherBucket.SECURITY, LauncherBucket.WALLET, LauncherBucket.UTILITIES -> palette.clusterUtility
        LauncherBucket.TOOLS, LauncherBucket.SMART_HOME, LauncherBucket.HARDWARE, LauncherBucket.TRANSPORT_DELIVERY, LauncherBucket.DEV -> palette.clusterAction
        else -> palette.textSecondary
    }
}

// ── Configuration model ──────────────────────────────────────────────────────

internal data class LauncherConfiguration(
    val assignments: Map<LauncherBucket, List<String>>,
    val hiddenBuckets: Set<LauncherBucket> = setOf(LauncherBucket.SYSTEM_BLOAT),
    val collapsedBuckets: Set<LauncherBucket> = setOf(
        LauncherBucket.TRANSPORT_DELIVERY,
        LauncherBucket.DATING,
        LauncherBucket.SYSTEM_BLOAT,
        LauncherBucket.MISC,
        LauncherBucket.HARDWARE
    ),
    val bucketOrder: List<LauncherBucket> = LauncherBucket.indexBuckets,
    val customTitles: Map<LauncherBucket, String> = emptyMap(),
    val homePackages: Set<String> = emptySet(),
    val tileSizes: Map<String, TileSize> = emptyMap(),
    val rssSources: List<Pair<String, String>> = defaultFeedSources.map { it.name to it.url }
) {
    fun packagesFor(bucket: LauncherBucket): List<String> = assignments[bucket].orEmpty()

    fun displayTitle(bucket: LauncherBucket): String = customTitles[bucket].orEmpty().ifBlank { bucket.title }

    fun isSelected(bucket: LauncherBucket, packageName: String): Boolean =
        assignments[bucket].orEmpty().contains(packageName)

    fun assign(bucket: LauncherBucket, packageName: String): LauncherConfiguration {
        val next = assignments.toMutableMap()
            .withDefault { emptyList() }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()

        LauncherBucket.entries.forEach { entry ->
            next[entry] = next[entry].orEmpty().filterNot { it == packageName }.toMutableList()
        }

        val current = next[bucket].orEmpty().toMutableList()
        current += packageName
        next[bucket] = current.distinct().toMutableList()
        return copy(assignments = next.mapValues { it.value.toList() })
    }

    fun remove(bucket: LauncherBucket, packageName: String): LauncherConfiguration {
        val next = assignments.toMutableMap()
        next[bucket] = assignments[bucket].orEmpty().filterNot { it == packageName }
        return copy(assignments = next)
    }

    fun unassign(packageName: String): LauncherConfiguration {
        val nextAssignments = assignments.mapValues { (_, packages) ->
            packages.filterNot { it == packageName }
        }
        return copy(
            assignments = nextAssignments,
            homePackages = homePackages - packageName,
            tileSizes = tileSizes - packageName
        )
    }

    fun toggle(bucket: LauncherBucket, packageName: String): LauncherConfiguration {
        return if (isSelected(bucket, packageName)) remove(bucket, packageName) else assign(bucket, packageName)
    }

    fun toggleVisibility(bucket: LauncherBucket): LauncherConfiguration {
        val next = hiddenBuckets.toMutableSet()
        if (next.contains(bucket)) next.remove(bucket) else next.add(bucket)
        return copy(hiddenBuckets = next)
    }

    fun toggleCollapse(bucket: LauncherBucket): LauncherConfiguration {
        val next = collapsedBuckets.toMutableSet()
        if (next.contains(bucket)) next.remove(bucket) else next.add(bucket)
        return copy(collapsedBuckets = next)
    }

    fun isCollapsed(bucket: LauncherBucket): Boolean = bucket in collapsedBuckets

    fun moveBucketUp(bucket: LauncherBucket): LauncherConfiguration {
        val next = bucketOrder.toMutableList()
        val index = next.indexOf(bucket)
        if (index > 0) { next.removeAt(index); next.add(index - 1, bucket) }
        return copy(bucketOrder = next)
    }

    fun moveBucket(fromIndex: Int, toIndex: Int): LauncherConfiguration {
        if (fromIndex == toIndex || fromIndex !in bucketOrder.indices || toIndex !in bucketOrder.indices) return this
        val next = bucketOrder.toMutableList()
        val bucket = next.removeAt(fromIndex)
        next.add(toIndex, bucket)
        return copy(bucketOrder = next)
    }

    fun addBucket(bucket: LauncherBucket): LauncherConfiguration {
        if (bucket in bucketOrder) return this
        return copy(bucketOrder = bucketOrder + bucket, hiddenBuckets = hiddenBuckets - bucket)
    }

    fun removeBucket(bucket: LauncherBucket): LauncherConfiguration {
        if (bucket !in bucketOrder) return this
        val removedPackages = assignments[bucket].orEmpty().toSet()
        val nextAssignments = assignments.toMutableMap()
        nextAssignments[bucket] = emptyList()
        val nextTitles = customTitles.toMutableMap()
        nextTitles.remove(bucket)
        return copy(
            assignments = nextAssignments,
            hiddenBuckets = hiddenBuckets - bucket,
            bucketOrder = bucketOrder.filterNot { it == bucket },
            customTitles = nextTitles,
            homePackages = homePackages - removedPackages,
            tileSizes = tileSizes - removedPackages
        )
    }

    fun renameBucket(bucket: LauncherBucket, title: String): LauncherConfiguration {
        val normalized = title.trim()
        val next = customTitles.toMutableMap()
        if (normalized.isBlank() || normalized == bucket.title) {
            next.remove(bucket)
        } else {
            next[bucket] = normalized
        }
        return copy(customTitles = next)
    }

    fun isOnHome(packageName: String): Boolean = homePackages.contains(packageName)

    fun setHomeVisibility(packageName: String, visible: Boolean): LauncherConfiguration {
        val next = homePackages.toMutableSet()
        if (visible) next.add(packageName) else next.remove(packageName)
        return copy(homePackages = next)
    }

    /** Returns the persisted or scorer-derived tile size for this package. */
    fun tileSize(packageName: String): TileSize = tileSizes[packageName] ?: TileSize.REGULAR

    fun setTileSize(packageName: String, size: TileSize): LauncherConfiguration =
        copy(tileSizes = tileSizes + (packageName to size))

    fun setRssSources(sources: List<Pair<String, String>>): LauncherConfiguration =
        copy(rssSources = sources)

    fun isAssigned(packageName: String): Boolean =
        LauncherBucket.entries.any { isSelected(it, packageName) }

    fun visiblePackages(): Set<String> =
        LauncherBucket.homeBuckets.flatMap { packagesFor(it) }.toSet()
}

// ── Composable state holder ──────────────────────────────────────────────────

@Composable
internal fun rememberLauncherConfiguration(
    installedApps: List<AppInfo>
): Pair<LauncherConfiguration, (LauncherConfiguration) -> Unit> {
    val context = LocalContext.current
    var configuration by remember { mutableStateOf(LauncherConfiguration(emptyMap())) }
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(installedApps) {
        if (installedApps.isNotEmpty() && !hasLoaded) {
            configuration = withContext(Dispatchers.IO) {
                LauncherConfigStore.load(context, installedApps)
            }
            hasLoaded = true
        } else if (hasLoaded) {
            configuration = withContext(Dispatchers.IO) {
                LauncherConfigStore.sanitize(configuration, installedApps)
            }
        }
    }

    LaunchedEffect(configuration, hasLoaded) {
        if (hasLoaded) {
            LauncherConfigStore.save(context, configuration)
        }
    }

    return configuration to { configuration = it }
}

// ── Persistence ──────────────────────────────────────────────────────────────

internal object LauncherConfigStore {
    private const val preferencesName = "ambient_launcher_sections"
    private const val configKey = "launcher_configuration_v4" // Bumped: fixes Didi/X/aistudio placement + Xiaomi matcher
    private const val tileSizesKey = "tile_sizes_v1"

    fun load(context: Context, installedApps: List<AppInfo>): LauncherConfiguration {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        
        // If v1/v2 exists but v3 doesn't, we are upgrading. Flush the old assignments and order.
        if (!prefs.contains(configKey)) {
             prefs.edit()
                 .remove("launcher_configuration_v1")
                 .remove("launcher_configuration_v2")
                 .remove("bucket_order")
                 .apply()
        }

        val json = prefs.getString(configKey, null)
        val hiddenJson = prefs.getString("hidden_buckets", null)
        val collapsedJson = prefs.getString("collapsed_buckets", null)
        val orderJson = prefs.getString("bucket_order", null)
        val titlesJson = prefs.getString("bucket_titles", null)
        val homeJson = prefs.getString("home_packages", null)
        val tileSizesJson = prefs.getString(tileSizesKey, null)
        val rssSourcesJson = prefs.getString("rss_sources_v2", null)

        val rssSources = if (!rssSourcesJson.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(rssSourcesJson)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(obj.getString("name") to obj.getString("url"))
                    }
                }
            }.getOrNull()
        } else null

        val hiddenBuckets = hiddenJson?.parseSet { key ->
            LauncherBucket.entries.find { it.key == key }
        } ?: setOf(LauncherBucket.SYSTEM_BLOAT, LauncherBucket.MISC)

        val collapsedBuckets = collapsedJson?.parseSet { key ->
            LauncherBucket.entries.find { it.key == key }
        } ?: setOf(
            LauncherBucket.TRANSPORT_DELIVERY,
            LauncherBucket.DATING,
            LauncherBucket.SYSTEM_BLOAT,
            LauncherBucket.MISC,
            LauncherBucket.HARDWARE
        )

        val bucketOrder = if (!orderJson.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(orderJson)
                val list = mutableListOf<LauncherBucket>()
                for (i in 0 until array.length()) {
                    val key = array.getString(i)
                    // Explicitly skip bloat and misc from ever entering the order list
                    if (key == "bloat" || key == "misc") continue
                    LauncherBucket.entries.find { it.key == key }?.let(list::add)
                }
                (list + LauncherBucket.indexBuckets.filterNot(list::contains)).distinct()
            }.getOrDefault(LauncherBucket.indexBuckets)
        } else {
            LauncherBucket.indexBuckets
        }

        val customTitles = if (!titlesJson.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(titlesJson)
                LauncherBucket.entries.mapNotNull { bucket ->
                    root.optString(bucket.key).takeIf { it.isNotBlank() }?.let { bucket to it }
                }.toMap()
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

        val homePackages = homeJson?.parseStringSet() ?: emptySet()

        val tileSizes = if (!tileSizesJson.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(tileSizesJson)
                buildMap {
                    root.keys().forEach { pkg ->
                        runCatching { TileSize.valueOf(root.getString(pkg)) }.getOrNull()
                            ?.let { put(pkg, it) }
                    }
                }
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

        val parsed = if (json.isNullOrBlank()) {
            val seeded = seedLauncherConfiguration(installedApps)
            seeded.copy(
                hiddenBuckets = if (!hiddenJson.isNullOrBlank()) hiddenBuckets else seeded.hiddenBuckets,
                collapsedBuckets = if (!collapsedJson.isNullOrBlank()) collapsedBuckets else seeded.collapsedBuckets,
                bucketOrder = if (!orderJson.isNullOrBlank()) bucketOrder else seeded.bucketOrder,
                customTitles = if (!titlesJson.isNullOrBlank()) customTitles else seeded.customTitles,
                homePackages = if (!homeJson.isNullOrBlank()) homePackages else seeded.homePackages,
                tileSizes = if (!tileSizesJson.isNullOrBlank()) tileSizes else seeded.tileSizes,
                rssSources = rssSources ?: seeded.rssSources
            )
        } else {
            runCatching {
                val root = JSONObject(json)
                val assignments = LauncherBucket.entries.associateWith { bucket ->
                    root.optJSONArray(bucket.key)?.toStringList().orEmpty()
                }
                val config = LauncherConfiguration(
                    assignments, hiddenBuckets, collapsedBuckets, bucketOrder, customTitles, homePackages, tileSizes,
                    rssSources = rssSources ?: defaultFeedSources.map { it.name to it.url }
                )
                if (LauncherBucket.entries.all { config.packagesFor(it).isEmpty() }) {
                    reseedWith(installedApps, hiddenBuckets, hiddenJson, collapsedBuckets, collapsedJson, bucketOrder, orderJson, customTitles, titlesJson, homePackages, homeJson, tileSizes, tileSizesJson, rssSources)
                } else {
                    config
                }
            }.getOrElse {
                reseedWith(installedApps, hiddenBuckets, hiddenJson, collapsedBuckets, collapsedJson, bucketOrder, orderJson, customTitles, titlesJson, homePackages, homeJson, tileSizes, tileSizesJson, rssSources)
            }
        }

        return sanitize(parsed, installedApps)
    }

    private fun reseedWith(
        installedApps: List<AppInfo>,
        hiddenBuckets: Set<LauncherBucket>, hiddenJson: String?,
        collapsedBuckets: Set<LauncherBucket>, collapsedJson: String?,
        bucketOrder: List<LauncherBucket>, orderJson: String?,
        customTitles: Map<LauncherBucket, String>, titlesJson: String?,
        homePackages: Set<String>, homeJson: String?,
        tileSizes: Map<String, TileSize>, tileSizesJson: String?,
        rssSources: List<Pair<String, String>>?
    ): LauncherConfiguration {
        val seeded = seedLauncherConfiguration(installedApps)
        return seeded.copy(
            hiddenBuckets = if (!hiddenJson.isNullOrBlank()) hiddenBuckets else seeded.hiddenBuckets,
            collapsedBuckets = if (!collapsedJson.isNullOrBlank()) collapsedBuckets else seeded.collapsedBuckets,
            bucketOrder = if (!orderJson.isNullOrBlank()) bucketOrder else seeded.bucketOrder,
            customTitles = if (!titlesJson.isNullOrBlank()) customTitles else seeded.customTitles,
            homePackages = if (!homeJson.isNullOrBlank()) homePackages else seeded.homePackages,
            tileSizes = if (!tileSizesJson.isNullOrBlank()) tileSizes else seeded.tileSizes,
            rssSources = rssSources ?: defaultFeedSources.map { it.name to it.url }
        )
    }

    fun save(context: Context, configuration: LauncherConfiguration) {
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

        val root = JSONObject()
        LauncherBucket.entries.forEach { bucket ->
            root.put(bucket.key, JSONArray(configuration.packagesFor(bucket)))
        }

        val hiddenArray = JSONArray().also { arr ->
            configuration.hiddenBuckets.forEach { arr.put(it.key) }
        }
        val collapsedArray = JSONArray().also { arr ->
            configuration.collapsedBuckets.forEach { arr.put(it.key) }
        }
        val orderArray = JSONArray().also { arr ->
            configuration.bucketOrder.forEach { arr.put(it.key) }
        }
        val titlesRoot = JSONObject().also { obj ->
            configuration.customTitles.forEach { (bucket, title) -> obj.put(bucket.key, title) }
        }
        val homeArray = JSONArray().also { arr ->
            configuration.homePackages.forEach { arr.put(it) }
        }
        val tileSizesRoot = JSONObject().also { obj ->
            configuration.tileSizes.forEach { (pkg, size) -> obj.put(pkg, size.name) }
        }
        val rssSourcesArray = JSONArray().also { arr ->
            configuration.rssSources.forEach { (name, url) ->
                arr.put(JSONObject().put("name", name).put("url", url))
            }
        }

        prefs.edit()
            .putString(configKey, root.toString())
            .putString("hidden_buckets", hiddenArray.toString())
            .putString("collapsed_buckets", collapsedArray.toString())
            .putString("bucket_order", orderArray.toString())
            .putString("bucket_titles", titlesRoot.toString())
            .putString("home_packages", homeArray.toString())
            .putString(tileSizesKey, tileSizesRoot.toString())
            .putString("rss_sources_v2", rssSourcesArray.toString())
            .apply()
    }

    fun sanitize(configuration: LauncherConfiguration, installedApps: List<AppInfo>): LauncherConfiguration {
        val installedPackages = installedApps.map { it.packageName }.toSet()
        return configuration.copy(
            assignments = LauncherBucket.entries.associateWith { bucket ->
                configuration.packagesFor(bucket).filter { it in installedPackages }
            },
            customTitles = configuration.customTitles.filterKeys { it in LauncherBucket.entries },
            homePackages = configuration.homePackages.filterTo(mutableSetOf()) { it in installedPackages },
            tileSizes = configuration.tileSizes.filterKeys { it in installedPackages }
        )
    }

    // ── Parse helpers ────────────────────────────────────────────────────────

    private fun <T> String.parseSet(mapper: (String) -> T?): Set<T> =
        runCatching {
            val array = JSONArray(this)
            buildSet { for (i in 0 until array.length()) mapper(array.getString(i))?.let(::add) }
        }.getOrDefault(emptySet())

    private fun String.parseStringSet(): Set<String> =
        runCatching {
            val array = JSONArray(this)
            buildSet { for (i in 0 until array.length()) array.optString(i).takeIf(String::isNotBlank)?.let(::add) }
        }.getOrDefault(emptySet())
}

// ── Seeding ──────────────────────────────────────────────────────────────────

/**
 * Assigns installed apps to buckets and sets home visibility + tile sizes using
 * [AppValueScorer]. Called on first boot or "Re-score" trigger.
 */
private fun seedLauncherConfiguration(installedApps: List<AppInfo>): LauncherConfiguration {
    val assignments = LauncherBucket.entries.associateWith { mutableListOf<String>() }.toMutableMap()

    installedApps.forEach { app ->
        when {
            matchesSystemBloat(app)     -> assignments.getValue(LauncherBucket.SYSTEM_BLOAT).add(app.packageName)
            matchesNews(app)            -> assignments.getValue(LauncherBucket.NEWS).add(app.packageName)
            matchesSecurity(app)        -> assignments.getValue(LauncherBucket.SECURITY).add(app.packageName)
            matchesSocial(app)          -> assignments.getValue(LauncherBucket.SOCIAL).add(app.packageName)
            matchesEntertainment(app)   -> assignments.getValue(LauncherBucket.ENTERTAINMENT).add(app.packageName)
            matchesUtilities(app)       -> assignments.getValue(LauncherBucket.UTILITIES).add(app.packageName)
            matchesAi(app)              -> assignments.getValue(LauncherBucket.AI).add(app.packageName)
            matchesBrowsers(app)        -> assignments.getValue(LauncherBucket.BROWSERS).add(app.packageName)
            matchesDev(app)             -> assignments.getValue(LauncherBucket.DEV).add(app.packageName)
            matchesWallet(app)          -> assignments.getValue(LauncherBucket.WALLET).add(app.packageName)
            matchesHealth(app)          -> assignments.getValue(LauncherBucket.HEALTH).add(app.packageName)
            matchesDating(app)          -> assignments.getValue(LauncherBucket.DATING).add(app.packageName)
            matchesSmartHome(app)       -> assignments.getValue(LauncherBucket.SMART_HOME).add(app.packageName)
            matchesTransportDelivery(app) -> assignments.getValue(LauncherBucket.TRANSPORT_DELIVERY).add(app.packageName)
            matchesHardware(app)        -> assignments.getValue(LauncherBucket.HARDWARE).add(app.packageName)
            matchesTools(app)           -> assignments.getValue(LauncherBucket.TOOLS).add(app.packageName)
            else                        -> assignments.getValue(LauncherBucket.MISC).add(app.packageName)
        }
    }

    val normalizedAssignments = assignments.mapValues { it.value.distinct() }

    // Only score-≥6 apps appear on home; score-8+ apps default to WIDE.
    val homePackages = buildSet<String> {
        installedApps.filter { AppValueScorer.shouldShowOnHome(it) }
            .forEach { add(it.packageName) }
    }
    val tileSizes = buildMap<String, TileSize> {
        installedApps.forEach { app ->
            val size = AppValueScorer.defaultTileSize(app)
            if (size != TileSize.REGULAR) put(app.packageName, size)
        }
    }

    return LauncherConfiguration(
        assignments = normalizedAssignments,
        homePackages = homePackages,
        tileSizes = tileSizes
    )
}

// ── Matchers ─────────────────────────────────────────────────────────────────

private fun matchesBrowsers(app: AppInfo): Boolean {
    val id = app.searchId
    // Specific browsers only; avoid generic "browser" or "edge" which catch bloat/tools
    return listOf(
        "com.android.chrome", "org.chromium.chrome",
        "com.brave.browser", "com.vivaldi.browser",
        "org.mozilla.firefox", "org.mozilla.focus",
        "com.microsoft.emmx", // Microsoft Edge
        "com.sec.android.app.sbrowser", // Samsung Internet
        "com.opera.browser", "com.opera.mini.native"
    ).any(id::contains) || (id.contains("browser") && !id.contains("adblock"))
}

private fun matchesAi(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "chatgpt", "openai", "claude", "anthropic", "gemini", "bard",
        "perplexity", "copilot", "grok", "deepseek", "kimi", "qwen",
        "manus", "aistudio", "poe", "consultant",
        "com.ambient.launcher.consultant"  // Explicit match for Consultant app
    ).any(id::contains)
}

/**
 * Banking apps (CommBank, ING) live in Security alongside authenticators and identity apps,
 * per the bucket layout in REQUIREMENTS.md.
 */
private fun matchesSecurity(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "commbank", "netbank",
        "ingdirect", "ing.direct", "ing banking",
        "bankwest", "westpac", "nab.", ".anz.", "up.bank",
        "revolut", "wise", "amex", "american.express",
        "authenticator", "bitwarden", "1password", "aegis",
        "password", ".pass.", "passkey",
        "duo mobile", "proton pass", "yubico",
        "myid", "my.id", "mygov", "service.nsw", "serviceapp", "nsw.gov"
    ).any(id::contains)
}

/** Wallet = Google Wallet, Samsung Pay, and similar tap-to-pay apps (not banking apps). */
private fun matchesWallet(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "wallet", "google.android.apps.walletnfcrel",
        "samsung.android.spay"
    ).any(id::contains)
}

private fun matchesHealth(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "health", "fitness", "runna", "nike.run", "strava", "training",
        "cyclers", "cycling", "medad", "medadvisor",
        "myfitnesspal", "medicare", "medicar",
        "garmin.android.apps.connectmobile",  // Garmin Connect
        "swimup"
    ).any(id::contains)
}

/**
 * Utilities covers: Phone, Messages, Gmail, Maps, Calendar, and similar
 * core communication/navigation/productivity tools (Samsung versions preferred).
 */
private fun matchesUtilities(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "samsung.android.dialer", "dialer",
        "samsung.android.messaging", "android.mms",
        "gmail", "google.android.gm",
        "samsung.android.app.notes",
        "samsung.android.app.calendar", "calendar"
    ).any(id::contains)
}

/** Social covers messaging, photos, and social surfaces. */
private fun matchesSocial(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "whatsapp", ".orca", "facebook",             // Messenger / Facebook
        "telegram", "signal", "discord", "slack",
        "instagram", "threads",
        "google.android.apps.photos", "photos", "lightroom"
    ).any(id::contains)
}

private fun matchesEntertainment(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "spotify", "music", "youtube", "netflix", "disney", "prime.video",
        "hulu", "hbo", "twitch", "audible", "overdrive", "libby", "pocketcasts",
        "podcast", "ted", "vimeo"
    ).any(id::contains)
}

private fun matchesNews(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "news", "ft.news", "com.ft", "financial.times",
        "guardian", "reuters", "ap.news", "apnews",
        "ground.news", "inoreader", "bbc", "economist", "times",
        "nytimes", "cnn", "abc.news", "substack", "medium",
        "daily.maverick", "elpais", "straitstimes", "timesofindia",
        "nikkei", "scmp", "allafrica", "mercopress", "france24",
        "dw.com", "nhk", "cbc.ca",
        "twitter", "com.x.android", "x.android"
    ).any(id::contains)
}

private fun matchesDating(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "tinder", "bumble", "hinge", "feeld", "coffee.meets.bagel",
        "cmb.", "dating", "coffeemeetsbagel"
    ).any(id::contains)
}

private fun matchesDev(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "google.android.apps.cloudconsole", "github",
        "tracker", "weighttracker", "weight.tracker", "com.checkitt"
    ).any(id::contains)
    // NOTE: bettercam intentionally removed — it belongs in Hardware, not Dev
}

private fun matchesSmartHome(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        // TP-Link / Tapo
        "tapo", "tp.link",
        // Xiaomi Home — label match ("xiaomi home") and package matches
        // Package is typically com.xiaomi.smarthome or com.mi.global.smarthome
        "xiaomi home", "mi home",
        "xiaomi.smarthome", "mi.global.smarthome", "xiaomi.miiotapp"
    ).any(id::contains)
}

private fun matchesTransportDelivery(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "waze", "didi", "uber", "doordash", "deliveroo",
        "opal.travel", "opal", "didi.chuxing",
        "google.android.apps.maps", "tripview", "maps"
    ).any(id::contains)
}

private fun matchesHardware(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "accubattery", "accu.battery",
        "adguard", "com.adguard",
        "bettercam", "camera.assistant",
        "devcheck", "dev.check",
        "display.assistant", "displayassistant",
        "one.hand.operation", "onehanded",
        "samsung.android.bixby.voiceinput"
    ).any(id::contains)
}

private fun matchesTools(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        "files", "myfiles",
        "calculator", "calc",
        "notes", "keep",
        "playstore", "play.store", "vending",
        "recorder", "voice.recorder",
        "translate", "translation",
        "clock", "skymee", "my.boost", "myboost", "amazon.shopping"
    ).any(id::contains)
}

private fun matchesSystemBloat(app: AppInfo): Boolean {
    val id = app.searchId
    return listOf(
        // Adblockers and "Browsers" that aren't primary
        "adblock", "abp.samsung", "blockplus.samsung", "betafish", "adblockplus",
        // System utilities that should be hidden
        "edge.touch", "samsung.android.app.edgetouch", "samsung.android.sidegesturepad",
        "one.hand.operation", "onehanded", "samsung.android.displayassistant",
        "reading.mode", "com.google.android.apps.accessibility.reader",
        "good.lock", "samsung.goodlock", "homestar", "nice.catch", "nicecatch",
        "camera.assistant", "samsung.android.app.cameraassistant",
        "samsung.android.app.settings", "com.android.settings",
        "com.google.android.apps.docs", "android.google.android.apps.docs", // Drive
        "com.samsung.android.app.goodcatch",
        "com.samsung.android.app.homestar",
        // Samsung customization & duplicates
        "theme.park", "theme", "samsung.android.themestore", "samsung.android.themecenter",
        "smart.switch", "smartswitch", "sec.android.easyMover",
        "sound.assistant", "samsung.android.soundassistant",
        "samsung.android.app.tips",
        "samsung.android.app.updatecenter",
        "samsung.android.mobileservice",
        "sec.android.app.samsungapps",
        "sec.android.diagmonagent",
        "samsung.android.app.dressroom",
        "samsung.android.service.aircommand",
        "samsung.android.bixby.agent",
        "samsung.android.game.gamehome",
        "samsung.android.app.smartcapture",
        "samsung.android.kids",
        "samsung.android.app.spage",
        "samsung.android.app.contacts",
        "samsung.android.calendar",
        "samsung.android.video",
        "samsung.android.app.sbrowser.beta",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.tachyon" // Meet
    ).any(id::contains)
}

// ── Extensions ───────────────────────────────────────────────────────────────

private fun JSONArray.toStringList(): List<String> =
    buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }

private val AppInfo.searchId: String
    get() = "${label.lowercase()} ${packageName.lowercase()}"
