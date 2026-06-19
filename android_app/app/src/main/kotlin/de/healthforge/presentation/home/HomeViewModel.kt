package de.healthforge.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.db.entities.MealPlanSlotEntity
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.data.db.entities.isDueToday
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.repository.DayNutrientTotals
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.IntakeRepository
import de.healthforge.data.repository.MealPlanRepository
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.data.repository.RecipeRepository
import de.healthforge.data.repository.SupplementRepository
import de.healthforge.data.repository.WaterIntakeRepository
import de.healthforge.domain.ComputeNutrientTargetsUseCase
import de.healthforge.domain.DailyTargets
import de.healthforge.domain.applyOverrides
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.notification.WaterReminderPrefs
import de.healthforge.notification.WaterReminderScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

/** A planned meal item with its slot's consumed state (REQ-HOME-PLAN-001). */
data class PlannedMealInfo(
    val item: MealPlanItemEntity,
    val slotConsumed: Boolean,
    val slotId: Long,
)

data class HomeState(
    val date: LocalDate = LocalDate.now(),
    val targets: DailyTargets = DailyTargets.FALLBACK,
    val totals: DayNutrientTotals = DayNutrientTotals.ZERO,
    val entries: List<IntakeEntryEntity> = emptyList(),
    val plannedMeals: List<PlannedMealInfo> = emptyList(),
    val trendTotals: Map<String, DayNutrientTotals> = emptyMap(),
    val waterTrend: Map<String, Int> = emptyMap(),
    val waterMl: Int = 0,
    /**
     * P7.S3 / REQ-HOME-WATER-BAR-001 — lineares Tages-Soll bis zur aktuellen
     * Uhrzeit (= `targets.waterMl * (minutesSinceMidnight / 1440)`).
     * Wird im UI als Ghost-Layer der Wasser-Bar gezeichnet.
     */
    val waterGhostMl: Int = 0,
    /**
     * P7.S3 / REQ-HOME-NUTRIENT-LIST-001 — in-memory Liste angepinnter
     * Nutrient-Keys. Default = [NutrientCatalog.defaultPinnedKeys].
     * Persistente Speicherung (UserProfile JSON) folgt in P7.S5.
     */
    val pinnedKeys: List<String> = NutrientCatalog.defaultPinnedKeys,
    /**
     * P7.S4 / REQ-HOME-NUTRIENT-LIST-001 — `PinnedNutrientCard` expanded:
     * `false` (default) = nur gepinnte Nährstoffe sichtbar (steady-state).
     * `true` = alle Nährstoffe sichtbar (gruppiert nach Kategorie) zum
     * Inline-Pinnen/Entpinnen via PushPin-Icon. Toggle via Chevron im
     * Card-Header. In-Memory only (Session-State).
     */
    val pinsExpanded: Boolean = false,
    val supplementChecklist: List<SupplementChecklistItem> = emptyList(),
    val showQuickAdd: Boolean = false,
    val quickAddQuery: String = "",
    val quickAddResults: List<IngredientDto> = emptyList(),
    val quickAddLoading: Boolean = false,
    val quickAddSelected: IngredientDto? = null,
    val quickAddPortion: String = "100",
    val quickAddTab: Int = 0,
    val quickAddRecipes: List<RecipeListItemDto> = emptyList(),
    val quickAddSupplements: List<SupplementEntity> = emptyList(),
    val quickAddRecipeLoading: Boolean = false,
    val waterReminderEnabled: Boolean = false,
    val error: String? = null,
    val recipeDtos: Map<String, RecipeListItemDto> = emptyMap(),
    val ingredientDtos: Map<String, IngredientDto> = emptyMap(),
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val intakeRepo: IntakeRepository,
    private val waterRepo: WaterIntakeRepository,
    private val ingredientRepo: IngredientRepository,
    private val supplementRepo: SupplementRepository,
    private val recipeRepo: RecipeRepository,
    private val planRepo: MealPlanRepository,
    private val waterReminderPrefs: WaterReminderPrefs,
    private val waterReminderScheduler: WaterReminderScheduler,
    private val profileRepo: ProfileRepository,
    targetsUseCase: ComputeNutrientTargetsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(waterReminderEnabled = waterReminderPrefs.enabled))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val dateFlow = MutableStateFlow(LocalDate.now())
    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    val targetsFlow: StateFlow<DailyTargets> = profileRepo.observe()
        .map { targetsUseCase(it.profile).applyOverrides(it.profile) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DailyTargets.FALLBACK)

    init {
        // P7.S5 Fix — Schedule water reminder on start if enabled.
        // Previously schedule() was only called on manual toggle or BootReceiver,
        // so the alarm was never set on fresh install.
        if (waterReminderPrefs.enabled) {
            waterReminderScheduler.schedule()
        }

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
                    waterGhostMl = computeWaterGhostMl(_state.value.targets.waterMl, _state.value.date),
                )
            }
            .launchIn(viewModelScope)

        // Fetch RecipeListItemDtos from server for BOTH intake entries AND planned items
        dateFlow
            .flatMapLatest { day ->
                combine(
                    intakeRepo.observeForDay(day),
                    planRepo.observeItemsForDay(day),
                ) { entries, planItems ->
                    val entryIds = entries.filter { it.sourceType == IntakeSourceType.RECIPE }.map { it.sourceId }
                    val planIds = planItems.filter { it.sourceType == IntakeSourceType.RECIPE }.map { it.sourceId }
                    (entryIds + planIds).distinct()
                }
            }
            .onEach { recipeIds ->
                if (recipeIds.isNotEmpty()) {
                    recipeRepo.batch(recipeIds).onSuccess { list ->
                        _state.value = _state.value.copy(recipeDtos = list.associateBy { it.id })
                    }
                } else {
                    _state.value = _state.value.copy(recipeDtos = emptyMap())
                }
            }
            .launchIn(viewModelScope)

        // Fetch IngredientDtos for planned food items (same pattern as recipeDtos)
        dateFlow
            .flatMapLatest { day ->
                combine(
                    intakeRepo.observeForDay(day),
                    planRepo.observeItemsForDay(day),
                ) { entries, planItems ->
                    val entryIds = entries.filter { it.sourceType == IntakeSourceType.INGREDIENT }.map { it.sourceId }
                    val planIds = planItems.filter { it.sourceType == IntakeSourceType.INGREDIENT }.map { it.sourceId }
                    (entryIds + planIds).distinct()
                }
            }
            .onEach { ingredientIds ->
                val map = mutableMapOf<String, IngredientDto>()
                ingredientIds.forEach { id ->
                    ingredientRepo.byId(id).onSuccess { dto -> map[id] = dto }
                }
                _state.value = _state.value.copy(ingredientDtos = map)
            }
            .launchIn(viewModelScope)

        // REQ-HOME-PLAN-001: Observe planned meals for today (from Plan tab)
        dateFlow
            .flatMapLatest { day ->
                combine(
                    planRepo.observeItemsForDay(day),
                    planRepo.observeSlotsForDay(day),
                ) { items, slots ->
                    val slotMap = slots.associateBy { it.id }
                    items.map { item ->
                        PlannedMealInfo(
                            item = item,
                            slotConsumed = slotMap[item.slotId]?.consumed ?: false,
                            slotId = item.slotId,
                        )
                    }
                }
            }
            .onEach { planned -> _state.value = _state.value.copy(plannedMeals = planned) }
            .launchIn(viewModelScope)

        // REQ-HOME-TREND-001: 7-day sparkline data
        dateFlow
            .flatMapLatest { day ->
                intakeRepo.observeTotalsForDateRange(day.minusDays(6), day)
            }
            .onEach { trend -> _state.value = _state.value.copy(trendTotals = trend) }
            .launchIn(viewModelScope)

        // REQ-HOME-TREND-001: 7-day water sparkline
        dateFlow
            .flatMapLatest { day ->
                waterRepo.observeSumForDateRange(day.minusDays(6), day)
            }
            .onEach { wTrend -> _state.value = _state.value.copy(waterTrend = wTrend) }
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
            .onEach { t ->
                _state.value = _state.value.copy(
                    targets = t,
                    waterGhostMl = computeWaterGhostMl(t.waterMl, _state.value.date),
                )
            }
            .launchIn(viewModelScope)

        // P7.S4 / REQ-HOME-NUTRIENT-LIST-001 — Pin-Reihenfolge aus
        // `UserProfileEntity.pinnedNutrientsJson` lesen. Fallback auf
        // `NutrientCatalog.defaultPinnedKeys` wenn JSON leer oder Profil
        // noch nicht initialisiert (Onboarding-Skip).
        profileRepo.observe()
            .map { parsePinnedKeys(it.profile?.pinnedNutrientsJson) }
            .distinctUntilChanged()
            .onEach { keys -> _state.value = _state.value.copy(pinnedKeys = keys) }
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
            quickAddTab = 0, quickAddRecipes = emptyList(), quickAddSupplements = emptyList(),
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
            // REQ-PLAN-QUICK-001: Create both IntakeEntry + MealPlanItem (for Plan sync)
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
                    consumed = false,
                )
            )
            // Also create a MealPlanItem so it appears in Plan (REQ-PLAN-QUICK-001)
            // We need a slot for today. Find or create a QUICK slot.
            val daySlots = planRepo.observeSlotsForDay(s.date).first()
            val quickSlot = daySlots.firstOrNull { it.slotType == "QUICK" }?.id
                ?: planRepo.addSlot(s.date, "QUICK")
            planRepo.addItem(
                de.healthforge.data.db.entities.MealPlanItemEntity(
                    slotId = quickSlot,
                    sourceType = IntakeSourceType.INGREDIENT,
                    sourceId = dto.id.toString(),
                    amount = grams,
                    snapshotName = dto.name_de,
                    snapshotKcalPer100g = dto.energy_kcal_per_100g,
                    snapshotProteinPer100g = dto.protein_g_per_100g,
                    snapshotCarbsPer100g = dto.carbs_g_per_100g,
                    snapshotFatPer100g = dto.fat_g_per_100g,
                )
            )
            closeQuickAdd()
        }
    }

    // ── Add-to-Plan: Recipe search ──
    fun searchAddRecipes(q: String) {
        _state.value = _state.value.copy(quickAddRecipeLoading = true, quickAddRecipes = emptyList())
        viewModelScope.launch {
            recipeRepo.browse(q = q.takeIf { it.isNotBlank() }).onSuccess { list ->
                _state.value = _state.value.copy(quickAddRecipes = list, quickAddRecipeLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(quickAddRecipeLoading = false, error = it.message)
            }
        }
    }

    // ── Add-to-Plan: Supplements list ──
    fun loadAddSupplements() {
        viewModelScope.launch {
            val list = supplementRepo.listAll()
            _state.value = _state.value.copy(quickAddSupplements = list)
        }
    }

    // ── Add-to-Plan: Select recipe → add to plan + intake ──
    fun selectAddRecipe(recipe: RecipeListItemDto) {
        val day = _state.value.date
        viewModelScope.launch {
            // Add to intake
            intakeRepo.add(IntakeEntryEntity(
                loggedAt = System.currentTimeMillis(), dayDateIso = day.toString(),
                sourceType = IntakeSourceType.RECIPE, sourceId = recipe.id,
                portionGrams = (recipe.servings * 100).toDouble(), snapshotName = recipe.title,
                snapshotKcalPer100g = recipe.kcal_per_100g,
                snapshotProteinPer100g = recipe.protein_per_100g,
                snapshotCarbsPer100g = recipe.carbs_per_100g,
                snapshotFatPer100g = recipe.fat_per_100g,
                consumed = false,
            ))
            // Add to plan
            val slots = planRepo.observeSlotsForDay(day).first()
            val quickSlot = slots.firstOrNull { it.slotType == "QUICK" }?.id
                ?: planRepo.addSlot(day, "QUICK")
            planRepo.addItem(de.healthforge.data.db.entities.MealPlanItemEntity(
                slotId = quickSlot, sourceType = IntakeSourceType.RECIPE,
                sourceId = recipe.id, amount = recipe.servings.toDouble(),
                snapshotName = recipe.title,
                snapshotKcalPer100g = recipe.kcal_per_100g,
                snapshotProteinPer100g = recipe.protein_per_100g,
                snapshotCarbsPer100g = recipe.carbs_per_100g,
                snapshotFatPer100g = recipe.fat_per_100g,
            ))
            closeQuickAdd()
        }
    }

    // ── Add-to-Plan: Select supplement → add to plan + intake ──
    fun selectAddSupplement(sup: SupplementEntity) {
        val day = _state.value.date
        viewModelScope.launch {
            intakeRepo.add(IntakeEntryEntity(
                loggedAt = System.currentTimeMillis(), dayDateIso = day.toString(),
                sourceType = IntakeSourceType.SUPPLEMENT, sourceId = sup.id.toString(),
                portionGrams = sup.defaultDose, snapshotName = sup.nameDe,
                snapshotBrand = sup.brand, snapshotKcalPer100g = sup.kcalPerDose,
                snapshotProteinPer100g = sup.proteinPerDose,
                snapshotCarbsPer100g = sup.carbsPerDose,
                snapshotFatPer100g = sup.fatPerDose,
                consumed = false,
            ))
            val slots = planRepo.observeSlotsForDay(day).first()
            val quickSlot = slots.firstOrNull { it.slotType == "QUICK" }?.id
                ?: planRepo.addSlot(day, "QUICK")
            planRepo.addItem(de.healthforge.data.db.entities.MealPlanItemEntity(
                slotId = quickSlot, sourceType = IntakeSourceType.SUPPLEMENT,
                sourceId = sup.id.toString(), amount = sup.defaultDose,
                snapshotName = sup.nameDe,
                snapshotKcalPer100g = sup.kcalPerDose,
                snapshotProteinPer100g = sup.proteinPerDose,
                snapshotCarbsPer100g = sup.carbsPerDose,
                snapshotFatPer100g = sup.fatPerDose,
            ))
            closeQuickAdd()
        }
    }

    fun setAddTab(tab: Int) {
        _state.value = _state.value.copy(quickAddTab = tab)
        if (tab == 2) loadAddSupplements()
    }

    /**
     * P7.S3a / REQ-HOME-WATER-BAR-001 — setzt die absolute Tages-Wassermenge.
     *
     * Wird vom Home-Slider gerufen, wenn der User den Thumb loslässt. Persistenz
     * via [WaterIntakeRepository.setDayTotal] (Day-Aggregate: alle Einträge des
     * Tages werden durch genau einen Aggregat-Eintrag mit [totalMl] ersetzt).
     */
    fun setWaterMl(totalMl: Int) {
        viewModelScope.launch {
            runCatching { waterRepo.setDayTotal(_state.value.date, totalMl.coerceAtLeast(0)) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    /** Toggle Wasser-Reminder (REQ-REMIND-001). */
    fun setWaterReminderEnabled(enabled: Boolean) {
        waterReminderPrefs.enabled = enabled
        if (enabled) waterReminderScheduler.schedule() else waterReminderScheduler.cancel()
        _state.value = _state.value.copy(waterReminderEnabled = enabled)
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

    /**
     * REQ-HOME-PLAN-001: "GEGESSEN" — marks a plan slot as consumed (creates
     * IntakeEntry from all slot items + sets slot.consumed=true).
     */
    fun markAsEaten(slotId: Long) {
        viewModelScope.launch {
            planRepo.markConsumed(slotId)
        }
    }

    /**
     * Undo "GEGESSEN": removes the intake entries created by markConsumed
     * for this slot, then sets slot.consumed=false.
     */
    fun markAsNotEaten(slotId: Long) {
        viewModelScope.launch {
            val slot = planRepo.observeSlotsForDay(_state.value.date).first()
                .firstOrNull { it.id == slotId } ?: return@launch
            if (!slot.consumed) return@launch
            // Find and delete intake entries created by this slot's consumption
            val items = planRepo.observeItemsForSlots(listOf(slotId)).first()
            val entries = intakeRepo.observeForDay(_state.value.date).first()
            val now = slot.consumedAt ?: System.currentTimeMillis()
            // Match entries created within 5 seconds of the slot's consumedAt
            for (entry in entries) {
                if (entry.sourceType == IntakeSourceType.RECIPE || entry.sourceType == IntakeSourceType.INGREDIENT) {
                    val match = items.any { it.sourceId == entry.sourceId && kotlin.math.abs(entry.loggedAt - now) < 5000 }
                    if (match) intakeRepo.deleteById(entry.id)
                }
            }
            // Reset slot to unconsumed
            planRepo.resetConsumed(slotId)
        }
    }

    /** Delete an item from intake log (and from plan if it has a slot). */
    fun deleteIntakeEntry(id: Long) {
        viewModelScope.launch { intakeRepo.deleteById(id) }
    }

    /** Toggle the consumed flag on an intake entry. */
    fun toggleEntryConsumed(entry: IntakeEntryEntity) {
        viewModelScope.launch {
            intakeRepo.update(entry.copy(consumed = !entry.consumed))
        }
    }

    /**
     * Cascade-delete: removes the IntakeEntry AND any linked MealPlanItem
     * (matched by sourceType + sourceId for the current day). Also cleans up the
     * parent MealPlanSlot if it becomes empty.
     *
     * This makes a swiped-away card behave as if the item was never eaten.
     */
    fun deleteIntakeEntryCascade(entry: IntakeEntryEntity) {
        viewModelScope.launch {
            // 1. Delete the intake entry
            intakeRepo.deleteById(entry.id)

            // 2. Find and delete matching MealPlanItem (same sourceType + sourceId for today)
            try {
                val dayItems = planRepo.observeItemsForDay(_state.value.date).first()
                val match = dayItems.firstOrNull {
                    it.sourceType == entry.sourceType && it.sourceId == entry.sourceId
                }
                if (match != null) {
                    planRepo.deleteItem(match.id)

                    // 3. If slot is now empty, delete the slot too
                    val remainingItems = dayItems.filter {
                        it.slotId == match.slotId && it.id != match.id
                    }
                    if (remainingItems.isEmpty()) {
                        planRepo.deleteSlot(match.slotId)
                    }
                }
            } catch (_: Exception) {
                // Plan cleanup is best-effort; intake deletion already succeeded
            }
        }
    }

    /** Delete a planned slot + all its items + associated intake entries. */
    fun deletePlannedSlot(slotId: Long) {
        viewModelScope.launch {
            runCatching { planRepo.deleteSlot(slotId) }
                .onFailure { _state.value = _state.value.copy(error = "Fehler beim Löschen: ${it.message}") }
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

    /**
     * P7.S4 / REQ-HOME-NUTRIENT-LIST-001 — togglet einen Nutrient-Key in der
     * Pin-Liste und persistiert sofort in
     * `UserProfileEntity.pinnedNutrientsJson` (kein Save-Button,
     * UsabilityMap §3 Home). Verhindert das Löschen aller Pins
     * (mind. 1 Pin bleibt). Bei fehlendem Profil-Row (Onboarding-Skip)
     * bleibt nur in-memory.
     */
    fun togglePin(key: String) {
        val cur = _state.value.pinnedKeys
        val next = when {
            key in cur && cur.size > 1 -> cur - key
            key in cur -> cur
            else -> cur + key
        }
        if (next == cur) return
        _state.value = _state.value.copy(pinnedKeys = next)
        viewModelScope.launch {
            val current = profileRepo.observe().first().profile ?: return@launch
            val out = org.json.JSONArray()
            next.forEach { out.put(it) }
            profileRepo.upsertProfile(
                current.copy(
                    pinnedNutrientsJson = out.toString(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /** Reorder-Helper für Drag&Drop (P7.S4 follow-up). Persistiert sofort. */
    fun reorderPins(newOrder: List<String>) {
        if (newOrder == _state.value.pinnedKeys) return
        _state.value = _state.value.copy(pinnedKeys = newOrder)
        viewModelScope.launch {
            val current = profileRepo.observe().first().profile ?: return@launch
            val out = org.json.JSONArray()
            newOrder.forEach { out.put(it) }
            profileRepo.upsertProfile(
                current.copy(
                    pinnedNutrientsJson = out.toString(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * P7.S4 — UI-Toggle Expand der Pin-Card.
     * Collapsed = nur gepinnte, Expanded = alle (mit Inline-Pin-Toggle).
     */
    fun togglePinsExpanded() {
        _state.value = _state.value.copy(pinsExpanded = !_state.value.pinsExpanded)
    }

    companion object {
        /**
         * P7.S4 — Parst `pinnedNutrientsJson` zu Key-Liste. Fallback auf
         * `NutrientCatalog.defaultPinnedKeys` bei null/leer/parse-error.
         */
        internal fun parsePinnedKeys(json: String?): List<String> {
            if (json.isNullOrBlank()) return NutrientCatalog.defaultPinnedKeys
            return runCatching {
                val arr = org.json.JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
                    .takeIf { it.isNotEmpty() }
            }.getOrNull() ?: NutrientCatalog.defaultPinnedKeys
        }

        /**
         * P7.S3 / REQ-HOME-WATER-BAR-001 \u2014 Anteil des Tages, der bis "jetzt"
         * vergangen ist, multipliziert mit dem Tagesziel. Bei Anzeige eines
         * vergangenen Tages wird 100% verwendet (Ghost = Goal); bei k\u00fcnftigem
         * Tag 0%.
         */
        internal fun computeWaterGhostMl(goalMl: Int, day: LocalDate): Int {
            val today = LocalDate.now()
            return when {
                day.isBefore(today) -> goalMl
                day.isAfter(today) -> 0
                else -> {
                    val now = java.time.LocalTime.now()
                    val secondsOfDay = now.toSecondOfDay().toDouble()
                    val frac = (secondsOfDay / 86_400.0).coerceIn(0.0, 1.0)
                    (goalMl * frac).toInt()
                }
            }
        }
    }
}
