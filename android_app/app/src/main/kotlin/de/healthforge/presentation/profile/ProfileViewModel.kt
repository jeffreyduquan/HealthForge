package de.healthforge.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.prefs.SettingsDataStore
import de.healthforge.data.repository.FullProfile
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.db.entities.UserProfileEntity
import de.healthforge.domain.ComputeNutrientTargetsUseCase
import de.healthforge.domain.DailyTargets
import de.healthforge.presentation.theme.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val settings: SettingsDataStore,
    private val computeTargets: ComputeNutrientTargetsUseCase,
) : ViewModel() {

    val profile: StateFlow<FullProfile?> =
        repo.observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * P7.S4 / REQ-PROFILE-LAYOUT-001 — Profil-abgeleitete Makro-Defaults (kcal/protein/carbs/fat/water).
     * Mikros nutzen den statischen DGE-Default aus [de.healthforge.domain.nutrition.NutrientCatalog].
     */
    val computedDefaults: StateFlow<DailyTargets> =
        repo.observe()
            .map { computeTargets(it?.profile) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DailyTargets.FALLBACK)

    val theme: StateFlow<ThemePreference> =
        settings.themePreference.stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    fun setTheme(t: ThemePreference) = viewModelScope.launch { settings.setThemePreference(t) }

    fun restartOnboarding() = viewModelScope.launch { settings.setOnboardingCompleted(false) }

    /** REQ-WATER-003: persist daily water goal (clamped to a sane range). */
    fun setWaterGoalMl(ml: Int) {
        val clamped = ml.coerceIn(250, 6000)
        viewModelScope.launch {
            val current = profile.value?.profile ?: UserProfileEntity()
            repo.upsertProfile(current.copy(waterGoalMl = clamped, updatedAt = System.currentTimeMillis()))
        }
    }

    /** P7.S4 / REQ-PROFILE-LAYOUT-001 — reset water goal to Catalog-Default. */
    fun resetWaterGoalMl() = setWaterGoalMl(2000)

    /**
     * REQ-PROFILE-GOALS-001 (P6.S6 Slice B) + P7.S4 (REQ-PROFILE-LAYOUT-001): persist per-nutrient daily goal.
     * Stored as JSON in `dailyNutrientGoalsJson` (key = nutrient slug from
     * [de.healthforge.domain.nutrition.NutrientCatalog], value = Double in [Nutrient.unit]).
     *
     * Special-case `slug == "water"` routes to [setWaterGoalMl] to keep single-source-of-truth
     * (the `waterGoalMl` column ist die kanonische Quelle für [DailyTargets]).
     */
    fun setNutrientGoal(slug: String, value: Double) {
        if (slug == "water") {
            setWaterGoalMl(value.toInt())
            return
        }
        viewModelScope.launch {
            // Auto-create default profile wenn noch keiner existiert (REQ-ONBOARD-002:
            // Skip-Onboarding-Pfad). Singleton-Row id=1L, alle anderen Felder Defaults.
            val current = profile.value?.profile ?: UserProfileEntity()
            val obj = runCatching { org.json.JSONObject(current.dailyNutrientGoalsJson) }.getOrElse { org.json.JSONObject() }
            obj.put(slug, value)
            repo.upsertProfile(current.copy(dailyNutrientGoalsJson = obj.toString(), updatedAt = System.currentTimeMillis()))
        }
    }

    /** P7.S4 / REQ-PROFILE-LAYOUT-001 — Reset entfernt den Override-Key; Default wird verwendet. */
    fun clearNutrientGoal(slug: String) {
        if (slug == "water") {
            resetWaterGoalMl()
            return
        }
        viewModelScope.launch {
            val current = profile.value?.profile ?: return@launch
            val obj = runCatching { org.json.JSONObject(current.dailyNutrientGoalsJson) }.getOrElse { org.json.JSONObject() }
            obj.remove(slug)
            repo.upsertProfile(current.copy(dailyNutrientGoalsJson = obj.toString(), updatedAt = System.currentTimeMillis()))
        }
    }

    /** REQ-PROFILE-GOALS-001 (P6.S6 Slice B): toggle pinned nutrient (de-dup, preserves order). */
    fun togglePinnedNutrient(slug: String) {
        viewModelScope.launch {
            val current = profile.value?.profile ?: return@launch
            val arr = runCatching { org.json.JSONArray(current.pinnedNutrientsJson) }.getOrElse { org.json.JSONArray() }
            val list = (0 until arr.length()).map { arr.getString(it) }.toMutableList()
            if (slug in list) list.remove(slug) else list.add(slug)
            val out = org.json.JSONArray()
            list.forEach { out.put(it) }
            repo.upsertProfile(current.copy(pinnedNutrientsJson = out.toString(), updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Profil-Redesign (post-Slice-4b-revert) — Allergien-Toggle aus FilterChip-Grid.
     * Vollständiges Replace pro Tap (idempotent), nutzt [ProfileRepository.replaceAllergies].
     */
    fun setAllergies(items: Set<AllergenType>) = viewModelScope.launch {
        repo.replaceAllergies(items)
    }

    /** Profil-Redesign — Intoleranzen-Toggle (FODMAP). */
    fun setIntolerances(items: Set<FodmapType>) = viewModelScope.launch {
        repo.replaceIntolerances(items)
    }
}
