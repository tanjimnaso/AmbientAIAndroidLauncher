# Design Consumption Rules: Visual Language by Intent

Each section of the launcher targets different consumption patterns. Typography, spacing, and hierarchy signal the intended interaction depth.

---

## Section Consumption Patterns

### 1. **Masthead** (Weather + Time + Day)
**Consumption Intent:** Quick-glance, peripheral vision  
**Visual Language:** 
- H1 (40sp Syne Light) for day name and temperature — high contrast, dominance without shouting
- T2 (15sp Inter) for date and time — secondary hierarchy, visual anchor
- Minimal cognitive load, assumes eyes already on screen
- **Spacing:** Tight vertical grouping (0dp), horizontal separation by content (left/right split)
- **Color:** textPrimary with full opacity — trust the background wallpaper for contrast

---

### 2. **AI Briefing Summary** (Today's Signal)
**Consumption Intent:** Quick scanning + light reading  
**Visual Language:**
- D1 (52sp Syne Light) for briefing headline — dominant, powerful opener
- T2 (15sp Inter) for briefing body — readable, secondary emphasis
- Progress bar below labels (week counter) — ambient feedback, no interaction required
- **Spacing:** 4dp above label, 12dp below progress bar, 0dp label-to-body
- **Color:** textPrimary body, textSecondary labels with 0.6 alpha

---

### 3. **RSS Headlines** (News Articles)
**Consumption Intent:** Skimming + discovery  
**Visual Language:**
- T1 (20sp Inter SemiBold) for headlines — scannable, hierarchical
- T2 (15sp Inter) for source metadata — supporting context, not primary focus
- Card-like presentation (elevated surfaces) signals discrete items
- **Spacing:** 12dp between items (tight cluster = related content)
- **Color:** textPrimary headlines, textSecondary metadata with 0.7 alpha

---

### 4. **Forecasts** (Predictions + Long-term Context)
**Consumption Intent:** Deep reading + contemplation  
**Visual Language:**
- T2 (15sp Inter) for claim text — readable body copy, supports scanning
- T3 (12sp Inter) for metadata (confidence, year, source) — supporting detail, right-aligned for scannability
- Tighter spacing (16dp between items) — signals logical grouping (single forecast concept = claim + metadata)
- **Spacing:** 16dp label-to-first-item, 16dp between items
- **Separation:** 48dp (spacingXL) from RSS articles above — signals new content category
- **Color:** textPrimary claims, accentHigh source labels, textSecondary metadata

---

### 5. **Timeline Bars** (Life Arc + Cosmic Context)
**Consumption Intent:** Contemplative, aspirational, reference frame  
**Visual Language:**
- T3 (12sp Inter SemiBold) labels — authoritative, structured, minimal personality
- 0.6 alpha on labels — reduce cognitive weight, treat as reference not narrative
- Progress bar (0.05 alpha background, 0.5 alpha fill) — ambient feedback, unobtrusive
- **Spacing:** 8dp label-to-bar (tighter than forecast groups, implies visual unity)
- **Color:** textSecondary labels, accentHigh progress fill — match Today's Signal aesthetic for tonal harmony
- **Hierarchy:** Three stacked bars (Life → Millennial → Universal) = personal → human → cosmic

---

## Design Principles Applied

**1. Size as Primary Language (Windows Phone / Zune)**
- Bigger = more important (D1 > H1 > D2 > T1 > T2 > T3)
- No cards needed; spacing and color do the work

**2. Spacing as Intent Signal**
- **Tight spacing (0–8dp):** Elements belong together (masthead parts, label-to-bar)
- **Medium spacing (16–24dp):** Related but distinct items (forecast items, article items)
- **Large spacing (32–48dp):** New category or mindset shift (RSS → Forecasts, Articles → Forecasts)

**3. Color & Alpha as Cognitive Load Control**
- **Full opacity (textPrimary):** Demands attention, primary narrative
- **0.6 alpha (textSecondary):** Metadata, context, ambient information
- **0.05 alpha (progress bar background):** Structural reference, nearly invisible

**4. Typography Pairing Strategy**
- **Syne (400–300 weights):** Display, headers, anchors — warm, distinctive
- **Inter (600–400 weights):** Body, metadata, readable copy — neutral, scannable

---

## Consumption Flow on Masthead Home

1. **Glance (0.5s):** Masthead weather + time (H1 dominance)
2. **Scan (2–5s):** Today's Signal (D1 headline + progress bar context)
3. **Read (5–15s):** AI Briefing summary (T2 body text, coherent narrative)
4. **Skim (10–20s):** RSS headlines (T1 scanning, discrete items)
5. **Contemplate (30s+):** Forecasts (T2 claims + metadata) → Timelines (cosmic perspective)

Each section's typography, spacing, and color reinforce this consumption curve.
