package de.healthforge.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wasser-Defizit-Scheduler (P7.S4 Slice 4c — REQ-WATER-005 / REQ-HOME-WATER-ALARM-001).
 *
 * Ersetzt den alten festen 2h-Intervall-Reminder. Plant einen periodischen "Check-Tick"
 * (Default 30 min, konfigurierbar via [WaterReminderPrefs.checkIntervalMin]). Bei jedem
 * Tick prüft [AlarmReceiver.handleWaterFire] das Wasser-Defizit gegen das Tagesziel und
 * feuert nur eine Notification, wenn der Rückstand [WaterReminderPrefs.deficitThresholdMl]
 * überschreitet. So bekommt der User keine sinnlosen "Trink-Wasser"-Pings, wenn er bereits
 * im Plan ist.
 *
 * - Inexact `setAndAllowWhileIdle` (LOW-Channel; keine `SCHEDULE_EXACT_ALARM`-Berechtigung nötig).
 * - Aktiv-Fenster: 08–22 lokal. Ticks außerhalb auf nächstes 08:00 verschoben.
 * - Chaining: AlarmReceiver ruft nach jedem Tick erneut [schedule] → kein Drift.
 */
@Singleton
class WaterReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: WaterReminderPrefs,
) {

    private val am: AlarmManager? get() = context.getSystemService<AlarmManager>()

    /** Berechnet nächsten Tick-Zeitpunkt in epoch-ms (oder null wenn disabled). */
    fun nextTriggerAt(now: LocalDateTime = LocalDateTime.now()): Long? {
        if (!prefs.enabled) return null
        val zone = ZoneId.systemDefault()
        var candidate = now.plusMinutes(prefs.checkIntervalMin.toLong()).withSecond(0).withNano(0)
        // Außerhalb des aktiven Fensters → auf nächstes 08:00 verschieben.
        if (candidate.hour >= WaterReminderPrefs.ACTIVE_HOUR_END) {
            candidate = candidate.plusDays(1)
                .withHour(WaterReminderPrefs.ACTIVE_HOUR_START)
                .withMinute(0)
        } else if (candidate.hour < WaterReminderPrefs.ACTIVE_HOUR_START) {
            candidate = candidate
                .withHour(WaterReminderPrefs.ACTIVE_HOUR_START)
                .withMinute(0)
        }
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    /** Plant nächsten Check-Tick; No-op wenn `prefs.enabled == false`. */
    fun schedule() {
        val mgr = am ?: return
        val triggerAt = nextTriggerAt() ?: return
        try {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
        } catch (_: SecurityException) {
            // No-op: Wasser-Check ist LOW-Priority, Security-Verlust akzeptabel.
        }
    }

    fun cancel() {
        am?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_WATER_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        // Fester Request-Code: nur ein einziger Wasser-Reminder-Alarm im System.
        private const val REQUEST_CODE = 0x57415452 // "WATR"
    }
}
