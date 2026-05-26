package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.LogEntrySymptomEntity
import de.healthforge.data.db.entities.LogEntryTagEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDefDao {
    @Query("SELECT * FROM symptom_def ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<SymptomDefEntity>>

    @Query("SELECT * FROM symptom_def ORDER BY isDefault DESC, name ASC")
    suspend fun all(): List<SymptomDefEntity>

    @Query("SELECT * FROM symptom_def WHERE id = :id")
    suspend fun byId(id: Long): SymptomDefEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SymptomDefEntity): Long

    @Update
    suspend fun update(entity: SymptomDefEntity)

    @Query("DELETE FROM symptom_def WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomById(id: Long): Int
}

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entry WHERE occurredAtEpochMs >= :fromEpochMs AND occurredAtEpochMs < :toEpochMs ORDER BY occurredAtEpochMs DESC")
    fun observeRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entry ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entry ORDER BY occurredAtEpochMs DESC")
    suspend fun listAll(): List<LogEntryEntity>

    @Query("SELECT * FROM log_entry WHERE id = :id")
    suspend fun byId(id: Long): LogEntryEntity?

    @Query("SELECT * FROM log_entry_symptom WHERE entryId = :entryId")
    suspend fun symptomsForEntry(entryId: Long): List<LogEntrySymptomEntity>

    @Query("SELECT * FROM log_entry_tag WHERE entryId = :entryId")
    suspend fun tagsForEntry(entryId: Long): List<LogEntryTagEntity>

    @Query("SELECT * FROM log_entry_symptom WHERE entryId IN (:entryIds)")
    fun observeSymptomsForEntries(entryIds: List<Long>): Flow<List<LogEntrySymptomEntity>>

    @Query("SELECT * FROM log_entry_tag WHERE entryId IN (:entryIds)")
    fun observeTagsForEntries(entryIds: List<Long>): Flow<List<LogEntryTagEntity>>

    @Insert
    suspend fun insertEntry(entry: LogEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: LogEntryEntity)

    @Query("DELETE FROM log_entry WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(rows: List<LogEntrySymptomEntity>)

    @Query("DELETE FROM log_entry_symptom WHERE entryId = :entryId")
    suspend fun clearSymptoms(entryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(rows: List<LogEntryTagEntity>)

    @Query("DELETE FROM log_entry_tag WHERE entryId = :entryId")
    suspend fun clearTags(entryId: Long)

    @Transaction
    suspend fun upsertWithChildren(
        entry: LogEntryEntity,
        symptoms: List<LogEntrySymptomEntity>,
        tags: List<LogEntryTagEntity>,
    ): Long {
        val id = if (entry.id == 0L) {
            insertEntry(entry)
        } else {
            updateEntry(entry)
            entry.id
        }
        clearSymptoms(id)
        clearTags(id)
        if (symptoms.isNotEmpty()) {
            insertSymptoms(symptoms.map { it.copy(entryId = id) })
        }
        if (tags.isNotEmpty()) {
            insertTags(tags.map { it.copy(entryId = id) })
        }
        return id
    }
}
