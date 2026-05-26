package de.healthforge.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.db.entities.SupplementReminderEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact alarms for supplement reminders via AlarmManager.
 * REQ-SUPP-005, REQ-REMIND-002. Survives reboot via [BootReceiver].
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val am: AlarmManager? get() = context.getSystemService<AlarmManager>()

    /** Computes next epoch-ms trigger time for the given reminder or `null` if disabled / past. */
    fun nextTriggerAt(r: SupplementReminderEntity, now: LocalDateTime = LocalDateTime.now()): Long? {
        if (!r.enabled) return null
        val zone = ZoneId.systemDefault()
        return when (r.frequency) {
            ReminderFrequency.ONCE -> r.triggerAtMillis?.takeIf { it > System.currentTimeMillis() }
            ReminderFrequency.DAILY -> {
                val h = r.hourOfDay ?: return null
                val m = r.minute ?: return null
                var t = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
                if (!t.isAfter(now)) t = t.plusDays(1)
                t.atZone(zone).toInstant().toEpochMilli()
            }
            ReminderFrequency.WEEKLY -> {
                val h = r.hourOfDay ?: return null
                val m = r.minute ?: return null
                val mask = r.daysOfWeekMask ?: return null
                if (mask == 0) return null
                for (offset in 0..7) {
                    val candidate = now.plusDays(offset.toLong()).withHour(h).withMinute(m).withSecond(0).withNano(0)
                    if (!candidate.isAfter(now)) continue
                    val bit = 1 shl (candidate.dayOfWeek.value - 1) // Mon=0..Sun=6 → bit 1..64
                    if (mask and bit != 0) {
                        return candidate.atZone(zone).toInstant().toEpochMilli()
                    }
                }
                null
            }
        }
    }

    fun schedule(r: SupplementReminderEntity, supplementName: String) {
        val mgr = am ?: return
        val triggerAt = nextTriggerAt(r) ?: return
        val pi = pendingIntentFor(r.id, supplementName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !mgr.canScheduleExactAlarms()) {
                // Fall back to inexact alarm if user hasn't granted SCHEDULE_EXACT_ALARM
                mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(reminderId: Long) {
        val mgr = am ?: return
        mgr.cancel(pendingIntentFor(reminderId, ""))
    }

    private fun pendingIntentFor(reminderId: Long, supplementName: String): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_SUPPLEMENT_NAME, supplementName)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Day-of-week mask helpers (Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64). */
    companion object {
        fun maskOf(days: Set<DayOfWeek>): Int =
            days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
        fun containsDay(mask: Int, d: DayOfWeek): Boolean =
            mask and (1 shl (d.value - 1)) != 0
    }
}
