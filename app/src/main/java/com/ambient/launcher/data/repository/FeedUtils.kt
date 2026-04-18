package com.ambient.launcher.data.repository

import com.ambient.launcher.RssFeedItem

// ---------------------------------------------------------------------------
// Feed quality filters — pure functions, no network, no LLM
// ---------------------------------------------------------------------------

internal object FeedFilter {
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

    // Formulaic clickbait constructions - Hoisted to private val to avoid per-call Regex compilation
    private val CLICKBAIT_PATTERNS = listOf(
        Regex("""^\d{1,2} (things|ways|reasons|tips|signs|facts)""", RegexOption.IGNORE_CASE),
        Regex("""^here'?s (why|what|how|everything)""", RegexOption.IGNORE_CASE),
        Regex("""you (won'?t believe|need to know|should know)""", RegexOption.IGNORE_CASE),
        Regex("""everything you need to know""", RegexOption.IGNORE_CASE),
        Regex("""^(watch|photos|video|gallery):""", RegexOption.IGNORE_CASE)
    )

    private val TITLE_CLEAN_REGEX = Regex("[^a-z0-9 ]")

    fun shouldInclude(item: RssFeedItem): Boolean {
        val lower = item.title.lowercase()
        return SPORTS_TERMS.none { lower.contains(it) }
            && OPINION_PREFIXES.none { lower.startsWith(it) }
            && CLICKBAIT_PATTERNS.none { it.containsMatchIn(item.title) }
    }

    fun fingerprint(title: String, stopWords: Set<String>): Set<String> =
        title.lowercase()
            .replace(TITLE_CLEAN_REGEX, " ")
            .split(" ")
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
}

// ---------------------------------------------------------------------------
// Cross-source deduplication via Jaccard title similarity — no LLM required
// ---------------------------------------------------------------------------

internal object FeedDeduplicator {
    private val STOP_WORDS = setOf(
        "the", "a", "an", "in", "on", "at", "to", "of", "for", "with",
        "is", "are", "was", "were", "be", "been", "has", "have", "had",
        "will", "would", "could", "should", "that", "this", "it", "its",
        "and", "or", "but", "not", "as", "by", "from", "after", "over",
        "new", "says", "said", "up", "down", "out", "than", "more", "than"
    )

    private fun jaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        return a.intersect(b).size.toFloat() / (a + b).size
    }

    // Input should already be sorted newest-first; the first item in each cluster wins.
    fun deduplicate(items: List<RssFeedItem>): List<RssFeedItem> {
        val clusters = mutableListOf<Pair<RssFeedItem, Set<String>>>()
        for (item in items) {
            val fp = FeedFilter.fingerprint(item.title, STOP_WORDS)
            val isDuplicate = clusters.any { (_, clusterFp) -> jaccard(fp, clusterFp) >= 0.40f }
            if (!isDuplicate) clusters.add(item to fp)
        }
        return clusters.map { it.first }
    }
}
