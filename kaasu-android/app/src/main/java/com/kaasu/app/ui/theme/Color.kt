package com.kaasu.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Kaasu's brand palette, resolved per theme.
 *
 * Screens used to reference the raw light-mode constants directly (`KaasuInk`, `KaasuBorder`, …).
 * That made the dark color scheme unreachable: near-black ink and warm cream borders were painted
 * on a dark background regardless of theme, so dark mode was defined but visually broken. Colors
 * are now read through [KaasuColors], which resolves against whichever palette the theme provided.
 *
 * These are deliberately NOT folded into `MaterialTheme.colorScheme`. The brand green and the
 * income/expense semantics carry meaning that the Material roles do not express — mapping "forest"
 * onto `primary` and "expense" onto `error` would lose the distinction between a branded surface
 * and an error state.
 */
@Immutable
data class KaasuPalette(
    // Brand
    val forest: Color,
    val onForest: Color,
    // Text
    val ink: Color,
    val muted: Color,
    val subtle: Color,
    // Structure
    val background: Color,
    val surface: Color,
    val border: Color,
    val divider: Color,
    val surfaceAlt: Color,
    // Semantic — money direction
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val amber: Color,
    val amberBg: Color,
)

// ── Light: the original "premium teal" palette, unchanged ─────────────────────

val LightKaasuPalette = KaasuPalette(
    forest     = Color(0xFF0F4A37),
    onForest   = Color(0xFFF5EFD9),
    ink        = Color(0xFF19211D),
    muted      = Color(0xFF8A8E7F),
    subtle     = Color(0xFF6C6F5A),
    background = Color(0xFFF5EFD9),
    surface    = Color(0xFFFFFFFF),
    border     = Color(0xFFECE3C4),
    divider    = Color(0xFFF1EACD),
    surfaceAlt = Color(0xFFE3DECC),
    income     = Color(0xFF1F8458),
    expense    = Color(0xFFB54040),
    transfer   = Color(0xFF1D4A7A),
    amber      = Color(0xFFA36B00),
    amberBg    = Color(0xFFFDECD1),
)

// ── Dark ──────────────────────────────────────────────────────────────────────
// Roles are mirrored, not inverted: `forest` stays the branded surface color but lightens enough
// to separate from the background, and `onForest` stays the text that sits on it. The money
// semantics brighten, because the light-mode income green and expense red are too dark to read as
// accents against a dark surface.

/**
 * Near-black. Deepest background, highest text contrast, and the branded green lifted enough to
 * read as a raised surface against it. Best for glanceable amounts and kindest to OLED panels.
 */
val NearBlackKaasuPalette = KaasuPalette(
    forest     = Color(0xFF1B5E45),
    onForest   = Color(0xFFEAF3ED),
    ink        = Color(0xFFF2F5F3),
    muted      = Color(0xFF8B9992),
    subtle     = Color(0xFFA9B8B1),
    background = Color(0xFF0B0F0D),
    surface    = Color(0xFF151A17),
    border     = Color(0xFF232B27),
    divider    = Color(0xFF1D241F),
    surfaceAlt = Color(0xFF1F2723),
    income     = Color(0xFF5FDCA0),
    expense    = Color(0xFFEF8080),
    transfer   = Color(0xFF7FB3E8),
    amber      = Color(0xFFE0A644),
    amberBg    = Color(0xFF3A2E18),
)

/**
 * Charcoal. The same structure on warmer, lifted grey-green surfaces — softer at night and closer
 * to the dark values Kaasu already shipped, at the cost of some contrast on the amounts.
 */
val CharcoalKaasuPalette = KaasuPalette(
    forest     = Color(0xFF1B5E45),
    onForest   = Color(0xFFEAF3ED),
    ink        = Color(0xFFE2EBE5),
    muted      = Color(0xFF93A39A),
    subtle     = Color(0xFFADBFB7),
    background = Color(0xFF141E1B),
    surface    = Color(0xFF1E2922),
    border     = Color(0xFF2F3D38),
    divider    = Color(0xFF27332D),
    surfaceAlt = Color(0xFF2A3830),
    income     = Color(0xFF5FDCA0),
    expense    = Color(0xFFEF8080),
    transfer   = Color(0xFF7FB3E8),
    amber      = Color(0xFFE0A644),
    amberBg    = Color(0xFF3A2E18),
)

/**
 * The dark palette in force. Swapping this one line changes every dark surface in the app — the
 * point of routing all colors through [KaasuColors] in the first place.
 */
val DarkKaasuPalette = NearBlackKaasuPalette

val LocalKaasuPalette = staticCompositionLocalOf { LightKaasuPalette }

/**
 * Theme-aware accessor for the brand palette: `KaasuColors.ink` instead of the old `KaasuInk`.
 *
 * Every property is `@Composable` on purpose — that is what makes the value follow the active
 * theme. A non-composable call site cannot read these, which is the intended constraint: it is
 * exactly the kind of call site that used to hardcode the light value.
 */
object KaasuColors {
    val forest: Color     @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.forest
    val onForest: Color   @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.onForest
    val ink: Color        @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.ink
    val muted: Color      @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.muted
    val subtle: Color     @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.subtle
    val background: Color @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.background
    val surface: Color    @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.surface
    val border: Color     @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.border
    val divider: Color    @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.divider
    val surfaceAlt: Color @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.surfaceAlt
    val income: Color     @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.income
    val expense: Color    @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.expense
    val transfer: Color   @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.transfer
    val amber: Color      @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.amber
    val amberBg: Color    @Composable @ReadOnlyComposable get() = LocalKaasuPalette.current.amberBg
}
