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
- Treat the home screen as a section system, not a one-off dashboard.
- Each app should belong to one launcher bucket at most.
- Hide Samsung/system clutter through the hidden bucket instead of polluting the main home.
- Keep the left industrial page for navigation, hidden/system access, and settings.

## 4. Compose Performance
- Use `derivedStateOf` for filtered app lists and bucket projections.
- Keep large surfaces simple; do not introduce live blur or heavy shader effects.
- Prefer reusable card primitives over repeatedly inlining complex UI blocks.

## 5. Visual Consistency
- Large rounded cards on home.
- Thin borders and regular-weight text for the industrial/navigation layer.
- Pull font styles from the shared typography object only.
- Do not reintroduce a generic bottom app bar unless it is clearly justified by the interaction model.

## 6. Deferred Integrations
- Messaging/email recents should be built later as dedicated surfaces, not hacked into the current app grouping layer.
- Widget hosting, notes editing, and feed/API integrations should sit on top of the current section model, not bypass it.
