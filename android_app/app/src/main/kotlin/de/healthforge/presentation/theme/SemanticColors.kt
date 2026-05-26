package de.healthforge.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic custom tokens beyond Material 3 (GUI.md §2.3).
 * Domain-specific colors for ratings, macros, water, symptoms.
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
)

val LightSemanticColors = SemanticColors(
    ratingRecommend    = Color(0xFF388E3C),
    ratingNotRecommend = Color(0xFFD32F2F),
    ratingMoreOften    = Color(0xFF4B6A1F),
    ratingIntolerant   = Color(0xFFBA1A1A),
    macroProtein       = Color(0xFF7E57C2),
    macroCarbs         = Color(0xFFFB8C00),
    macroFat           = Color(0xFFFFB300),
    macroCalories      = Color(0xFF4B6A1F),
    water              = Color(0xFF0288D1),
    symptomSeverity1   = Color(0xFFC8E6C9),
    symptomSeverity5   = Color(0xFFB71C1C),
)

val DarkSemanticColors = SemanticColors(
    ratingRecommend    = Color(0xFF81C784),
    ratingNotRecommend = Color(0xFFEF9A9A),
    ratingMoreOften    = Color(0xFFB0D17F),
    ratingIntolerant   = Color(0xFFFFB4AB),
    macroProtein       = Color(0xFFB39DDB),
    macroCarbs         = Color(0xFFFFB74D),
    macroFat           = Color(0xFFFFD54F),
    macroCalories      = Color(0xFFB0D17F),
    water              = Color(0xFF4FC3F7),
    symptomSeverity1   = Color(0xFF388E3C),
    symptomSeverity5   = Color(0xFFEF5350),
)
