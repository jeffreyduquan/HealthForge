package de.healthforge.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.prefs.SettingsDataStore
import de.healthforge.data.repository.FullProfile
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.presentation.theme.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    val profile: StateFlow<FullProfile?> =
        repo.observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val theme: StateFlow<ThemePreference> =
        settings.themePreference.stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    fun setTheme(t: ThemePreference) = viewModelScope.launch { settings.setThemePreference(t) }

    fun restartOnboarding() = viewModelScope.launch { settings.setOnboardingCompleted(false) }

    /** REQ-WATER-003: persist daily water goal (clamped to a sane range). */
    fun setWaterGoalMl(ml: Int) {
        val clamped = ml.coerceIn(250, 6000)
        viewModelScope.launch {
            val current = profile.value?.profile ?: return@launch
            repo.upsertProfile(current.copy(waterGoalMl = clamped, updatedAt = System.currentTimeMillis()))
        }
    }
}
