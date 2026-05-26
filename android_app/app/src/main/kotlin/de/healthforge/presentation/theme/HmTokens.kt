package de.healthforge.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Hm-Tokens — design-system extension beyond Material 3 ColorScheme.
 * Source: docs/HistamindDesignReference.md §2, §4, §5.
 *
 * Provided per-theme via [LocalHmTokens] CompositionLocal in [HealthForgeTheme].
 * Differentiates Dark (Hero, Glass-Path) from Light (Clean-Path, no glass).
 */
data class HmTokens(
    /** Page background color (below ambient backdrop). */
    val background: Color,
    /** Solid card surface color (used when glass disabled). */
    val cardSurface: Color,
    /** Top stop for glass fill linear-gradient (top→bottom). */
    val glassFillTop: Color,
    /** Bottom stop for glass fill linear-gradient. */
    val glassFillBottom: Color,
    /** Glass border (1dp hairline). */
    val glassBorder: Color,
    /** Foreground text — primary (Manrope w700/w600 surfaces). */
    val fgPrimary: Color,
    /** Foreground text — secondary (Manrope w400/w500 surfaces). */
    val fgSecondary: Color,
    /** Foreground text — tertiary (timestamps, captions, micro-meta). */
    val fgTertiary: Color,
    /** Ambient backdrop violet stop (top of radial gradient). */
    val ambientViolet: Color,
    /** Ambient backdrop cyan stop (bottom of radial gradient). */
    val ambientCyan: Color,
    /** Violet glow for FAB + primary CTAs (drop-shadow). */
    val violetGlow: Color,
    /** Linear-gradient brush for primary accents (FAB, primary button, pill stripe). */
    val accentGradient: Brush,
    /** Whether glass effect should be rendered (dark = true, light = false). */
    val isGlassEnabled: Boolean,
)

val DarkHmTokens = HmTokens(
    background       = HmDarkBackground,
    cardSurface      = HmDarkCardSurface,
    glassFillTop     = HmDarkGlassFillTop,
    glassFillBottom  = HmDarkGlassFillBot,
    glassBorder      = HmDarkGlassBorder,
    fgPrimary        = HmDarkFgPrimary,
    fgSecondary      = HmDarkFgSecondary,
    fgTertiary       = HmDarkFgTertiary,
    ambientViolet    = AmbientViolet,
    ambientCyan      = AmbientCyan,
    violetGlow       = VioletGlow,
    accentGradient   = AccentGradient,
    isGlassEnabled   = true,
)

val LightHmTokens = HmTokens(
    background       = HmLightBackground,
    cardSurface      = HmLightCardSurface,
    glassFillTop     = Color(0x00FFFFFF),
    glassFillBottom  = Color(0x00FFFFFF),
    glassBorder      = HmLightGlassBorder,
    fgPrimary        = HmLightFgPrimary,
    fgSecondary      = HmLightFgSecondary,
    fgTertiary       = HmLightFgTertiary,
    ambientViolet    = AmbientViolet,
    ambientCyan      = AmbientCyan,
    violetGlow       = VioletGlow,
    accentGradient   = AccentGradient,
    isGlassEnabled   = false,
)

val LocalHmTokens = staticCompositionLocalOf<HmTokens> { DarkHmTokens }
