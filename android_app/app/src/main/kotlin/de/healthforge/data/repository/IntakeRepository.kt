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
            entries.filter { it.consumed }.fold(DayNutrientTotals.ZERO) { acc, e ->
                // Supplements store per-dose values, not per-100g → multiplier = 1.0
                val f = if (e.sourceType == IntakeSourceType.SUPPLEMENT) 1.0 else e.portionGrams / 100.0
                DayNutrientTotals(
                    kcal = acc.kcal + (e.snapshotKcalPer100g ?: 0.0) * f,
                    proteinG = acc.proteinG + (e.snapshotProteinPer100g ?: 0.0) * f,
                    carbsG = acc.carbsG + (e.snapshotCarbsPer100g ?: 0.0) * f,
                    fatG = acc.fatG + (e.snapshotFatPer100g ?: 0.0) * f,
                )
            }
        }

    /**
     * Returns a map of ISO-date to DayNutrientTotals for all days in start..end.
     * ZERO for days with no entries. Used for 7-day sparklines (REQ-HOME-TREND-001).
     */
    fun observeTotalsForDateRange(start: LocalDate, end: LocalDate): Flow<Map<String, DayNutrientTotals>> =
        dao.observeForDateRange(start.toString(), end.toString()).map { entries ->
            val dates = java.util.HashSet<String>()
            val map = java.util.LinkedHashMap<String, DayNutrientTotals>()
            for (e in entries) {
                if (!e.consumed) continue
                dates.add(e.dayDateIso)
                // Supplements store per-dose values, not per-100g → multiplier = 1.0
                val f = if (e.sourceType == IntakeSourceType.SUPPLEMENT) 1.0 else e.portionGrams / 100.0
                val prev = map.getOrDefault(e.dayDateIso, DayNutrientTotals.ZERO)
                map[e.dayDateIso] = DayNutrientTotals(
                    kcal = prev.kcal + (e.snapshotKcalPer100g ?: 0.0) * f,
                    proteinG = prev.proteinG + (e.snapshotProteinPer100g ?: 0.0) * f,
                    carbsG = prev.carbsG + (e.snapshotCarbsPer100g ?: 0.0) * f,
                    fatG = prev.fatG + (e.snapshotFatPer100g ?: 0.0) * f,
                )
            }
            // Ensure all dates in range are present (missing = ZERO)
            var d = start
            while (!d.isAfter(end)) {
                val iso = d.toString()
                if (iso !in map) map[iso] = DayNutrientTotals.ZERO
                d = d.plusDays(1)
            }
            map.toSortedMap()
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
