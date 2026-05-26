package de.healthforge.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic custom tokens beyond Material 3 (GUI.md §2.3 + HistamindDesignReference.md §2).
 * Domain-specific colors for ratings, macros, water, symptoms — and status tokens
 * (overUl/relax/good) added in P6.S2.
 *
 * Provided via [LocalSemanticColors] CompositionLocal — read in screens via
 * `LocalSemanticColors.current.macroProtein` etc.
 */
data class SemanticColors(
    val ratingRecommend: Color,
    val ratingNotRecommend: Color,
    val ratingMoreOften: Color,
    val ratingIntolerant: Color,
    val macroProtein: Color,
    val macroCarbs: Color,
    val macroFat: Color,
    val macroCalories: Color,
    val water: Color,
    val symptomSeverity1: Color,
    val symptomSeverity5: Color,
    // P6.S2 — Histamind Status tokens (Severity-Bar, NRP-Card, Log).
    val statusOverUl: Color,
    val statusRelax: Color,
    val statusGood: Color,
)

val LightSemanticColors = SemanticColors(
    ratingRecommend    = StatusGood,
    ratingNotRecommend = StatusOverUl,
    ratingMoreOften    = AmbientViolet,
    ratingIntolerant   = StatusOverUl,
    macroProtein       = AmbientViolet,
    macroCarbs         = StatusRelax,
    macroFat           = Color(0xFFFFD54F),
    macroCalories      = AmbientViolet,
    water              = AmbientCyan,
    symptomSeverity1   = StatusGood,
    symptomSeverity5   = StatusOverUl,
    statusOverUl       = StatusOverUl,
    statusRelax        = StatusRelax,
    statusGood         = StatusGood,
)

val DarkSemanticColors = SemanticColors(
    ratingRecommend    = StatusGood,
    ratingNotRecommend = StatusOverUl,
    ratingMoreOften    = AmbientViolet,
    ratingIntolerant   = StatusOverUl,
    macroProtein       = AmbientViolet,
    macroCarbs         = StatusRelax,
    macroFat           = Color(0xFFFFD54F),
    macroCalories      = AmbientViolet,
    water              = AmbientCyan,
    symptomSeverity1   = StatusGood,
    symptomSeverity5   = StatusOverUl,
    statusOverUl       = StatusOverUl,
    statusRelax        = StatusRelax,
    statusGood         = StatusGood,
)
