package de.healthforge.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.presentation.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onRestartOnboarding: () -> Unit,
    onOpenGroups: () -> Unit = {},
    onOpenSymptomManager: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    vm: ProfileViewModel = hiltViewModel(),
) {
    val full by vm.profile.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Profil") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val p = full?.profile
            Text(
                p?.displayName?.takeIf { it.isNotBlank() } ?: "Anonymes Profil",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (p != null) {
                Text("Alter: ${p.ageYears ?: "\u2013"}")
                Text("Gr\u00f6\u00dfe: ${p.heightCm ?: "\u2013"} cm")
                Text("Gewicht: ${p.weightKg ?: "\u2013"} kg")
                Text("Aktivit\u00e4t: ${p.activityLevel?.name ?: "\u2013"}")
                Text("Ziel: ${p.dietGoal?.name ?: "\u2013"}")
                Text("Mahlzeiten: ${p.mealSlotsJson}")
            }
            val allergies = full?.allergies?.joinToString { it.germanLabel } ?: ""
            val intol = full?.intolerances?.joinToString { it.germanLabel } ?: ""
            Text("Allergien: ${allergies.ifBlank { "keine" }}")
            Text("Intoleranzen: ${intol.ifBlank { "keine" }}")

            Spacer(Modifier.height(8.dp))
            Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { t ->
                    FilterChip(
                        selected = theme == t,
                        onClick = { vm.setTheme(t) },
                        label = {
                            Text(when (t) {
                                ThemePreference.LIGHT -> "Hell"
                                ThemePreference.DARK -> "Dunkel"
                                ThemePreference.SYSTEM -> "System"
                            })
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Wasserziel", style = MaterialTheme.typography.titleMedium)
            val waterMl = p?.waterGoalMl ?: 2000
            Text("$waterMl ml pro Tag")
            Slider(
                value = waterMl.toFloat(),
                onValueChange = { vm.setWaterGoalMl(it.toInt()) },
                valueRange = 500f..5000f,
                steps = 8, // 500 .. 5000 in 500ml-Schritten
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenGroups,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Meine Gruppen") }
            OutlinedButton(
                onClick = onOpenSymptomManager,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Symptome verwalten") }
            OutlinedButton(
                onClick = onOpenExport,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Daten exportieren") }
            OutlinedButton(
                onClick = onOpenInsights,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Erkenntnisse") }
            OutlinedButton(
                onClick = {
                    vm.restartOnboarding()
                    onRestartOnboarding()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Onboarding wiederholen") }
        }
    }
}
