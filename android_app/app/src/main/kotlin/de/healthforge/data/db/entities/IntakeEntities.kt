package de.healthforge.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Source type for an intake entry. REQ-INTAKE-003. */
enum class IntakeSourceType { RECIPE, INGREDIENT, SUPPLEMENT }

/**
 * Local-only intake log entry. REQ-INTAKE-001/002/003.
 *
 * - Snapshot fields (`snapshotName`, `snapshotKcalPer100g`, ...) make entries resilient
 *   to server-side deletion/edits of the underlying ingredient/recipe.
 * - `dayDateIso` is `LocalDate.toString()` (e.g. `"2024-09-26"`); enables fast day queries.
 * - `loggedAt` is epoch ms (insertion / consumption time).
 */
@Entity(
    tableName = "intake_entry",
    indices = [Index("dayDateIso"), Index("loggedAt")],
)
data class IntakeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val loggedAt: Long,
    val dayDateIso: String,
    val sourceType: IntakeSourceType,
    val sourceId: String,
    val portionGrams: Double,
    val snapshotName: String,
    val snapshotBrand: String? = null,
    val snapshotKcalPer100g: Double? = null,
    val snapshotProteinPer100g: Double? = null,
    val snapshotCarbsPer100g: Double? = null,
    val snapshotFatPer100g: Double? = null,
)

/**
 * Local-only water intake entry. REQ-WATER-001/004.
 *
 * One row per add-event (so deletions are granular).
 */
@Entity(
    tableName = "water_intake",
    indices = [Index("dayDateIso"), Index("loggedAt")],
)
data class WaterIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val loggedAt: Long,
    val dayDateIso: String,
    val volumeMl: Int,
)
