# Ambient Launcher

Information first design for android, only tested on S22 Ultra, Android 16.
Inspired from Windows Phone.
Built in Kotlin and Jetpack Compose.

The current direction is:
- a vertically scrollable home page made of large rounded cards
- a left-swipe industrial index page with text-only navigation
- editable app grouping into buckets like `News`, `Utilities`, `Security`, `Social`, `Browsers`, `Wallet`, and `AI`
- a hidden bucket for Samsung/system clutter that should stay out of the main home

## Current Features
- **Two-page launcher flow:** Home on the right, industrial index on the left.
- **Scrollable home:** The main home screen is a vertical stack of grouped sections, not a fixed dashboard.
- **Editable app grouping:** Apps can be assigned to a single bucket and persisted locally.
- **Hidden apps bucket:** Samsung tools/bloat can be moved out of the main home and managed separately.
- **Ambient wallpaper hosting:** The launcher uses the current system wallpaper as the true backplane and layers its own scrims over it.
- **Time-based ambient theme:** Early morning, day, blue hour, and late night modes.
- **Typography system:** `DM Serif Display` for major headings and `Inter` for body/UI text.
- **Launcher library overlay:** Launch mode for all visible apps and edit mode for section assignment.

## Current Limitations
- **Ambient AI is still mocked:** `AgenticAIViewModel` returns placeholder responses.
- **RSS is still mocked:** `DashboardViewModel` returns placeholder feed items.
- **Notes are not implemented yet:** there is no launcher-native notes tool or Samsung Notes integration yet.
- **Messaging/email recents are deferred:** social apps currently launch as normal app cards.
- **Widget hosting is deferred:** cards are launcher-native right now, not hosted Android widgets.

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
