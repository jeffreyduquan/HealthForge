package de.healthforge.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.ActivityLevel
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.BiologicalSex
import de.healthforge.data.db.entities.DietGoal
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.db.entities.HistamineSensitivity
import de.healthforge.data.db.entities.MealSlot
import de.healthforge.data.db.entities.UserProfileEntity
import de.healthforge.data.prefs.SettingsDataStore
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.domain.NutritionMath
import de.healthforge.presentation.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val stepIndex: Int = 0,
    val displayName: String = "",
    val ageYears: Int? = null,
    val sex: BiologicalSex? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activity: ActivityLevel? = null,
    val goal: DietGoal? = null,
    val allergies: Set<AllergenType> = emptySet(),
    val intolerances: Set<FodmapType> = emptySet(),
    val histamine: HistamineSensitivity = HistamineSensitivity.NONE,
    val mealSlots: Set<MealSlot> = setOf(MealSlot.FRUEHSTUECK, MealSlot.MITTAG, MealSlot.ABENDESSEN),
    val maxPrepTimeMin: Int? = null,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val committing: Boolean = false,
    val done: Boolean = false,
) {
    /** Calculated daily kcal target if all required inputs are present; else null. */
    val computedKcalTarget: Int?
        get() {
            val a = ageYears ?: return null
            val s = sex ?: return null
            val h = heightCm ?: return null
            val w = weightKg ?: return null
            val act = activity ?: return null
            val g = goal ?: return null
            val bmr = NutritionMath.bmr(w, h, a, s)
            val tdee = NutritionMath.tdee(bmr, act)
            return NutritionMath.targetKcal(tdee, g)
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun next() = _state.update { it.copy(stepIndex = it.stepIndex + 1) }
    fun back() = _state.update { it.copy(stepIndex = (it.stepIndex - 1).coerceAtLeast(0)) }
    fun goTo(step: Int) = _state.update { it.copy(stepIndex = step) }

    fun setDisplayName(v: String) = _state.update { it.copy(displayName = v) }
    fun setAge(v: Int?) = _state.update { it.copy(ageYears = v) }
    fun setSex(v: BiologicalSex) = _state.update { it.copy(sex = v) }
    fun setHeight(v: Int?) = _state.update { it.copy(heightCm = v) }
    fun setWeight(v: Double?) = _state.update { it.copy(weightKg = v) }
    fun setActivity(v: ActivityLevel) = _state.update { it.copy(activity = v) }
    fun setGoal(v: DietGoal) = _state.update { it.copy(goal = v) }
    fun toggleAllergy(a: AllergenType) = _state.update {
        it.copy(allergies = if (a in it.allergies) it.allergies - a else it.allergies + a)
    }
    fun toggleIntolerance(f: FodmapType) = _state.update {
        it.copy(intolerances = if (f in it.intolerances) it.intolerances - f else it.intolerances + f)
    }
    fun setHistamine(v: HistamineSensitivity) = _state.update { it.copy(histamine = v) }
    fun toggleMealSlot(m: MealSlot) = _state.update {
        it.copy(mealSlots = if (m in it.mealSlots) it.mealSlots - m else it.mealSlots + m)
    }
    fun setMaxPrepTime(v: Int?) = _state.update { it.copy(maxPrepTimeMin = v) }
    fun setTheme(v: ThemePreference) = _state.update { it.copy(theme = v) }

    fun commit() {
        val s = _state.value
        _state.update { it.copy(committing = true) }
        viewModelScope.launch {
            profileRepo.upsertProfile(
                UserProfileEntity(
                    id = 1L,
                    displayName = s.displayName.ifBlank { null },
                    ageYears = s.ageYears,
                    biologicalSex = s.sex,
                    heightCm = s.heightCm,
                    weightKg = s.weightKg,
                    activityLevel = s.activity,
                    dietGoal = s.goal,
                    histamineSensitivity = s.histamine,
                    mealSlotsJson = s.mealSlots.joinToString(",") { it.name },
                    maxPrepTimeMin = s.maxPrepTimeMin,
                    onboardingCompleted = true,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            profileRepo.replaceAllergies(s.allergies)
            profileRepo.replaceIntolerances(s.intolerances)
            settings.setThemePreference(s.theme)
            settings.setOnboardingCompleted(true)
            _state.update { it.copy(committing = false, done = true) }
        }
    }
}
