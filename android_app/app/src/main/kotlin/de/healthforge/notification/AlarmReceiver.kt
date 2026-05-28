package de.healthforge.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import de.healthforge.MainActivity
import de.healthforge.R
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.repository.IntakeRepository
import de.healthforge.data.repository.ProfileRepository
import de.healthforge.data.repository.SupplementRepository
import de.healthforge.data.repository.WaterIntakeRepository
import de.healthforge.domain.ComputeNutrientTargetsUseCase
import de.healthforge.domain.applyOverrides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Fired by AlarmManager at reminder trigger time.
 *
 * - [ACTION_FIRE]: posts a Notification (with "Genommen"-action) and re-schedules
 *   DAILY/WEEKLY occurrences. ONCE reminders are disabled after fire (REQ-SUPP-005).
 * - [ACTION_TAKEN]: triggered when user taps the "Genommen"-action on the notification.
 *   Writes an [IntakeEntryEntity] with `source=SUPPLEMENT` (REQ-SUPP-003) and cancels
 *   the visible notification.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repo: SupplementRepository
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var intakeRepo: IntakeRepository
    @Inject lateinit var waterScheduler: WaterReminderScheduler
    @Inject lateinit var waterPrefs: WaterReminderPrefs
    @Inject lateinit var waterIntakeRepo: WaterIntakeRepository
    @Inject lateinit var profileRepo: ProfileRepository
    @Inject lateinit var computeTargets: ComputeNutrientTargetsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        // Wasser-Reminder hat keine `reminderId` — separat behandeln.
        if (intent.action == ACTION_WATER_FIRE) {
            handleWaterFire(context)
            return
        }
        if (intent.action == ACTION_WATER_SNOOZE) {
            handleWaterSnooze(context)
            return
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val name = intent.getStringExtra(EXTRA_SUPPLEMENT_NAME).orEmpty()
        if (reminderId <= 0) return

        when (intent.action) {
            ACTION_FIRE -> handleFire(context, reminderId, name)
            ACTION_TAKEN -> handleTaken(context, reminderId, name)
        }
    }

    private fun handleFire(context: Context, reminderId: Long, name: String) {
        postNotification(context, reminderId.toInt(), reminderId, name)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val r = repo.reminderById(reminderId) ?: return@launch
                when (r.frequency) {
                    ReminderFrequency.ONCE -> repo.upsertReminder(r.copy(enabled = false))
                    ReminderFrequency.DAILY, ReminderFrequency.WEEKLY -> scheduler.schedule(r, name)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleTaken(context: Context, reminderId: Long, name: String) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val r = repo.reminderById(reminderId) ?: return@launch
                val supplement = repo.byId(r.supplementId) ?: return@launch
                intakeRepo.add(
                    IntakeEntryEntity(
                        loggedAt = System.currentTimeMillis(),
                        dayDateIso = LocalDate.now().toString(),
                        sourceType = IntakeSourceType.SUPPLEMENT,
                        sourceId = supplement.id.toString(),
                        // Convention: portionGrams stores dose-count for SUPPLEMENT entries.
                        portionGrams = supplement.defaultDose,
                        snapshotName = if (name.isNotBlank()) name else supplement.nameDe,
                        snapshotBrand = supplement.brand,
                        snapshotKcalPer100g = supplement.kcalPerDose,
                        snapshotProteinPer100g = supplement.proteinPerDose,
                        snapshotCarbsPer100g = supplement.carbsPerDose,
                        snapshotFatPer100g = supplement.fatPerDose,
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        notifId: Int,
        reminderId: Long,
        supplementName: String,
    ) {
        val mgr = context.getSystemService<NotificationManager>() ?: return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context, notifId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val takenIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SUPPLEMENT_NAME, supplementName)
        }
        val takenPi = PendingIntent.getBroadcast(
            context, notifId + ACTION_TAKEN_REQUEST_OFFSET, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, NotificationChannels.SUPPLEMENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Supplement-Erinnerung")
            .setContentText(
                if (supplementName.isNotBlank()) "Zeit für: $supplementName" else "Zeit für deine Einnahme"
            )
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(0, "Genommen", takenPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        mgr.notify(notifId, notif)
    }

    /**
     * P7.S4 Slice 4c — Wasser-Defizit-Check (REQ-WATER-005 / REQ-HOME-WATER-ALARM-001).
     *
     * Bei jedem Tick: aktuelles Wasser-Defizit gegen das Tagesziel bei linearem
     * Soll-Verlauf (08–22 Uhr). Wenn Defizit ≥ [WaterReminderPrefs.deficitThresholdMl]
     * → Notification. Sonst still chained zum nächsten Tick.
     *
     * Tagesziel = `applyOverrides(computeTargets(profile)).waterMl` — Single-Source.
     */
    private fun handleWaterFire(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (!waterPrefs.enabled) return@launch
                val today = LocalDate.now()
                val full = profileRepo.observe().first()
                val targets = computeTargets(full.profile).applyOverrides(full.profile)
                val goalMl = targets.waterMl
                val actualMl = waterIntakeRepo.sumForDay(today)
                val expectedMl = expectedWaterByNow(goalMl, LocalTime.now())
                val deficitMl = (expectedMl - actualMl).coerceAtLeast(0)
                if (deficitMl >= waterPrefs.deficitThresholdMl) {
                    postWaterNotification(context, deficitMl, goalMl)
                    // Slice 4c.1: Eskalation hochzählen (cap N).
                    waterPrefs.escalationLevel = (waterPrefs.escalationLevel + 1)
                        .coerceAtMost(WaterReminderPrefs.ESCALATION_INTERVALS_MIN.size)
                } else {
                    // Kein Defizit → Eskalation zurücksetzen, Basis-Intervall.
                    waterPrefs.escalationLevel = 0
                }
                // Chain: nächsten Tick einplanen.
                waterScheduler.schedule()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postWaterNotification(context: Context, deficitMl: Int, goalMl: Int) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context, WATER_NOTIF_ID, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_WATER_SNOOZE
        }
        val snoozePi = PendingIntent.getBroadcast(
            context, WATER_SNOOZE_REQUEST, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, NotificationChannels.WATER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Wasser trinken")
            .setContentText("Rückstand: $deficitMl ml von $goalMl ml. Zeit für ein Glas Wasser.")
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(0, "+30 min", snoozePi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        mgr.notify(WATER_NOTIF_ID, notif)
    }

    /**
     * P7.S4 Slice 4c.2 — User tippt "+30 min" auf der Wasser-Notification.
     * - Cancelt sichtbare Notification.
     * - Resettet Eskalations-Level auf 0 (kein aggressives Re-Ping).
     * - Plant manuell einen Tick in 30 min (auch wenn `checkIntervalMin` != 30).
     */
    private fun handleWaterSnooze(context: Context) {
        NotificationManagerCompat.from(context).cancel(WATER_NOTIF_ID)
        waterPrefs.escalationLevel = 0
        if (!waterPrefs.enabled) return
        val now = java.time.LocalDateTime.now()
        var snoozeAt = now.plusMinutes(SNOOZE_MIN).withSecond(0).withNano(0)
        // Falls Snooze in inaktives Fenster fällt → auf nächstes 08:00 verschieben.
        if (snoozeAt.hour >= WaterReminderPrefs.ACTIVE_HOUR_END) {
            snoozeAt = snoozeAt.plusDays(1)
                .withHour(WaterReminderPrefs.ACTIVE_HOUR_START).withMinute(0)
        } else if (snoozeAt.hour < WaterReminderPrefs.ACTIVE_HOUR_START) {
            snoozeAt = snoozeAt
                .withHour(WaterReminderPrefs.ACTIVE_HOUR_START).withMinute(0)
        }
        val triggerAt = snoozeAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService<android.app.AlarmManager>() ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            WATER_FIRE_REQUEST,
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_WATER_FIRE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) { /* low-priority */ }
    }

    companion object {
        const val ACTION_FIRE = "de.healthforge.action.REMINDER_FIRE"
        const val ACTION_TAKEN = "de.healthforge.action.REMINDER_TAKEN"
        const val ACTION_WATER_FIRE = "de.healthforge.action.WATER_REMINDER_FIRE"
        const val ACTION_WATER_SNOOZE = "de.healthforge.action.WATER_REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SUPPLEMENT_NAME = "supplement_name"
        private const val ACTION_TAKEN_REQUEST_OFFSET = 1_000_000
        private const val WATER_NOTIF_ID = 0x57415452 // "WATR"
        // Muss identisch zu WaterReminderScheduler.REQUEST_CODE sein → gleiche PI ersetzen.
        private const val WATER_FIRE_REQUEST = 0x57415452
        private const val WATER_SNOOZE_REQUEST = 0x57415453 // "WATS"
        private const val SNOOZE_MIN = 30L

        /**
         * Linearer Soll-Verlauf zwischen 08:00 und 22:00 Uhr (REQ-HOME-WATER-ALARM-001).
         * Vor 08:00 → 0 (kein Defizit erwartet). Nach 22:00 → volles Ziel.
         */
        internal fun expectedWaterByNow(goalMl: Int, now: LocalTime): Int {
            val start = LocalTime.of(WaterReminderPrefs.ACTIVE_HOUR_START, 0)
            val end = LocalTime.of(WaterReminderPrefs.ACTIVE_HOUR_END, 0)
            if (now < start) return 0
            if (now >= end) return goalMl
            val windowMin = java.time.Duration.between(start, end).toMinutes().toDouble()
            val elapsedMin = java.time.Duration.between(start, now).toMinutes().toDouble()
            return (goalMl * (elapsedMin / windowMin)).toInt().coerceIn(0, goalMl)
        }
    }
}
