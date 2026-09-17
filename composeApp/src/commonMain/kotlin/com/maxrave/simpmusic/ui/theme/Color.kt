package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Brand =====

/**
 * Brand seed color: the Orinify violet. The whole Material 3 ColorScheme is generated from this
 * color at runtime — see [AppTheme] — and it is also used literally at ~50 call sites as the app
 * accent (Analytics figures and charts, the Apple Music header subtitle, selected rows and chips).
 * Those two jobs are what fixes the value, and they read different parts of it.
 *
 * It is the hue of the launcher icon's gradient end (`#7C3AED` in `ic_launcher_background.xml`,
 * HCT hue 301.9), lifted to tone 64 at the highest chroma sRGB holds there.
 *
 * Why not `#7C3AED` itself:
 *  - The generated scheme would be identical. `PaletteStyle.TonalSpot` builds its primary palette
 *    as `TonalPalette.fromHueAndChroma(seedHue, 36.0)` and takes `primary` at tone 80 (dark) /
 *    40 (light), so it reads the seed's HUE and discards its chroma and tone. `#7C3AED` and this
 *    value both yield `primary = #D2BCFD` dark / `#67548E` light, byte for byte; the gradient's
 *    other stop `#3B1E8A` is 5° off in hue and yields `#CCBDFF`, the same colour to the eye.
 *  - The literal uses would not be identical. `#7C3AED` is HCT tone 43: 3.7:1 against the AMOLED
 *    black this app pins `background`/`surface` to, below WCAG AA. At tone 64 the same hue gives
 *    7.6:1 — AAA — close to the 11.7:1 the old blue seed gave, so no accent site loses legibility.
 *
 * Light theme is unaffected by the lift for the same reason: `primary` resolves to `#67548E`,
 * 6.2:1 on the `#FAFAFA` page background, matching what the previous seed produced.
 */
val seed = Color(0xFFAD85FD)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF4081)

/** Currently playing lyric line. */
val lyricActiveColor = Color(0xFFFFFF00)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

// Light-theme counterparts of the shimmer tokens.
val shimmerBackgroundLight = Color(0x7EDCD8D8)
val shimmerLineLight = Color(0xFFCFC8C8)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Desktop shell =====
// Spotify-style layering for the desktop window: the window itself takes the extreme, panels
// step one shade back towards the middle so they read as raised. The light panel is not here —
// it comes from colorScheme.surfaceContainer, which already sits at the right distance.

val desktopWindowDark = Color(0xFF000000)
val desktopWindowLight = Color(0xFFFFFFFF)
val desktopPanelDark = Color(0xFF121212)

// ===== Desktop window controls =====
// The macOS traffic lights. Fixed by convention rather than by theme — users read these by
// colour, so they stay the same in light and dark.

val windowCloseButton = Color(0xFFFF605C)
val windowCloseButtonHover = Color(0xFFE54942)
val windowMinimiseButton = Color(0xFFFFBD44)
val windowMinimiseButtonHover = Color(0xFFE5A93D)
val windowMaximiseButton = Color(0xFF00CA4E)
val windowMaximiseButtonHover = Color(0xFF00B344)

// ===== Legacy — do not add new usages =====

/**
 * Old M3 primary (lavender). Kept only for the SettingScreen storage bar,
 * which stays untouched by owner's decision.
 */
@Deprecated("Legacy storage bar color only — use MaterialTheme.colorScheme.primary in new code")
val md_theme_dark_primary = Color(0xFFB2C5FF)
