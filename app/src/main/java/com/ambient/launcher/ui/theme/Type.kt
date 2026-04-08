package com.ambient.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ambient.launcher.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_thin, weight = FontWeight.Thin),
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_semibold, weight = FontWeight.Bold)
)

val DmSerifDisplayFamily = FontFamily(
    Font(R.font.dm_serif_display_regular, weight = FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DmSerifDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 86.sp,
        lineHeight = 82.sp,
        letterSpacing = (-2.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DmSerifDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSerifDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSerifDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.12.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.45.sp
    )
)
