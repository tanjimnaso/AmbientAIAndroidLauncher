# Design Maturation

## Current State
The launcher now has two primary modes:
1. **Ambient Home** (scroll-first): curated app tiles, briefing cards, and temporal awareness.
2. **Industrial Index** (swipe-first): full app inventory organized in collapsible buckets.

## Maturation Strategy

### 1. Typography Hierarchy Audit

**Target alignment** with ART_DIRECTION.md 5-level hierarchy:

| Section | Target | Rationale |
|---------|--------|-----------|
| Year + percent | D1 (44–52sp Light) | Year progress deserves dominant display weight |
| Day Name | D1 (44–52sp Light) | Day name is the anchor |
| Labels | T3 (11–12sp) | Metadata recedes |

---

### 2. Composition: No Section Headers on Home

**Principle:** *No section headers on home.* The home page is a flat, silent grid. Visual grouping is achieved via spacing.

---

### 3. Implementation Checklist

- [x] **Typography:** Scale year progress + day name to 44–52sp Syne Light
- [x] **Headers:** Remove explicit section headers from home cards
- [x] **Spacing:** Use 40dp gaps between logical clusters
