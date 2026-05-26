package de.healthforge.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.healthforge.presentation.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide non-sensitive settings persisted via DataStore<Preferences>.
 *
 * For secrets (tokens, SQLCipher key) use [SecureTokenStore] / [SqlCipherKeyProvider].
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val themePreference: Flow<ThemePreference> =
        dataStore.data.map { prefs ->
            ThemePreference.fromName(prefs[KEY_THEME])
        }

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] == "true"
        }

    suspend fun setThemePreference(value: ThemePreference) {
        dataStore.edit { it[KEY_THEME] = value.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed.toString() }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_preference")
        val KEY_ONBOARDING_COMPLETED = stringPreferencesKey("onboarding_completed")
    }
}
