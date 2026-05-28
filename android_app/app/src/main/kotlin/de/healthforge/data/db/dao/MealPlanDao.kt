package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.db.entities.MealPlanSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Query("SELECT * FROM meal_plan_slot WHERE dayDateIso = :day ORDER BY position ASC, id ASC")
    fun observeSlotsForDay(day: String): Flow<List<MealPlanSlotEntity>>

    @Query("SELECT * FROM meal_plan_slot WHERE dayDateIso BETWEEN :start AND :end ORDER BY dayDateIso ASC, position ASC, id ASC")
    suspend fun slotsBetween(start: String, end: String): List<MealPlanSlotEntity>

    @Query("SELECT * FROM meal_plan_item WHERE slotId IN (:slotIds)")
    suspend fun itemsForSlotsOnce(slotIds: List<Long>): List<MealPlanItemEntity>

    @Query("SELECT * FROM meal_plan_slot WHERE id = :id")
    suspend fun slotById(id: Long): MealPlanSlotEntity?

    @Query("SELECT * FROM meal_plan_item WHERE slotId = :slotId ORDER BY id ASC")
    fun observeItemsForSlot(slotId: Long): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_item WHERE slotId IN (:slotIds)")
    fun observeItemsForSlots(slotIds: List<Long>): Flow<List<MealPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: MealPlanSlotEntity): Long

    @Update
    suspend fun updateSlot(slot: MealPlanSlotEntity)

    @Query("DELETE FROM meal_plan_slot WHERE id = :id")
    suspend fun deleteSlotById(id: Long)

    @Query("DELETE FROM meal_plan_item WHERE slotId = :slotId")
    suspend fun deleteItemsBySlot(slotId: Long)

    @Insert
    suspend fun insertItem(item: MealPlanItemEntity): Long

    @Delete
    suspend fun deleteItem(item: MealPlanItemEntity)

    @Query("DELETE FROM meal_plan_item WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Transaction
    suspend fun deleteSlotCascade(slotId: Long) {
        deleteItemsBySlot(slotId)
        deleteSlotById(slotId)
    }
}
