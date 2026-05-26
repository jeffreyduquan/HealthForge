package de.healthforge.domain.insights

import de.healthforge.data.db.dao.IntakeEntryDao
import de.healthforge.data.db.dao.LogEntryDao
import de.healthforge.data.db.dao.SymptomDefDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads local Room data + invokes [LiftCorrelationCalculator]. Strict local-only:
 * no network / Retrofit imports anywhere in this package (REQ-INSIGHT-* privacy guarantee).
 *
 * Manual-trigger from `InsightsScreen` is the supported entry point (WorkManager-Job ist
 * akzeptierter Drift in P4.S3, siehe SprintPlan).
 */
@Singleton
class CalculateInsightsUseCase @Inject constructor(
    private val intakeDao: IntakeEntryDao,
    private val logDao: LogEntryDao,
    private val symptomDefDao: SymptomDefDao,
) {

    suspend operator fun invoke(): InsightsReport {
        val intakes = intakeDao.listAll()
        val logs = logDao.listAll()
        val symptomDefs = symptomDefDao.all().associateBy { it.id }
        // Pull symptoms for every log-entry; this is O(n) DB calls but bounded by recent log count.
        val occurrences = buildList {
            for (entry in logs) {
                val rows = logDao.symptomsForEntry(entry.id)
                for (r in rows) {
                    val def = symptomDefs[r.symptomId] ?: continue
                    add(
                        LogOccurrence(
                            symptomId = r.symptomId,
                            symptomName = def.name,
                            occurredAtEpochMs = entry.occurredAtEpochMs,
                            severity = entry.severity.coerceIn(1, 5),
                        ),
                    )
                }
            }
        }
        return LiftCorrelationCalculator.compute(
            CorrelationInput(intakes = intakes, logs = occurrences),
        )
    }
}
