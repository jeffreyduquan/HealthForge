package de.healthforge.data.repository

import de.healthforge.data.db.dao.MealPlanDao
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.db.entities.MealPlanSlotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepository @Inject constructor(
    private val dao: MealPlanDao,
    private val intakeRepo: IntakeRepository,
) {

    fun observeSlotsForDay(day: LocalDate): Flow<List<MealPlanSlotEntity>> =
        dao.observeSlotsForDay(day.toString())

    fun observeItemsForSlots(slotIds: List<Long>): Flow<List<MealPlanItemEntity>> =
        dao.observeItemsForSlots(slotIds)

    suspend fun addSlot(day: LocalDate, slotType: String, time: Int? = null): Long {
        return dao.insertSlot(
            MealPlanSlotEntity(
                dayDateIso = day.toString(),
                slotType = slotType,
                timeOfDayMinutes = time,
            ),
        )
    }

    suspend fun addItem(item: MealPlanItemEntity): Long = dao.insertItem(item)

    suspend fun deleteSlot(slotId: Long) = dao.deleteSlotCascade(slotId)

    suspend fun deleteItem(id: Long) = dao.deleteItemById(id)

    /**
     * P7.S4 / REQ-PLAN-WATER-GOAL-001 — setzt das tagesübergreifende
     * Wasserziel-Override für alle Slots eines Tages. `null` = Override zurücksetzen
     * (Profil-Default greift). Wenn keine Slots existieren → no-op (0 rows updated);
     * UI zeigt den Slider nur, wenn es bereits Slots gibt.
     */
    suspend fun setWaterGoalForDay(day: LocalDate, value: Int?): Int =
        dao.updateWaterGoalForDay(day.toString(), value)

    /** REQ-PLAN-004: "Habe gegessen" → copy alle Items als IntakeEntry, slot.consumed=true. */
    suspend fun markConsumed(slotId: Long): Int {
        val slot = dao.slotById(slotId) ?: return 0
        if (slot.consumed) return 0
        val items = dao.observeItemsForSlots(listOf(slotId)).first()
        val nowMs = System.currentTimeMillis()
        for (it in items) {
            // Bei RECIPE konvertieren wir Portionen heuristisch in Gramm (250g/Portion default).
            val portionGrams = when (it.sourceType) {
                IntakeSourceType.RECIPE -> it.amount * 250.0
                IntakeSourceType.INGREDIENT -> it.amount
                IntakeSourceType.SUPPLEMENT -> it.amount
            }
            intakeRepo.add(
                IntakeEntryEntity(
                    loggedAt = nowMs,
                    dayDateIso = slot.dayDateIso,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    portionGrams = portionGrams,
                    snapshotName = it.snapshotName,
                    snapshotKcalPer100g = it.snapshotKcalPer100g,
                    snapshotProteinPer100g = it.snapshotProteinPer100g,
                    snapshotCarbsPer100g = it.snapshotCarbsPer100g,
                    snapshotFatPer100g = it.snapshotFatPer100g,
                ),
            )
        }
        dao.updateSlot(slot.copy(consumed = true, consumedAt = nowMs))
        return items.size
    }
}
