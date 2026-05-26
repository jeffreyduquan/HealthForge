package de.healthforge.data.repository

import de.healthforge.data.db.dao.AllergyDao
import de.healthforge.data.db.dao.IntoleranceDao
import de.healthforge.data.db.dao.UserProfileDao
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.AllergyEntity
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.db.entities.IntoleranceEntity
import de.healthforge.data.db.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class FullProfile(
    val profile: UserProfileEntity?,
    val allergies: Set<AllergenType>,
    val intolerances: Set<FodmapType>,
)

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: UserProfileDao,
    private val allergyDao: AllergyDao,
    private val intoleranceDao: IntoleranceDao,
) {

    fun observe(): Flow<FullProfile> =
        combine(
            profileDao.observeProfile(),
            allergyDao.observeAll(),
            intoleranceDao.observeAll(),
        ) { p, a, i ->
            FullProfile(p, a.map { it.allergen }.toSet(), i.map { it.fodmap }.toSet())
        }

    suspend fun upsertProfile(p: UserProfileEntity) = profileDao.upsert(p)

    suspend fun replaceAllergies(items: Set<AllergenType>) {
        allergyDao.clear()
        if (items.isNotEmpty()) allergyDao.insertAll(items.map(::AllergyEntity))
    }

    suspend fun replaceIntolerances(items: Set<FodmapType>) {
        intoleranceDao.clear()
        if (items.isNotEmpty()) intoleranceDao.insertAll(items.map(::IntoleranceEntity))
    }
}
