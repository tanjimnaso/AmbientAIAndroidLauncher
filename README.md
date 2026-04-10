# Ambient Launcher

Information first design for android, only tested on S22 Ultra, Android 16.
Inspired from Windows Phone.
Built in Kotlin and Jetpack Compose.

The current direction is:
- a vertically scrollable home page with a clear stack: header, project cards, calendar rail, app tiles, note input
- a left-swipe industrial index page with text-only navigation
- editable app grouping into buckets like `News`, `Utilities`, `Security`, `Social`, `Browsers`, `Wallet`, `AI`, and `Health`
- a home organiser where apps can be app-menu-only or visible on home

## Current Features
- **Two-page launcher flow:** Home on the right, industrial index on the left.
- **Scrollable home:** The main home screen is a vertical editorial stack, not a fixed dashboard.
- **Editable app grouping:** Apps can be assigned to a single bucket, moved back to app-menu-only, and persisted locally.
- **Home organiser:** Home-visible apps are controlled separately from bucket assignment.
- **Pure app tile field:** Home app tiles are uniform `1x1` soft squares with tiny icons and small bottom labels, without section cards or headers.
- **Project-card top stack:** RSS and Ambient AI now live in a pastel project-card row above the calendar rail.
- **Ambient wallpaper hosting:** The launcher uses the current system wallpaper as the true backplane and layers its own scrims over it.
- **Time-based ambient theme:** Early morning, day, blue hour, and late night modes.
- **Typography system:** `DM Serif Display` for major headings and `Inter` for body/UI text.
- **Real RSS lead card:** `DashboardViewModel` fetches and caches live feed items for the top story surface.
- **Weather header:** The home header uses coarse location and Open-Meteo for a lightweight temperature/range readout.
- **Ambient AI fallback path:** Ambient AI uses Gemini when `geminiApiKey` is configured in `local.properties`, otherwise it falls back to a local launcher-native response.
- **Bottom note input:** Home ends with a minimal note field instead of a titled AI card.

## Current Limitations
- **Notes are not implemented yet:** there is no launcher-native notes tool or Samsung Notes integration yet.
- **Messaging/email recents are deferred:** social apps currently launch as normal app cards.
- **Widget hosting is deferred:** cards are launcher-native right now, not hosted Android widgets.
- **Gemini requires local setup:** live AI only works if a local API key is configured.

## Build Baseline
- Android Gradle Plugin `9.1.0`
- Gradle wrapper `9.3.1`
- `compileSdk` / `targetSdk` `36`
- Compose BOM `2026.02.01`

Build with:

```bash
./gradlew assembleDebug
```

## Documentation
- [Requirements](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/REQUIREMENTS.md)
- [Architecture](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/ARCHITECTURE.md)
- [Art Direction](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/ART_DIRECTION.md)
- [Best Practices](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/BEST_PRACTICES.md)
