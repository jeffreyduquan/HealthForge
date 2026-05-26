package de.healthforge.presentation.lebensmittel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.ProfileRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LebensmittelState(
    val query: String = "",
    val results: List<IngredientDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val applyProfileFilters: Boolean = true,
    val excludedAllergens: Set<AllergenType> = emptySet(),
    val excludedFodmap: Set<FodmapType> = emptySet(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LebensmittelViewModel @Inject constructor(
    private val ingredients: IngredientRepository,
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LebensmittelState())
    val state: StateFlow<LebensmittelState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        // Hydrate the default filter state from the user's profile (REQ-QUALITY-FIX-001).
        viewModelScope.launch {
            val full = profile.observe().first()
            _state.value = _state.value.copy(
                excludedAllergens = full.allergies,
                excludedFodmap = full.intolerances,
            )
        }
        // Debounced search trigger.
        queryFlow
            .debounce(250)
            .distinctUntilChanged()
            .onEach { q -> runSearch(q) }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(q: String) {
        _state.value = _state.value.copy(query = q)
        queryFlow.value = q
    }

    fun toggleApplyProfileFilters() {
        _state.value = _state.value.copy(applyProfileFilters = !_state.value.applyProfileFilters)
        runSearch(_state.value.query)
    }

    fun toggleAllergen(a: AllergenType) {
        val cur = _state.value.excludedAllergens
        _state.value = _state.value.copy(
            excludedAllergens = if (a in cur) cur - a else cur + a,
        )
        runSearch(_state.value.query)
    }

    fun toggleFodmap(f: FodmapType) {
        val cur = _state.value.excludedFodmap
        _state.value = _state.value.copy(
            excludedFodmap = if (f in cur) cur - f else cur + f,
        )
        runSearch(_state.value.query)
    }

    private fun runSearch(q: String) {
        searchJob?.cancel()
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(results = emptyList(), loading = false, error = null)
            return
        }
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val s = _state.value
            val excludeAllergens = if (s.applyProfileFilters) s.excludedAllergens else emptySet()
            val excludeFodmap = if (s.applyProfileFilters) s.excludedFodmap else emptySet()
            ingredients.search(trimmed, 50, excludeAllergens, excludeFodmap)
                .onSuccess { list ->
                    _state.value = _state.value.copy(results = list, loading = false)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        results = emptyList(),
                        loading = false,
                        error = t.message ?: "Fehler bei der Suche",
                    )
                }
        }
    }
}
