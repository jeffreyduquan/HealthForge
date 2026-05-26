package de.healthforge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.healthforge.notification.NotificationChannels

@HiltAndroidApp
class HealthForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
    }
}
