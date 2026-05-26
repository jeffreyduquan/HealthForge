package de.healthforge.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import de.healthforge.R

// =============================================================================
// HealthForge Typography — P6.S2 Histamind-Fusion (LOCKED 2026-05-26)
// Source: docs/HistamindDesignReference.md §3
// Font-Family: Manrope (downloadable via Google Fonts provider).
// Supersedes: Roboto-default scale from GUI.md §3.1 (v0.1 LOCKED 2025-05-25).
// =============================================================================

private val manropeProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private val manropeFont = GoogleFont("Manrope")

val ManropeFamily = FontFamily(
    Font(googleFont = manropeFont, fontProvider = manropeProvider, weight = FontWeight.W400),
    Font(googleFont = manropeFont, fontProvider = manropeProvider, weight = FontWeight.W500),
    Font(googleFont = manropeFont, fontProvider = manropeProvider, weight = FontWeight.W600),
    Font(googleFont = manropeFont, fontProvider = manropeProvider, weight = FontWeight.W700),
    Font(googleFont = manropeFont, fontProvider = manropeProvider, weight = FontWeight.W800),
)

// Histamind Type-Scale (12 styles) — sizes/weights/letterSpacing/lineHeight per §3.
val HealthForgeTypography = Typography(
    displayLarge   = TextStyle(fontFamily = ManropeFamily, fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.8).sp),
    displayMedium  = TextStyle(fontFamily = ManropeFamily, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.6).sp),
    displaySmall   = TextStyle(fontFamily = ManropeFamily, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.4).sp),
    headlineLarge  = TextStyle(fontFamily = ManropeFamily, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = ManropeFamily, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontFamily = ManropeFamily, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.2).sp),
    titleLarge     = TextStyle(fontFamily = ManropeFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.W600),
    titleMedium    = TextStyle(fontFamily = ManropeFamily, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.W600),
    titleSmall     = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.W600),
    bodyLarge      = TextStyle(fontFamily = ManropeFamily, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W400),
    bodyMedium     = TextStyle(fontFamily = ManropeFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W400),
    bodySmall      = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.W400),
    labelLarge     = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.3.sp),
    labelMedium    = TextStyle(fontFamily = ManropeFamily, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.W500, letterSpacing = 0.4.sp),
    labelSmall     = TextStyle(fontFamily = ManropeFamily, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.W500, letterSpacing = 0.6.sp),
)
