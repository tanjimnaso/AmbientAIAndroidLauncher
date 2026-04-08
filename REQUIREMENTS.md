# Project Requirements

## Functional Requirements
1. **Launcher Fundamentals**
   - Register as an Android Home launcher.
   - Query installed launchable applications.
   - Provide a full-screen library overlay for launching apps.
   - Allow apps to be hidden from the main home without being removed from search/library.

2. **Home System**
   - Main home page must be vertically scrollable.
   - Home page must be composed of grouped sections, not a fixed icon grid.
   - Apps must be assignable to a single bucket:
     - `News`
     - `Utilities`
     - `Security`
     - `Social`
     - `Browsers`
     - `Wallet`
     - `AI`
     - `Hidden`
   - Browser apps should be able to appear as larger standalone squares.
   - Wallet/banking apps should be able to appear together as a lighter grouped rectangle.

3. **Secondary Navigation**
   - Swiping left from home should reveal an industrial text-led index page.
   - That page should expose section jumps, hidden/system access, and a settings entry point.
   - The persistent bottom apps bar should not exist.

4. **Ambient Features**
   - Keep a launcher-native AI composer on home.
   - Support time-based theme switching across multiple day phases.
   - Use the current system wallpaper as the base visual layer rather than copying a custom in-app wallpaper bitmap.

5. **Deferred Feature Areas**
   - Social/message recents are deferred to a future pass.
   - Widget hosting is deferred to a future pass.
   - Launcher-native notes and Samsung Notes integration are deferred.
   - Feed/API integrations beyond the current mock state are deferred.

## Non-Functional Requirements
- **Battery Efficiency:** No live blur, no unnecessary continuous rendering, minimal background work.
- **Typography Consistency:** Use the shared typography system only.
- **Performance:** Home scrolling, section rendering, and library filtering must remain smooth on a large app set.
- **Config Persistence:** Section assignment and hidden-app state must survive app restarts.
