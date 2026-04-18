package com.ambient.launcher.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import androidx.compose.ui.graphics.Color
import com.ambient.launcher.defaultFeedSources
import com.ambient.launcher.ui.theme.AmbientPalette
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.json.JSONArray

internal object PairSerializer : KSerializer<Pair<String, String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Pair", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Pair<String, String>) {
        encoder.encodeString("${value.first}|${value.second}")
    }
    override fun deserialize(decoder: Decoder): Pair<String, String> {
        val s = decoder.decodeString()
        val parts = s.split("|")
        return parts[0] to parts[1]
    }
}

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
@Serializable
internal enum class TileSize { SMALL, REGULAR, WIDE }

// ── Buckets ──────────────────────────────────────────────────────────────────

@Serializable
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
        LauncherBucket.TOOLS, LauncherBucket.SMART_HOME, LauncherBucket.HARDWARE, LauncherBucket.TRANSPORT_DELIVERY, LauncherBucket.DEV -> palette.clusterUtility
        else -> palette.textSecondary
    }
}

// ── Configuration model ──────────────────────────────────────────────────────

@Serializable
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
    val rssSources: List<@Serializable(with = PairSerializer::class) Pair<String, String>> = defaultFeedSources.map { it.name to it.url }
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

    /**
     * Merges this configuration with non-null overrides. Used during seeding to
     * preserve user settings while updating assignments.
     */
    fun mergeWith(
        hidden: Set<LauncherBucket>?,
        collapsed: Set<LauncherBucket>?,
        order: List<LauncherBucket>?,
        titles: Map<LauncherBucket, String>?,
        home: Set<String>?,
        sizes: Map<String, TileSize>?,
        rss: List<Pair<String, String>>?
    ): LauncherConfiguration = copy(
        hiddenBuckets = hidden ?: hiddenBuckets,
        collapsedBuckets = collapsed ?: collapsedBuckets,
        bucketOrder = order ?: bucketOrder,
        customTitles = titles ?: customTitles,
        homePackages = home ?: homePackages,
        tileSizes = sizes ?: tileSizes,
        rssSources = rss ?: rssSources
    )
}

// ── Composable state holder ──────────────────────────────────────────────────

@OptIn(kotlinx.coroutines.FlowPreview::class)
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
            snapshotFlow { configuration }
                .debounce(1000L)
                .collectLatest { config ->
                    withContext(Dispatchers.IO) {
                        LauncherConfigStore.save(context, config)
                    }
                }
        }
    }

    return configuration to { configuration = it }
}

// ── Persistence ──────────────────────────────────────────────────────────────

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_config")

internal object LauncherConfigStore {
    private val CONFIG_KEY = stringPreferencesKey("launcher_config_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun load(context: Context, installedApps: List<AppInfo>): LauncherConfiguration {
        val prefs = context.launcherDataStore.data.first()
        val serialized = prefs[CONFIG_KEY]

        val config = if (serialized == null) {
            seedLauncherConfiguration(installedApps)
        } else {
            runCatching {
                json.decodeFromString<LauncherConfiguration>(serialized)
            }.getOrElse { seedLauncherConfiguration(installedApps) }
        }

        return sanitize(config, installedApps)
    }

    suspend fun save(context: Context, configuration: LauncherConfiguration) {
        context.launcherDataStore.edit { prefs ->
            prefs[CONFIG_KEY] = json.encodeToString(configuration)
        }
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
}

// ── Seeding ──────────────────────────────────────────────────────────────────

private val BUCKET_KEYWORDS = mapOf(
    LauncherBucket.SYSTEM_BLOAT to listOf(
        "adblock", "abp.samsung", "blockplus.samsung", "betafish", "adblockplus",
        "edge.touch", "samsung.android.app.edgetouch", "samsung.android.sidegesturepad",
        "reading.mode", "com.google.android.apps.accessibility.reader",
        "good.lock", "samsung.goodlock", "homestar", "nice.catch", "nicecatch",
        "camera.assistant", "samsung.android.app.cameraassistant",
        "samsung.android.app.settings", "com.android.settings",
        "com.google.android.apps.docs", "android.google.android.apps.docs",
        "com.samsung.android.app.goodcatch", "com.samsung.android.app.homestar",
        "theme.park", "theme", "samsung.android.themestore", "samsung.android.themecenter",
        "smart.switch", "smartswitch", "sec.android.easyMover",
        "sound.assistant", "samsung.android.soundassistant", "samsung.android.app.tips",
        "samsung.android.app.updatecenter", "samsung.android.mobileservice",
        "sec.android.app.samsungapps", "sec.android.diagmonagent",
        "samsung.android.app.dressroom", "samsung.android.service.aircommand",
        "samsung.android.bixby.agent", "samsung.android.game.gamehome",
        "samsung.android.app.smartcapture", "samsung.android.kids",
        "samsung.android.app.spage", "samsung.android.app.contacts",
        "samsung.android.calendar", "samsung.android.video",
        "samsung.android.app.sbrowser.beta", "googlequicksearchbox", "tachyon"
    ),
    LauncherBucket.NEWS to listOf(
        "news", "ft.news", "com.ft", "financial.times", "guardian", "reuters", "ap.news", "apnews",
        "ground.news", "inoreader", "bbc", "economist", "times", "nytimes", "cnn", "abc.news",
        "substack", "medium", "daily.maverick", "elpais", "straitstimes", "timesofindia",
        "nikkei", "scmp", "allafrica", "mercopress", "france24", "dw.com", "nhk", "cbc.ca",
        "twitter", "com.x.android", "x.android"
    ),
    LauncherBucket.SECURITY to listOf(
        "commbank", "netbank", "ingdirect", "ing.direct", "ing banking", "bankwest", "westpac",
        "nab.", ".anz.", "up.bank", "revolut", "wise", "amex", "american.express",
        "authenticator", "bitwarden", "1password", "aegis", "password", ".pass.", "passkey",
        "duo mobile", "proton pass", "yubico", "myid", "my.id", "mygov", "service.nsw",
        "serviceapp", "nsw.gov"
    ),
    LauncherBucket.SOCIAL to listOf(
        "whatsapp", ".orca", "facebook", "telegram", "signal", "discord", "slack",
        "instagram", "threads", "google.android.apps.photos", "photos", "lightroom"
    ),
    LauncherBucket.ENTERTAINMENT to listOf(
        "spotify", "music", "youtube", "netflix", "disney", "prime.video", "hulu", "hbo",
        "twitch", "audible", "overdrive", "libby", "pocketcasts", "podcast", "ted", "vimeo"
    ),
    LauncherBucket.AI to listOf(
        "chatgpt", "openai", "claude", "anthropic", "gemini", "bard", "perplexity", "copilot",
        "grok", "deepseek", "kimi", "qwen", "manus", "aistudio", "poe", "consultant",
        "com.ambient.launcher.consultant"
    ),
    LauncherBucket.BROWSERS to listOf(
        "com.android.chrome", "org.chromium.chrome", "com.brave.browser", "com.vivaldi.browser",
        "org.mozilla.firefox", "org.mozilla.focus", "com.microsoft.emmx",
        "com.sec.android.app.sbrowser", "com.opera.browser", "com.opera.mini.native", "browser"
    ),
    LauncherBucket.HEALTH to listOf(
        "health", "fitness", "runna", "nike.run", "strava", "training", "cyclers", "cycling",
        "medad", "medadvisor", "myfitnesspal", "medicare", "medicar", "connectmobile", "swimup"
    ),
    LauncherBucket.UTILITIES to listOf(
        "dialer", "messaging", "android.mms", "gmail", "google.android.gm",
        "samsung.android.app.notes", "samsung.android.app.calendar", "calendar"
    ),
    LauncherBucket.TOOLS to listOf(
        "files", "myfiles", "calculator", "calc", "notes", "keep", "playstore", "play.store",
        "vending", "recorder", "voice.recorder", "translate", "translation", "clock", "skymee",
        "my.boost", "myboost", "amazon.shopping"
    ),
    LauncherBucket.SMART_HOME to listOf(
        "tapo", "tp.link", "xiaomi home", "mi home", "xiaomi.smarthome", "mi.global.smarthome",
        "xiaomi.miiotapp"
    ),
    LauncherBucket.HARDWARE to listOf(
        "accubattery", "accu.battery", "adguard", "com.adguard", "bettercam", "camera.assistant",
        "devcheck", "dev.check", "display.assistant", "displayassistant", "one.hand.operation",
        "onehanded", "samsung.android.bixby.voiceinput"
    ),
    LauncherBucket.TRANSPORT_DELIVERY to listOf(
        "waze", "didi", "uber", "doordash", "deliveroo", "opal.travel", "opal", "didi.chuxing",
        "google.android.apps.maps", "tripview", "maps"
    ),
    LauncherBucket.DEV to listOf(
        "google.android.apps.cloudconsole", "github", "tracker", "weighttracker",
        "weight.tracker", "com.checkitt"
    ),
    LauncherBucket.WALLET to listOf(
        "wallet", "google.android.apps.walletnfcrel", "samsung.android.spay"
    ),
    LauncherBucket.DATING to listOf(
        "tinder", "bumble", "hinge", "feeld", "coffee.meets.bagel", "cmb.", "dating",
        "coffeemeetsbagel"
    )
)

/**
 * Assigns installed apps to buckets and sets home visibility + tile sizes using
 * [AppValueScorer]. Called on first boot or "Re-score" trigger.
 */
private fun seedLauncherConfiguration(installedApps: List<AppInfo>): LauncherConfiguration {
    val assignments = LauncherBucket.entries.associateWith { mutableListOf<String>() }.toMutableMap()

    installedApps.forEach { app ->
        val id = app.searchId
        val bucket = BUCKET_KEYWORDS.entries.find { (_, keywords) ->
            keywords.any { id.contains(it) }
        }?.key ?: LauncherBucket.MISC
        
        assignments.getValue(bucket).add(app.packageName)
    }

    val normalizedAssignments = assignments.mapValues { it.value.distinct() }

    val homePackages = installedApps.filter { AppValueScorer.shouldShowOnHome(it) }
        .map { it.packageName }.toSet()

    val tileSizes = installedApps.mapNotNull { app ->
        val size = AppValueScorer.defaultTileSize(app)
        if (size != TileSize.REGULAR) app.packageName to size else null
    }.toMap()

    return LauncherConfiguration(
        assignments = normalizedAssignments,
        homePackages = homePackages,
        tileSizes = tileSizes
    )
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
