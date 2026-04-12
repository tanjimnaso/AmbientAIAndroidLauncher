# AmbientAIAndroidLauncher TODO

## Session Audit & Progress
- [x] **Analytical Grid:** 90-degree corners, 2dp gaps, edge-to-edge bleed.
- [x] **Typography Migration:** Syne (Display) and Inter (Body) integrated.
- [x] **Battery UX:** Direct status line (card-less), calculation bug fixed (scales off 100%).
- [x] **Today Feed:** Typography-first, card-less, shows 5 headlines using Inter Regular.
- [x] **Project Cards (HCI/Tufte):** 
    - Moved progress bar to **TOP** for better visual hierarchy.
    - Removed active border glow (Rams: less noise).
    - Added Tufte-inspired inline task lists (direct glanceability).
- [x] **Color Management:** Solved device/macbook discrepancy by neutralizing hex values (#363636).
- [x] **Navigation:** Fixed right-swipe bug to correctly open app menu.
- [x] **Reading Connector:** notification-listener path implemented for Perlego/Audible hooks.
- [x] **AI Briefing:** Samsung-style "Vibe" summary wired into the home header using Gemini.
- [x] **Today Settings:** RSS Editor and Custom AI Instruction UI implemented (revised to earthy palette).
- [x] **UI Overhaul:** Pop-up dialogs upgraded to large, airy, rounded-corner UI with muted earthy colors.
- [x] **News Engine (In Progress):** Multi-perspective cognition engine started (Professional, Local, Global, Contrarian lenses).

---

## Current Status
- **Performance:** Stable for 9-15 app home screens. `BoxWithConstraints` + flattened grid prevents frame drops.
- **HCI Alignment:** Moving from Command-based (app icons) to Intent/Status-based (Today, Project Progress, Battery Status).
- **Theme:** Shifted from neon/tech colors to Muted Earthy/Clay palette for airy feel.

---

## High Priority (Do Next)

### 1. News Engine Implementation
- [x] **In-App Analysis Screen:** `AnalysisScreen.kt` — Gemini Pro deep-dive, no external app launch.
- [ ] **Perspective Router:** Build `getPromptForPerspective` in `AgenticAIViewModel`.
- [ ] **Lens Selector UI:** Add perspective chips below headlines in `LauncherScreen`.
- [ ] **Google Drive Sync:** Implement `Drive REST API` for `launcher_settings.json` and `feedback.json`.
- [ ] **Feedback Loop:** Implement Thumbs Up/Down tracking in local Room DB to tune future "Few-Shot" prompts.

### 2. Media UX
- [ ] **Media Strip Polish:** Ensure active playback correctly triggers the strip visibility and Rams-inspired "Active State" dot.

### 3. Core Feature Backlog
- [ ] **Industrial Index — Type Bleed:** Signals swipeability on the App Menu page.
- [ ] **Clipboard Drop-Zone:** Direct manipulation for screenshot-to-IDE path (near Gemini pill).

---

---

## Built but not rendering / not wired in

These components exist in the codebase and compile, but are not connected to the home screen or any active UI path.

### `AnalysisScreen` — IN PROGRESS
Full-screen in-app deep analysis powered by Gemini Pro.
Tap the ambient Signal → loading state → structured 5-part response rendered as reading view.
Same overlay pattern as `ArticleViewerScreen`.
Model: `gemini-2.0-pro-exp` | Cache: 24h | Dismiss: back / swipe-right

### `StateLayerDashboard` (`home/StateLayerDashboard.kt`)
Circular arc gauge panel for health, leverage/network, skills, and time allocation metrics.
Removed from the home screen during the April 2026 news-first redesign (commit `468fe78`).
The composable is complete and data-ready — it just needs to be re-inserted into `LauncherScreen` or wired to a slide-over panel.
**To wire:** call `StateLayerDashboard(metrics = ...)` with placeholder `GaugeMetric` values to start; real data requires a health/fitness integration.

### `TimeHorizonDashboard` (`home/TimeHorizonDashboard.kt`)
Long-term probabilistic life/event timeline (personal milestones, AGI forecasts, cosmic scale).
Also removed in the April 2026 redesign. The file exists and is complete.
**To restore:** add it back as a page in the horizontal pager in `LauncherScreen`, or as a full-screen route accessible from the home header.

### `MediaStrip` (`home/MediaStrip.kt`)
Now-playing strip that shows current media title and basic playback controls.
`MediaViewModel` is instantiated in `MainActivity` but `MediaStrip(viewModel)` is never called anywhere in the UI tree.
**To wire:** add `MediaStrip(viewModel = mediaViewModel)` near the top of `HomePage` in `LauncherScreen.kt`. It only renders when media is active.

### `TodoPanel` (`home/TodoPanel.kt`)
An older slide-over panel variant of the to-do UI. Superseded by the full-screen `TodoScreen` which is the currently active implementation.
`TodoPanel` is dead code — it is never called. Can be deleted or redesigned as a compact inline summary widget.

### `ReadingViewModel` (`ReadingViewModel.kt`)
Instantiated in `MainActivity` but never passed to any composable and has no UI connected to it.
Intended for the Perlego/Audible reading context feature (notification listener path).
**Status:** ViewModel scaffolding only. No active integration.

### Multi-perspective News Engine (todo item marked "In Progress")
The todo item "News Engine: Multi-Perspective Cognition Engine" is not implemented.
The RSS feed is a single flat stream. There is no Perspective Router, no lens selector UI, no Room feedback table, and no Google Drive sync.
The `AgenticAIViewModel` has some prompt-wrapping logic but it is not connected to a lens system.

---

## Operational Rules for AI Agents
- **Rule 1: No Synchronous Loading.** Never load icons or network data on the main thread.
- **Rule 2: Flat Layouts.** No cards within cards. Edge-to-edge bleed on wallpaper.
- **Rule 3: State Management.** Push transformations to ViewModels. No `derivedStateOf` thrashing.
- **Rule 4: Theme Consistency.** Always use `AmbientTheme.palette` tokens (Earthy/Muted palette).
- **Rule 5: Keep it Ambient.** High data density, low visual noise.
- **Rule 6: Grid Discipline.** 90-degree corners only. Analytical, modular spacing (2dp).
