package de.healthforge.presentation.lebensmittel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.IngredientDto
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
)

@HiltViewModel
class IngredientDetailViewModel @Inject constructor(
    private val ingredientRepo: IngredientRepository,
    private val intakeRepo: IntakeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IngredientDetailState())
    val state: StateFlow<IngredientDetailState> = _state.asStateFlow()

    fun load(id: String) {
        if (_state.value.item?.id == id) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            ingredientRepo.byId(id)
                .onSuccess { _state.value = IngredientDetailState(item = it) }
                .onFailure { _state.value = IngredientDetailState(error = it.message ?: "Fehler beim Laden") }
        }
    }

    fun openAddToPlanDialog() { _state.update { it.copy(showAddToPlan = true) } }
    fun dismissAddToPlanDialog() { _state.update { it.copy(showAddToPlan = false) } }

    fun addToPlan(grams: Double) {
        val item = _state.value.item ?: return
        viewModelScope.launch {
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
            ))
            _state.update { it.copy(showAddToPlan = false) }
        }
    }
}
