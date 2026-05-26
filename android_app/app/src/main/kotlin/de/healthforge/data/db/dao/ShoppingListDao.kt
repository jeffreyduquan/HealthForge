package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.healthforge.data.db.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO für [ShoppingListItemEntity] — Einkaufsliste (REQ-SHOP-001/002/003).
 */
@Dao
interface ShoppingListDao {

    @Query("SELECT MAX(runId) FROM shopping_list_item")
    suspend fun latestRunId(): Long?

    @Query("SELECT * FROM shopping_list_item WHERE runId = :runId ORDER BY category ASC, name ASC")
    fun observeRun(runId: Long): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItemEntity>): List<Long>

    @Update
    suspend fun update(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_item SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list_item WHERE runId = :runId")
    suspend fun deleteRun(runId: Long)

    @Query("DELETE FROM shopping_list_item WHERE runId < :keepRunId")
    suspend fun deleteOldRuns(keepRunId: Long)
}
