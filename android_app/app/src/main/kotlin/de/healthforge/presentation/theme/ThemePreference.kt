package de.healthforge.presentation.theme

/**
 * User-selected theme preference (REQ-PROFILE / GUI.md §9.1).
 * Persisted in DataStore (key `theme_preference`). Default = [SYSTEM].
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromName(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
