package com.ambient.launcher.home

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ambient.launcher.ui.theme.InterFontFamily
import com.ambient.launcher.ui.theme.SyneFontFamily

/**
 * ResponsiveTypography
 *
 * Windows Phone / Zune–inspired typography system for slab phones (360–480dp width).
 * Optimized for 480p power-user narrow mode baseline. Respects Configuration.fontScale
 * (user accessibility settings) automatically via sp units.
 *
 * Five-tier hierarchy (ART_DIRECTION.md compliant):
 *  - D1: Dominant ambient anchors (news headline, time)
 *  - H1: Primary headers (masthead, section context)
 *  - D2: Secondary section dividers
 *  - T1: Content headlines (reads, items)
 *  - T2: Body & secondary text
 *  - T3: Captions & metadata
 */

internal object ResponsiveTypography {

    // ── D1: Dominant Headline (News Summary, Masthead Context) ─────────────
    // Syne Light, 52sp. Owns ~7–8% visual weight at 480p.
    // Usage: AI briefing headline, time display in hero context
    val d1 = TextStyle(
        fontFamily = SyneFontFamily,
        fontSize = 52.sp,
        fontWeight = FontWeight.Light,    // 300
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp
    )

    // ── H1: Primary Header (Masthead, Section Context) ────────────────────
    // Syne Light, 40sp. Provides warm, readable anchor without D1 dominance.
    // Usage: Day name, time (secondary), section grouping
    val h1 = TextStyle(
        fontFamily = SyneFontFamily,
        fontSize = 40.sp,
        fontWeight = FontWeight.Light,    // 300
        lineHeight = 44.sp,
        letterSpacing = (-0.3).sp
    )

    // ── D2: Secondary Section Header (Today's Signal, Forecast Label) ──────
    // Syne Regular, 32sp. Signals content boundary without shouting.
    // Usage: Index section labels, editorial grouping
    val d2 = TextStyle(
        fontFamily = SyneFontFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,   // 400
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp
    )

    // ── D3: Editorial Body (AI Briefing Headline, Insight Signal) ──────────
    // Syne Regular, 17sp. Compact editorial readability.
    // Usage: AI briefing headline, major signals
    val d3 = TextStyle(
        fontFamily = SyneFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,   // 400
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    )

    // ── T1: Content Headline (RSS Titles, Major Items) ────────────────────
    // Inter Regular, 17sp. Scannable, readable without the strain of bold weight.
    // Usage: Read titles, major item headers, card titles
    val t1 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,   // 400
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    )

    // ── T2: Body & Secondary Text (Forecast Details, Supporting Metadata) ──
    // Inter Regular, 15sp. Readable, secondary emphasis.
    // Usage: Forecast text, body copy, supporting details
    val t2 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,   // 400
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    // ── T3: Captions & Micro Labels (Index Numbers, Fine Details) ─────────
    // Inter Regular, 12sp. Metadata, numbers, secondary info.
    // Usage: Index labels (01, 02, 03), time sublabels, fine captions
    val t3 = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,   // 400
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    // ── T3 Numeric Variant (for index numbers, timestamps) ─────────────────
    // Mono-spaced friendly, same size/weight as T3
    val t3Numeric = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 1.sp  // Slightly wider for clarity on small numbers
    )
}
