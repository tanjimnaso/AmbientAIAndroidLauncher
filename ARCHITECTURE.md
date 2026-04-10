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
  - RSS Aggregator Service: Fetches multi-source news (Chatham House, FT, etc.).
  - Perspective Engine: Applies lenses (Career, Sydney, Global, Contrarian) to processed headlines.
  - Weather, Battery, App list.
- **`AgenticAIViewModel`**
  - Gemini-backed persona engine. 
  - Perspective-aware querying: Wraps user intent in Lens-specific system prompts.
  - Deep Link Generator: Bridges "Ambient" summary-scan to "Deep Dive" Gemini App sessions via custom URI intents.

---

### 5. News Engine (Multi-Perspective Cognition)
- News is no longer a static feed. It is passed through a **Perspective Router** to provide personalized career/local/global insights.
- Feedback loop utilizes `FeedbackTable` (Room DB) to store ratings, which are injected as "Few-Shot" context into future AI interactions.
- Remote configuration synced via Google Drive to allow seamless profile/prompt management.

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
