package de.healthforge.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

/**
 * Seeds the 15 default symptoms on initial DB creation (REQ-LOG-003).
 * Names are intentionally German (target locale).
 *
 * Runs once on `onCreate`. After destructive migration (current P1/P2/P3 dev
 * policy) the callback re-fires, so seed remains consistent.
 */
internal object LogDefaultSymptomSeed {
    val NAMES: List<String> = listOf(
        "Kopfschmerz",
        "Bauchschmerz",
        "Blähungen",
        "Durchfall",
        "Verstopfung",
        "Übelkeit",
        "Müdigkeit",
        "Konzentrationsschwäche",
        "Hautausschlag",
        "Juckreiz",
        "Gelenkschmerz",
        "Muskelschmerz",
        "Schlaflosigkeit",
        "Reizbarkeit",
        "Sodbrennen",
    )

    fun callback(): RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Executors.newSingleThreadExecutor().execute {
                db.beginTransaction()
                try {
                    NAMES.forEach { name ->
                        val escaped = name.replace("'", "''")
                        db.execSQL(
                            "INSERT OR IGNORE INTO symptom_def (name, isDefault) VALUES ('$escaped', 1)"
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}
