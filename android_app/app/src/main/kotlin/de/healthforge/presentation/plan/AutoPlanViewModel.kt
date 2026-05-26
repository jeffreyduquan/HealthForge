package de.healthforge.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.network.AutoPlanGenerateRequest
import de.healthforge.data.network.AutoPlanGenerateResponse
import de.healthforge.data.repository.AutoPlanRepository
import de.healthforge.data.repository.MealPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AutoPlanUiState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val preview: AutoPlanGenerateResponse? = null,
    val error: String? = null,
    val committing: Boolean = false,
    val committed: Boolean = false,
)

@HiltViewModel
class AutoPlanViewModel @Inject constructor(
    private val autoPlanRepo: AutoPlanRepository,
    private val planRepo: MealPlanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AutoPlanUiState())
    val state: StateFlow<AutoPlanUiState> = _state.asStateFlow()

    fun open() { _state.update { AutoPlanUiState(visible = true) } }
    fun dismiss() { _state.update { AutoPlanUiState() } }

    fun generate(req: AutoPlanGenerateRequest) {
        _state.update { it.copy(loading = true, error = null, preview = null) }
        viewModelScope.launch {
            autoPlanRepo.generate(req)
                .onSuccess { resp -> _state.update { it.copy(loading = false, preview = resp) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Fehler") } }
        }
    }

    /** Swap a single (date, slotTag) entry in the in-memory preview without re-fetching. */
    fun removeSlot(date: LocalDate, slotTag: String) {
        val cur = _state.value.preview ?: return
        val nextDays = cur.days.map { d ->
            if (d.date == date.toString()) d.copy(slots = d.slots.filterNot { it.slot_tag == slotTag })
            else d
        }
        _state.update { it.copy(preview = cur.copy(days = nextDays)) }
    }

    fun commit() {
        val preview = _state.value.preview ?: return
        _state.update { it.copy(committing = true) }
        viewModelScope.launch {
            runCatching {
                preview.days.forEach { day ->
                    val date = LocalDate.parse(day.date)
                    day.slots.forEach { s ->
                        val slotId = planRepo.addSlot(date, s.slot_tag)
                        planRepo.addItem(
                            MealPlanItemEntity(
                                slotId = slotId,
                                sourceType = IntakeSourceType.RECIPE,
                                sourceId = s.recipe_id,
                                amount = 1.0,
                                snapshotName = s.title,
                            ),
                        )
                    }
                }
            }.onSuccess {
                _state.update { it.copy(committing = false, committed = true) }
            }.onFailure { e ->
                _state.update { it.copy(committing = false, error = e.message ?: "Commit fehlgeschlagen") }
            }
        }
    }
}
