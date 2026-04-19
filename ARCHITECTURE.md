# Architecture

## Technology Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 primitives
- **Build:** Android Gradle Plugin `9.1.0`, Gradle wrapper `9.3.1`
- **Android target:** `compileSdk 36`, `targetSdk 36`
- **Persistence:** `SharedPreferences` for launcher config, home visibility, RSS cache, weather cache, notes, ambient-mode overrides.
- **Network:** OkHttp for RSS, weather, and Gemini.

---

## Package Layout

```
com.ambient.launcher
├── MainActivity.kt              — thin activity shell; owns DashboardViewModel + BriefingViewModel
├── DashboardViewModel.kt        — weather, battery, app list, RSS orchestration
├── BriefingViewModel.kt         — Gemini briefing + deep analysis
├── HttpClient.kt                — shared OkHttp client
├── BatteryUtils.kt              — discharge modelling
├── data/repository/
│   ├── AppRepository.kt         — PackageManager + UsageStatsManager queries
│   ├── FeedRepository.kt        — parallel RSS fetch + cache
│   ├── FeedUtils.kt             — filter, dedup, freshness gate
│   └── WeatherRepository.kt     — Open-Meteo client + cache
├── home/
│   ├── LauncherScreen.kt        — 3-page pager, masthead variants, ambient reveal
│   ├── HomeAppsScreen.kt        — main page: asymmetric hero + row, NowMoment
│   ├── NowMoment.kt             — rotating headline / weather / deep-time glance
│   ├── Masthead.kt              — time, date, weather, battery; fullness-aware
│   ├── AppMenuCard.kt           — swipe-up app menu, 15 buckets
│   ├── SPenNotesScreen.kt       — full-screen canvas with auto-hide toolbar
│   ├── AnalysisScreen.kt        — Gemini-powered structured analysis + TTS
│   ├── ArticleViewerScreen.kt   — reading-mode article + TTS + media strip
│   ├── LauncherCards.kt         — briefing card, feed list, signal line
│   ├── LauncherConfig.kt        — bucket model, pins, home visibility
│   ├── LauncherDialogs.kt       — long-press edit / bucket picker
│   ├── LauncherLayout.kt        — shared spacing + sizing constants
│   ├── ResponsiveTypography.kt  — screen-size-adaptive type scale
│   ├── TodaysSignal.kt          — contextual week/month line
│   ├── WallpaperHelper.kt       — system wallpaper access helpers
│   └── AppValueScorer.kt        — REACH × FREQUENCY seeding heuristic
├── tts/
│   ├── TtsController.kt         — single TextToSpeech owner
│   ├── TtsPlaybackService.kt    — foreground service; lock-screen controls
│   ├── TtsMediaStrip.kt         — bottom media bar with scrubber
│   ├── HighlightableText.kt     — sentence + word highlight synced to TTS
│   └── ArticleTtsPrep.kt        — sentence segmentation, word offsets
└── ui/theme/
    ├── Theme.kt                 — AmbientTheme + 4 palettes + AmbientMode
    ├── Color.kt                 — palette types
    ├── Type.kt                  — Syne + Inter font registration, 5-tier scale
    ├── GrainOverlay.kt          — tiled noise via PorterDuff.OVERLAY
    └── AmbientSettings.kt       — app-scoped StateFlow for ink mode, palette override
```

---

## The 3-Page Pager

`LauncherScreen` hosts a `HorizontalPager` with three pages:

| Page | Index | Content |
|---|---|---|
| News | 0 | `BriefingCard` + feed list + analysis entry |
| Main | 1 (default) | `HomeAppsScreen` — asymmetric layout, NowMoment |
| Notes | 2 | `SPenNotesScreen` — canvas with auto-hide toolbar |

### Masthead Fullness

The masthead is drawn at the top across all three pages, but its **fullness** varies with pager position. A `derivedStateOf` computes:

```kotlin
val mastheadFullness = pagerState.currentPage + pagerState.currentPageOffsetFraction
```

Fullness `1f` = main page, full detail (time + secondary lines). Below `0.8f` we're off-main and secondary lines fade out. This is passed as `fullness: Float` into `Masthead` which gates rendering of the date/season and battery-hours sub-rows.

### Ambient Reveal

On the main page only, the secondary line sits at 30% opacity by default. A `PointerEventPass.Initial` listener on the root Box watches for any press — without consuming — and flips `mastheadRevealed = true`. An `animateFloatAsState` pulses the alpha to 1.0, and a `LaunchedEffect(mastheadRevealed) { delay(3000); mastheadRevealed = false }` restores quiet mode.

The same pattern applies inside `SPenNotesScreen`: toolbar auto-hides after 4s, tap the top 80dp strip to bring it back. `AnimatedVisibility` (not manual alpha) wraps the toolbar so invisible buttons don't capture taps.

---

## Main Page Layout (HomeAppsScreen)

Asymmetric, off-grid — the exact opposite of the classic launcher grid:

```
┌─────────────────────────────────┐
│      (weight 0.4 — breathing)   │
│                                 │
│          · Now moment ·         │   ← rotating line, 30s cadence
│                                 │
│      (weight 0.6 — breathing)   │
│                                 │
│   [app] [app] [app] [app]       │   ← secondary row of 4
│                                 │
│                    ┌────────┐   │
│                    │  hero  │   │   ← contextual hero, bottom-right
│                    └────────┘   │
└─────────────────────────────────┘
```

**Contextual hero** — `pickContextualHero(topApps, pinnedRow, hour)` recomputes every 5 minutes via a `LaunchedEffect` hour-tick. It filters out apps already in the pinned row and picks the highest-used app matching the current time-of-day preference buckets:

```kotlin
val preferredBuckets = when (hour) {
    in 5..10  -> listOf(NEWS, AI, UTILITIES)
    in 11..16 -> listOf(SOCIAL, UTILITIES, AI)
    in 17..21 -> listOf(ENTERTAINMENT, NEWS, SOCIAL)
    else      -> listOf(TOOLS, UTILITIES, NEWS)
}
```

**Now moment** — `NowMoment.kt` rotates every 30 seconds between three sources: top headline ("NOW"), weather sentence ("TODAY"), and a `centuryGlanceForToday()` deep-time glance ("DEEP TIME"). Crossfade is 800ms; daily glance is keyed to `LocalDate.toEpochDay() % list.size` so the same glance shows all day.

---

## Theme Layer

### Four Ambient Modes

`AmbientMode` enum with four palettes, auto-switched by local hour via `rememberAmbientMode()`:

| Mode | Hours | Character |
|---|---|---|
| `DAYLIGHT_OUTDOOR` | 6–10 | bright, cool, high chroma text |
| `DAY_INTERIOR_HI` | 11–16 | warm paper, muted |
| `DUSK` | 17–20 | slate blue + amber |
| `TWILIGHT` | 21–5 | deep burnt umber, warm cream text |

Each palette defines `AmbientPalette(mainBackground, panel, elevatedPanel, accentHigh, textPrimary, textSecondary, tileBackground, errorAccent, inkColor, clusterIntelligence, clusterUtility, clusterCommunication, clusterAssistant, clusterHealth, iconOverlayOpacity)`.

### Typography

Five-level hierarchy in `Type.kt`:
- D1 (44–52sp, Syne Light) — ambient anchor
- D2 (28–34sp, Syne Regular) — section titles
- T1 (16–18sp, Inter SemiBold) — tile labels
- T2 (14–15sp, Inter Regular) — body / metadata
- T3 (11–12sp, Inter Regular) — captions / index

`ResponsiveTypography.kt` scales these by screen density so tablets don't get microtext.

### Grain + Ink Mode

- `GrainOverlay` paints a cached 192×192 noise bitmap via `BitmapShader` + `PorterDuffXfermode(OVERLAY)` at 4% alpha. The bitmap is `remember`-cached by tile size + seed so it's drawn once per composition tree.
- `AmbientSettings` exposes a `StateFlow<Boolean>` for ink-mode (monochrome desaturation). Consumed via `collectAsStateWithLifecycle()`.

---

## Data Layers

### DashboardViewModel
- **Weather** — `WeatherRepository` wraps Open-Meteo; coarse location; 30-minute refresh.
- **Battery** — `BroadcastReceiver` on `ACTION_BATTERY_CHANGED`; linear discharge model from unplug point; `batteryState: StateFlow<BatteryState>`.
- **Apps** — `AppRepository` queries `LauncherApps` + `UsageStatsManager` (7-day window if permission granted); icons lazy-loaded via `rememberAppIcon` with a 150-entry `LruCache`.
- **Feed** — triggers `FeedRepository.refresh()`; exposes `feed: StateFlow<List<RssFeedItem>>`.

### BriefingViewModel
- **Briefing** — Gemini Flash; single sentence; 6-hour cache.
- **Analysis** — Gemini Pro; structured sections (SYSTEMIC · HISTORICAL · PHILOSOPHY · ECONOMIC · FORECAST); 24-hour cache under `briefing_cache/last_analysis`.
- On briefing-tap, builds a 5-part deep-dive prompt and opens the Gemini app via `ACTION_SEND`, falling back to launch → browser search.

---

## RSS Pipeline

Five sequential stages in `FeedRepository` + `FeedUtils`:

```
Parallel fetch (9 sources, 8 items/source, 10s timeout)
  → FeedFilter       — drops sports, opinion columns, clickbait
  → 48-hour gate     — drops items older than 48h or with unparseable dates (epoch = 0L)
  → sort newest-first
  → FeedDeduplicator — Jaccard title similarity ≥ 0.40, keep newest
  → distinctBy URL   — exact-duplicate safety net
  → take(30)
```

**Sources** (`defaultFeedSources`, overridable via SharedPrefs `rss_sources_v2`): AP, Reuters, BBC World, BBC Tech, Guardian, NPR, Politico, Ars Technica, Hacker News.

**Implementation notes**
- `parseEpochMillis()` returns `0L` on failure — never `System.currentTimeMillis()` — so malformed dates can't float to the top.
- `FeedFilter` uses three narrow blocklists (sports terms, opinion prefixes, clickbait regex) to avoid false positives.
- `FeedDeduplicator` fingerprints titles (strip stop-words + punctuation), then Jaccard-compares pairs; input is pre-sorted newest-first so the cluster head is always the freshest version.
- Per-source cap of 8 items prevents any single outlet dominating.

---

## TTS Pipeline

A single `TtsController` owns one `TextToSpeech` instance app-wide. `TtsPlaybackService` is a foreground service exposing lock-screen controls.

`ArticleTtsPrep` segments article text into sentences with character offsets; `HighlightableText` composes a `Text` with two overlays — current sentence (background tint) and current word (underline) — driven by TTS progress callbacks.

Used by both `ArticleViewerScreen` (web article reading) and `AnalysisScreen` (Gemini deep dive).

---

## State Persistence

`LauncherConfig` persists to `SharedPreferences`:
- Bucket assignments (`Map<String, BucketId>`)
- Home-visible package set
- Pinned row packages + hero package (ordered list)
- Bucket ordering, hidden buckets
- Battery `resetPct`
- RSS source overrides
- Ambient palette override (null = auto by hour)

Each app belongs to at most one bucket. Apps can be bucket-assigned but kept app-menu-only (`isOnHome = false`). Section removal moves apps back to unassigned.

---

## Wallpaper Strategy
- `windowShowWallpaper=true` in the activity theme.
- Compose draws transparent content + ambient scrims over the system wallpaper.
- No direct wallpaper bitmap access; `WallpaperHelper` only handles the subtle top/bottom gradient scrims.
