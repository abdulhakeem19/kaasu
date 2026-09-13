package com.kaasu.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KaasuTypography = Typography(
    // Big amount display — ₹47,820 on dashboard card
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.5).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp
    ),
    // Screen / section titles
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    // Toolbar / top-level labels
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // Body copy
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    // Labels / chips / captions
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    ),
)
