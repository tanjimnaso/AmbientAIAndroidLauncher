# Codex & AI Agent Instructions

If you are an AI assistant, Codex, or an LLM modifying this codebase, **read this first**.

## Context
This is an Android 16 Launcher built in Kotlin and Jetpack Compose.
The goal is to create a calm, ambient launcher with an editorial "Windows Phone-like" flat tile layout, removing the noisy card-in-card conventions typical of Android. 

## Golden Rules
1. **Never load app icons synchronously or in bulk on startup.** 
   - `PackageManager.loadIcon()` is extremely heavy. Keep `AppInfo` as a lightweight data class containing only the `packageName` and `label`. 
   - Use `rememberAppIcon(packageName)` at the Compose leaf node when the icon actually needs to be drawn.

2. **No "Cards Within Cards".**
   - The home layout uses a flat grid (FlowRow) of app tiles sitting directly on the `WallpaperBackplane`. 
   - Do not wrap sections in `Surface` components with heavy paddings.
   - Do not add text headers to home sections. Visual grouping is achieved by spacing alone on the home page. The left "Industrial Index" page handles textual context.

3. **Avoid Compose State Thrashing.**
   - Do not use `derivedStateOf` to iterate over 100+ installed apps during every scroll or drag event. 
   - Pre-compute bucket/app assignments in `remember` blocks tied to `configuration` changes, or push it down to a ViewModel.

4. **Location & Weather Handling.**
   - On Android 11+ (API 30+), `getLastKnownLocation` often silently returns `null` if no other app is actively pulling location. Use `LocationManager.getCurrentLocation` with a `CancellationSignal` inside a suspend coroutine to reliably fetch the weather.

5. **Theming & Colors.**
   - Do not introduce raw hex colors (e.g., `Color(0xFF...)`) into UI files. 
   - Only use `AmbientTheme.palette` surfaces (`panel`, `elevatedPanel`, `searchPanel`, `chipBackground`).

6. **Time Horizon Dashboard.**
   - The timelines use vertical leader lines for labels.
   - Do not use diagonal lines or overlapping text; use staggered vertical offsets or multi-line wrapping if needed.
   - The entire timeline area is a unified interactive target that triggers the "Forecast Observation Deck" (Slide-out Panel).

## Making Changes
- Always review `todo.md` and `REQUIREMENTS.md` before attempting a refactor.
- If testing `SharedPreferences` defaults or seeding configuration logic (`LauncherConfigStore`), always use `adb shell pm clear com.ambient.launcher` to ensure a clean slate, otherwise your logic may not trigger.
- Keep the `MainActivity.kt` extremely thin. Put Compose logic in `LauncherScreen.kt` and UI widgets in `LauncherCards.kt`.
