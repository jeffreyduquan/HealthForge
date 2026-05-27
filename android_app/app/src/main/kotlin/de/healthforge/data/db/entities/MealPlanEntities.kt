package de.healthforge.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mahlzeitenplan-Slot (REQ-PLAN-001/002/003). Local-only.
 *
 * - `dayDateIso` = `LocalDate.toString()` (z.B. `"2024-09-26"`).
 * - `slotType` ist String (BREAKFAST/LUNCH/DINNER/SNACK), bewusst nicht enum-typisiert
 *   damit zukünftige Custom-Slots ohne Migration funktionieren.
 * - `timeOfDayMinutes` ist Minutes since midnight (0..1439), nullable für unscheduled.
 * - `consumed` markiert "Habe gegessen"-Slot; doppeltes Eintragen verhindert Snackbar.
 */
@Entity(
    tableName = "meal_plan_slot",
    indices = [Index("dayDateIso"), Index("dayDateIso", "slotType")],
)
data class MealPlanSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayDateIso: String,
    val slotType: String,
    val timeOfDayMinutes: Int? = null,
    val position: Int = 0,
    val consumed: Boolean = false,
    val consumedAt: Long? = null,
    /**
     * P7.S1 / REQ-PLAN-WATER-GOAL-001 — Tages-Wasserziel in ml, Override pro Tag.
     * `null` = Profile-Default (UserProfileEntity.dailyNutrientGoalsJson["water"]) gilt.
     */
    val waterGoalMl: Int? = null,
)

/**
 * Item innerhalb eines Slots (REQ-PLAN-002). Snapshot-Felder gemäß REQ-RECIPE-009
 * für Resilienz gegen Server-Side-Updates des Quell-Recipes/-Ingredients.
 */
@Entity(
    tableName = "meal_plan_item",
    indices = [Index("slotId")],
)
data class MealPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val slotId: Long,
    val sourceType: IntakeSourceType, // RECIPE | INGREDIENT
    val sourceId: String,
    /** Bei RECIPE = Portionen, bei INGREDIENT = Gramm. */
    val amount: Double,
    val snapshotName: String,
    val snapshotKcalPer100g: Double? = null,
    val snapshotProteinPer100g: Double? = null,
    val snapshotCarbsPer100g: Double? = null,
    val snapshotFatPer100g: Double? = null,
)
