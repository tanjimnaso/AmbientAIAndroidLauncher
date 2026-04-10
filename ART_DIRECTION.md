# Art Direction

## The Vibe
Calm ambient launcher meets editorial grouping, with a secondary industrial navigation layer.

The project is no longer aiming for a generic "sci-fi dashboard". The current visual language is:
- dark ocean wallpaper and scrims
- variable-size rounded tile geometry on home, where size signals importance
- sparse grouping and deliberate whitespace without card-within-card nesting
- a separate text-only, thin-line industrial page for structure and navigation

---

## Core Visual Rules

### 1. Typography

**Font pairing: `Syne` + `Inter`**
- `Syne` replaces `DM Serif Display` for all display and ambient moments.
  Syne is a geometric sans-serif display face with architectural character — it harmonizes
  with Inter (same geometric foundation) rather than contrasting against it, producing a
  more cohesive system across both the ambient home and the industrial index.
- `Inter` remains for body, labels, settings, and utility text. Do not introduce a third typeface.

**Five-level type hierarchy:**

| Level | Role | Size | Weight | Face | Usage |
|---|---|---|---|---|---|
| D1 | Ambient anchor | 44–52sp | Light 300 | Syne | Day name, time display |
| D2 | Section / card title | 28–34sp | Regular 400 | Syne | Index section names, card headings |
| T1 | Navigation label | 16–18sp | SemiBold 600 | Inter | Tile labels, index sub-labels |
| T2 | Body | 14–15sp | Regular 400 | Inter | Quote, card body, metadata |
| T3 | Micro / caption | 11–12sp | Regular 400 | Inter | Index number prefix (01, 02…), tile sublabels |

**Critical ratio:** D1 must be at least 3× the size of T3. The current industrial index uses
`indexLabel` at ~14sp and section `title` at ~22sp — a 1.6× ratio that reads as two columns,
not as label + headline. Target is closer to 3:1 (12sp prefix, 34sp section name).

**Type bleeding:**
Type may intentionally extend beyond the container edge using:
- `TextOverflow.Visible` + `softWrap = false` on single-line display text
- `Modifier.graphicsLayer { clip = false }` on the parent container
- The existing `horizontalBleed()` modifier for full-bleed rows

This is used to signal swipeability on the industrial index page (partially visible adjacent
section title bleeds from the right edge, echoing the Windows Phone Pivot pattern).

### 2. Color

- Home page stays ambient, cool, and wallpaper-led.
- Late night is deep ocean — not pure OLED black.
- Small high-brightness accents are acceptable; large white surfaces are not.
- The industrial page is pale/paper-toned with thin black rules.

**Dark mode tile luminance (critical fix):**
The three dark palettes (EARLY_MORNING, BLUE_HOUR, LATE_NIGHT) currently use
`tileBackground = #181C20` (luminance ≈ 0.009) against a wallpaper scrim at approximately
`#0D1A26` (luminance ≈ 0.006). The contrast ratio is ~1.05:1 — effectively invisible.

Target tile color: `#3E5268` (blue-cast, harmonizes with ocean wallpaper, luminance ≈ 0.066,
contrast ratio ≈ 2.2:1 against the scrim). A 1dp border at `rgba(255,255,255,0.10)` may be
added to tile edges as a depth highlight. DAY mode tiles (`#F2F0E9`) are already well-contrasted
and do not need adjustment.

Material 3 principle applied: in dark mode, higher-elevation surfaces are *lighter*, not just
shadowed. Tiles float above the wallpaper — they must be perceptibly lighter than the scrim.

### 3. Shapes

- Home uses launcher-owned rounded tile geometry and soft ambient surfaces.
- Three tile sizes exist: SMALL (0.5×0.5), REGULAR (1×1), WIDE (2×1).
  Size signals importance — high-value apps default to WIDE, standard apps to REGULAR.
- Text-only rails and the industrial page use thin borders and flatter structure.
- App icons sit inside the tile system; the icon itself does not define the shape.
- Icons are contained by launcher geometry: consistent inset, slightly smaller than the tile
  surface, so OEM icon shapes do not visually dominate.

### 4. Composition

- Group by intent, not by default Android launcher convention.
- **No section headers on home.** The home page is a flat, silent grid. The industrial index
  provides the textual structure.
- **No cards inside cards.** App sections float directly on the ambient background.
- Variable tile sizes break up the grid visually, creating focal points and natural eye movement.
  A row of all REGULAR tiles is monotonous — WIDE tiles anchor rows and guide the eye.
- Home shows approximately 15–20 apps maximum, determined by value scoring.
  The full app inventory lives in the industrial index layer, not on home.
- SMALL tiles only appear grouped: pairs of 2 side-by-side, stacked 2 deep, occupying
  one REGULAR tile slot. They are never mixed with REGULAR tiles in the same row.

### 5. Navigation

- Home is scroll-first.
- Left page is index-first.
- Avoid a persistent bottom launcher bar.
- Home keeps only the ~15–20 launch surfaces worth immediate reach.
- The rest of the app inventory belongs in the industrial/index layer.
- The industrial index page uses the type-bleed technique for its section heading row to
  signal the swipe gesture spatially, without chevrons or arrows.

---

## App Value Principle

Not all apps earn a home screen tile. Home is a curated edit, not an inventory.

An app earns home placement by scoring ≥ 6 on the REACH × FREQUENCY matrix:

```
REACH:      Reflex (3) — opened without thinking
            Browse (2) — opened with mild intent
            Lookup (1) — opened to find something specific

FREQUENCY:  Daily  (3)
            Weekly (2)
            Rarely (1)

Score = REACH × FREQUENCY. Threshold for home = 6.
```

High-value apps (score 8–10) default to WIDE tiles. Standard home apps (6–7) default to
REGULAR. Apps below 6 go to the industrial index only. Users can override any assignment
via long-press on the tile.

---

## Reference Mapping

- **Industrial menu references (mb8, mb9):** Inform the left-swipe index page. Index number
  prefix should be dramatically smaller than the section name (~3:1 ratio).
- **Editorial card reference (mb14 "Your Projects"):** Content clusters at top of card; the
  lower half is breathing room, not a second label zone. Metadata sits adjacent to the title.
- **Minimal clock reference (mb15):** Single dominant typographic element. "Friday" owns the
  screen. Everything else recedes.
- **Interest tag reference (mb11):** Informs text-led pill rails and lighter navigation.
