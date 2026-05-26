package de.healthforge.presentation.essen.rezepte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.RecipeDetailDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

@HiltViewModel
class RecipeBrowseViewModel @Inject constructor(
    private val repo: RecipeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeBrowseUiState())
    val state: StateFlow<RecipeBrowseUiState> = _state.asStateFlow()

    init { refresh() }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
    }

    fun toggleSlot(slot: String) {
        _state.update {
            val next = it.slotFilter.toMutableSet().apply { if (!add(slot)) remove(slot) }
            it.copy(slotFilter = next)
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
            val result = repo.browse(
                q = s.query,
                slot = s.slotFilter.toList(),
                prepMax = s.prepMaxMinutes,
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
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repo: RecipeRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedState["id"]) { "missing nav arg `id`" }

    private val _state = MutableStateFlow(RecipeDetailUiState())
    val state: StateFlow<RecipeDetailUiState> = _state.asStateFlow()

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
                    load()
                    _state.update { it.copy(ratingBusy = false) }
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
}
