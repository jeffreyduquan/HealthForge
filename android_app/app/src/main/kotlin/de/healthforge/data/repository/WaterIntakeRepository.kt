package de.healthforge.data.repository

import de.healthforge.data.db.dao.WaterIntakeDao
import de.healthforge.data.db.entities.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterIntakeRepository @Inject constructor(
    private val dao: WaterIntakeDao,
) {

    fun observeSumForDay(day: LocalDate): Flow<Int> = dao.observeSumForDay(day.toString())

    /** P7.S4 Slice 4c — Snapshot-Read für WaterDeficitScheduler (AlarmReceiver-Pfad). */
    suspend fun sumForDay(day: LocalDate): Int = dao.sumForDay(day.toString())

    fun observeForDay(day: LocalDate): Flow<List<WaterIntakeEntity>> = dao.observeForDay(day.toString())

    /** Returns daily total ml for each day in start..end (REQ-HOME-TREND-001 water). */
    fun observeSumForDateRange(start: LocalDate, end: LocalDate): Flow<Map<String, Int>> =
        dao.observeWaterForDateRange(start.toString(), end.toString()).map { entries ->
            val map = mutableMapOf<String, Int>()
            for (e in entries) {
                map[e.dayDateIso] = (map[e.dayDateIso] ?: 0) + e.volumeMl
            }
            // Fill missing days with 0
            var d = start
            while (!d.isAfter(end)) {
                val iso = d.toString()
                if (iso !in map) map[iso] = 0
                d = d.plusDays(1)
            }
            map.toSortedMap()
        }

    /** Returns the inserted row id (REQ-WATER-001/002, P6.S7 F-005 Undo-Support). */
    suspend fun add(day: LocalDate, volumeMl: Int): Long {
        require(volumeMl in 1..5000) { "volumeMl must be 1..5000" }
        return dao.insert(
            WaterIntakeEntity(
                loggedAt = System.currentTimeMillis(),
                dayDateIso = day.toString(),
                volumeMl = volumeMl,
            )
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    /**
     * P7.S3a / REQ-HOME-WATER-BAR-001 — setzt die absolute Tagesmenge.
     *
     * Der Slider auf dem Home-Screen repräsentiert den absoluten Wert
     * (nicht ein Delta zum Aufaddieren). Beim Setzen werden alle bisherigen
     * Einträge des Tages durch genau einen Aggregat-Eintrag mit [totalMl]
     * ersetzt. Bei [totalMl] == 0 verbleibt der Tag eintragslos.
     */
    suspend fun setDayTotal(day: LocalDate, totalMl: Int) {
        require(totalMl >= 0) { "totalMl must be >= 0" }
        dao.replaceDayTotal(day.toString(), totalMl, System.currentTimeMillis())
    }
}
