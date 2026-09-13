package com.kaasu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// The Material scheme is derived from the brand palette rather than maintained beside it, so a
// palette swap moves every surface in the app instead of half of them.
private fun schemeFrom(p: KaasuPalette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary              = p.income,
        onPrimary            = Color(0xFF002118),
        primaryContainer     = p.forest,
        onPrimaryContainer   = p.onForest,
        secondary            = p.subtle,
        onSecondary          = p.background,
        secondaryContainer   = p.surfaceAlt,
        onSecondaryContainer = p.ink,
        background           = p.background,
        onBackground         = p.ink,
        surface              = p.surface,
        onSurface            = p.ink,
        surfaceVariant       = p.divider,
        onSurfaceVariant     = p.muted,
        outline              = p.border,
        error                = p.expense,
        onError              = Color(0xFF500000),
    )
} else {
    lightColorScheme(
        primary              = p.forest,
        onPrimary            = p.onForest,
        primaryContainer     = p.surfaceAlt,
        onPrimaryContainer   = p.forest,
        secondary            = p.muted,
        onSecondary          = Color.White,
        secondaryContainer   = p.surfaceAlt,
        onSecondaryContainer = p.ink,
        background           = p.background,
        onBackground         = p.ink,
        surface              = p.surface,
        onSurface            = p.ink,
        surfaceVariant       = p.divider,
        onSurfaceVariant     = p.subtle,
        outline              = p.border,
        error                = p.expense,
        onError              = Color.White,
        errorContainer       = Color(0xFFFFF0F0),
        onErrorContainer     = p.expense,
    )
}

@Composable
fun KaasuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use the branded palette — dynamic color overwrites the premium teal design
    val kaasuPalette = if (darkTheme) DarkKaasuPalette else LightKaasuPalette
    val colorScheme = schemeFrom(kaasuPalette, darkTheme)

    // The brand palette rides alongside the Material scheme rather than inside it, so screens can
    // read `KaasuColors.ink` and follow the theme without every color having to fit a Material role.
    CompositionLocalProvider(LocalKaasuPalette provides kaasuPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = KaasuTypography,
            content     = content
        )
    }
}
