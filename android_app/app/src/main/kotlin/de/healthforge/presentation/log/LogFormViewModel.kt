package de.healthforge.presentation.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import de.healthforge.data.repository.LogRepository
import de.healthforge.domain.IsLogEntryEditableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogFormUiState(
    val entryId: Long = 0L,
    val occurredAtEpochMs: Long = System.currentTimeMillis(),
    val mood: Int = 5,
    val sleepQuality: Int? = null,
    val sleepHours: String = "",
    val selectedSymptoms: Map<Long, Int> = emptyMap(),
    val tags: List<String> = emptyList(),
    val note: String = "",
    val symptoms: List<SymptomDefEntity> = emptyList(),
    val editable: Boolean = true,
    val isLoading: Boolean = true,
    val message: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class LogFormViewModel @Inject constructor(
    private val repo: LogRepository,
    private val isEditable: IsLogEntryEditableUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val entryId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L
    private val _state = MutableStateFlow(LogFormUiState(entryId = entryId))
    val state: StateFlow<LogFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val symptoms = repo.allSymptoms()
            if (entryId != 0L) {
                val details = repo.loadWithDetails(entryId)
                if (details == null) {
                    _state.update { it.copy(isLoading = false, message = "Eintrag nicht gefunden") }
                } else {
                    _state.update {
                        it.copy(
                            entryId = details.entry.id,
                            occurredAtEpochMs = details.entry.occurredAtEpochMs,
                            mood = details.entry.mood,
                            sleepQuality = details.entry.sleepQuality,
                            sleepHours = details.entry.sleepHours?.toString().orEmpty(),
                            selectedSymptoms = details.symptoms.associate { (s, sev) -> s.id to sev },
                            tags = details.tags,
                            note = details.entry.note.orEmpty(),
                            symptoms = symptoms,
                            editable = isEditable(details.entry.occurredAtEpochMs),
                            isLoading = false,
                        )
                    }
                }
            } else {
                _state.update { it.copy(symptoms = symptoms, isLoading = false) }
            }
        }
    }

    fun setMood(v: Int) = _state.update { it.copy(mood = v.coerceIn(1, 10)) }
    fun setSleepQuality(v: Int?) = _state.update { it.copy(sleepQuality = v?.coerceIn(1, 5)) }
    fun setSleepHours(v: String) = _state.update { it.copy(sleepHours = v) }
    fun toggleSymptom(id: Long) = _state.update { s ->
        val cur = s.selectedSymptoms.toMutableMap()
        if (cur.containsKey(id)) cur.remove(id) else cur[id] = 3
        s.copy(selectedSymptoms = cur)
    }
    fun setSeverity(id: Long, sev: Int) = _state.update { s ->
        val cur = s.selectedSymptoms.toMutableMap()
        cur[id] = sev.coerceIn(1, 5)
        s.copy(selectedSymptoms = cur)
    }
    fun addTag(tag: String) = _state.update {
        val t = tag.trim()
        if (t.isEmpty() || it.tags.contains(t)) it else it.copy(tags = it.tags + t)
    }
    fun removeTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    fun save() {
        val s = _state.value
        if (!s.editable) {
            _state.update { it.copy(message = "Eintrag ist nicht mehr editierbar") }
            return
        }
        val hours = s.sleepHours.replace(',', '.').toDoubleOrNull()
        if (s.sleepHours.isNotBlank() && hours == null) {
            _state.update { it.copy(message = "Schlafstunden ungültig") }
            return
        }
        viewModelScope.launch {
            runCatching {
                repo.upsert(
                    entry = LogEntryEntity(
                        id = s.entryId,
                        occurredAtEpochMs = s.occurredAtEpochMs,
                        mood = s.mood,
                        sleepQuality = s.sleepQuality,
                        sleepHours = hours,
                        note = s.note.ifBlank { null },
                    ),
                    symptoms = s.selectedSymptoms.toList(),
                    tags = s.tags,
                )
            }.onSuccess { _state.update { it.copy(saved = true) } }
                .onFailure { _state.update { it.copy(message = "Speichern fehlgeschlagen: ${it.message}") } }
        }
    }

    fun delete() {
        val id = _state.value.entryId
        if (id == 0L) return
        viewModelScope.launch {
            runCatching { repo.delete(id) }
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { it.copy(message = "Löschen fehlgeschlagen") } }
        }
    }
}
