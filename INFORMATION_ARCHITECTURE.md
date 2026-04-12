# Information Architecture: Top-Middle-Bottom System

## Three-Level Hierarchy

```
┌─────────────────────────────────────────────┐
│  LEVEL 0: THE INFINITE                      │
│  (Tetlock / Smil / Life Arc)                │
│  Visual: Horizon, progress bars             │
│  Temporal scope: Years to decades           │
│  Cognitive function: Context & meaning      │
└─────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────┐
│  LEVEL 1: THE STATE                         │
│  (Health / Super / Skills)                  │
│  Visual: Gauges, trajectory indicators      │
│  Temporal scope: Months (recent trend)      │
│  Cognitive function: Self-assessment        │
└─────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────┐
│  LEVEL 2: THE IMMEDIATE                     │
│  (News / Tasks / Apps)                      │
│  Visual: Grid, actionable items             │
│  Temporal scope: Days to weeks              │
│  Cognitive function: Task execution         │
└─────────────────────────────────────────────┘
```

---

## Level 0: The Infinite (✓ Implemented)

### TimeHorizonDashboard
**Purpose:** Establish long-term context. "Where are you in your life? What's the wider arc?"

**Components:**
- Year progress (27.6% through 2026)
- Seasonal context (Southern Hemisphere)
- Life progress arc (1993–2088, 40% complete)
- 12-month activity heatmap
- Future milestones (2029 AGI, 2035 Living Intelligence, 2053 Super, 2088 Genetic Horizon)

**Source data:** LocalDate.now(), hardcoded lifespan model

**Visual language:** Horizon line, progress bar (year/life), milestone pins on timeline

**Insight:** This layer answers "Why does today matter?" by showing where today sits on a 95-year arc. It's contemplative, not actionable.

---

## Level 1: The State (✗ Missing — PRIORITY)

## System Principles

### 1. Temporal Nesting
- **Level 0** views long-term trends (years, life arc)
- **Level 1** shows medium-term trajectory (weeks, months)
- **Level 2** surfaces immediate actions (days, hours)

### 2. Cognitive Flow
1. **Orient** (Level 0): "Where am I in the big picture?"
2. **Assess** (Level 1): "How am I progressing on what matters?"
3. **Execute** (Level 2): "What do I do today?"

### 3. Visual Distinctness
- **Level 0:** Horizon, arc, timeline — *contemplate*
- **Level 1:** Gauges, arcs, sparklines — *assess*
- **Level 2:** Grid, list, cards — *act*

Each level has its own visual language so users can quickly identify which layer they're looking at.

### 4. No Information Overload
- **Level 0:** ~5 key data points (year %, season, life %, heatmap, milestones)
- **Level 1:** ~4 gauges (health, leverage, skills, time) — collapsed by default, expand to detail
- **Level 2:** ~6 cards (briefing, focus, reading, media, calendar, apps)

---

## Design Language: Gauges for Level 1

### Gauge Anatomy (Material 3 Circular Progress)

```
┌────────────────┐
│   Health       │
│     76%        │
│   ████░░░      │  ← Arc indicator
│  ↑ +3% week    │  ← Trend sparkline
└────────────────┘
```

**Visual properties:**
- Arc color adaptive to ambient mode (accentHigh in dark, supporting color in light)
- Percentage centered (D2 or T1 size)
- Trend indicator (↑/→/↓ with small number)
- Optional: small sparkline showing 4-week trend

**Integration:**
- 2×2 grid of gauges (Health, Super, Skills, Time)
- Stacked vertically in compact form, or expandable
- Tap gauge to see detail breakdown + historical trend chart

---

## Implementation Roadmap

### Now
✓ Level 0: TimeHorizonDashboard (integrated)
✓ Level 2: TodayInfoCard, ProjectCards, MediaStrip, Calendar, Apps

### Next Sprint (Phase 1)
- [ ] Create **HealthGauge** component (MyFitnessPal + Garmin integration)
- [ ] Create **TimeAllocationGauge** (Calendar parsing)
- [ ] Create **StateLayerDashboard** wrapping both
- [ ] Insert StateLayerDashboard between Level 0 and Level 2 in HomePage

### Future (Phase 2)
- [ ] **SuperTrajectory** gauge (LinkedIn, GitHub, visibility metrics)
- [ ] **SkillDashboard** (time investment + learning progress)
- [ ] Collapsible detail sheets for each gauge
- [ ] Historical trend charts (tap gauge to expand)
- [ ] Notifications for anomalies ("Meetings exceeded baseline by 3h this week")

---

## Data Integration Points

| Level | Component | Data Source | Refresh |
|-------|-----------|-------------|---------|
| 0 | Year/Life Arc | LocalDate.now() | Real-time |
| 0 | Heatmap | LocalDate month tracking | Real-time |
| 0 | Milestones | Hardcoded list | Manual |
| **1** | **Health** | **MyFitnessPal API** | **Daily** |
| **1** | **Super** | **LinkedIn/GitHub APIs** | **Weekly** |
| **1** | **Skills** | **Time-tracking + calendar** | **Weekly** |
| **1** | **Time** | **Google Calendar API** | **Real-time** |
| 2 | Briefing | RSS feeds | Hourly |
| 2 | ProjectCards | Local state (LauncherConfiguration) | Real-time |
| 2 | Media | Spotify/Audible APIs | Real-time |
| 2 | Calendar | Google Calendar API | Real-time |
| 2 | Apps | System app inventory | On launch |

---

## Example HomePage Flow (Mature State)

```
HomeHeader (weather)
  ↓
Spacer (32dp)
  ↓
TimeHorizonDashboard ✓
  ├─ Year progress (27.6%)
  ├─ Seasonal context
  ├─ Life arc (1993–2088)
  ├─ 12-month heatmap
  └─ Milestones
  ↓
Spacer (40dp)
  ↓
StateLayerDashboard (Level 1) ← NEW
  ├─ Health Gauge (76% ↑)
  ├─ Super Gauge (64% ↑)
  ├─ Skills Gauge (62% →)
  └─ Time Allocation (summary bar)
  ↓
Spacer (40dp)
  ↓
TodayInfoCard (briefing)
  ↓
Spacer (24dp)
  ↓
ProjectCard (Focus)
ProjectCard (Reading)
  ↓
MediaStrip
CalendarPreview
  ↓
App Grid (curated 15–20)
```

This creates a **coherent narrative:** *"Here's where you are on your life arc. Here's how you're progressing. Here's what you do today."*

---

## Design Philosophy Alignment

This 3-level system reinforces ART_DIRECTION.md principles:

✓ **Calm, editorial:** Gauges are subtle (arc indicators, not overwhelming dashboards)  
✓ **Sparse, intentional:** ~4 metrics per layer, not dozens  
✓ **Hierarchical:** Level 0 dominates visually, Level 1 assessed, Level 2 actionable  
✓ **Syne + Inter:** Gauge labels use T3/T2 hierarchy; percentages use D1  
✓ **Dark ocean:** Gauge arcs use accentHigh, which adapts per ambient mode  
✓ **Variable sizing:** Gauges are compact containers; tap to expand for detail  
