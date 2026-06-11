package de.healthforge.presentation.lebensmittel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.repository.IngredientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientDetailState(
    val item: IngredientDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class IngredientDetailViewModel @Inject constructor(
    private val ingredientRepo: IngredientRepository,
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
}
