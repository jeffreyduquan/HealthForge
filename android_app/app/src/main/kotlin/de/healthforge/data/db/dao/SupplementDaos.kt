package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {

    @Query("SELECT * FROM supplement ORDER BY nameDe COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplement WHERE id = :id")
    suspend fun byId(id: Long): SupplementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(s: SupplementEntity): Long

    @Update
    suspend fun update(s: SupplementEntity)

    @Delete
    suspend fun delete(s: SupplementEntity)

    @Query("DELETE FROM supplement WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SupplementReminderDao {

    @Query("SELECT * FROM supplement_reminder ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SupplementReminderEntity>>

    @Query("SELECT * FROM supplement_reminder WHERE supplementId = :sid ORDER BY createdAt DESC")
    fun observeForSupplement(sid: Long): Flow<List<SupplementReminderEntity>>

    @Query("SELECT * FROM supplement_reminder WHERE enabled = 1")
    suspend fun listEnabled(): List<SupplementReminderEntity>

    @Query("SELECT * FROM supplement_reminder WHERE id = :id")
    suspend fun byId(id: Long): SupplementReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: SupplementReminderEntity): Long

    @Update
    suspend fun update(r: SupplementReminderEntity)

    @Delete
    suspend fun delete(r: SupplementReminderEntity)

    @Query("DELETE FROM supplement_reminder WHERE id = :id")
    suspend fun deleteById(id: Long)
}
