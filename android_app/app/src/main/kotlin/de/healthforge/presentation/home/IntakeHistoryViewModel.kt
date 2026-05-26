package de.healthforge.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.repository.IntakeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntakeHistoryViewModel @Inject constructor(
    private val intakeRepo: IntakeRepository,
) : ViewModel() {

    val entries: StateFlow<List<IntakeEntryEntity>> =
        intakeRepo.observeRecent(limit = 500)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { intakeRepo.deleteById(id) }
    }
}
