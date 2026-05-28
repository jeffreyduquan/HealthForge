package de.healthforge.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.db.entities.MealPlanSlotEntity
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.MealPlanRepository
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.data.repository.RecipeRepository
import de.healthforge.domain.ComputeNutrientTargetsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SlotWithItems(
    val slot: MealPlanSlotEntity,
    val items: List<MealPlanItemEntity>,
)

data class PickerSuggestions(
    val recipes: List<RecipeListItemDto> = emptyList(),
    val ingredients: List<IngredientDto> = emptyList(),
)

data class PlanUiState(
    val selectedDay: LocalDate = LocalDate.now(),
    val slots: List<SlotWithItems> = emptyList(),
    val message: String? = null,
    /**
     * P7.S4 / REQ-PLAN-WATER-GOAL-001 — Effektives Tages-Wasserziel in ml.
     * = `slots.firstOrNull()?.waterGoalMl ?? profileWaterDefaultMl`.
     * Wird im DayHeader-Slider angezeigt; der Slider unterscheidet visuell
     * Override (Slot-Wert ≠ Profil-Default) von Default.
     */
    val effectiveWaterGoalMl: Int = 2000,
    /** Profil-Default (für Reset-Verhalten + Slider-Default-Markierung). */
    val profileWaterDefaultMl: Int = 2000,
    /** `true` = mindestens ein Slot trägt eigenen waterGoalMl. */
    val hasWaterGoalOverride: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepo: MealPlanRepository,
    private val recipeRepo: RecipeRepository,
    private val ingredientRepo: IngredientRepository,
    profileRepo: ProfileRepository,
    targetsUseCase: ComputeNutrientTargetsUseCase,
) : ViewModel() {

    private val _day = MutableStateFlow(LocalDate.now())
    private val _message = MutableStateFlow<String?>(null)

    private val profileWaterFlow: StateFlow<Int> = profileRepo.observe()
        .map { targetsUseCase(it.profile).waterMl }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2000)

    val state: StateFlow<PlanUiState> = combine(
        _day.flatMapLatest { d ->
            planRepo.observeSlotsForDay(d).flatMapLatest { slots ->
                if (slots.isEmpty()) flowOf(emptyList())
                else planRepo.observeItemsForSlots(slots.map { it.id }).map { items ->
                    slots.map { s -> SlotWithItems(s, items.filter { it.slotId == s.id }) }
                }
            }
        },
        _day,
        _message,
        profileWaterFlow,
    ) { slots, day, msg, profileWater ->
        val override = slots.firstOrNull()?.slot?.waterGoalMl
        PlanUiState(
            selectedDay = day,
            slots = slots,
            message = msg,
            effectiveWaterGoalMl = override ?: profileWater,
            profileWaterDefaultMl = profileWater,
            hasWaterGoalOverride = override != null,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun selectDay(day: LocalDate) { _day.value = day }

    fun addSlot(slotType: String) = viewModelScope.launch {
        planRepo.addSlot(_day.value, slotType)
    }

    fun deleteSlot(slotId: Long) = viewModelScope.launch { planRepo.deleteSlot(slotId) }

    fun deleteItem(itemId: Long) = viewModelScope.launch { planRepo.deleteItem(itemId) }

    fun addRecipeItem(slotId: Long, recipe: RecipeListItemDto, servings: Double = 1.0) = viewModelScope.launch {
        planRepo.addItem(
            MealPlanItemEntity(
                slotId = slotId,
                sourceType = IntakeSourceType.RECIPE,
                sourceId = recipe.id,
                amount = servings,
                snapshotName = recipe.title,
            ),
        )
    }

    fun addIngredientItem(slotId: Long, ing: IngredientDto, grams: Double = 100.0) = viewModelScope.launch {
        planRepo.addItem(
            MealPlanItemEntity(
                slotId = slotId,
                sourceType = IntakeSourceType.INGREDIENT,
                sourceId = ing.id,
                amount = grams,
                snapshotName = ing.name_de,
                snapshotKcalPer100g = ing.energy_kcal_per_100g,
                snapshotProteinPer100g = ing.protein_g_per_100g,
                snapshotCarbsPer100g = ing.carbs_g_per_100g,
                snapshotFatPer100g = ing.fat_g_per_100g,
            ),
        )
    }

    fun markConsumed(slotId: Long) = viewModelScope.launch {
        val n = planRepo.markConsumed(slotId)
        _message.value = if (n > 0) "$n Eintrag/Einträge ins Tagebuch übernommen" else "Bereits gegessen"
    }

    fun clearMessage() { _message.value = null }

    /**
     * P7.S4 / REQ-PLAN-WATER-GOAL-001 — Tages-Wasserziel-Override setzen.
     * `value=null` löscht den Override (Profil-Default greift wieder).
     * No-op wenn keine Slots existieren (UI zeigt Slider nur dann).
     */
    fun setWaterGoalForDay(value: Int?) = viewModelScope.launch {
        val n = planRepo.setWaterGoalForDay(_day.value, value)
        if (n == 0 && value != null) {
            _message.value = "Erst eine Mahlzeit hinzufügen"
        }
    }

    fun resetWaterGoalForDay() = setWaterGoalForDay(null)

    // ----- Picker queries (one-shot) -----
    private val _picker = MutableStateFlow(PickerSuggestions())
    val picker: StateFlow<PickerSuggestions> = _picker.asStateFlow()

    fun searchRecipes(q: String) = viewModelScope.launch {
        if (q.isBlank()) { _picker.update { it.copy(recipes = emptyList()) }; return@launch }
        recipeRepo.browse(q = q, limit = 12).onSuccess { list ->
            _picker.update { it.copy(recipes = list) }
        }
    }

    fun searchIngredients(q: String) = viewModelScope.launch {
        if (q.isBlank()) { _picker.update { it.copy(ingredients = emptyList()) }; return@launch }
        ingredientRepo.search(q, limit = 12).onSuccess { list ->
            _picker.update { it.copy(ingredients = list) }
        }
    }

    fun clearPicker() = _picker.update { PickerSuggestions() }
}
