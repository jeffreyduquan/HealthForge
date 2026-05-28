package de.healthforge.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistierte Einstellungen für den Wasser-Reminder (REQ-REMIND-001).
 *
 * Bewusst SharedPreferences statt Room: keine sensitiven Daten, einfacher Boolean+Int,
 * keine Migration nötig. Default = aus (User opt-in gemäß "MAY ... if enabled").
 */
@Singleton
class WaterReminderPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var intervalHours: Int
        get() = prefs.getInt(KEY_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS)
        set(value) {
            prefs.edit().putInt(KEY_INTERVAL_HOURS, value.coerceIn(MIN_INTERVAL_HOURS, MAX_INTERVAL_HOURS)).apply()
        }

    /**
     * P7.S4 Slice 4c — WaterDeficitScheduler: Check-Intervall in Minuten.
     * Bei jedem Tick wird im AlarmReceiver der Defizit gegen das Tagesziel berechnet
     * und (nur) bei Überschreiten von [deficitThresholdMl] eine Notification gefeuert.
     */
    var checkIntervalMin: Int
        get() = prefs.getInt(KEY_CHECK_INTERVAL_MIN, DEFAULT_CHECK_INTERVAL_MIN)
        set(value) {
            prefs.edit().putInt(KEY_CHECK_INTERVAL_MIN, value.coerceIn(MIN_CHECK_INTERVAL_MIN, MAX_CHECK_INTERVAL_MIN)).apply()
        }

    /** Minimaler Rückstand gegen das Tagesziel (in ml), bevor genotificiert wird. */
    var deficitThresholdMl: Int
        get() = prefs.getInt(KEY_DEFICIT_THRESHOLD_ML, DEFAULT_DEFICIT_THRESHOLD_ML)
        set(value) {
            prefs.edit().putInt(KEY_DEFICIT_THRESHOLD_ML, value.coerceIn(50, 1000)).apply()
        }

    companion object {
        private const val PREFS_NAME = "hf_water_reminder"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_INTERVAL_HOURS = "interval_hours"
        private const val KEY_CHECK_INTERVAL_MIN = "check_interval_min"
        private const val KEY_DEFICIT_THRESHOLD_ML = "deficit_threshold_ml"
        const val DEFAULT_INTERVAL_HOURS = 2
        const val MIN_INTERVAL_HOURS = 1
        const val MAX_INTERVAL_HOURS = 6
        const val DEFAULT_CHECK_INTERVAL_MIN = 30
        const val MIN_CHECK_INTERVAL_MIN = 15
        const val MAX_CHECK_INTERVAL_MIN = 120
        const val DEFAULT_DEFICIT_THRESHOLD_ML = 200
        /** Aktives Reminder-Fenster: 08:00–22:00 lokal. */
        const val ACTIVE_HOUR_START = 8
        const val ACTIVE_HOUR_END = 22
    }
}
