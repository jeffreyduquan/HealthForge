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

    suspend fun add(day: LocalDate, volumeMl: Int) {
        require(volumeMl in 1..5000) { "volumeMl must be 1..5000" }
        dao.insert(
            WaterIntakeEntity(
                loggedAt = System.currentTimeMillis(),
                dayDateIso = day.toString(),
                volumeMl = volumeMl,
            )
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
