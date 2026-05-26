package de.healthforge.presentation.lebensmittel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientSuggestRequest
import de.healthforge.data.repository.IngredientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * REQ-INGREDIENT-CREATE-WIZARD-001 — 4-Step Wizard zum Vorschlagen eines neuen
 * Lebensmittels. Ersetzt den ehemaligen `IngredientSuggestDialog`.
 */
data class IngredientWizardState(
    val stepIndex: Int = 0,
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val kcal: Float = 100f,
    val proteinG: Float = 5f,
    val carbsG: Float = 10f,
    val fatG: Float = 5f,
    val showAdvancedNutrients: Boolean = false,
    val sugarG: Float? = null,
    val satfatG: Float? = null,
    val fiberG: Float? = null,
    val saltG: Float? = null,
    val histamineScore: Int? = null,
    val allergens: Set<AllergenType> = emptySet(),
    val fodmap: Set<FodmapType> = emptySet(),
    val submitting: Boolean = false,
    val submitError: String? = null,
    val done: Boolean = false,
) {
    val canAdvanceFromStep1: Boolean get() = name.trim().isNotEmpty()
}

const val INGREDIENT_WIZARD_TOTAL_STEPS = 4

@HiltViewModel
class IngredientSuggestWizardViewModel @Inject constructor(
    private val ingredients: IngredientRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(IngredientWizardState())
    val state: StateFlow<IngredientWizardState> = _state.asStateFlow()

    fun init(initialName: String) {
        if (_state.value.name.isBlank() && initialName.isNotBlank()) {
            _state.update { it.copy(name = initialName) }
        }
    }

    fun next() {
        val s = _state.value
        if (s.stepIndex >= INGREDIENT_WIZARD_TOTAL_STEPS - 1) return
        _state.update { it.copy(stepIndex = it.stepIndex + 1) }
    }

    fun back() {
        if (_state.value.stepIndex == 0) return
        _state.update { it.copy(stepIndex = it.stepIndex - 1) }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setBrand(v: String) = _state.update { it.copy(brand = v) }
    fun setBarcode(v: String) = _state.update { it.copy(barcode = v) }
    fun setKcal(v: Float) = _state.update { it.copy(kcal = v) }
    fun setProtein(v: Float) = _state.update { it.copy(proteinG = v) }
    fun setCarbs(v: Float) = _state.update { it.copy(carbsG = v) }
    fun setFat(v: Float) = _state.update { it.copy(fatG = v) }
    fun toggleAdvanced() = _state.update { it.copy(showAdvancedNutrients = !it.showAdvancedNutrients) }
    fun setSugar(v: Float?) = _state.update { it.copy(sugarG = v) }
    fun setSatfat(v: Float?) = _state.update { it.copy(satfatG = v) }
    fun setFiber(v: Float?) = _state.update { it.copy(fiberG = v) }
    fun setSalt(v: Float?) = _state.update { it.copy(saltG = v) }
    fun setHistamine(v: Int?) = _state.update { it.copy(histamineScore = v) }
    fun toggleAllergen(a: AllergenType) = _state.update {
        it.copy(allergens = if (a in it.allergens) it.allergens - a else it.allergens + a)
    }
    fun toggleFodmap(f: FodmapType) = _state.update {
        it.copy(fodmap = if (f in it.fodmap) it.fodmap - f else it.fodmap + f)
    }

    fun submit() {
        val s = _state.value
        if (!s.canAdvanceFromStep1 || s.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, submitError = null) }
            val req = IngredientSuggestRequest(
                name_de = s.name.trim(),
                brand = s.brand.trim().ifBlank { null },
                barcode = s.barcode.trim().ifBlank { null },
                energy_kcal_per_100g = s.kcal.toDouble(),
                protein_g_per_100g = s.proteinG.toDouble(),
                carbs_g_per_100g = s.carbsG.toDouble(),
                sugar_g_per_100g = s.sugarG?.toDouble(),
                fat_g_per_100g = s.fatG.toDouble(),
                satfat_g_per_100g = s.satfatG?.toDouble(),
                fiber_g_per_100g = s.fiberG?.toDouble(),
                salt_g_per_100g = s.saltG?.toDouble(),
                histamine_score = s.histamineScore,
                allergens = s.allergens.map { it.name },
                fodmap_flags = s.fodmap.map { it.name },
            )
            ingredients.suggest(req)
                .onSuccess { _state.update { it.copy(submitting = false, done = true) } }
                .onFailure { t ->
                    _state.update {
                        it.copy(submitting = false, submitError = t.message ?: "Fehler beim Einreichen")
                    }
                }
        }
    }
}
