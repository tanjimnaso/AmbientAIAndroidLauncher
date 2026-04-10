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

## Operational Rules for AI Agents
- **Rule 1: No Synchronous Loading.** Never load icons or network data on the main thread.
- **Rule 2: Flat Layouts.** No cards within cards. Edge-to-edge bleed on wallpaper.
- **Rule 3: State Management.** Push transformations to ViewModels. No `derivedStateOf` thrashing.
- **Rule 4: Theme Consistency.** Always use `AmbientTheme.palette` tokens (Earthy/Muted palette).
- **Rule 5: Keep it Ambient.** High data density, low visual noise.
- **Rule 6: Grid Discipline.** 90-degree corners only. Analytical, modular spacing (2dp).
