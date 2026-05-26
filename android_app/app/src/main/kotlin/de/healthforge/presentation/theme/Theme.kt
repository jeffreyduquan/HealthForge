package de.healthforge.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal exposing [SemanticColors] (rating, macro, water, symptom tokens).
 * Read via `LocalSemanticColors.current.macroProtein` inside `@Composable` scopes.
 */
val LocalSemanticColors = staticCompositionLocalOf<SemanticColors> {
    error("SemanticColors not provided — wrap composable in HealthForgeTheme {}")
}

// === LOCKED color schemes (GUI.md §2.1/2.2). Dynamic-Color disabled (§9.2). ===

private val LightColors = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary             = LightTertiary,
    onTertiary           = LightOnTertiary,
    tertiaryContainer    = LightTertiaryContainer,
    onTertiaryContainer  = LightOnTertiaryContainer,
    error                = LightError,
    onError              = LightOnError,
    errorContainer       = LightErrorContainer,
    onErrorContainer     = LightOnErrorContainer,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    outline              = LightOutline,
    outlineVariant       = LightOutlineVariant,
)

private val DarkColors = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary             = DarkTertiary,
    onTertiary           = DarkOnTertiary,
    tertiaryContainer    = DarkTertiaryContainer,
    onTertiaryContainer  = DarkOnTertiaryContainer,
    error                = DarkError,
    onError              = DarkOnError,
    errorContainer       = DarkErrorContainer,
    onErrorContainer     = DarkOnErrorContainer,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVariant,
)

/**
 * Root theme wrapper. Resolves [ThemePreference] → effective dark/light scheme and
 * provides [SemanticColors] via CompositionLocal.
 *
 * GUI.md §9.1: LIGHT/DARK forced; SYSTEM follows `isSystemInDarkTheme()`.
 */
@Composable
fun HealthForgeTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HealthForgeTypography,
            shapes = HealthForgeShapes,
            content = content,
        )
    }
}
