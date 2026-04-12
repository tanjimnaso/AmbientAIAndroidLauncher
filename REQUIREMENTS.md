# Project Requirements

## Functional Requirements

### 1. Launcher Fundamentals
- Register as an Android Home launcher.
- Provide a full-screen library overlay for launching apps.
- **Ambient Focus:** Launcher as a "Personal Intent OS" rather than a utility grid. Prioritize reducing interaction friction and cognitive load.

### 2. Home System
- Main home page must be vertically scrollable.
- Home page must be composed as a vertical editorial stack:
  header → project cards → calendar rail → app tile grid → action cards (Wallet, Battery) → note input.
- Home app grids must have **no section headers** to reduce visual noise.
- Home app sections render as a flat grid of tiles floating directly over the ambient background.
- App tiles use a **6-column internal grid** supporting three sizes:
  - `SMALL` (1 col span, 0.5×0.5 of regular) — grouped only, always in 2×2 blocks
  - `REGULAR` (2 col span, 1×1 baseline)
  - `WIDE` (4 col span, 2×1 — twice as wide, same height as regular)
- Default tile size is determined by the app's value score:
  - Score 8–10 → WIDE
  - Score 6–7 → REGULAR
  - Score < 6 → does not appear on home
- User may override any tile's size via long-press → size picker.
- Home should display approximately **15–20 apps maximum**, not the full assigned inventory.
- The rest of the assigned apps remain available in the industrial index / app menu.

#### The Focused 12 & News Triage
- **Focused 12:** Limit home screen app density to high-intent applications.
- **News Triage Strip:** Consolidate secondary/obligatory news sources (AP, Reuters, Ground) into a single shared "Triage Strip" to minimize screen real estate and reduce passive consumption.
- **Direct Manipulation:** Integrate "Clipboard Drop-Zone" next to the Gemini pill for instant screenshot sharing to development environments.

#### Home vs App Menu
- Each app is either **home-visible** or **app-menu-only**, independently of its bucket.
- Value scoring determines the default `isOnHome` state at first-boot seeding.
- User overrides persist across restarts.

#### Known home-visible apps (reference):
| Bucket | Home | App Menu Only |
|---|---|---|
| News | X | AP News, FT, Ground News, Reuters |
| Browsers | Brave, Chrome | — |
| AI | Claude, ChatGPT, Gemini, Consultant | Grok, Perplexity, Kimi, AI Studio |
| Social | Messenger, WhatsApp, Photos | — |
| Health | Strava, Training, Nike Run Club, Medicare | Runna, MedAdvisor, MyFitnessPal, Cyclers |
| Security | CommBank, ING Banking | Authenticator ×2, myID, Service NSW |
| Utilities | Phone, Messages, Gmail, Maps, TripView, Notes | Amazon, Skymee → unassigned |

### 3. App Value Scoring
- On first boot (or when "Re-score" is triggered), each installed app is scored automatically
  using a keyword/package-name heuristic (`AppValueScorer`).
- Score = REACH × FREQUENCY, both on a 1–3 scale. Threshold for home placement = 6.
- Score determines default: `isOnHome`, tile size, and bucket assignment suggestion.
- `PACKAGE_USAGE_STATS` permission may be requested optionally to improve accuracy.
  If granted, real open-frequency data replaces the heuristic frequency estimate.
- User overrides (bucket, home visibility, tile size) always win over computed scores.

### 4. Secondary Navigation
- Swiping left from home reveals an industrial text-led index page.
- Index page exposes section jumps, app assignment/editing, and a settings entry point.
- No persistent bottom apps bar.
- Index section heading uses type-bleed to signal swipeability (partially visible adjacent
  section title at the right screen edge).

### 5. Ambient Features
- Live weather in the home header (Open-Meteo, location-based).
- RSS project card with curated sources and local cache.
- Battery action card: shows `{pct}%` as title and `{h}h {m}m` (or "charging") as subtitle.
  - Total estimated discharge: 29h 35m (29.583h) at 100%.
  - Countdown resets when charger is removed: `remainingHours = 29.583 × (pct / resetPct)`.
  - `resetPct` = percentage at the moment the charger was disconnected.
  - Battery monitoring via `BroadcastReceiver` registered in `DashboardViewModel`, not inline
    in composables.
  - **Interaction:** Tapping the battery readout opens the system's Battery Usage Summary screen.
- Wallet action card: taps through to Google Wallet or the primary wallet app.
- Both action cards are `aspectRatio(2f/3f)` — 50% taller than wide.
- Time-based theme switching: EARLY_MORNING (5–7:30), DAY (7:30–17:30),
  BLUE_HOUR (17:30–20:00), LATE_NIGHT (20:00–5:00).
- System wallpaper as base visual layer (`windowShowWallpaper=true`).

### 6. Forecasting & Time Horizon
- **Life & Forecast Timeline:** Visualize the user's life arc (birth to expected life years) integrated with global technological forecasts (AGI, Singularity).
- **Universal Scale:** A secondary timeline contextualizing the human era within the cosmic scale (Big Bang to Heat Death).
- **Forecast Observation Deck:** An interactive slide-out panel accessible by tapping the timelines.
  - Aggregates probabilistic data from Metaculus (Community Predictions) and Manifold (Market Odds).
  - Provides AI-generated "Reality Sync" context, translating global forecasts into personal impact.
- **Visual Presentation:** Use clean, vertical leader lines to associate labels with timeline points. Stagger labels and use multi-line text for high-density forecast clusters.

### 7. Deferred Feature Areas
- Social/message recents are deferred to a future pass.
- Widget hosting is deferred to a future pass.
- Launcher-native notes and Samsung Notes integration are deferred.
- AI backend integrations beyond the current Gemini path are deferred.
- Media now-playing strip is planned but deferred.
- Static location map is planned but deferred.

---

## Design Philosophy: HCI Principles for Ambient UI
- **Bridging the Gulf of Execution:** Actions (weather, device control) must be live, card-based outcomes rather than app launches.
- **Signifiers over Affordances:** Ensure tiles act as "Status Indicators" (e.g., showing unread counts or balances) rather than static icon buttons.
- **Intent-Based Outcome Specification:** The Gemini Pill is the central control surface, capable of morphing from input to status-indicator based on system events (timers, workouts).
- **Battery Efficiency:** No live blur, no unnecessary continuous rendering, minimal background work.
- **Typography Consistency:** Use `Syne` for display (D1–D2) and `Inter` for body/labels (T1–T3) only.
  Do not introduce a third typeface without updating the typography rules first.
- **Performance:** Home scrolling, tile grid rendering, and library filtering must remain smooth.
  No synchronous loading on the main thread (icons, network, battery reads).
- **Config Persistence:** Bucket assignment, home visibility, tile size, section ordering, and
  battery `resetPct` must survive app restarts via SharedPreferences (DataStore migration planned).
- **Tile Visibility:** Dark mode tiles must be visually distinct from the wallpaper scrim.
  Target contrast ≥ 2.2:1 between tile surface and scrim background.
