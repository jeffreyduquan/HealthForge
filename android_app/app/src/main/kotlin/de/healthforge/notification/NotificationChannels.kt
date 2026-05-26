package de.healthforge.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/** Notification channels. Created idempotently on app start. */
object NotificationChannels {
    const val SUPPLEMENT = "ch_supplement"
    const val MEAL = "ch_meal"
    const val WATER = "ch_water"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService<NotificationManager>() ?: return
        listOf(
            NotificationChannel(SUPPLEMENT, "Supplements", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Erinnerungen für Supplement-Einnahmen"
            },
            NotificationChannel(MEAL, "Mahlzeiten", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Mahlzeit-Erinnerungen"
            },
            NotificationChannel(WATER, "Wasser", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Wasser-Erinnerungen"
            },
        ).forEach(mgr::createNotificationChannel)
    }
}
