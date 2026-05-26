package de.healthforge.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import de.healthforge.data.repository.LogRepository
import de.healthforge.domain.IsLogEntryEditableUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryRowUi(
    val entry: LogEntryEntity,
    val symptomNames: List<String>,
    val tagCount: Int,
    val editable: Boolean,
)

data class QuickAddDraft(
    val mood: Int = 5,
    val sleepQuality: Int? = null,
    val sleepHours: String = "",
    val selectedSymptoms: Map<Long, Int> = emptyMap(), // symptomId → severity 1..5
    val tags: List<String> = emptyList(),
    val note: String = "",
)

data class LogUiState(
    val symptoms: List<SymptomDefEntity> = emptyList(),
    val rows: List<EntryRowUi> = emptyList(),
    val draft: QuickAddDraft = QuickAddDraft(),
    val message: String? = null,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogViewModel @Inject constructor(
    private val repo: LogRepository,
    private val isEditable: IsLogEntryEditableUseCase,
) : ViewModel() {

    private val _draft = MutableStateFlow(QuickAddDraft())
    private val _message = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)

    private val rowsFlow = repo.observeRecent(50).flatMapLatest { entries ->
        val ids = entries.map { it.id }
        combine(
            repo.observeSymptomsForEntries(ids),
            repo.observeTagsForEntries(ids),
            repo.observeSymptoms(),
        ) { sympRows, tagRows, defs ->
            val defById = defs.associateBy { it.id }
            entries.map { e ->
                val symNames = sympRows.filter { it.entryId == e.id }
                    .mapNotNull { defById[it.symptomId]?.name }
                val tags = tagRows.count { it.entryId == e.id }
                EntryRowUi(
                    entry = e,
                    symptomNames = symNames,
                    tagCount = tags,
                    editable = isEditable(e.occurredAtEpochMs),
                )
            }
        }
    }

    val state: StateFlow<LogUiState> = combine(
        repo.observeSymptoms(),
        rowsFlow,
        _draft,
        _message,
        _isSaving,
    ) { symptoms, rows, draft, msg, saving ->
        LogUiState(symptoms = symptoms, rows = rows, draft = draft, message = msg, isSaving = saving)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun setMood(v: Int) = _draft.update { it.copy(mood = v.coerceIn(1, 10)) }
    fun setSleepQuality(v: Int?) = _draft.update { it.copy(sleepQuality = v?.coerceIn(1, 5)) }
    fun setSleepHours(v: String) = _draft.update { it.copy(sleepHours = v) }
    fun toggleSymptom(id: Long) = _draft.update { d ->
        val cur = d.selectedSymptoms.toMutableMap()
        if (cur.containsKey(id)) cur.remove(id) else cur[id] = 3
        d.copy(selectedSymptoms = cur)
    }
    fun setSeverity(symptomId: Long, sev: Int) = _draft.update { d ->
        val cur = d.selectedSymptoms.toMutableMap()
        cur[symptomId] = sev.coerceIn(1, 5)
        d.copy(selectedSymptoms = cur)
    }
    fun addTag(tag: String) = _draft.update {
        val t = tag.trim()
        if (t.isEmpty() || it.tags.contains(t)) it else it.copy(tags = it.tags + t)
    }
    fun removeTag(tag: String) = _draft.update { it.copy(tags = it.tags - tag) }
    fun setNote(v: String) = _draft.update { it.copy(note = v) }

    fun addCustomSymptom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.addCustomSymptom(trimmed) }
                .onFailure { _message.value = "Symptom konnte nicht angelegt werden" }
        }
    }

    fun clearMessage() { _message.value = null }

    fun save() {
        val d = _draft.value
        val hours = d.sleepHours.replace(',', '.').toDoubleOrNull()
        if (d.sleepHours.isNotBlank() && hours == null) {
            _message.value = "Schlafstunden ungültig"
            return
        }
        _isSaving.value = true
        viewModelScope.launch {
            runCatching {
                repo.upsert(
                    entry = LogEntryEntity(
                        occurredAtEpochMs = System.currentTimeMillis(),
                        mood = d.mood,
                        sleepQuality = d.sleepQuality,
                        sleepHours = hours,
                        note = d.note.ifBlank { null },
                    ),
                    symptoms = d.selectedSymptoms.toList(),
                    tags = d.tags,
                )
            }.onSuccess {
                _draft.value = QuickAddDraft()
                _message.value = "Eintrag gespeichert"
            }.onFailure {
                _message.value = "Speichern fehlgeschlagen: ${it.message}"
            }
            _isSaving.value = false
        }
    }
}
