# Architecture

## Technology Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 primitives
- **Build:** Android Gradle Plugin `9.1.0`, Gradle wrapper `9.3.1`
- **Android target:** `compileSdk 36`, `targetSdk 36`
- **Persistence today:** `SharedPreferences` for launcher config, home visibility, tile sizes,
  RSS cache, weather cache, and battery reset point
- **Persistence planned:** `Room` for notes/feed/cache layers; `DataStore` for launcher config
- **Network today:** OkHttp for RSS and weather
- **Network planned:** Retrofit / OkHttp for richer AI and feed integrations

---

## Current App Structure

### 1. Activity Shell
- `MainActivity` is intentionally thin.
- Owns `DashboardViewModel` and `AgenticAIViewModel`.
- Hands control to the launcher UI package.

### 2. Home Package
- **`home/LauncherScreen.kt`**
  Orchestrates pager navigation, vertical home scrolling, section ordering, and home-vs-index
  app density. Collects `batteryState` from `DashboardViewModel` and passes it to `HomePage`.
  *Added:* Supports "Clipboard Drop-Zone" for high-friction workflows (screenshots to IDE).

- **`home/LauncherCards.kt`**
  Header, project-card row, calendar rail, app tile grid, action cards (Wallet, Battery),
  and note input. Tiles are architected as **status-indicators** (badge-aware) rather than mere static icons.

- **`home/AppDrawer.kt`**
  Full-screen library/editor overlay. Long-press on a home tile opens the edit dialog, which
  includes bucket assignment, home visibility toggle, and tile size picker (SMALL / REGULAR / WIDE).

- **`home/LauncherConfig.kt`**
  Bucket model, assignment persistence, home visibility, tile size per package, and default
  seeding heuristics. Persists a `Map<String, TileSize>` alongside existing assignment maps.

- **`home/LauncherLayout.kt`**
  Shared spacing, sizing, and edge-bleed layout logic.
  The 6-column tile grid constants live here.

- **`home/AppValueScorer.kt`** *(planned)*
  Scores installed apps using REACH × FREQUENCY (1–3 each). Outputs `AppValueScore` which
  determines default `isOnHome`, default `TileSize`, and suggested bucket. Run once at first
  boot or on manual re-score trigger. Optionally enhanced by `UsageStatsManager` data if the
  `PACKAGE_USAGE_STATS` permission is granted.

### 3. Theme Layer
- **`ui/theme/Theme.kt`**
  Four ambient modes: `EARLY_MORNING`, `DAY`, `BLUE_HOUR`, `LATE_NIGHT`.
  **Design Language:** Muted earthy/clay tones (Charcoal, Sage, Ochre). 
  - Airiness achieved via generous padding and 28.dp rounded dialog containers.
  - No neon accents.
- **`ui/theme/Type.kt`**
  Five-level hierarchy:
  - D1 (44–52sp, Syne Light) — ambient anchor
  - D2 (28–34sp, Syne Regular) — section titles
  - T1 (16–18sp, Inter SemiBold) — tile labels
  - T2 (14–15sp, Inter Regular) — body/metadata
  - T3 (11–12sp, Inter Regular) — index/captions

---

### 4. Data Layers
- **`DashboardViewModel`**
  - RSS Aggregator: Fetches 9 curated sources in parallel (OkHttp, 10s timeout, browser UA).
  - Weather: Open-Meteo API, coarse location, 30-minute refresh cycle.
  - Battery: `BroadcastReceiver` on `ACTION_BATTERY_CHANGED`, models discharge as linear from unplug point.
  - App list: `LauncherApps`, sorted by `UsageStatsManager` (7-day window) if permission granted.
- **`BriefingViewModel`**
  - **Ambient Signal** (Flash, fast): Consumes `feedItems`, calls `BuildConfig.GEMINI_MODEL` (Flash), generates a single editorial sentence. 4-hour TTL cache, hash-based dedup skips redundant calls.
  - **Deep Analysis** (Pro, on demand): Separate `generateAnalysis(briefingText)` method targeting `gemini-2.0-pro-exp`. Returns a structured 5-part response (Systemic Explanation, Historical Parallel, Literature/Philosophy, Tech & Economic Analysis, Probabilistic Forecasts). 24-hour cache keyed by briefing text hash.
  - Both share the same OkHttp client and response-parsing path.
- **`AgenticAIViewModel`**
  - Gemini-backed assistant wired to the briefing tap action.
  - On tap, builds a structured 5-part analysis prompt and sends it directly to the Gemini app via `ACTION_SEND`. Falls back to launching Gemini, then browser search.

---

### 5. RSS Pipeline

The feed pipeline runs entirely without an LLM and has five sequential stages:

```
Parallel fetch (9 sources, 8 items/source)
  → FeedFilter       — drops sports, personal opinion columns, and clickbait patterns
  → 48-hour gate     — drops articles older than 48h or with unparseable dates (epoch = 0L)
  → sort newest-first
  → FeedDeduplicator — Jaccard title similarity ≥ 0.40 collapses same-story clusters; newest wins
  → distinctBy URL   — safety net for exact duplicate links
  → take(30)
```

**Sources** (defined in `defaultFeedSources`, overridable via SharedPrefs `rss_sources_v2`):
| Source | Category |
|---|---|
| Associated Press | World / wire |
| Reuters | World / wire |
| BBC World | World |
| BBC Technology | Technology |
| The Guardian World | World |
| NPR News | Politics / US |
| Politico | Politics |
| Ars Technica | Technology / Science |
| Hacker News (hnrss.org) | Technology / community |

**Key implementation details:**
- `parseEpochMillis()` returns `0L` on failure (not `System.currentTimeMillis()`). Articles with `0L` are excluded by the freshness gate — this prevents articles with malformed dates from floating to the top of the feed.
- `FeedFilter` checks against three blocklists: sports league/competition terms, opinion column prefixes (`"opinion:"`, `"why i "`, etc.), and clickbait regex patterns. Blocklists are intentionally narrow to avoid false positives.
- `FeedDeduplicator` fingerprints each title by removing stop-words and punctuation, then computes pairwise Jaccard similarity. The input is already sorted newest-first so the cluster head is always the most recent version of a story.
- Per-source cap is 8 items so no single outlet can dominate the 30-item feed.

---

### 6. Analysis Screen

Tapping the ambient Signal on the home screen opens an in-app `AnalysisScreen` — a full-screen reading view that calls Gemini Pro and renders the structured analysis without leaving the launcher.

**Flow:**
```
tap Signal
  → AnalysisScreen (loading spinner)
  → BriefingViewModel.generateAnalysis(briefingText)
     → cache hit? → render immediately
     → cache miss → Gemini Pro API call (~3-5s)
  → LazyColumn reading view (same structure as ArticleViewerScreen)
  → back / swipe-right → dismiss
```

**Key differences from ArticleViewerScreen:**
- Content source is Gemini Pro API, not HTML fetch + Jsoup parse
- Input is the current briefing sentence (already in state)
- Sections rendered as labeled blocks (SYSTEMIC · HISTORICAL · PHILOSOPHY · ECONOMIC · FORECAST)
- 24h cache stored in `briefing_cache` SharedPrefs under key `last_analysis`

**Model selection:**
- Ambient Signal: `BuildConfig.GEMINI_MODEL` (Flash — speed matters)  
- Deep Analysis: `gemini-2.0-pro-exp` (hardcoded constant — quality matters)

---

## Tile Grid System

The home tile grid uses a **6-column internal grid** (`LazyVerticalGrid(GridCells.Fixed(6))`),
replacing the previous `FlowRow(maxItemsInEachRow = 3)`.

| TileSize | Col span | AspectRatio | Visual size |
|---|---|---|---|
| SMALL | 1 of 6 | 1f | 0.5 × 0.5 of regular |
| REGULAR | 2 of 6 | 1f | baseline 1×1 |
| WIDE | 4 of 6 | 2f | 2×1 (same height as regular) |

SMALL tiles may only appear in 2×2 groups (4 tiles occupying one REGULAR slot).
They must not be mixed with REGULAR tiles in the same row.

Default tile size is set by `AppValueScorer` at seeding time:
- Score 8–10 → WIDE
- Score 6–7 → REGULAR

User overrides are persisted in `LauncherConfig.tileSizes: Map<String, TileSize>`.

---

## State Model
- Installed apps are queried from `PackageManager`; icons load lazily via `rememberAppIcon`
  (Dispatchers.IO, LruCache of 150 entries).
- **Clipboard/Screenshot Monitor:** Observes system clipboard/recent screenshot buffer to expose
  direct manipulation targets in the `AgenticAIViewModel`.
- Launcher config stores: bucket assignments, home-visible package set, tile sizes per package,
  bucket ordering, hidden buckets, and battery `resetPct`.
- Each app belongs to at most one bucket.
- Apps can be assigned to a bucket but kept app-menu-only (`isOnHome = false`).
- Section removal moves apps back to unassigned/app-menu-only.

## Wallpaper Strategy
- `windowShowWallpaper=true` in activity theme.
- Compose draws transparent content and ambient scrims over the real system wallpaper.
- No direct wallpaper bitmap access.

---

## Planned Next Layers
- `AppValueScorer` — keyword heuristic + optional `UsageStatsManager` scoring.
- Tile size picker in the long-press edit UI (SMALL / REGULAR / WIDE segmented control).
- Font migration: `Syne` downloaded from Google Fonts, registered in `Type.kt`.
- Dark mode `tileBackground` color update across all three dark palettes.
- `DataStore` migration from `SharedPreferences` for launcher config.
- Real widget hosting for selected sections.
- Dedicated repositories for feed, notes, and AI.
- Media now-playing strip.
- Static location map.
