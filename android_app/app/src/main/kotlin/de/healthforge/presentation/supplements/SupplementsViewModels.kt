package de.healthforge.presentation.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.data.repository.SupplementRepository
import de.healthforge.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplementsListState(
    val items: List<SupplementEntity> = emptyList(),
)

@HiltViewModel
class SupplementsListViewModel @Inject constructor(
    private val repo: SupplementRepository,
) : ViewModel() {

    val state: StateFlow<SupplementsListState> =
        repo.observeAll()
            .map { SupplementsListState(items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SupplementsListState())

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

data class SupplementEditState(
    val id: Long = 0L,
    val name: String = "",
    val brand: String = "",
    val unitLabel: String = "Tablette",
    val defaultDose: String = "1",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val notes: String = "",
    val reminders: List<SupplementReminderEntity> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val suggesting: Boolean = false,
    val suggestMessage: String? = null,
)

@HiltViewModel
class SupplementEditViewModel @Inject constructor(
    private val repo: SupplementRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplementEditState())
    val state: StateFlow<SupplementEditState> = _state.asStateFlow()

    fun load(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            val s = repo.byId(id) ?: return@launch
            _state.value = _state.value.copy(
                id = s.id,
                name = s.nameDe,
                brand = s.brand.orEmpty(),
                unitLabel = s.unitLabel,
                defaultDose = s.defaultDose.toString(),
                kcal = s.kcalPerDose?.toString().orEmpty(),
                protein = s.proteinPerDose?.toString().orEmpty(),
                carbs = s.carbsPerDose?.toString().orEmpty(),
                fat = s.fatPerDose?.toString().orEmpty(),
                notes = s.notes.orEmpty(),
            )
            repo.observeReminders(id).collect { rs ->
                _state.value = _state.value.copy(reminders = rs)
            }
        }
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun setBrand(v: String) { _state.value = _state.value.copy(brand = v) }
    fun setUnit(v: String) { _state.value = _state.value.copy(unitLabel = v) }
    fun setDose(v: String) { _state.value = _state.value.copy(defaultDose = v) }
    fun setKcal(v: String) { _state.value = _state.value.copy(kcal = v) }
    fun setProtein(v: String) { _state.value = _state.value.copy(protein = v) }
    fun setCarbs(v: String) { _state.value = _state.value.copy(carbs = v) }
    fun setFat(v: String) { _state.value = _state.value.copy(fat = v) }
    fun setNotes(v: String) { _state.value = _state.value.copy(notes = v) }

    fun save() {
        val s = _state.value
        val dose = s.defaultDose.replace(',', '.').toDoubleOrNull()
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name darf nicht leer sein"); return
        }
        if (dose == null || dose <= 0.0) {
            _state.value = s.copy(error = "Dosis muss > 0 sein"); return
        }
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val entity = SupplementEntity(
                    id = s.id,
                    nameDe = s.name.trim(),
                    brand = s.brand.trim().ifEmpty { null },
                    unitLabel = s.unitLabel.trim().ifEmpty { "Stück" },
                    defaultDose = dose,
                    kcalPerDose = s.kcal.replace(',', '.').toDoubleOrNull(),
                    proteinPerDose = s.protein.replace(',', '.').toDoubleOrNull(),
                    carbsPerDose = s.carbs.replace(',', '.').toDoubleOrNull(),
                    fatPerDose = s.fat.replace(',', '.').toDoubleOrNull(),
                    notes = s.notes.trim().ifEmpty { null },
                    createdAt = 0L,
                    updatedAt = 0L,
                )
                val newId = repo.upsert(entity)
                _state.value = _state.value.copy(id = if (s.id == 0L) newId else s.id, saving = false, saved = true)
            } catch (e: Throwable) {
                _state.value = _state.value.copy(saving = false, error = e.message)
            }
        }
    }

    /** Add or update a reminder, then (re-)schedule. */
    fun saveReminder(r: SupplementReminderEntity) {
        viewModelScope.launch {
            val newId = repo.upsertReminder(r.copy(supplementId = _state.value.id.takeIf { it > 0 } ?: r.supplementId))
            val final = repo.reminderById(newId) ?: return@launch
            scheduler.cancel(final.id)
            if (final.enabled) {
                val name = repo.byId(final.supplementId)?.nameDe.orEmpty()
                scheduler.schedule(final, name)
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            scheduler.cancel(id)
            repo.deleteReminder(id)
        }
    }

    fun toggleReminderEnabled(r: SupplementReminderEntity) {
        viewModelScope.launch {
            val updated = r.copy(enabled = !r.enabled)
            repo.upsertReminder(updated)
            scheduler.cancel(updated.id)
            if (updated.enabled) {
                val name = repo.byId(updated.supplementId)?.nameDe.orEmpty()
                scheduler.schedule(updated, name)
            }
        }
    }

    fun newReminderTemplate(): SupplementReminderEntity =
        SupplementReminderEntity(
            id = 0L,
            supplementId = _state.value.id,
            label = "Erinnerung",
            frequency = ReminderFrequency.DAILY,
            hourOfDay = 9,
            minute = 0,
            enabled = true,
            createdAt = 0L,
        )

    /** REQ-SUPP-004 — propose the current supplement entry to the global catalog (Admin-Review). */
    fun suggestPublic() {
        val s = _state.value
        val dose = s.defaultDose.replace(',', '.').toDoubleOrNull()
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name darf nicht leer sein"); return
        }
        if (dose == null || dose <= 0.0) {
            _state.value = s.copy(error = "Dosis muss > 0 sein"); return
        }
        _state.value = s.copy(suggesting = true, suggestMessage = null, error = null)
        viewModelScope.launch {
            val local = SupplementEntity(
                id = 0L,
                nameDe = s.name.trim(),
                brand = s.brand.trim().ifEmpty { null },
                unitLabel = s.unitLabel.trim().ifEmpty { "Stück" },
                defaultDose = dose,
                kcalPerDose = s.kcal.replace(',', '.').toDoubleOrNull(),
                proteinPerDose = s.protein.replace(',', '.').toDoubleOrNull(),
                carbsPerDose = s.carbs.replace(',', '.').toDoubleOrNull(),
                fatPerDose = s.fat.replace(',', '.').toDoubleOrNull(),
                notes = s.notes.trim().ifEmpty { null },
                createdAt = 0L,
                updatedAt = 0L,
            )
            repo.suggestPublic(local).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        suggesting = false,
                        suggestMessage = "Vorschlag eingereicht. Ein Admin prüft den Eintrag.",
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        suggesting = false,
                        suggestMessage = "Vorschlag fehlgeschlagen: ${e.message ?: "Unbekannter Fehler"}",
                    )
                },
            )
        }
    }

    fun clearSuggestMessage() {
        _state.value = _state.value.copy(suggestMessage = null)
    }
}
