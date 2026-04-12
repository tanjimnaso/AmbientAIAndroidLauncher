# Ambient Launcher

An information-first Android launcher that puts live news, AI briefings, and personal context where your wallpaper used to be. Tested on S22 Ultra, Android 16. Built in Kotlin and Jetpack Compose.

The design philosophy is calm density — high information, low visual noise. No app grid. No widgets. A vertical editorial stack you scroll through like a newspaper, then swipe left to an index of everything else.

## What it does

### Home screen
When you unlock your phone you see, top to bottom:

1. **Masthead** — current time, temperature, weather summary, and battery remaining (not percentage — actual hours left until empty, modelled from your real discharge rate).

2. **Recent apps strip** — your 6 most-used apps from the past 7 days, sorted by actual screen time. No icons buried in a grid.

3. **Week signal** — a short contextual line telling you where you are in the week/month.

4. **AI briefing** — a single sentence synthesising today's top headlines via Gemini. Tap it to open a structured 5-part deep-dive prompt (systemic explanation, historical parallel, economic analysis, probabilistic forecast) sent straight into the Gemini app.

5. **Live headlines** — a scrollable list of up to 30 stories, pulled from 9 curated sources: AP, Reuters, BBC World, BBC Technology, The Guardian, NPR, Politico, Ars Technica, and Hacker News. The feed filters out sports, opinion columns, and clickbait automatically. Stories about the same event from multiple sources are collapsed into one item. Nothing older than 48 hours appears. Tap any headline to read a clean reading-mode version in-app.

### App index (swipe left)
A text-only industrial index of all your apps, grouped into buckets you define: News, Utilities, Security, Social, Browsers, Wallet, AI, Health. No icons — just names in a tight list.

### To-do (swipe right on home)
A full-screen, gesture-driven task list. Swipe left to open, back button or swipe right to close. Tasks persist to a local file between sessions.

### App management
Long-press any app tile to edit it: assign it to a bucket, remove it from the home screen (it stays in the index), or change its tile size (Small / Regular / Wide). Changes persist locally.

## Technical notes

- **Weather** — Open-Meteo, free, no API key needed. Refreshes every 30 minutes.
- **AI briefing** — requires a `geminiApiKey` in `local.properties`. Without it, no briefing is generated.
- **RSS** — entirely client-side, no backend. Sources are configurable via the Today Settings screen.
- **No tracking, no accounts, no cloud sync.**

## Current Limitations
- **Notes** — the note input field is present but nothing is saved yet.
- **Media strip** — built but not connected to the home screen. Media playback controls are not visible.
- **Messaging/email recents** — not implemented; social apps launch normally.
- **Widget hosting** — all cards are launcher-native, not Android widgets.
- **Gemini requires local setup** — live AI briefing only works with a configured API key.

## Build Baseline
- Android Gradle Plugin `9.1.0`
- Gradle wrapper `9.3.1`
- `compileSdk` / `targetSdk` `36`
- Compose BOM `2026.02.01`

Build with:

```bash
./gradlew assembleDebug
```

## Documentation
- [Requirements](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/REQUIREMENTS.md)
- [Architecture](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/ARCHITECTURE.md)
- [Art Direction](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/ART_DIRECTION.md)
- [Best Practices](/Users/tanjimislam/AndroidStudioProjects/AmbientAIAndroidLauncher/BEST_PRACTICES.md)
