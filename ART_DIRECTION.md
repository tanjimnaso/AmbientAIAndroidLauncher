# Art Direction

The launcher is designed to be **quiet, literate, and slightly analogue** — closer to a well-laid newspaper than a phone home screen. The visual system is built from a small number of durable choices rather than a thick stylesheet.

---

## Four Ambient Modes

Palettes switch automatically based on a combination of **local time (primary)** and **ambient light (secondary refinement)**. The clock gates the palette window; lux only decides `DAYLIGHT_OUTDOOR` vs `DAY_INTERIOR_DIM` within the day window. Dusk and twilight windows ignore lux entirely. See `LightingSignals.kt` for the full fusion pipeline.

| Mode | Condition | Character |
|---|---|---|
| `DAYLIGHT_OUTDOOR` | 06:30–16:30, lux ≥ 800 (default) | Bright white background, charcoal text, high-chroma cluster hues. Readable in direct sun. |
| `DAY_INTERIOR_DIM` | 06:30–16:30, lux ≤ 250 | Cool neutral slate background, crisp bright text, softened hues at 75% alpha. Dim office / heavy overcast / basement. Chromatically *neutral* (not warm) so it reads as "still daytime" rather than sunset. |
| `DUSK` | 16:30–18:30 | Dusty plum-smoke background, muted iris accent, soft lavender-white text. |
| `TWILIGHT` | 18:30–06:30 | Deep burnt umber background, warm cream text, sienna-amber accent. Candle-warm. |

Palette transitions are animated over **1000ms** via `animateColorAsState` so the change is peripheral, not jarring.

### Core Hues (pre-palette-adjustment)

| Hue | Role | Base color |
|---|---|---|
| Intelligence | AI, news, reading | `#0EA5E9` sky |
| Utility | Tools, system | `#22C55E` green |
| Communication | Social, messaging | `#3B82F6` blue |
| Assistant | Launcher, shell | `#8B5CF6` violet |
| Health | Safety, errors | `#DC2626` red |

Each palette either uses these directly (DaylightOutdoor), pulls them to 75% alpha (DayInteriorDim), or substitutes muted versions tuned to the background (Dusk, Twilight).

### Monochrome Ink Mode

A single `AmbientSettings.monochrome` `StateFlow<Boolean>` toggle collapses all cluster hues into the palette's `inkColor`. Background, text, and accent stay — so the palette still feels seasonal, but all app-badge colour is gone. For reading-focused sessions.

---

## Typography

Two families, rigidly used:

- **Syne** — display only. Geometric, slightly eccentric. Used for the masthead time and section titles.
- **Inter** — everything else. Body, labels, captions, UI.

### The 5-Tier Scale

| Tier | Size | Family | Role |
|---|---|---|---|
| D1 | 44–52 sp, Light | Syne | Ambient anchor (masthead clock) |
| D2 | 28–34 sp, Regular | Syne | Section titles |
| T1 | 16–18 sp, SemiBold | Inter | Tile labels, primary body |
| T2 | 14–15 sp, Regular | Inter | Secondary body, metadata |
| T3 | 11–12 sp, Regular | Inter | Captions, index, micro-labels |

`ResponsiveTypography.kt` scales the base sizes by screen density class so tablets don't shrink to microtext.

### Rules
- **No bold unless structural.** Weight creates hierarchy at D1/D2/T1 only.
- **Letter-spacing favours openness.** Caps always carry +1.5sp to +1.8sp tracking.
- **Alpha over weight for de-emphasis.** Secondary text sits at 0.85–0.50 alpha; we don't reach for a lighter weight.
- **Ambient reveal relies on alpha, never size.** A hidden masthead line is 0.30 alpha, not shrunken.

---

## Motion

Motion is **slow and non-announcing**:

| Purpose | Duration | Curve |
|---|---|---|
| Palette transitions | 1000ms | tween |
| NowMoment crossfade | 800ms | tween |
| Ambient reveal in | 200ms | tween |
| Ambient reveal out | 400ms | tween |

**Nothing snaps.** No spring curves on primary surfaces. No wobble, no overshoot. Reveals start quickly but fade out slowly, which reads as "the system noticed but didn't interrupt you."

---

## Texture

A **4%-alpha tiled grain overlay** is painted over the whole launcher using `PorterDuff.OVERLAY` blend. The bitmap is a 192×192 deterministic noise pattern (seeded once, cached forever), tiled via `BitmapShader` with `Shader.TileMode.REPEAT`.

Why OVERLAY and not flat alpha: OVERLAY preserves the palette's identity — dark pixels stay dark, bright stay bright, only the mids pick up tooth. This keeps burnt umber looking like burnt umber instead of muddy grey.

The overlay has two jobs:
1. **Wabi-sabi tooth.** Pure digital surfaces feel clinical; a tiny amount of texture reads as paper, vellum, or dyed linen.
2. **OLED moire defeat.** On AMOLED at low brightness, flat colour bands can shimmer. 4% noise breaks the banding without being visible as noise.

---

## Layout Principles

### Off-grid by default
The main page is intentionally asymmetric. A row of 4 secondary apps sits **above-left**; a larger contextual hero sits **bottom-right**. The negative space is load-bearing — it's what makes the layout feel composed rather than populated.

### Editorial stack on the news page
Briefing → signal line → feed list, all one column, generous gutter, ragged-right body. Reads like a long-form paper.

### One thing at a time on the notes page
Full-screen canvas. Auto-hiding toolbar. Nothing else in view.

### Ambient reveal over persistent chrome
Secondary chrome (the masthead's date/battery sub-line; the notes toolbar) is hidden by default and summoned with a non-consuming tap. This is the cornerstone of the calm aesthetic — **the launcher should look empty until you ask something of it.**

---

## What we don't do

- **No neon accents.** Max chroma is palette-level; saturation gets cut hard at the cluster layer.
- **No dark mode / light mode toggle.** Palettes switch by lux + time. The user doesn't pick.
- **No shadows.** Elevation is conveyed by panel colour, not drop-shadow.
- **No widgets.** All cards are launcher-native Compose, not `AppWidgetHost`.
- **No icons in running text.** Glyphs belong in their own slot or they don't appear.
- **No progress bars, no spinners** except for genuinely async loads (Gemini analysis). Everything else reveals when ready.
