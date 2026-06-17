package de.healthforge.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.MeApi
import de.healthforge.data.network.SubmissionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionsState(
    val loading: Boolean = false,
    val error: String? = null,
    val ingredients: List<SubmissionDto> = emptyList(),
    val recipes: List<SubmissionDto> = emptyList(),
    val supplements: List<SubmissionDto> = emptyList(),
)

@HiltViewModel
class SubmissionsViewModel @Inject constructor(
    private val meApi: MeApi,
) : ViewModel() {

    private val _state = MutableStateFlow(SubmissionsState())
    val state: StateFlow<SubmissionsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { meApi.mySubmissions() }
                .onSuccess { dto ->
                    _state.value = SubmissionsState(
                        ingredients = dto.ingredients,
                        recipes = dto.recipes,
                        supplements = dto.supplements,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message ?: "Fehler")
                }
        }
    }
}
