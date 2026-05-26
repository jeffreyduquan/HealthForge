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
import de.healthforge.data.repository.SupplementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
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

    override fun onReceive(context: Context, intent: Intent) {
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

    companion object {
        const val ACTION_FIRE = "de.healthforge.action.REMINDER_FIRE"
        const val ACTION_TAKEN = "de.healthforge.action.REMINDER_TAKEN"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SUPPLEMENT_NAME = "supplement_name"
        private const val ACTION_TAKEN_REQUEST_OFFSET = 1_000_000
    }
}
