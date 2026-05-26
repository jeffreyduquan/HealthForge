package de.healthforge.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.dao.ShoppingListDao
import de.healthforge.data.db.entities.ShoppingListItemEntity
import de.healthforge.domain.shopping.BuildShoppingListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ShoppingListUiState(
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now().plusDays(2),
    val runId: Long? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val build: BuildShoppingListUseCase,
    private val dao: ShoppingListDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoppingListUiState())
    val state: StateFlow<ShoppingListUiState> = _state.asStateFlow()

    val items: StateFlow<List<ShoppingListItemEntity>> =
        _state
            .flatMapLatest { s -> s.runId?.let { dao.observeRun(it) } ?: emptyFlow() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val latest = dao.latestRunId()
            if (latest != null) _state.update { it.copy(runId = latest) }
        }
    }

    fun setRange(start: LocalDate, end: LocalDate) {
        if (end.isBefore(start)) return
        _state.update { it.copy(start = start, end = end) }
    }

    fun generate() {
        if (_state.value.isLoading) return
        val s = _state.value
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { build.build(s.start, s.end) }
                .fold(
                    onSuccess = { newRunId ->
                        if (newRunId == null) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    message = "Keine planten Mahlzeiten im gewählten Zeitraum.",
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    runId = newRunId,
                                    message = "Einkaufsliste aktualisiert.",
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(isLoading = false, message = e.message ?: "Fehler")
                        }
                    },
                )
        }
    }

    fun toggle(id: Long, checked: Boolean) {
        viewModelScope.launch { dao.setChecked(id, checked) }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
