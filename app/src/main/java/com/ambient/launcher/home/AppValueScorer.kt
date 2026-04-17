package com.ambient.launcher.home

/**
 * Scores installed apps for automatic home placement and default tile sizing.
 *
 * Score = REACH × FREQUENCY (each 1–3). Threshold for home = 6.
 *
 *   REACH:     3 = Reflex (opened without thinking — phone, banking)
 *              2 = Browse (opened with intent — browser, AI, notes)
 *              1 = Lookup (opened to reference something)
 *
 *   FREQUENCY: 3 = Daily   2 = Weekly   1 = Rarely
 *
 * Score bands:
 *   9  → always home  (Reflex × Daily)
 *   8  → high value   (Browse × Daily, intentional creation/reading)
 *   6  → medium       (Browse × Daily, utility)
 *   4  → index only   (Lookup or Browse × Weekly)
 *   2  → hidden/skip  (system, unknown)
 */
internal object AppValueScorer {

    fun score(app: AppInfo): Int {
        val id = app.searchId
        return when {
            isAlwaysHome(id)  -> 9
            isHighValue(id)   -> 8
            isMediumValue(id) -> 6
            isLowValue(id)    -> 4
            else              -> 2
        }
    }

    fun shouldShowOnHome(app: AppInfo): Boolean = score(app) >= 6

    /**
     * Gmail, Notes, FT, X default to WIDE. All others default to REGULAR.
     * Users can override via long-press → size picker.
     */
    fun defaultTileSize(app: AppInfo): TileSize =
        if (isWideTile(app.searchId)) TileSize.WIDE else TileSize.REGULAR

    // ── Score bands ──────────────────────────────────────────────────────────

    /** Reflex × Daily = 9. Phone, primary messaging, banking. */
    private fun isAlwaysHome(id: String) = listOf(
        "dialer", "samsung.android.dialer",
        "whatsapp", ".orca",                           // Messenger
        "samsung.android.messaging", "android.mms",
        "commbank", "netbank",
        "ingdirect", "ing.direct",
        "bankwest", "up.bank", "westpac", "nab.", ".anz."
    ).any(id::contains)

    /** Browse × Daily (intentional creation / reading) = 8. */
    private fun isHighValue(id: String) = listOf(
        "gmail", "google.android.gm",
        "notes", "samsung.android.app.notes", "google.android.keep",
        "com.ft", "ft.news", "financial times",
        "twitter", "com.x.android", "x.android"
    ).any(id::contains)

    /** Browse × Daily (utility, navigation, primary tools) = 6. */
    private fun isMediumValue(id: String) = listOf(
        "chrome", "brave.browser", "firefox", "vivaldi",
        "claude", "chatgpt", "openai.chat", "anthropic",
        "gemini", "google.android.apps.bard",
        "consultant", "deepseek", "aistudio", "google.android.apps.aistudio",
        "google.android.apps.maps", "waze", "tripview", "opal",
        "strava", "training", "nike.run", "cyclers",
        "google.android.apps.photos", "lightroom",
        "spotify", "amazon.shopping", "service.nsw"
    ).any(id::contains)

    /** Lookup / Browse × Weekly = 4 → index only. */
    private fun isLowValue(id: String) = listOf(
        "perplexity", "grok.x", "kimi", "manus", "qwen",
        "runna", "medad", "myfitnesspal", "medicar",
        "authenticator", "myid", "mygov",
        "doordash", "ubereats", "skymee"
    ).any(id::contains)

    // ── Wide-tile defaults ───────────────────────────────────────────────────

    /** Gmail, Notes, FT, X default to WIDE tiles. */
    private fun isWideTile(id: String) = listOf(
        "gmail", "google.android.gm",
        "notes", "samsung.android.app.notes", "google.android.keep",
        "com.ft", "ft.news", "financial times",
        "twitter", "com.x.android", "x.android"
    ).any(id::contains)

    private val AppInfo.searchId: String
        get() = "${label.lowercase()} ${packageName.lowercase()}"
}
