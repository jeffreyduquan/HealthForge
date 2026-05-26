package de.healthforge.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.healthforge.data.db.AppDatabase
import de.healthforge.data.db.SqlCipherKeyProvider
import de.healthforge.data.db.dao.AllergyDao
import de.healthforge.data.db.dao.IntakeEntryDao
import de.healthforge.data.db.dao.IntoleranceDao
import de.healthforge.data.db.dao.SupplementDao
import de.healthforge.data.db.dao.SupplementReminderDao
import de.healthforge.data.db.dao.UserProfileDao
import de.healthforge.data.db.dao.WaterIntakeDao
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: SqlCipherKeyProvider,
    ): AppDatabase {
        // sqlcipher-android needs its native lib loaded once before any DB call.
        System.loadLibrary("sqlcipher")
        val passphrase = keyProvider.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()  // P1 only; remove from P2
            .build()
    }

    @Provides fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideAllergyDao(db: AppDatabase): AllergyDao = db.allergyDao()
    @Provides fun provideIntoleranceDao(db: AppDatabase): IntoleranceDao = db.intoleranceDao()
    @Provides fun provideIntakeEntryDao(db: AppDatabase): IntakeEntryDao = db.intakeEntryDao()
    @Provides fun provideWaterIntakeDao(db: AppDatabase): WaterIntakeDao = db.waterIntakeDao()
    @Provides fun provideSupplementDao(db: AppDatabase): SupplementDao = db.supplementDao()
    @Provides fun provideSupplementReminderDao(db: AppDatabase): SupplementReminderDao =
        db.supplementReminderDao()
}
