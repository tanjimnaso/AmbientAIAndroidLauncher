# Best Practices

## 1. Build And Tooling
- Always use the committed Gradle wrapper.
- Keep AGP, wrapper, and Compose/Kotlin tooling aligned before doing feature work.
- Do not rely on ad hoc machine-level Gradle versions.

## 2. Wallpaper And System Integration
- Prefer `windowShowWallpaper` and transparent launcher surfaces over direct wallpaper bitmap access.
- Avoid private Samsung effects and private system hooks.
- Keep One UI as the wallpaper owner; the launcher should adapt, not replace that stack.

## 3. Home System Discipline
- Treat the home screen as a flat section system directly floating on the wallpaper.
- Each app should belong to one launcher bucket at most.
- Keep home visibility separate from assignment so apps can stay in the app menu without appearing on home.
- **Do not wrap apps in nested section cards.** Let the tiles sit directly on the wallpaper grid.
- Keep the left industrial page for navigation, app editing, and settings.

## 4. Compose Performance
- **Lazy Icon Loading:** Do NOT load Android app `Drawable`s or `ImageBitmap`s in bulk during startup inside `PackageManager` queries. Always keep state models (like `AppInfo`) lightweight with only text/IDs, and fetch icons lazily on the UI thread (via `rememberAppIcon` or Coil) only when the tile renders.
- **Avoid State Thrashing:** Do not chain multiple `derivedStateOf` blocks mapping hundreds of apps in the UI thread for simple interactions.
- Keep large surfaces simple; do not introduce live blur or heavy shader effects.
- Prefer reusable card primitives over repeatedly inlining complex UI blocks.

## 5. Visual Consistency
- Large rounded cards on home.
- Thin borders and regular-weight text for the industrial/navigation layer.
- Pull font styles from the shared typography object only.
- Do not reintroduce a generic bottom app bar unless it is clearly justified by the interaction model.

## 6. Deferred Integrations
- Messaging/email recents should be built later as dedicated surfaces, not hacked into the current app grouping layer.
- Widget hosting and notes editing should sit on top of the current section model, not bypass it.
- Weather and RSS should stay lightweight and launcher-native rather than trying to embed heavy widget templates.
