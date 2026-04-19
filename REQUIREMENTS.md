# Requirements

## Scope

A single-user, offline-first Android launcher that replaces the system home app. Primary target: Samsung Galaxy S22 Ultra on Android 16. Secondary: any Android 14+ device.

---

## Functional Requirements

### Home surface
- Must register as a launcher (`ACTION_MAIN` + `CATEGORY_HOME`) and survive reboots as the default.
- Must present a 3-page horizontal pager: News ← Main → S-Pen Notes.
- Must default to the Main page on every launch.
- Must draw over the system wallpaper (`windowShowWallpaper=true`) with transparent composition.
- Must restore pager position across orientation changes within a session, but reset to Main on cold start.

### Masthead
- Must show local time, temperature, weather summary, and battery hours-remaining.
- Time must update every minute without redrawing the full launcher.
- On the Main page, the secondary line (date/season/battery) sits at 30% opacity and reveals to 100% for 3 seconds on any screen tap, then fades.
- On News and Notes pages, the secondary line is always fully visible.

### News page
- Must fetch 9 configured RSS sources in parallel with a 10-second per-source timeout.
- Must filter sports, opinion columns, and clickbait using narrow keyword/regex blocklists (no ML).
- Must drop items older than 48 hours or with unparseable dates.
- Must collapse same-story duplicates via Jaccard title similarity ≥ 0.40.
- Must cap per-source at 8 items and total feed at 30.
- Must cache the feed to `SharedPreferences` and serve stale on network failure.
- Must expose a single-sentence Gemini briefing above the feed.
- Briefing tap must open a 5-part structured deep-dive prompt in the Gemini app, falling back to launch → browser search.

### Main page
- Must display a row of 4 user-pinned apps above-left and a contextual hero tile bottom-right.
- Contextual hero must recompute every 5 minutes and pick the highest-used app matching the current time-of-day bucket preference.
- Must display the "Now moment" line above the pinned row, rotating every 30 seconds between headline, weather, and deep-time glance.
- Must support swipe-up to open the full app menu (15 buckets).
- Must support long-press on any pinned app to edit bucket assignment, remove from home, or change tile size.

### Notes page
- Must provide a full-screen drawing canvas supporting pen and eraser.
- Toolbar must auto-hide after 4 seconds of inactivity.
- Top 80dp edge must serve as a tap target to re-reveal the toolbar.
- Must support left-handed and right-handed toolbar positioning.

### Article viewer
- Must open in-app when any feed headline is tapped.
- Must render a clean reading-mode view (no ads, nav, or chrome from the source).
- Must provide TTS playback with sentence + word highlighting synced to speech.
- Must expose a bottom media-control strip (play/pause, scrub, skip) while speaking.

### Analysis screen
- Must accept the current briefing sentence as input.
- Must call `gemini-2.5-pro` and render five labeled sections: SYSTEMIC · HISTORICAL · PHILOSOPHY · ECONOMIC · FORECAST.
- Must cache the last result for 24 hours.
- Must support the same TTS playback affordances as the article viewer.

### App menu
- Must list all installed apps grouped into 15 user-editable buckets.
- Must sort each bucket by 7-day usage frequency when `PACKAGE_USAGE_STATS` is granted, alphabetically otherwise.
- Must persist bucket assignments, home visibility, and bucket ordering to `SharedPreferences`.

### Ambient theming
- Must select one of four palettes (DAYLIGHT_OUTDOOR, DAY_INTERIOR_HI, DUSK, TWILIGHT) automatically using lux sensor + local time.
- Must animate palette transitions over 1000ms.
- Must expose a monochrome "ink mode" toggle that collapses cluster hues to a single ink color while preserving the palette's background/text/accent.

---

## Non-Functional Requirements

### Performance
- Cold start to Main page: < 500ms on S22 Ultra.
- Palette transitions must not drop frames on a 120Hz display.
- Icon loading must use a `LruCache` of at least 150 entries, loaded on `Dispatchers.IO`.
- No main-thread network I/O, no main-thread disk I/O beyond `SharedPreferences` reads.

### Privacy
- No user accounts. No cloud sync. No analytics. No telemetry.
- Weather uses Open-Meteo with coarse location only (city-level grid).
- Gemini API calls are stateless per-request and carry no user identifier.
- All persistence is local to the device.

### Resilience
- Every network call must tolerate failure by serving cached content or gracefully hiding the affected section.
- Malformed RSS dates must yield `epoch = 0L`, not `System.currentTimeMillis()`, so they can be filtered out of the freshness gate.
- Missing Gemini API key must not crash the launcher; briefing and analysis sections must simply not render.

### Accessibility
- Minimum tap target: 48dp × 48dp.
- All icon buttons must carry `contentDescription`.
- TTS playback must be interruptible via standard media-button intents.

---

## Build Targets

- **Kotlin:** 2.0+
- **Android Gradle Plugin:** 9.1.0
- **Gradle wrapper:** 9.3.1
- **compileSdk / targetSdk:** 36
- **minSdk:** 34 (Android 14)
- **Compose BOM:** 2026.02.01

## Permissions

| Permission | Purpose | Required |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Enumerate installed apps for the menu | Yes |
| `POST_NOTIFICATIONS` | Future AOD-via-notification path | No |
| `PACKAGE_USAGE_STATS` | Usage-based sort and contextual hero | Optional, graceful fallback |
| `ACCESS_COARSE_LOCATION` | Weather grid lookup | Optional, weather hidden if denied |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | TTS playback service | Yes (for TTS feature) |

---

## Out of Scope

The following are explicitly **not** planned:
- Cloud sync, multi-device state
- Android widget hosting (`AppWidgetHost`)
- Calendar, email, or messaging integrations
- Third-party theme marketplace
- Tablet-optimised landscape layout
- A settings screen with dozens of toggles — configuration is either automatic or a single long-press
