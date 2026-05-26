package de.healthforge.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local supplement definition. REQ-SUPP-001/002.
 *
 * Nutrition values are PER DOSE (not per 100g) because a dose is the natural unit
 * (1 tablet, 1 capsule, 1 ml). For intake-log integration the snapshot row in
 * [IntakeEntryEntity] uses `portionGrams` to represent dose-count (e.g. 2.0 = 2 tabs).
 */
@Entity(
    tableName = "supplement",
    indices = [Index("nameDe")],
)
data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nameDe: String,
    val brand: String? = null,
    val unitLabel: String,                  // "Tablette" / "Kapsel" / "ml" / "g"
    val defaultDose: Double,                // e.g. 1.0, 2.0, 5.0 (ml)
    val kcalPerDose: Double? = null,
    val proteinPerDose: Double? = null,
    val carbsPerDose: Double? = null,
    val fatPerDose: Double? = null,
    /** Free-form micronutrient JSON (e.g. {"vitamin_d3":"2000 IE","b12":"1000 µg"}). */
    val micronutrientsJson: String? = null,
    val notes: String? = null,
    val imageUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Frequency mode for a supplement reminder. REQ-SUPP-005, REQ-REMIND-001/002. */
enum class ReminderFrequency {
    ONCE,           // single fire at [triggerAtMillis]
    DAILY,          // every day at [hourOfDay]:[minute]
    WEEKLY,         // [daysOfWeekMask] bitmask Mon=1,Tue=2,Wed=4,Thu=8,Fri=16,Sat=32,Sun=64
}

/**
 * Reminder for a supplement. AlarmManager exact-alarm. REQ-SUPP-005, REQ-REMIND-001/002/004.
 *
 * @param triggerAtMillis used for ONCE; the next epoch-ms to fire (cleared after fire)
 * @param hourOfDay 0..23 for DAILY/WEEKLY
 * @param minute 0..59 for DAILY/WEEKLY
 * @param daysOfWeekMask bitmask for WEEKLY (Mon=1, Tue=2, Wed=4, ...)
 */
@Entity(
    tableName = "supplement_reminder",
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("supplementId"), Index("enabled")],
)
data class SupplementReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val supplementId: Long,
    val label: String,
    val frequency: ReminderFrequency,
    val triggerAtMillis: Long? = null,
    val hourOfDay: Int? = null,
    val minute: Int? = null,
    val daysOfWeekMask: Int? = null,
    val enabled: Boolean = true,
    val createdAt: Long,
)

/**
 * Returns true if this reminder is "due today" for purposes of the Home-Checklist:
 * - DAILY: always true
 * - WEEKLY: bit for `day.dayOfWeek` is set in `daysOfWeekMask`
 * - ONCE: true iff `triggerAtMillis` falls on the same local date as [day]
 */
fun SupplementReminderEntity.isDueToday(day: LocalDate): Boolean = when (frequency) {
    ReminderFrequency.DAILY -> true
    ReminderFrequency.WEEKLY -> {
        val mask = daysOfWeekMask ?: 0
        val bit = 1 shl (day.dayOfWeek.value - 1)
        (mask and bit) != 0
    }
    ReminderFrequency.ONCE -> {
        val triggerMs = triggerAtMillis ?: return false
        val triggerDay = java.time.Instant.ofEpochMilli(triggerMs)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        triggerDay == day
    }
}
