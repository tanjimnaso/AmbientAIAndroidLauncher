# Architecture

## Technology Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 primitives
- **Build:** Android Gradle Plugin `9.1.0`, Gradle wrapper `9.3.1`
- **Android target:** `compileSdk 36`, `targetSdk 36`
- **Persistence today:** `SharedPreferences` for launcher section configuration
- **Persistence planned:** `Room` for notes/feed/cache layers
- **Network planned:** Retrofit / OkHttp for real RSS and AI integrations

## Current App Structure
1. **Activity Shell**
   - `MainActivity` is intentionally thin.
   - It owns the top-level `DashboardViewModel` and `AgenticAIViewModel`.
   - It hands control to the launcher UI package.

2. **Home Package**
   - `home/LauncherScreen.kt`
     - Orchestrates pager navigation, vertical home scrolling, and library overlay state.
   - `home/LauncherCards.kt`
     - Reusable card surfaces and home section rendering.
   - `home/AppDrawer.kt`
     - Full-screen library/editor overlay.
   - `home/LauncherConfig.kt`
     - Bucket model, assignment persistence, and default seeding heuristics.
   - `home/LauncherLayout.kt`
     - Shared spacing, sizing, and edge-bleed layout logic.

3. **Theme Layer**
   - `ui/theme/Theme.kt`
     - Four ambient modes: `EARLY_MORNING`, `DAY`, `BLUE_HOUR`, `LATE_NIGHT`
   - `ui/theme/Type.kt`
     - `DM Serif Display` for large headings
     - `Inter` for body, labels, and control text

4. **Mock Data Layers**
   - `DashboardViewModel`
     - Mock feed items only
   - `AgenticAIViewModel`
     - Mock AI replies only

## UI Flow
1. **Page 0**
   - Industrial text-led index page
   - Section jumps
   - Hidden/system entry
   - Settings entry

2. **Page 1**
   - Scrollable ambient home
   - AI card
   - bucket rails and grouped cards
   - composer

3. **Overlay**
   - Library `Launch` mode for app launching
   - Library `Edit` mode for bucket assignment and hiding apps

## State Model
- Installed apps are queried from `PackageManager`.
- Launcher config is stored as bucket-to-package assignments.
- Each app belongs to at most one bucket.
- `Hidden` is treated as a normal bucket for persistence, but it is excluded from home.

## Wallpaper Strategy
- The launcher no longer tries to read and scale the wallpaper bitmap directly.
- The activity theme uses `windowShowWallpaper=true`.
- Compose draws transparent content and ambient scrims over the real system wallpaper.

## Planned Next Layers
- Replace `SharedPreferences` launcher config with `DataStore`.
- Add real widget hosting for selected sections.
- Add real repositories for feed, notes, and AI.
- Split social/message summaries into dedicated section models later.
