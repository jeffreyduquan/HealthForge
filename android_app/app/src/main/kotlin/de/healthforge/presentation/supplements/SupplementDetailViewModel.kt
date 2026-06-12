package de.healthforge.presentation.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplementDetailState(
    val supplement: SupplementEntity? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class SupplementDetailViewModel @Inject constructor(
    private val supplementRepo: SupplementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplementDetailState())
    val state: StateFlow<SupplementDetailState> = _state.asStateFlow()

    fun load(id: String) {
        val longId = id.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val sup = supplementRepo.byId(longId)
            _state.value = SupplementDetailState(supplement = sup, loading = false)
        }
    }
}
