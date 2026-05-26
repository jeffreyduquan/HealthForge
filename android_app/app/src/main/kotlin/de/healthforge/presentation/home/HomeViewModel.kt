package de.healthforge.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.data.db.entities.isDueToday
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.repository.DayNutrientTotals
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.IntakeRepository
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.data.repository.SupplementRepository
import de.healthforge.data.repository.WaterIntakeRepository
import de.healthforge.domain.ComputeNutrientTargetsUseCase
import de.healthforge.domain.DailyTargets
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/** Item of the Home-screen Supplement-Checklist (REQ-SUPP-005 follow-up). */
data class SupplementChecklistItem(
    val reminder: SupplementReminderEntity,
    val supplement: SupplementEntity,
    val taken: Boolean,
)

data class HomeState(
    val date: LocalDate = LocalDate.now(),
    val targets: DailyTargets = DailyTargets.FALLBACK,
    val totals: DayNutrientTotals = DayNutrientTotals.ZERO,
    val entries: List<IntakeEntryEntity> = emptyList(),
    val waterMl: Int = 0,
    val supplementChecklist: List<SupplementChecklistItem> = emptyList(),
    val showQuickAdd: Boolean = false,
    val quickAddQuery: String = "",
    val quickAddResults: List<IngredientDto> = emptyList(),
    val quickAddLoading: Boolean = false,
    val quickAddSelected: IngredientDto? = null,
    val quickAddPortion: String = "100",
    val showWaterCustom: Boolean = false,
    val waterCustomMl: String = "",
    val error: String? = null,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val intakeRepo: IntakeRepository,
    private val waterRepo: WaterIntakeRepository,
    private val ingredientRepo: IngredientRepository,
    private val supplementRepo: SupplementRepository,
    profileRepo: ProfileRepository,
    targetsUseCase: ComputeNutrientTargetsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val dateFlow = MutableStateFlow(LocalDate.now())
    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    val targetsFlow: StateFlow<DailyTargets> = profileRepo.observe()
        .map { targetsUseCase(it.profile) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DailyTargets.FALLBACK)

    init {
        // Recompute totals + water + entries whenever date changes.
        dateFlow
            .onEach { d -> _state.value = _state.value.copy(date = d) }
            .flatMapLatest { day ->
                combine(
                    intakeRepo.observeForDay(day),
                    intakeRepo.observeTotalsForDay(day),
                    waterRepo.observeSumForDay(day),
                ) { entries, totals, water ->
                    Triple(entries, totals, water)
                }
            }
            .onEach { (entries, totals, water) ->
                _state.value = _state.value.copy(
                    entries = entries,
                    totals = totals,
                    waterMl = water,
                )
            }
            .launchIn(viewModelScope)

        // Supplement-Checklist: today's enabled reminders + which were taken already.
        dateFlow
            .flatMapLatest { day ->
                combine(
                    supplementRepo.observeAllReminders(),
                    supplementRepo.observeAll(),
                    intakeRepo.observeForDay(day),
                ) { reminders, supplements, entries ->
                    val byId = supplements.associateBy { it.id }
                    val takenSupplementIds = entries
                        .filter { it.sourceType == IntakeSourceType.SUPPLEMENT }
                        .mapNotNull { it.sourceId.toLongOrNull() }
                        .toSet()
                    reminders
                        .filter { it.enabled && it.isDueToday(day) }
                        .mapNotNull { r ->
                            val s = byId[r.supplementId] ?: return@mapNotNull null
                            SupplementChecklistItem(
                                reminder = r,
                                supplement = s,
                                taken = r.supplementId in takenSupplementIds,
                            )
                        }
                        .sortedWith(compareBy({ it.taken }, { it.reminder.hourOfDay ?: 0 }, { it.reminder.minute ?: 0 }))
                }
            }
            .onEach { list -> _state.value = _state.value.copy(supplementChecklist = list) }
            .launchIn(viewModelScope)

        targetsFlow
            .onEach { t -> _state.value = _state.value.copy(targets = t) }
            .launchIn(viewModelScope)

        queryFlow
            .debounce(250)
            .distinctUntilChanged()
            .onEach { runSearch(it) }
            .launchIn(viewModelScope)
    }

    fun setDate(d: LocalDate) { dateFlow.value = d }

    fun openQuickAdd() {
        _state.value = _state.value.copy(showQuickAdd = true)
    }
    fun closeQuickAdd() {
        _state.value = _state.value.copy(
            showQuickAdd = false, quickAddQuery = "", quickAddResults = emptyList(),
            quickAddSelected = null, quickAddPortion = "100", error = null,
        )
        queryFlow.value = ""
    }
    fun onQuickAddQuery(q: String) {
        _state.value = _state.value.copy(quickAddQuery = q)
        queryFlow.value = q
    }
    fun onQuickAddSelect(dto: IngredientDto) {
        _state.value = _state.value.copy(quickAddSelected = dto)
    }
    fun onQuickAddClearSelection() {
        _state.value = _state.value.copy(quickAddSelected = null)
    }
    fun onQuickAddPortion(p: String) {
        _state.value = _state.value.copy(quickAddPortion = p)
    }

    fun confirmQuickAdd() {
        val s = _state.value
        val dto = s.quickAddSelected ?: return
        val grams = s.quickAddPortion.toDoubleOrNull() ?: return
        if (grams <= 0) return
        viewModelScope.launch {
            intakeRepo.add(
                IntakeEntryEntity(
                    loggedAt = System.currentTimeMillis(),
                    dayDateIso = s.date.toString(),
                    sourceType = IntakeSourceType.INGREDIENT,
                    sourceId = dto.id.toString(),
                    portionGrams = grams,
                    snapshotName = dto.name_de,
                    snapshotBrand = dto.brand,
                    snapshotKcalPer100g = dto.energy_kcal_per_100g,
                    snapshotProteinPer100g = dto.protein_g_per_100g,
                    snapshotCarbsPer100g = dto.carbs_g_per_100g,
                    snapshotFatPer100g = dto.fat_g_per_100g,
                )
            )
            closeQuickAdd()
        }
    }

    fun addWater(volumeMl: Int) {
        viewModelScope.launch {
            runCatching { waterRepo.add(_state.value.date, volumeMl) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
    fun openWaterCustom() { _state.value = _state.value.copy(showWaterCustom = true, waterCustomMl = "") }
    fun closeWaterCustom() { _state.value = _state.value.copy(showWaterCustom = false) }
    fun onWaterCustomChange(v: String) { _state.value = _state.value.copy(waterCustomMl = v) }
    fun confirmWaterCustom() {
        val v = _state.value.waterCustomMl.toIntOrNull() ?: return
        if (v <= 0 || v > 5000) return
        addWater(v)
        closeWaterCustom()
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { intakeRepo.deleteById(id) }
    }

    /**
     * Manually mark a supplement as taken from the Home-checklist tap (bypassing the
     * notification action). REQ-SUPP-003.
     */
    fun markSupplementTaken(item: SupplementChecklistItem) {
        val day = _state.value.date
        viewModelScope.launch {
            intakeRepo.add(
                IntakeEntryEntity(
                    loggedAt = System.currentTimeMillis(),
                    dayDateIso = day.toString(),
                    sourceType = IntakeSourceType.SUPPLEMENT,
                    sourceId = item.supplement.id.toString(),
                    portionGrams = item.supplement.defaultDose,
                    snapshotName = item.supplement.nameDe,
                    snapshotBrand = item.supplement.brand,
                    snapshotKcalPer100g = item.supplement.kcalPerDose,
                    snapshotProteinPer100g = item.supplement.proteinPerDose,
                    snapshotCarbsPer100g = item.supplement.carbsPerDose,
                    snapshotFatPer100g = item.supplement.fatPerDose,
                )
            )
        }
    }

    private fun runSearch(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.value = _state.value.copy(quickAddResults = emptyList(), quickAddLoading = false)
            return
        }
        _state.value = _state.value.copy(quickAddLoading = true)
        searchJob = viewModelScope.launch {
            val res = ingredientRepo.search(q.trim(), limit = 20).getOrElse { emptyList() }
            _state.value = _state.value.copy(quickAddResults = res, quickAddLoading = false)
        }
    }
}
