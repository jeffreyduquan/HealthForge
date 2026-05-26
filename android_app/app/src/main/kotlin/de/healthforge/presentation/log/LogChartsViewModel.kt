package de.healthforge.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.repository.LogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DayBucket(
    val date: LocalDate,
    val severityAvg: Double?,
    val entryCount: Int,
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
        repo.observeRange(from, now + 24L * 3600L * 1000L).map { entries ->
            build(days, entries, zone)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    fun setRange(days: Int) { _range.value = days }

    private fun build(
        days: Int,
        entries: List<LogEntryEntity>,
        zone: ZoneId,
    ): ChartsUiState {
        val today = LocalDate.now(zone)
        val first = today.minusDays((days - 1).toLong())
        val byDay = entries.groupBy {
            java.time.Instant.ofEpochMilli(it.occurredAtEpochMs).atZone(zone).toLocalDate()
        }
        val buckets = (0 until days).map { offset ->
            val d = first.plusDays(offset.toLong())
            val list = byDay[d].orEmpty()
            val sevAvg = list.map { it.severity }.takeIf { it.isNotEmpty() }?.average()
            DayBucket(d, sevAvg, list.size)
        }
        return ChartsUiState(rangeDays = days, data = buckets, isLoading = false)
    }
}
