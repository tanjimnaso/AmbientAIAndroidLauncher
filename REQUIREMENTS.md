# Project Requirements

## Functional Requirements
1. **Launcher Fundamentals:**
   - Must register as an Android Home Launcher.
   - Query and list installed applications (excluding system bloat where possible).
   - Searchable "All Apps" drawer with an instant pop-up keyboard.
2. **Dashboard Features:**
   - **AI Chat:** Persistent text input at the bottom connecting to an Agentic AI system via Fireworks API.
   - **RSS Feed:** Parse and display XML RSS feeds in a scrolling list.
   - **Quick Notes:** Offline note-taking using a Room Database (+ icon).
3. **Chrome PWA Integration:**
   - The primary app ecosystem focus is a "Chrome PWA setup" to minimize native app reliance.
4. **UI/UX Requirements:**
   - Single-screen main dashboard.
   - Edge-to-edge layout (drawing behind navigation and status bars).
   - Time-based theme switching (e.g., Light Ocean Day / Dark Sci-Fi Night).

## Non-Functional Requirements
- **Battery Efficiency:** Minimal background sync, OLED black theme at night, no real-time rendering effects (e.g., live blur).
- **Typography Consistency:** Enforce a single typography configuration globally to eliminate visual noise.
- **Performance:** App search and filtering must use real-time derived states for 60fps+ rendering.