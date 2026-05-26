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
 * One symptom-diary entry. Multiple entries per day allowed (REQ-LOG-004).
 * 7-day editable window enforced in domain layer (REQ-LOG-006).
 */
@Entity(tableName = "log_entry")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAtEpochMs: Long,
    val mood: Int,
    val sleepQuality: Int? = null,
    val sleepHours: Double? = null,
    val note: String? = null,
)

/**
 * Join row: which symptoms were tagged on a log entry, with severity 1–5.
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
    val severity: Int,
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
