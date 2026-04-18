# Information Architecture

## Implementation Status

- **Ambient Home:** Integrated (Pager Page 1)
- **Industrial Index:** Integrated (Pager Page 0)
- **Time Horizon:** Integrated as a section within Home

---

## Design Principles

### 1. Temporal Nesting
- **Big Picture:** Long-term trends (years, life arc)
- **Immediate:** Actionable items (days, hours)

### 2. Visual Distinctness
- **Ambient:** Horizon, arc, timeline — *contemplate*
- **Industrial:** Grid, list, cards — *act*

---

## Data Integration Points

| Component | Data Source | Refresh |
|-----------|-------------|---------|
| Year/Life Arc | LocalDate.now() | Real-time |
| Heatmap | LocalDate month tracking | Real-time |
| Milestones | Hardcoded list | Manual |
| Briefing | RSS feeds | Hourly |
| ProjectCards | Local state | Real-time |
| Calendar | Google Calendar API | Real-time |
| Apps | System app inventory | On launch |
