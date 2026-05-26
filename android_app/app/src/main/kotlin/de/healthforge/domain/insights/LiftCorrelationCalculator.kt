package de.healthforge.domain.insights

import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.LogEntrySymptomEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Co-occurrence window in milliseconds: symptom that occurs between 4h and 48h after an intake counts. */
private const val MIN_GAP_MS = 4L * 60L * 60L * 1000L
private const val MAX_GAP_MS = 48L * 60L * 60L * 1000L

/** Hard thresholds — REQ-INSIGHT-002. */
const val INSIGHT_MIN_LIFT: Double = 1.5
const val INSIGHT_MIN_N: Int = 3

/** Lock-screen threshold — REQ-INSIGHT-001. */
const val INSIGHT_MIN_LOG_DAYS: Int = 14

/**
 * Per-intake projection: aggregated by (sourceType:sourceId, dayDateIso) for stable food identity.
 * We keep the snapshot name from any intake-row of that food (most-recent wins).
 */
data class FoodRef(val sourceType: String, val sourceId: String) {
    val key: String get() = "$sourceType:$sourceId"
}

data class LogOccurrence(
    val symptomId: Long,
    val symptomName: String,
    val occurredAtEpochMs: Long,
    val severity: Int, // 1..5
)

data class CorrelationInput(
    val intakes: List<IntakeEntryEntity>,
    val logs: List<LogOccurrence>,
)

data class CorrelationResult(
    val food: FoodRef,
    val foodName: String,
    val symptomId: Long,
    val symptomName: String,
    val n: Int,
    val lift: Double,
    val avgSeverity: Double,
    val score: Double, // ranking score = lift * (avgSeverity / 5)
)

data class InsightsReport(
    val distinctLogDays: Int,
    val unlocked: Boolean, // distinctLogDays >= INSIGHT_MIN_LOG_DAYS
    val totalResults: Int, // before threshold cut
    val topResults: List<CorrelationResult>, // ranked, filtered
)

/**
 * Lift-Korrelations-Berechner — pure function. REQ-INSIGHT-001..003.
 *
 * - Food-Identität = sourceType:sourceId (Snapshot-Name aus dem aktuellsten Intake).
 * - Co-Occurrence: ein Symptom zählt zur Food, wenn es 4–48h nach dem Intake auftrat.
 *   Beide Werte werden pro **Tag** (LocalDate von Zeitstempel) gezählt, nicht pro Event,
 *   um Mehrfach-Logs am selben Tag nicht doppelt zu gewichten.
 * - Lift = P(Symptom | Food) / P(Symptom) ; mit P(Symptom)= symptomDays/totalDays,
 *   P(Symptom|Food)= coDays/foodDays.
 * - severity-weighted-Aggregation (REQ-INSIGHT-003): score = lift × avg(severity) / 5.
 * - Filter (REQ-INSIGHT-002): nur Resultate mit lift > 1.5 UND n ≥ 3.
 * - Lock-Screen (REQ-INSIGHT-001): unlocked = (#distinkter Log-Tage) ≥ 14.
 */
object LiftCorrelationCalculator {

    fun compute(input: CorrelationInput, zone: ZoneId = ZoneId.systemDefault()): InsightsReport {
        if (input.intakes.isEmpty() || input.logs.isEmpty()) {
            return InsightsReport(
                distinctLogDays = input.logs.map { dayOf(it.occurredAtEpochMs, zone) }.toSet().size,
                unlocked = false,
                totalResults = 0,
                topResults = emptyList(),
            )
        }

        val logDays: Set<LocalDate> = input.logs.map { dayOf(it.occurredAtEpochMs, zone) }.toSet()
        val distinctLogDays = logDays.size

        // distinct days where any intake OR any log happened
        val intakeDays: Set<LocalDate> = input.intakes.map { LocalDate.parse(it.dayDateIso) }.toSet()
        val totalDays = (logDays + intakeDays).size.coerceAtLeast(1)

        // Group intakes by food + day, and pick canonical snapshotName per food.
        val foodDayMap: MutableMap<FoodRef, MutableMap<LocalDate, MutableList<Long>>> = mutableMapOf()
        val foodNameCache: MutableMap<FoodRef, Pair<Long, String>> = mutableMapOf() // (latestLoggedAt, name)
        for (e in input.intakes) {
            val ref = FoodRef(e.sourceType.name, e.sourceId)
            val date = LocalDate.parse(e.dayDateIso)
            val perDay = foodDayMap.getOrPut(ref) { mutableMapOf() }
            perDay.getOrPut(date) { mutableListOf() }.add(e.loggedAt)
            val existing = foodNameCache[ref]
            if (existing == null || existing.first < e.loggedAt) {
                foodNameCache[ref] = e.loggedAt to e.snapshotName
            }
        }

        // Group logs by symptom + name.
        val symptomNames: MutableMap<Long, String> = mutableMapOf()
        val logsBySymptom: MutableMap<Long, MutableList<LogOccurrence>> = mutableMapOf()
        for (lo in input.logs) {
            symptomNames.putIfAbsent(lo.symptomId, lo.symptomName)
            logsBySymptom.getOrPut(lo.symptomId) { mutableListOf() }.add(lo)
        }

        // For each symptom: distinct days, total severity / count for marginal P(symptom).
        val symptomDayCount: Map<Long, Int> = logsBySymptom.mapValues { (_, occs) ->
            occs.map { dayOf(it.occurredAtEpochMs, zone) }.toSet().size
        }

        val results = mutableListOf<CorrelationResult>()
        for ((food, daysWithIntakes) in foodDayMap) {
            val foodDayCount = daysWithIntakes.size
            if (foodDayCount == 0) continue
            for ((symptomId, occs) in logsBySymptom) {
                val coDays = mutableSetOf<LocalDate>()
                val coSeverities = mutableListOf<Int>()
                for ((day, intakeTimes) in daysWithIntakes) {
                    for (t in intakeTimes) {
                        val matching = occs.firstOrNull { lo ->
                            val gap = lo.occurredAtEpochMs - t
                            gap in MIN_GAP_MS..MAX_GAP_MS
                        }
                        if (matching != null) {
                            coDays.add(day)
                            coSeverities.add(matching.severity)
                        }
                    }
                }
                val n = coDays.size
                if (n < INSIGHT_MIN_N) continue
                val pSymptomGivenFood = n.toDouble() / foodDayCount
                val pSymptom = (symptomDayCount[symptomId] ?: 0).toDouble() / totalDays
                if (pSymptom <= 0.0) continue
                val lift = pSymptomGivenFood / pSymptom
                if (lift <= INSIGHT_MIN_LIFT) continue
                val avgSeverity = coSeverities.average()
                val score = lift * (avgSeverity / 5.0)
                results += CorrelationResult(
                    food = food,
                    foodName = foodNameCache[food]?.second ?: food.key,
                    symptomId = symptomId,
                    symptomName = symptomNames[symptomId] ?: "?",
                    n = n,
                    lift = lift,
                    avgSeverity = avgSeverity,
                    score = score,
                )
            }
        }

        val sorted = results.sortedByDescending { it.score }
        return InsightsReport(
            distinctLogDays = distinctLogDays,
            unlocked = distinctLogDays >= INSIGHT_MIN_LOG_DAYS,
            totalResults = sorted.size,
            topResults = sorted,
        )
    }

    private fun dayOf(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** Companion constant used by tests / call-sites to resolve a symptom name from a [SymptomDefEntity]. */
    fun nameOf(def: SymptomDefEntity): String = def.name
}
