package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeEntryDao {

    @Query("SELECT * FROM intake_entry WHERE dayDateIso = :day ORDER BY loggedAt DESC")
    fun observeForDay(day: String): Flow<List<IntakeEntryEntity>>

    @Query("SELECT * FROM intake_entry ORDER BY loggedAt DESC LIMIT :limit OFFSET :offset")
    fun observeRecent(limit: Int = 200, offset: Int = 0): Flow<List<IntakeEntryEntity>>

    @Query(
        "SELECT sourceType || ':' || sourceId AS ref " +
            "FROM intake_entry " +
            "GROUP BY sourceType, sourceId " +
            "ORDER BY MAX(loggedAt) DESC LIMIT :limit"
    )
    fun observeRecentRefs(limit: Int = 6): Flow<List<String>>

    @Query("SELECT * FROM intake_entry WHERE id = :id")
    suspend fun byId(id: Long): IntakeEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: IntakeEntryEntity): Long

    @Update
    suspend fun update(entry: IntakeEntryEntity)

    @Delete
    suspend fun delete(entry: IntakeEntryEntity)

    @Query("DELETE FROM intake_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM intake_entry ORDER BY loggedAt DESC")
    suspend fun listAll(): List<IntakeEntryEntity>
}

@Dao
interface WaterIntakeDao {

    @Query("SELECT COALESCE(SUM(volumeMl), 0) FROM water_intake WHERE dayDateIso = :day")
    fun observeSumForDay(day: String): Flow<Int>

    /** P7.S4 Slice 4c — suspendierter Read für den WaterDeficitScheduler (AlarmReceiver-Pfad). */
    @Query("SELECT COALESCE(SUM(volumeMl), 0) FROM water_intake WHERE dayDateIso = :day")
    suspend fun sumForDay(day: String): Int

    @Query("SELECT * FROM water_intake WHERE dayDateIso = :day ORDER BY loggedAt DESC")
    fun observeForDay(day: String): Flow<List<WaterIntakeEntity>>

    @Insert
    suspend fun insert(entry: WaterIntakeEntity): Long

    @Query("DELETE FROM water_intake WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM water_intake WHERE dayDateIso = :day")
    suspend fun deleteForDay(day: String)

    /**
     * P7.S3a / REQ-HOME-WATER-BAR-001 — absolute Tagesmenge setzen.
     * Ersetzt alle Einträge des Tages durch genau einen Aggregat-Eintrag mit
     * [totalMl]. Wenn [totalMl] == 0, werden alle Einträge gelöscht und keiner
     * neu angelegt.
     */
    @Transaction
    suspend fun replaceDayTotal(day: String, totalMl: Int, loggedAt: Long) {
        deleteForDay(day)
        if (totalMl > 0) {
            insert(
                WaterIntakeEntity(
                    loggedAt = loggedAt,
                    dayDateIso = day,
                    volumeMl = totalMl,
                )
            )
        }
    }

    @Query("SELECT * FROM water_intake ORDER BY loggedAt DESC")
    suspend fun listAll(): List<WaterIntakeEntity>
}
