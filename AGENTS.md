# AI Agent Instructions

If you are an AI assistant modifying this codebase, **read this first**.

## Context
This is an Android 16 Launcher built in Kotlin + Jetpack Compose. The goal is a **calm, ambient launcher** — editorial layout, hairline typography, slow motion, lux-aware palettes, asymmetric main page. Not a grid launcher. See [ART_DIRECTION.md](ART_DIRECTION.md) for the visual system and [ARCHITECTURE.md](ARCHITECTURE.md) for the code layout.

## Golden Rules

1. **Never load app icons synchronously or in bulk on startup.**
   - `PackageManager.loadIcon()` is heavy. Keep `AppInfo` lightweight — `packageName` + `label` only.
   - Use `rememberAppIcon(packageName)` at the leaf Compose node that actually draws the icon. Icons are cached in a 150-entry `LruCache` loaded on `Dispatchers.IO`.

2. **No "cards within cards."**
   - Home and News pages draw directly on the ambient background. No nested `Surface` wrappers. No drop shadows.
   - Visual grouping is done with spacing and colour, not enclosure.

3. **Avoid Compose state thrashing.**
   - Do not use `derivedStateOf` to iterate over 100+ installed apps on scroll/drag events.
   - Pre-compute bucket assignments in `remember` blocks tied to config keys, or push the work into a ViewModel.

4. **Location & Weather.**
   - On Android 11+, `getLastKnownLocation` often returns `null`. Use `LocationManager.getCurrentLocation` with a `CancellationSignal` inside a suspend coroutine. See `WeatherRepository`.

5. **Theming & colours.**
   - Do not introduce raw hex colours (`Color(0xFF...)`) into UI files. Only use `AmbientTheme.palette` surfaces.
   - When adding new colour roles, add them to `AmbientPalette` in [Theme.kt](app/src/main/java/com/ambient/launcher/ui/theme/Theme.kt) first — define for all four palettes.

6. **Motion discipline.**
   - Reveals: 200ms in / 400ms out tween. Palette transitions: 1000ms. NowMoment: 800ms crossfade.
   - No springs on primary surfaces. Nothing snaps. See [ART_DIRECTION.md](ART_DIRECTION.md).

7. **Ambient reveal pattern.**
   - Secondary chrome (masthead sub-line, notes toolbar) hides at low alpha or unmounts, and reveals on non-consuming tap via `PointerEventPass.Initial`.
   - When using `AnimatedVisibility` for reveal, prefer unmounting over `graphicsLayer { alpha = ... }` so invisible buttons can't capture taps.

## Before You Change Anything

- Read [REQUIREMENTS.md](REQUIREMENTS.md) for scope boundaries.
- Check [BEST_PRACTICES.md](BEST_PRACTICES.md) for Compose conventions.
- Keep [MainActivity.kt](app/src/main/java/com/ambient/launcher/MainActivity.kt) thin. Compose logic lives in `home/LauncherScreen.kt`.

## Testing Config Changes
Seeding logic (`LauncherConfig` defaults) only runs on first install. When iterating on defaults:
```bash
adb shell pm clear com.ambient.launcher
```
