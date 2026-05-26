package de.healthforge.data.repository

import de.healthforge.data.db.dao.IntakeEntryDao
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Aggregated nutrient totals for a single day. */
data class DayNutrientTotals(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    companion object { val ZERO = DayNutrientTotals(0.0, 0.0, 0.0, 0.0) }
}

@Singleton
class IntakeRepository @Inject constructor(
    private val dao: IntakeEntryDao,
) {

    fun observeForDay(day: LocalDate): Flow<List<IntakeEntryEntity>> =
        dao.observeForDay(day.toString())

    fun observeTotalsForDay(day: LocalDate): Flow<DayNutrientTotals> =
        dao.observeForDay(day.toString()).map { entries ->
            entries.fold(DayNutrientTotals.ZERO) { acc, e ->
                val f = e.portionGrams / 100.0
                DayNutrientTotals(
                    kcal = acc.kcal + (e.snapshotKcalPer100g ?: 0.0) * f,
                    proteinG = acc.proteinG + (e.snapshotProteinPer100g ?: 0.0) * f,
                    carbsG = acc.carbsG + (e.snapshotCarbsPer100g ?: 0.0) * f,
                    fatG = acc.fatG + (e.snapshotFatPer100g ?: 0.0) * f,
                )
            }
        }

    fun observeRecent(limit: Int = 200): Flow<List<IntakeEntryEntity>> =
        dao.observeRecent(limit = limit)

    /** Returns up to [limit] most-recently-used `(sourceType, sourceId)` refs (REQ-HOME-004 quick-add). */
    fun observeRecentRefs(limit: Int = 6): Flow<List<Pair<IntakeSourceType, String>>> =
        dao.observeRecentRefs(limit = limit).map { rows ->
            rows.mapNotNull { row ->
                val idx = row.indexOf(':')
                if (idx <= 0) null else {
                    val t = runCatching { IntakeSourceType.valueOf(row.substring(0, idx)) }.getOrNull()
                    val id = row.substring(idx + 1)
                    if (t == null) null else t to id
                }
            }
        }

    suspend fun add(entry: IntakeEntryEntity): Long = dao.insert(entry)

    suspend fun update(entry: IntakeEntryEntity) = dao.update(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun byId(id: Long): IntakeEntryEntity? = dao.byId(id)

    companion object {
        /** Convenience: today as ISO-string in the device's local zone. */
        fun todayIso(): String = LocalDate.now(ZoneId.systemDefault()).toString()
    }
}
