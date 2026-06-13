package de.healthforge.presentation.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.repository.IntakeRepository
import de.healthforge.data.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplementDetailState(
    val supplement: SupplementEntity? = null,
    val loading: Boolean = false,
    val showAddToPlan: Boolean = false,
    val navigateToHome: Boolean = false,
)

@HiltViewModel
class SupplementDetailViewModel @Inject constructor(
    private val supplementRepo: SupplementRepository,
    private val intakeRepo: IntakeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplementDetailState())
    val state: StateFlow<SupplementDetailState> = _state.asStateFlow()

    fun load(id: String) {
        val longId = id.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val sup = supplementRepo.byId(longId)
            _state.value = SupplementDetailState(supplement = sup, loading = false)
        }
    }

    fun openAddToPlanDialog() { _state.update { it.copy(showAddToPlan = true) } }
    fun dismissAddToPlanDialog() { _state.update { it.copy(showAddToPlan = false) } }

    fun addToPlan(grams: Double) {
        val sup = _state.value.supplement ?: return
        viewModelScope.launch {
            intakeRepo.add(IntakeEntryEntity(
                loggedAt = System.currentTimeMillis(),
                dayDateIso = java.time.LocalDate.now().toString(),
                sourceType = IntakeSourceType.SUPPLEMENT,
                sourceId = sup.id.toString(),
                portionGrams = grams,
                snapshotName = sup.nameDe,
                snapshotBrand = sup.brand,
                snapshotKcalPer100g = sup.kcalPerDose,
                snapshotProteinPer100g = sup.proteinPerDose,
                snapshotCarbsPer100g = sup.carbsPerDose,
                snapshotFatPer100g = sup.fatPerDose,
            ))
            _state.update { it.copy(showAddToPlan = false, navigateToHome = true) }
        }
    }

    fun onNavigatedToHome() { _state.update { it.copy(navigateToHome = false) } }
}
