package de.healthforge.presentation.lebensmittel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.prefs.IngredientRatingStore
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.IntakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientDetailState(
    val item: IngredientDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val showAddToPlan: Boolean = false,
    val navigateHome: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
)

@HiltViewModel
class IngredientDetailViewModel @Inject constructor(
    private val ingredientRepo: IngredientRepository,
    private val intakeRepo: IntakeRepository,
    private val ratingStore: IngredientRatingStore,
) : ViewModel() {

    private val _state = MutableStateFlow(IngredientDetailState())
    val state: StateFlow<IngredientDetailState> = _state.asStateFlow()

    fun load(id: String) {
        if (_state.value.item?.id == id) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            ingredientRepo.byId(id)
                .onSuccess { item ->
                    _state.value = IngredientDetailState(
                        item = item,
                        isLiked = ratingStore.isLiked(id),
                        isDisliked = ratingStore.isDisliked(id),
                    )
                }
                .onFailure { _state.value = IngredientDetailState(error = it.message ?: "Fehler beim Laden") }
        }
    }

    fun toggleLike() {
        val id = _state.value.item?.id ?: return
        ratingStore.toggleLike(id)
        _state.update { it.copy(isLiked = !it.isLiked, isDisliked = false) }
    }

    fun toggleDislike() {
        val id = _state.value.item?.id ?: return
        ratingStore.toggleDislike(id)
        _state.update { it.copy(isDisliked = !it.isDisliked, isLiked = false) }
    }

    fun openAddToPlanDialog() { _state.update { it.copy(showAddToPlan = true) } }
    fun dismissAddToPlanDialog() { _state.update { it.copy(showAddToPlan = false) } }

    fun addToPlan(grams: Double) {
        val item = _state.value.item ?: return
        viewModelScope.launch {
            // Build full micronutrient snapshot: vitamins + minerals + secondary macros
            val micro = mutableMapOf<String, Double>()
            micro.putAll(item.micronutrients)
            item.sugar_g_per_100g?.let { micro["sugar"] = it }
            item.fiber_g_per_100g?.let { micro["fiber"] = it }
            item.salt_g_per_100g?.let { micro["salt"] = it }
            item.satfat_g_per_100g?.let { micro["satfat"] = it }
            val microJson = if (micro.isNotEmpty()) org.json.JSONObject(micro).toString() else null
            intakeRepo.add(IntakeEntryEntity(
                loggedAt = System.currentTimeMillis(),
                dayDateIso = java.time.LocalDate.now().toString(),
                sourceType = IntakeSourceType.INGREDIENT,
                sourceId = item.id,
                portionGrams = grams,
                snapshotName = item.name_de,
                snapshotBrand = item.brand,
                snapshotKcalPer100g = item.energy_kcal_per_100g,
                snapshotProteinPer100g = item.protein_g_per_100g,
                snapshotCarbsPer100g = item.carbs_g_per_100g,
                snapshotFatPer100g = item.fat_g_per_100g,
                snapshotMicronutrientsJson = microJson,
                consumed = false,
            ))
            _state.update { it.copy(showAddToPlan = false, navigateHome = true) }
        }
    }
}
