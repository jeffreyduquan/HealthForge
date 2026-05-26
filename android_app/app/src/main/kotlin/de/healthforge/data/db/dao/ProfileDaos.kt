package de.healthforge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.healthforge.data.db.entities.AllergyEntity
import de.healthforge.data.db.entities.IntoleranceEntity
import de.healthforge.data.db.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)
}

@Dao
interface AllergyDao {
    @Query("SELECT * FROM allergy")
    fun observeAll(): Flow<List<AllergyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AllergyEntity>)

    @Query("DELETE FROM allergy")
    suspend fun clear()
}

@Dao
interface IntoleranceDao {
    @Query("SELECT * FROM intolerance")
    fun observeAll(): Flow<List<IntoleranceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IntoleranceEntity>)

    @Query("DELETE FROM intolerance")
    suspend fun clear()
}
