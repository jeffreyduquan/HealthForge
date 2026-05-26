package de.healthforge.data.repository

import de.healthforge.data.db.dao.SupplementDao
import de.healthforge.data.db.dao.SupplementReminderDao
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** REQ-SUPP-001..003/005/006. Local-only in P1 (REQ-SUPP-002). */
@Singleton
class SupplementRepository @Inject constructor(
    private val supplementDao: SupplementDao,
    private val reminderDao: SupplementReminderDao,
) {
    fun observeAll(): Flow<List<SupplementEntity>> = supplementDao.observeAll()
    suspend fun byId(id: Long): SupplementEntity? = supplementDao.byId(id)

    suspend fun upsert(s: SupplementEntity): Long {
        require(s.nameDe.isNotBlank()) { "name darf nicht leer sein" }
        require(s.unitLabel.isNotBlank()) { "unitLabel darf nicht leer sein" }
        require(s.defaultDose > 0.0) { "defaultDose muss > 0 sein" }
        val now = System.currentTimeMillis()
        val toSave = if (s.id == 0L) s.copy(createdAt = now, updatedAt = now) else s.copy(updatedAt = now)
        return if (s.id == 0L) supplementDao.insert(toSave) else { supplementDao.update(toSave); s.id }
    }

    suspend fun delete(id: Long) = supplementDao.deleteById(id)

    // --- Reminders -----------------------------------------------------------

    fun observeReminders(supplementId: Long): Flow<List<SupplementReminderEntity>> =
        reminderDao.observeForSupplement(supplementId)

    fun observeAllReminders(): Flow<List<SupplementReminderEntity>> = reminderDao.observeAll()

    suspend fun listEnabledReminders(): List<SupplementReminderEntity> = reminderDao.listEnabled()

    suspend fun upsertReminder(r: SupplementReminderEntity): Long {
        val now = System.currentTimeMillis()
        val toSave = if (r.id == 0L) r.copy(createdAt = now) else r
        return if (r.id == 0L) reminderDao.insert(toSave) else { reminderDao.update(toSave); r.id }
    }

    suspend fun deleteReminder(id: Long) = reminderDao.deleteById(id)
    suspend fun reminderById(id: Long): SupplementReminderEntity? = reminderDao.byId(id)
}
