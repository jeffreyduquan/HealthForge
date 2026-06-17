package de.healthforge.presentation.essen.rezepte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.RecipeDetailDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.network.GroupSummaryDto
import de.healthforge.data.repository.GroupRepository
import de.healthforge.data.repository.IntakeRepository
import de.healthforge.data.repository.RecipeRepository
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.home.HomeViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================ Browse ============================

data class RecipeBrowseUiState(
    val items: List<RecipeListItemDto> = emptyList(),
    val query: String = "",
    val slotFilter: Set<String> = emptySet(),
    val prepMaxMinutes: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val applyProfileFilters: Boolean = true,
    val excludedAllergens: Set<AllergenType> = emptySet(),
    val excludedFodmap: Set<FodmapType> = emptySet(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeBrowseViewModel @Inject constructor(
    private val repo: RecipeRepository,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeBrowseUiState())
    val state: StateFlow<RecipeBrowseUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    val pinnedKeys: StateFlow<List<String>> = profileRepo.observe()
        .map { HomeViewModel.parsePinnedKeys(it.profile?.pinnedNutrientsJson) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutrientCatalog.defaultPinnedKeys)

    init {
        // Hydrate profile filter state
        viewModelScope.launch {
            val full = profileRepo.observe().first()
            _state.update { it.copy(
                excludedAllergens = full.allergies,
                excludedFodmap = full.intolerances,
            ) }
            refresh()
        }
        // Debounced search on query changes
        queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
    }

    fun toggleSlot(slot: String) {
        _state.update {
            val next = it.slotFilter.toMutableSet().apply { if (!add(slot)) remove(slot) }
            it.copy(slotFilter = next)
        }
        refresh()
    }

    fun toggleApplyProfileFilters() {
        _state.update { it.copy(applyProfileFilters = !it.applyProfileFilters) }
        refresh()
    }

    fun toggleAllergen(a: AllergenType) {
        _state.update {
            it.copy(excludedAllergens = if (a in it.excludedAllergens) it.excludedAllergens - a else it.excludedAllergens + a)
        }
        refresh()
    }

    fun toggleFodmap(f: FodmapType) {
        _state.update {
            it.copy(excludedFodmap = if (f in it.excludedFodmap) it.excludedFodmap - f else it.excludedFodmap + f)
        }
        refresh()
    }

    fun setPrepMax(max: Int?) {
        _state.update { it.copy(prepMaxMinutes = max) }
        refresh()
    }

    fun refresh() {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val excludeAllergens = if (s.applyProfileFilters)
                s.excludedAllergens.map { it.name } + s.excludedFodmap.map { it.name }
            else emptyList()
            val result = repo.browse(
                q = s.query,
                slot = s.slotFilter.toList(),
                prepMax = s.prepMaxMinutes,
                excludeAllergens = excludeAllergens.takeIf { it.isNotEmpty() },
                scope = "PUBLIC_OR_MINE",
            )
            result.fold(
                onSuccess = { list -> _state.update { it.copy(items = list, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Fehler") } },
            )
        }
    }

    fun search() = refresh()
}

// ============================ Detail ============================

data class RecipeDetailUiState(
    val recipe: RecipeDetailDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val likeBusy: Boolean = false,
    val ratingBusy: Boolean = false,
    val reportBusy: Boolean = false,
    val reportSubmitted: Boolean = false,
    val message: String? = null,
    val myGroups: List<GroupSummaryDto>? = null, // null = dialog closed
    val showAddToPlan: Boolean = false,
    val navigateHome: Boolean = false,
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repo: RecipeRepository,
    private val groupRepo: GroupRepository,
    private val intakeRepo: IntakeRepository,
    private val tokenStore: de.healthforge.data.prefs.SecureTokenStore,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedState["id"]) { "missing nav arg `id`" }

    private val _state = MutableStateFlow(RecipeDetailUiState())
    val state: StateFlow<RecipeDetailUiState> = _state.asStateFlow()

    val currentUserId: String? = tokenStore.userId

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repo.detail(recipeId).fold(
                onSuccess = { d -> _state.update { it.copy(recipe = d, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Fehler") } },
            )
        }
    }

    fun toggleLike() {
        val current = _state.value.recipe ?: return
        if (_state.value.likeBusy) return
        _state.update { it.copy(likeBusy = true) }
        viewModelScope.launch {
            val res = if (current.liked_by_me) repo.unlike(recipeId) else repo.like(recipeId)
            res.fold(
                onSuccess = {
                    val newLiked = !current.liked_by_me
                    val newCount = if (newLiked) current.like_count + 1 else (current.like_count - 1).coerceAtLeast(0)
                    _state.update {
                        it.copy(
                            recipe = current.copy(liked_by_me = newLiked, like_count = newCount),
                            likeBusy = false,
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(likeBusy = false, error = e.message) } },
            )
        }
    }

    fun rate(value: String?) {
        val current = _state.value.recipe ?: return
        if (_state.value.ratingBusy) return
        _state.update { it.copy(ratingBusy = true) }
        viewModelScope.launch {
            val res = if (value == null) repo.revokeCommunityRating(recipeId)
                      else repo.communityRate(recipeId, value)
            res.fold(
                onSuccess = {
                    // Optimistic update — NO full reload (was: load())
                    val prevRecommend = if (current.my_community_rating == "RECOMMEND") current.community_recommend_count - 1 else current.community_recommend_count
                    val prevNot = if (current.my_community_rating == "NOT_RECOMMEND") current.community_not_recommend_count - 1 else current.community_not_recommend_count
                    val newRecommend = if (value == "RECOMMEND") prevRecommend + 1 else prevRecommend
                    val newNot = if (value == "NOT_RECOMMEND") prevNot + 1 else prevNot

                    _state.update {
                        it.copy(
                            recipe = current.copy(
                                my_community_rating = value,
                                community_recommend_count = newRecommend.coerceAtLeast(0),
                                community_not_recommend_count = newNot.coerceAtLeast(0),
                            ),
                            ratingBusy = false,
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(ratingBusy = false, error = e.message) } },
            )
        }
    }

    fun report(reason: String) {
        if (_state.value.reportBusy) return
        _state.update { it.copy(reportBusy = true, message = null) }
        viewModelScope.launch {
            repo.report(recipeId, reason).fold(
                onSuccess = {
                    _state.update { it.copy(reportBusy = false, reportSubmitted = true, message = "Meldung gesendet. Danke!") }
                },
                onFailure = { e ->
                    _state.update { it.copy(reportBusy = false, message = e.message ?: "Melden fehlgeschlagen") }
                },
            )
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }

    /** OPEN: Zeigt Dialog mit Gruppen, in denen User Admin/Owner/Contributor ist. */
    fun openAddToGroupDialog() {
        viewModelScope.launch {
            groupRepo.myGroups().onSuccess { groups ->
                val manageable = groups.filter { g ->
                    g.myRole in listOf("OWNER", "ADMIN", "CONTRIBUTOR")
                }
                _state.update { it.copy(myGroups = manageable) }
            }
        }
    }

    /** Weist das Rezept einer Gruppe zu. */
    fun assignToGroup(groupId: String) {
        viewModelScope.launch {
            repo.assignToGroup(recipeId, groupId).fold(
                onSuccess = { _state.update { it.copy(message = "Rezept zur Gruppe hinzugefügt", myGroups = null) } },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Fehler", myGroups = null) } },
            )
        }
    }

    fun closeAddToGroupDialog() { _state.update { it.copy(myGroups = null) } }

    fun openAddToPlanDialog() { _state.update { it.copy(showAddToPlan = true) } }
    fun dismissAddToPlanDialog() { _state.update { it.copy(showAddToPlan = false) } }

    fun addToPlan(grams: Double) {
        val r = _state.value.recipe ?: return
        viewModelScope.launch {
            intakeRepo.add(IntakeEntryEntity(
                loggedAt = System.currentTimeMillis(),
                dayDateIso = java.time.LocalDate.now().toString(),
                sourceType = IntakeSourceType.RECIPE,
                sourceId = r.id,
                portionGrams = grams,
                snapshotName = r.title,
                snapshotKcalPer100g = null,
                snapshotProteinPer100g = null,
                snapshotCarbsPer100g = null,
                snapshotFatPer100g = null,
            ))
            _state.update { it.copy(showAddToPlan = false, navigateHome = true, message = "Zum Plan hinzugefügt") }
        }
    }
}
