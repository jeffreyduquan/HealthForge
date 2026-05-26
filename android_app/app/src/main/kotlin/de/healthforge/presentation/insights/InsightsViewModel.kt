package de.healthforge.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.domain.insights.CalculateInsightsUseCase
import de.healthforge.domain.insights.InsightsReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val loading: Boolean = false,
    val report: InsightsReport? = null,
    val error: String? = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val useCase: CalculateInsightsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { useCase() }
                .onSuccess { r -> _state.update { it.copy(loading = false, report = r) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Fehler") } }
        }
    }
}
