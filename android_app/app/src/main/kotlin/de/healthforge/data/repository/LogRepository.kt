package de.healthforge.data.repository

import de.healthforge.data.db.dao.LogEntryDao
import de.healthforge.data.db.dao.SymptomDefDao
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.LogEntrySymptomEntity
import de.healthforge.data.db.entities.LogEntryTagEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory composite of a `LogEntryEntity` together with its symptoms+tags,
 * suitable for UI consumption.
 */
data class LogEntryWithDetails(
    val entry: LogEntryEntity,
    val symptoms: List<SymptomDefEntity>,
    val tags: List<String>,
)

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogEntryDao,
    private val symptomDao: SymptomDefDao,
) {
    fun observeSymptoms(): Flow<List<SymptomDefEntity>> = symptomDao.observeAll()

    suspend fun allSymptoms(): List<SymptomDefEntity> = symptomDao.all()

    suspend fun addCustomSymptom(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Symptom-Name darf nicht leer sein" }
        return symptomDao.insert(SymptomDefEntity(name = trimmed, isDefault = false))
    }

    suspend fun renameCustomSymptom(id: Long, newName: String) {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty())
        val existing = symptomDao.byId(id) ?: return
        if (existing.isDefault) return
        symptomDao.update(existing.copy(name = trimmed))
    }

    suspend fun deleteCustomSymptom(id: Long): Boolean = symptomDao.deleteCustomById(id) > 0

    fun observeRecent(limit: Int = 50): Flow<List<LogEntryEntity>> = logDao.observeRecent(limit)

    fun observeRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<LogEntryEntity>> =
        logDao.observeRange(fromEpochMs, toEpochMs)

    fun observeSymptomsForEntries(entryIds: List<Long>): Flow<List<LogEntrySymptomEntity>> =
        if (entryIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else logDao.observeSymptomsForEntries(entryIds)

    fun observeTagsForEntries(entryIds: List<Long>): Flow<List<LogEntryTagEntity>> =
        if (entryIds.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else logDao.observeTagsForEntries(entryIds)

    suspend fun loadWithDetails(entryId: Long): LogEntryWithDetails? {
        val entry = logDao.byId(entryId) ?: return null
        val sympRows = logDao.symptomsForEntry(entryId)
        val tagRows = logDao.tagsForEntry(entryId)
        val defs = symptomDao.all().associateBy { it.id }
        val symptoms = sympRows.mapNotNull { row -> defs[row.symptomId] }
        return LogEntryWithDetails(entry, symptoms, tagRows.map { it.tag })
    }

    suspend fun upsert(
        entry: LogEntryEntity,
        symptomIds: List<Long>,
        tags: List<String>,
    ): Long = logDao.upsertWithChildren(
        entry = entry,
        symptoms = symptomIds.map { sid ->
            LogEntrySymptomEntity(entryId = entry.id, symptomId = sid)
        },
        tags = tags.map { LogEntryTagEntity(entryId = entry.id, tag = it.trim()) }
            .filter { it.tag.isNotEmpty() },
    )

    suspend fun delete(entryId: Long) = logDao.deleteEntry(entryId)
}
