# Design Maturation: Temporal Consciousness Layer

## Current State
The launcher now has three integrated layers:
1. **Ambient Home** (scroll-first): curated app tiles, briefing cards, media strips
2. **Time Horizon Dashboard** (newly integrated): temporal awareness, life progress, milestones
3. **Industrial Index** (swipe-first): full app inventory organized in collapsible buckets

## Maturation Strategy

### 1. TimeHorizonDashboard Type Hierarchy Audit

**Current state:** Mixes labelSmall (11sp) headings with various body sizes.

**Target alignment** with ART_DIRECTION.md 5-level hierarchy:

| Section | Current | Target | Rationale |
|---------|---------|--------|-----------|
| "2026" + percent | headlineLarge (28sp) | D1 (44–52sp Light) | Year progress deserves dominant display weight |
| "SATURDAY" | headlineMedium (32sp Light) | D1 (44–52sp Light) | Day name is the anchor — keep bold |
| Date/Week labels | labelSmall (11sp) | T3 (11–12sp) | Metadata, correct weight already |
| "12-Month Activity" | labelSmall (11sp) | T3 (11–12sp) | Section label, reduce emphasis |
| "Life Progress" | labelSmall (11sp) | T3 (11–12sp) | Section label, reduce emphasis |
| "Future Milestones" | labelSmall (11sp) | T3 (11–12sp) | Section label, reduce emphasis |
| Season badge | labelSmall (10sp Bold) | T3 (11–12sp SemiBold) | Shift from Bold to SemiBold for consistency |
| Age/context text | bodySmall (14sp) | T2 (14–15sp) | Body metadata, correct weight |

**Action:** Scale year percentage and day name to 44–52sp using Syne Light. This creates the "minimal clock reference" effect where temporal context owns the screen, and granular details recede.

---

### 2. Color Refinement: Night Mode Contrast Fix

**Current issue:** On LATE_NIGHT mode, tile and panel backgrounds may be too close to wallpaper scrim in luminance.

**Fix:** Apply ART_DIRECTION principle (Material 3: higher-elevation surfaces are lighter in dark mode):

- **TimeHorizonDashboard background** (if contained in a card):
  - Current: `drawerBackground` (approx. #181C20, luminance ~0.009)
  - Target: Use a 10% opacity white wash over the wallpaper, or apply `#3E5268` (2.2:1 contrast)
  - Or: Remove background entirely and let elements float on the wallpaper (preferred for ambient feel)

- **Section labels** (12-Month, Life Progress, etc.):
  - Current: `textSecondary` (mid-tone gray)
  - Target: `textSecondary.copy(alpha = 0.5f)` — reduce contrast further to signal "subordinate to main content"

- **Progress bars and visualizations:**
  - Accent colors (`accentHigh`, `accentHalo`) are already correct for dark mode
  - Ensure minimum 2:1 contrast for bars against wallpaper scrim

**Action:** Test all four ambient modes (EARLY_MORNING, DAY, BLUE_HOUR, LATE_NIGHT) with the revised colors.

---

### 3. Composition: No Section Headers on Home

**Current issue:** TimeHorizonDashboard has explicit section headings ("12-Month Activity", "Life Progress", "Future Milestones").

**ART_DIRECTION principle:** *No section headers on home.* The home page is a flat, silent grid. The industrial index provides textual structure.

**Solution - Recommended: Ultra-subtle labels**
- Reduce section label opacity to `textSecondary.copy(alpha = 0.3f)`
- Use thin divider lines: `textPrimary.copy(alpha = 0.05f)` (nearly invisible)
- Maintains visual clarity without "editorial section header" feel
- Keeps design ambient and contemplative

---

### 4. Variable Sizing & Composition

**Current state:** TimeHorizonDashboard is a monolithic column with equal visual weight throughout.

**Maturation:**
- Year progress (27.6% through 2026) → **focal point** (larger percentage, dominant typography)
- Day + season + week → **supporting context** (subdued, grouped tightly)
- Heatmap → **scannable visual** (bar chart, no label, minimal text)
- Life arc → **reflective element** (larger, invites contemplation)
- Milestones → **auxiliary detail** (consider collapse/expand)

**Spacing refinements:**
- Year header + progress bar: 8dp → 6dp (tighten focal point)
- Below heatmap: 32dp (increase breathing room)
- Above life arc: 40dp (signal section boundary)
- Between milestone items: 8–10dp (compact detail)

---

### 5. Seasonal Indicator Color Refinement

**Current colors:**
- Summer: #E8A860 (warm orange)
- Autumn: #C9834E (rust)
- Winter: #5A8FB5 (cool slate blue)
- Spring: #6BAE7F (green)

**Verify:** These work well against dark ocean wallpaper palette. Check that white text on badges maintains 4.5:1 contrast for accessibility. Consider thin border (`rgba(255,255,255,0.15f)`) for depth in dark modes.

---

### 6. Future Enhancements (Phase 2)

1. **Collapsible milestones section** — use same collapse pattern as industrial index
2. **Tap to expand milestones** — modal with Kurzweil context, countdown timer, reading list
3. **Calendar sync heatmap** — event density overlay on 12-month bars
4. **Custom milestones** — long-press to add personal life goals
5. **Smart collapse on small screens** — show only year progress + heatmap on devices < 6"

---

## Implementation Checklist

- [ ] **Typography:** Scale year progress + day name to 44–52sp Syne Light
- [ ] **Color:** Verify contrast ratios (2.2:1 minimum) on all four ambient modes
- [ ] **Headers:** Reduce section label opacity to 0.3f (ultra-subtle)
- [ ] **Spacing:** Adjust gaps per section (6dp year header, 40dp around life arc, 10dp milestones)
- [ ] **Season colors:** Test across all ambient modes for harmony with wallpaper
- [ ] **Documentation:** Update CLAUDE.md with TimeHorizonDashboard maintenance notes

---

## Design System Outcome

With these maturation steps:

✓ **Unified typography** — D1/D2/T1/T2/T3 hierarchy consistent across ambient + index  
✓ **Color consistency** — Dark mode surfaces properly elevated, 2.2:1+ contrast  
✓ **Composition clarity** — Flat grid with variable emphasis, no section headers  
✓ **Temporal awareness** — Integrated without dominating; supports contemplation  

This positions the launcher as a **calm, editorial, temporally-aware launcher** — contemplative and intentional, not frenetic or data-dense.
