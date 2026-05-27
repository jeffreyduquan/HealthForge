package de.healthforge.data.repository

import de.healthforge.data.db.dao.WaterIntakeDao
import de.healthforge.data.db.entities.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterIntakeRepository @Inject constructor(
    private val dao: WaterIntakeDao,
) {

    fun observeSumForDay(day: LocalDate): Flow<Int> = dao.observeSumForDay(day.toString())

    fun observeForDay(day: LocalDate): Flow<List<WaterIntakeEntity>> = dao.observeForDay(day.toString())

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
        require(totalMl in 0..5000) { "totalMl must be 0..5000" }
        dao.replaceDayTotal(day.toString(), totalMl, System.currentTimeMillis())
    }
}
