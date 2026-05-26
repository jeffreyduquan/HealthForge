package de.healthforge.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Symptom catalog — both default (seeded) and user-added entries live here
 * (REQ-LOG-003). `isDefault=true` rows MUST NOT be renamed or deleted from UI.
 */
@Entity(
    tableName = "symptom_def",
    indices = [Index(value = ["name"], unique = true)],
)
data class SymptomDefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean,
)

/**
 * One symptom-event entry. Multiple entries per day allowed (REQ-LOG-004).
 * 7-day editable window enforced in domain layer (REQ-LOG-006).
 *
 * P6.S6 (F-010): Mood/Sleep entfernt — Modell ist nun event-basiert mit einer
 * Severity je Event (1..5) und tagged Symptomen aus dem Katalog. Per-Symptom-
 * Severity (in `log_entry_symptom`) entfällt zugunsten dieser einen Event-Severity.
 */
@Entity(tableName = "log_entry")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAtEpochMs: Long,
    val severity: Int = 3, // 1..5; default „mittel"
    val note: String? = null,
)

/**
 * Join row: which symptoms were tagged on a log entry. Severity sits on the
 * parent `LogEntryEntity` (one Severity per Event) since P6.S6 (F-010).
 */
@Entity(
    tableName = "log_entry_symptom",
    primaryKeys = ["entryId", "symptomId"],
    foreignKeys = [
        ForeignKey(
            entity = LogEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SymptomDefEntity::class,
            parentColumns = ["id"],
            childColumns = ["symptomId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["symptomId"])],
)
data class LogEntrySymptomEntity(
    val entryId: Long,
    val symptomId: Long,
)

/**
 * Free-form tags per log entry (e.g. "Periode", "Stress", "Reise").
 */
@Entity(
    tableName = "log_entry_tag",
    primaryKeys = ["entryId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = LogEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LogEntryTagEntity(
    val entryId: Long,
    val tag: String,
)
