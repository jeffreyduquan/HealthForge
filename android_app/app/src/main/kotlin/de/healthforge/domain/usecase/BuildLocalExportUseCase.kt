package de.healthforge.domain.usecase

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.healthforge.data.db.dao.IntakeEntryDao
import de.healthforge.data.db.dao.LogEntryDao
import de.healthforge.data.db.dao.SupplementDao
import de.healthforge.data.db.dao.SupplementReminderDao
import de.healthforge.data.db.dao.SymptomDefDao
import de.healthforge.data.db.dao.UserProfileDao
import de.healthforge.data.db.dao.WaterIntakeDao
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import de.healthforge.data.db.entities.UserProfileEntity
import de.healthforge.data.db.entities.WaterIntakeEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REQ-EXPORT-001 / -003 / -004 — Lokaler JSON-Export aller On-Device-Daten.
 *
 * Wird vom Android-Client erzeugt, weil Intake, Wasser, Logs und Supplement-
 * Erinnerungen nie den Server erreichen (Privacy-by-Design, vgl. Architecture).
 *
 * Server-Daten (Account, eigene Rezepte, Supplement-Vorschläge) liefert
 * separat `GET /v1/export/full`.
 */
@JsonClass(generateAdapter = false)
data class LocalExportPayload(
    val schema: String,
    val generated_at_epoch_ms: Long,
    val profile: UserProfileEntity?,
    val intake_entries: List<IntakeEntryEntity>,
    val water_entries: List<WaterIntakeEntity>,
    val symptom_definitions: List<SymptomDefEntity>,
    val log_entries: List<LogEntryEntity>,
    val supplements: List<SupplementEntity>,
    val supplement_reminders: List<SupplementReminderEntity>,
)

@Singleton
class BuildLocalExportUseCase @Inject constructor(
    private val profileDao: UserProfileDao,
    private val intakeDao: IntakeEntryDao,
    private val waterDao: WaterIntakeDao,
    private val symptomDefDao: SymptomDefDao,
    private val logEntryDao: LogEntryDao,
    private val supplementDao: SupplementDao,
    private val reminderDao: SupplementReminderDao,
    private val moshi: Moshi,
) {
    suspend operator fun invoke(): ByteArray {
        val payload = LocalExportPayload(
            schema = SCHEMA,
            generated_at_epoch_ms = System.currentTimeMillis(),
            profile = profileDao.getProfile(),
            intake_entries = intakeDao.listAll(),
            water_entries = waterDao.listAll(),
            symptom_definitions = symptomDefDao.all(),
            log_entries = logEntryDao.listAll(),
            supplements = supplementDao.listAll(),
            supplement_reminders = reminderDao.listAll(),
        )
        val adapter = moshi.adapter(LocalExportPayload::class.java).indent("  ")
        return adapter.toJson(payload).toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val SCHEMA = "healthforge.local-export.v1"
    }
}
