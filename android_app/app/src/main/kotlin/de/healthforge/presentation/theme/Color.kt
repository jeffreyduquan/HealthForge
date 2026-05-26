package de.healthforge.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =============================================================================
// HealthForge Color Tokens — P6.S2 Histamind-Fusion (LOCKED 2026-05-26)
// Source: docs/HistamindDesignReference.md §2
// Supersedes: Olive-Green palette from GUI.md §2 (v0.1 LOCKED 2025-05-25).
// =============================================================================

// ----- Hm Accent (shared light + dark) -----
val AmbientViolet      = Color(0xFF7C5CFF)
val AmbientCyan        = Color(0xFF4DD0E1)
val VioletGlow         = Color(0x6E7C5CFF)  // ~43% alpha for FAB-Shadows

val StatusOverUl       = Color(0xFFFF5470)
val StatusRelax        = Color(0xFFFFB454)
val StatusGood         = Color(0xFF22D3A6)

/** Linear-Gradient for primary actions (FAB / Button-Background / Section-Pill-Strip). */
val AccentGradient = Brush.linearGradient(listOf(AmbientViolet, AmbientCyan))

// ----- DARK (Hero — Glass-Path) -----
val HmDarkBackground     = Color(0xFF070A12)
val HmDarkCardSurface    = Color(0xB3141A26)   // #141A26 @ 70% alpha
val HmDarkGlassFillTop   = Color(0x1FF5F7FA)   // white @ 12%
val HmDarkGlassFillBot   = Color(0x0AF5F7FA)   // white @ 4%
val HmDarkGlassBorder    = Color(0x1AFFFFFF)   // white @ 10%
val HmDarkFgPrimary      = Color(0xFFF5F7FA)
val HmDarkFgSecondary    = Color(0xCCF5F7FA)   // 80%
val HmDarkFgTertiary     = Color(0x80F5F7FA)   // 50%

// ----- LIGHT (Sekundär — Clean-Path, kein Glas) -----
val HmLightBackground    = Color(0xFFF4F5F8)
val HmLightCardSurface   = Color(0xFFFFFFFF)
val HmLightGlassBorder   = Color(0x141B1F26)   // #1B1F26 @ 8%
val HmLightFgPrimary     = Color(0xFF1B1F26)
val HmLightFgSecondary   = Color(0xB31B1F26)   // 70%
val HmLightFgTertiary    = Color(0x801B1F26)   // 50%

// =============================================================================
// Material-3 ColorScheme-Mappings (used by Theme.kt)
// =============================================================================
// We keep the LightX / DarkX naming for backwards-compat with Theme.kt API;
// values are remapped from Olive → Hm tokens.

// === LIGHT THEME ===
val LightPrimary              = AmbientViolet
val LightOnPrimary            = Color(0xFFFFFFFF)
val LightPrimaryContainer     = Color(0xFFE7E0FF)
val LightOnPrimaryContainer   = Color(0xFF1B0A6B)
val LightSecondary            = AmbientCyan
val LightOnSecondary          = Color(0xFF002E33)
val LightSecondaryContainer   = Color(0xFFCEF5FA)
val LightOnSecondaryContainer = Color(0xFF00363D)
val LightTertiary             = StatusGood
val LightOnTertiary           = Color(0xFF003827)
val LightTertiaryContainer    = Color(0xFFA8F0D2)
val LightOnTertiaryContainer  = Color(0xFF002111)
val LightError                = StatusOverUl
val LightOnError              = Color(0xFFFFFFFF)
val LightErrorContainer       = Color(0xFFFFDADC)
val LightOnErrorContainer     = Color(0xFF410008)
val LightBackground           = HmLightBackground
val LightOnBackground         = HmLightFgPrimary
val LightSurface              = HmLightCardSurface
val LightOnSurface            = HmLightFgPrimary
val LightSurfaceVariant       = Color(0xFFE3E4EA)
val LightOnSurfaceVariant     = Color(0xFF45474D)
val LightOutline              = HmLightGlassBorder
val LightOutlineVariant       = Color(0x0A1B1F26)

// === DARK THEME (Hero) ===
val DarkPrimary               = AmbientViolet
val DarkOnPrimary             = Color(0xFFFFFFFF)
val DarkPrimaryContainer      = Color(0xFF3B2C99)
val DarkOnPrimaryContainer    = Color(0xFFE7E0FF)
val DarkSecondary             = AmbientCyan
val DarkOnSecondary           = Color(0xFF00363D)
val DarkSecondaryContainer    = Color(0xFF00565F)
val DarkOnSecondaryContainer  = Color(0xFFCEF5FA)
val DarkTertiary              = StatusGood
val DarkOnTertiary            = Color(0xFF003827)
val DarkTertiaryContainer     = Color(0xFF005238)
val DarkOnTertiaryContainer   = Color(0xFFA8F0D2)
val DarkError                 = StatusOverUl
val DarkOnError               = Color(0xFF690014)
val DarkErrorContainer        = Color(0xFF8C001D)
val DarkOnErrorContainer      = Color(0xFFFFDADC)
val DarkBackground            = HmDarkBackground
val DarkOnBackground          = HmDarkFgPrimary
val DarkSurface               = HmDarkBackground       // Base surface = bg; glass renders ueber GlassCard
val DarkOnSurface             = HmDarkFgPrimary
val DarkSurfaceVariant        = Color(0xFF1A2030)
val DarkOnSurfaceVariant      = HmDarkFgSecondary
val DarkOutline               = HmDarkGlassBorder
val DarkOutlineVariant        = Color(0x0DFFFFFF)
