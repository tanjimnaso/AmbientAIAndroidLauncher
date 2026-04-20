# Ambient Launcher

A calm, information-first Android launcher. Built in Kotlin + Jetpack Compose, targeted at the Samsung S22 Ultra / Android 16.

The design philosophy is **quiet density** — news, weather, time-of-day, and a handful of chosen apps, presented as a single editorial surface. No app grid. No widgets. No notifications clamouring for attention. The launcher recedes until you look at it, then offers exactly one thing to land on.

---

## Three pages

A horizontal pager with three destinations. The main page is the default.

### ← News page
- **Briefing** — a single synthesised sentence from the day's top headlines, via Gemini. Tap it to open a structured deep-dive prompt (systemic / historical / economic / probabilistic) in the Gemini app.
- **Live headlines** — up to 30 stories from 9 curated sources (AP, Reuters, BBC World, BBC Tech, Guardian, NPR, Politico, Ars Technica, Hacker News). Sports, opinion, and clickbait are filtered. Same-event duplicates collapse. Nothing older than 48 hours.
- **Article viewer** — tap any headline for a clean reading-mode page with sentence-wrap TTS, tap-to-seek, and a bottom media control strip.
- **Analysis screen** — long-form generated analysis with sentence + word highlighting synced to TTS playback.

### · Main page (default)
An **asymmetric** app layout, intentionally off-grid:
- A **row of 4** secondary pinned apps sitting above-left
- A **contextual hero tile** bottom-right — larger, swaps by time of day (news/AI in the morning, social/utilities at midday, entertainment at evening, tools at night)
- A **"Now" moment** above the pinned row — one quiet line of text, rotating every 30 seconds between three sources:
  - **NOW** — top headline from the briefing pipeline
  - **TODAY** — a weather sentence
  - **DEEP TIME** — a hand-curated century-scale glance, keyed to the day (Halley's Comet in 2061, Andromeda collision in 4.5 billion years, Voyager 1 reaching Gliese 445, etc.)

Swipe up from the main page for the full app menu, grouped into 15 semantic buckets.

### → S-Pen notes page
A full-screen canvas. Pen/eraser, left/right-handed toggle. The toolbar auto-hides after 4 seconds — tap the top edge to bring it back.

---

## Calm design, intentional ambient touches

The launcher uses a small set of deliberate techniques to stay visually quiet:

- **Variant masthead with ambient reveal.** The time/date/weather header is drawn in two tiers. On the main page, the secondary line (date, season, battery hours) fades to 30% opacity until you tap the screen, then briefly pulses to full opacity for 3 seconds before receding again. Off the main page, it stays fully visible.
- **Grain overlay.** A 4%-alpha tiled noise texture painted over the whole launcher using `PorterDuff.OVERLAY` blend. Preserves palette identity while breaking up OLED moire — a wabi-sabi texture rather than flat colour.
- **Four ambient palettes**, selected by clock + lux + motion-gated camera sample:
  - `DAYLIGHT_OUTDOOR` — bright, cool (day, lux ≥ 800)
  - `DAY_INTERIOR_DIM` — cool neutral slate, bright text (day, lux ≤ 250)
  - `DUSK` — slate and amber
  - `TWILIGHT` — deep burnt umber with warm cream text
- **Monochrome ink toggle.** A single switch desaturates the entire launcher to black-on-paper, for reading-focused sessions.
- **Hairline typography.** Syne for display, Inter for body. Letter-spacing favours openness. No bold unless structural.
- **Ink-like motion.** Crossfades are 800ms; reveals are 200ms in / 400ms out. Nothing snaps.

---

## Other features

- **Weather** — Open-Meteo, no API key, refreshes every 30 minutes.
- **App management** — long-press any pinned app to reassign its bucket, remove it, or change its tile size. Changes persist to `SharedPreferences`.
- **No tracking, no accounts, no cloud sync.** Everything runs client-side.

---

## Setup

Add a Gemini API key to `local.properties` to enable the briefing and analysis pipeline:

```properties
geminiApiKey=YOUR_KEY_HERE
```

Without a key, the launcher still runs — just without AI summaries.

## Build

```bash
./gradlew assembleDebug
```

- Android Gradle Plugin `9.1.0`
- Gradle wrapper `9.3.1`
- `compileSdk` / `targetSdk` `36`
- Compose BOM `2026.02.01`

## Documentation

- [Architecture](ARCHITECTURE.md) — code structure and data flow
- [Art Direction](ART_DIRECTION.md) — palettes, typography, motion
- [App Organization](APP_ORGANIZATION.md) — bucket taxonomy
- [Best Practices](BEST_PRACTICES.md) — coding rules for contributors
- [Agents](AGENTS.md) — conventions for AI-assisted changes
- [System UI Guide](SYSTEM_UI_GUIDE.md) — status/nav bar handling
