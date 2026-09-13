package com.kaasu.app.core.util

import androidx.compose.ui.graphics.Color

fun String.toComposeColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrDefault(Color.Gray)

fun Int.toColorHex(): String = String.format("#%06X", 0xFFFFFF and this)

val CATEGORY_COLORS = listOf(
    "#E53935", "#F57C00", "#F9A825", "#43A047",
    "#00ACC1", "#1E88E5", "#8E24AA", "#757575"
)
