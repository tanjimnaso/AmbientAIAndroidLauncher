# Best Practices

## 1. Build and tooling
- Always use the committed Gradle wrapper.
- Keep AGP, wrapper, and Compose/Kotlin tooling aligned before feature work.
- Do not rely on machine-level Gradle installs.

## 2. Wallpaper and system integration
- Prefer `windowShowWallpaper=true` and transparent surfaces over direct wallpaper bitmap access.
- Avoid private Samsung effects or private system hooks.
- Let One UI own the wallpaper — the launcher adapts, it doesn't replace.

## 3. Home system discipline
- Treat the home surface as a flat composition directly over the wallpaper. No nested `Surface` wrappers, no enclosure-based grouping.
- Each app belongs to at most one launcher bucket.
- Keep home visibility separate from bucket assignment so apps can live in the app menu without appearing on the home page.
- The main page is intentionally asymmetric (row of 4 + bottom-right hero). Do not flatten it back into a grid.
- Long-press is the only edit affordance. Do not add gear icons or edit-mode toggles.

## 4. Compose performance
- **Lazy icon loading.** Never load `Drawable`s or `ImageBitmap`s in bulk during `PackageManager` queries. Keep `AppInfo` lightweight (text + IDs only) and fetch icons lazily at the leaf via `rememberAppIcon`.
- **Avoid state thrashing.** Don't chain multiple `derivedStateOf` blocks mapping hundreds of apps on scroll/drag. Do it once in a ViewModel or in a keyed `remember`.
- **Keep large surfaces simple.** No live blur, no heavy shaders. The grain overlay is the only full-surface effect — and its bitmap is cached.
- **Prefer primitives.** `HomeAppTile`, `SecondaryAppIcon`, and the masthead tiers are the reusable building blocks. Reach for them before inlining new Compose blocks.

## 5. Visual consistency
- Pull all type styles from `ResponsiveTypography` / `Type.kt`. Never declare raw `TextStyle` inline in a screen file.
- Pull all colours from `AmbientTheme.palette`. Never use raw hex `Color(0xFF...)` in UI files — add a palette role in [Theme.kt](app/src/main/java/com/ambient/launcher/ui/theme/Theme.kt) first.
- No drop shadows. Elevation is panel colour.
- No bottom app bar. Navigation is the pager.

## 6. Motion
- Palette transitions: 1000ms tween.
- NowMoment crossfade: 800ms tween.
- Ambient reveals: 200ms in, 400ms out (asymmetric — reveal starts quickly, recedes slowly).
- No spring curves on primary surfaces.

## 7. Deferred integrations
- Messaging/email recents: if added later, build as dedicated surfaces, not hacked into the bucket layer.
- AOD integration: see the README note — path is via silent ongoing notification, not via replacing Samsung's AOD.
- Widget hosting: not planned. All cards stay launcher-native Compose.
- Weather and RSS stay lightweight and client-side. No heavy widget templates.
