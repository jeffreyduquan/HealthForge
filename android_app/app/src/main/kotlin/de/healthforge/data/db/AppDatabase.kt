package de.healthforge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.healthforge.data.db.dao.AllergyDao
import de.healthforge.data.db.dao.IntakeEntryDao
import de.healthforge.data.db.dao.IntoleranceDao
import de.healthforge.data.db.dao.LogEntryDao
import de.healthforge.data.db.dao.MealPlanDao
import de.healthforge.data.db.dao.ShoppingListDao
import de.healthforge.data.db.dao.SupplementDao
import de.healthforge.data.db.dao.SupplementReminderDao
import de.healthforge.data.db.dao.SymptomDefDao
import de.healthforge.data.db.dao.UserProfileDao
import de.healthforge.data.db.dao.WaterIntakeDao
import de.healthforge.data.db.entities.AllergyEntity
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntoleranceEntity
import de.healthforge.data.db.entities.LogEntryEntity
import de.healthforge.data.db.entities.LogEntrySymptomEntity
import de.healthforge.data.db.entities.LogEntryTagEntity
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.db.entities.MealPlanSlotEntity
import de.healthforge.data.db.entities.ShoppingListItemEntity
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.data.db.entities.SymptomDefEntity
import de.healthforge.data.db.entities.UserProfileEntity
import de.healthforge.data.db.entities.WaterIntakeEntity

/**
 * Root Room database, encrypted with SQLCipher.
 *
 * Schema version bumps require Flyway-style migration here — for P1 we re-create on
 * version-mismatch; from P2 onwards proper migration objects MUST be added.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        AllergyEntity::class,
        IntoleranceEntity::class,
        IntakeEntryEntity::class,
        WaterIntakeEntity::class,
        SupplementEntity::class,
        SupplementReminderEntity::class,
        MealPlanSlotEntity::class,
        MealPlanItemEntity::class,
        SymptomDefEntity::class,
        LogEntryEntity::class,
        LogEntrySymptomEntity::class,
        LogEntryTagEntity::class,
        ShoppingListItemEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun allergyDao(): AllergyDao
    abstract fun intoleranceDao(): IntoleranceDao
    abstract fun intakeEntryDao(): IntakeEntryDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun supplementDao(): SupplementDao
    abstract fun supplementReminderDao(): SupplementReminderDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun symptomDefDao(): SymptomDefDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {
        const val DB_NAME = "healthforge.db"
    }
}
