package de.healthforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import de.healthforge.data.prefs.SettingsDataStore
import de.healthforge.presentation.navigation.HealthForgeNavHost
import de.healthforge.presentation.theme.HealthForgeTheme
import de.healthforge.presentation.theme.ThemePreference
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by settings.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
            val onboardingDone by settings.onboardingCompleted.collectAsState(initial = false)
            HealthForgeTheme(preference = theme) {
                HealthForgeNavHost(onboardingCompleted = onboardingDone)
            }
        }
    }
}
