package de.healthforge.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.LogEntrySymptomEntity
import de.healthforge.data.repository.LogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DayBucket(
    val date: LocalDate,
    val moodAvg: Double?,
    val severityAvg: Double?,
)

data class ChartsUiState(
    val rangeDays: Int = 7,
    val data: List<DayBucket> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogChartsViewModel @Inject constructor(
    private val repo: LogRepository,
) : ViewModel() {

    private val _range = MutableStateFlow(7)

    val state: StateFlow<ChartsUiState> = _range.flatMapLatest { days ->
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val from = now - days * 24L * 3600L * 1000L
        repo.observeRange(from, now + 24L * 3600L * 1000L).flatMapLatest { entries ->
            val ids = entries.map { it.id }
            combine(
                kotlinx.coroutines.flow.flowOf(entries),
                repo.observeSymptomsForEntries(ids),
            ) { e, sym ->
                build(days, e, sym, zone)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    fun setRange(days: Int) { _range.value = days }

    private fun build(
        days: Int,
        entries: List<LogEntryEntity>,
        symptomRows: List<LogEntrySymptomEntity>,
        zone: ZoneId,
    ): ChartsUiState {
        val today = LocalDate.now(zone)
        val first = today.minusDays((days - 1).toLong())
        val sympByEntry = symptomRows.groupBy { it.entryId }
        val byDay = entries.groupBy {
            java.time.Instant.ofEpochMilli(it.occurredAtEpochMs).atZone(zone).toLocalDate()
        }
        val buckets = (0 until days).map { offset ->
            val d = first.plusDays(offset.toLong())
            val list = byDay[d].orEmpty()
            val moodAvg = list.map { it.mood }.takeIf { it.isNotEmpty() }?.average()
            val severities = list.flatMap { e ->
                sympByEntry[e.id].orEmpty().map { it.severity }
            }
            val sevAvg = severities.takeIf { it.isNotEmpty() }?.average()
            DayBucket(d, moodAvg, sevAvg)
        }
        return ChartsUiState(rangeDays = days, data = buckets, isLoading = false)
    }
}
