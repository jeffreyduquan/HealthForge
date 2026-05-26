package de.healthforge.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.repository.ExportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repo: ExportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun downloadServerJson() = run("Server-Daten (JSON)") { repo.downloadServerJson() }
    fun downloadServerPdf() = run("Server-Daten (PDF)") { repo.downloadServerPdf() }
    fun exportLocalJson() = run("Lokale Daten (JSON)") { repo.exportLocalJson() }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun run(label: String, block: suspend () -> Result<android.net.Uri>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = block()
            val msg = result.fold(
                onSuccess = { "$label gespeichert: $it" },
                onFailure = { "$label fehlgeschlagen: ${it.message ?: it::class.simpleName}" },
            )
            _state.update { ExportUiState(busy = false, message = msg) }
        }
    }
}
