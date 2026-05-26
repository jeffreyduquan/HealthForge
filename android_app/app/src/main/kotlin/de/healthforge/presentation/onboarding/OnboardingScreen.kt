package de.healthforge.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.ActivityLevel
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.BiologicalSex
import de.healthforge.data.db.entities.DietGoal
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.db.entities.HistamineSensitivity
import de.healthforge.data.db.entities.MealSlot
import de.healthforge.presentation.theme.ThemePreference

private const val TOTAL_STEPS = 14

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onFinished() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Onboarding (${s.stepIndex + 1}/$TOTAL_STEPS)") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (s.stepIndex + 1f) / TOTAL_STEPS },
                modifier = Modifier.fillMaxWidth(),
            )
            when (s.stepIndex) {
                0 -> StepWelcome()
                1 -> StepDisplayName(s.displayName, vm::setDisplayName)
                2 -> StepAge(s.ageYears, vm::setAge)
                3 -> StepSex(s.sex, vm::setSex)
                4 -> StepHeight(s.heightCm, vm::setHeight)
                5 -> StepWeight(s.weightKg, vm::setWeight)
                6 -> StepActivity(s.activity, vm::setActivity)
                7 -> StepGoal(s.goal, vm::setGoal)
                8 -> StepAllergies(s.allergies, vm::toggleAllergy)
                9 -> StepIntolerances(s.intolerances, s.histamine, vm::toggleIntolerance, vm::setHistamine)
                10 -> StepMealSlots(s.mealSlots, vm::toggleMealSlot)
                11 -> StepMaxPrep(s.maxPrepTimeMin, vm::setMaxPrepTime)
                12 -> StepTheme(s.theme, vm::setTheme)
                13 -> StepReview(s)
            }
            Spacer(Modifier.height(8.dp))
            NavButtons(
                stepIndex = s.stepIndex,
                committing = s.committing,
                onBack = vm::back,
                onNext = vm::next,
                onFinish = vm::commit,
            )
        }
    }
}

@Composable
private fun NavButtons(
    stepIndex: Int,
    committing: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (stepIndex > 0) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Zur\u00fcck") }
        }
        if (stepIndex < TOTAL_STEPS - 1) {
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Weiter") }
        } else {
            Button(
                onClick = onFinish,
                enabled = !committing,
                modifier = Modifier.weight(1f),
            ) { Text(if (committing) "Speichern\u2026" else "Fertig") }
        }
    }
}

@Composable
private fun StepWelcome() {
    Text("Willkommen bei HealthForge", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Wir richten dein Profil ein, damit Empfehlungen zu deinen Bed\u00fcrfnissen passen. " +
            "Alle Daten bleiben verschl\u00fcsselt auf diesem Ger\u00e4t.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun StepDisplayName(value: String, onChange: (String) -> Unit) {
    Text("Wie sollen wir dich nennen?", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Anzeigename (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepAge(value: Int?, onChange: (Int?) -> Unit) {
    NumberStep("Wie alt bist du?", "Alter (Jahre)", value?.toString().orEmpty()) {
        onChange(it.toIntOrNull())
    }
}

@Composable
private fun StepSex(value: BiologicalSex?, onChange: (BiologicalSex) -> Unit) {
    Text("Biologisches Geschlecht", style = MaterialTheme.typography.titleLarge)
    Text(
        "F\u00fcr die Berechnung des Grundumsatzes (Mifflin\u2013St Jeor).",
        style = MaterialTheme.typography.bodySmall,
    )
    BiologicalSex.entries.forEach { opt ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = value == opt, onClick = { onChange(opt) })
            Text(when (opt) {
                BiologicalSex.MALE -> "M\u00e4nnlich"
                BiologicalSex.FEMALE -> "Weiblich"
                BiologicalSex.OTHER -> "Divers / keine Angabe"
            })
        }
    }
}

@Composable
private fun StepHeight(value: Int?, onChange: (Int?) -> Unit) {
    NumberStep("K\u00f6rpergr\u00f6\u00dfe", "Gr\u00f6\u00dfe (cm)", value?.toString().orEmpty()) {
        onChange(it.toIntOrNull())
    }
}

@Composable
private fun StepWeight(value: Double?, onChange: (Double?) -> Unit) {
    NumberStep(
        title = "K\u00f6rpergewicht",
        label = "Gewicht (kg)",
        value = value?.toString().orEmpty(),
    ) { onChange(it.replace(',', '.').toDoubleOrNull()) }
}

@Composable
private fun NumberStep(
    title: String,
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepActivity(value: ActivityLevel?, onChange: (ActivityLevel) -> Unit) {
    Text("Wie aktiv bist du?", style = MaterialTheme.typography.titleLarge)
    ActivityLevel.entries.forEach { opt ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = value == opt, onClick = { onChange(opt) })
            Text(when (opt) {
                ActivityLevel.SEDENTARY -> "Sitzend (kaum Sport)"
                ActivityLevel.LIGHT -> "Leicht aktiv (1\u20133x Sport / Woche)"
                ActivityLevel.MODERATE -> "Moderat (3\u20135x Sport / Woche)"
                ActivityLevel.ACTIVE -> "Aktiv (6\u20137x Sport / Woche)"
                ActivityLevel.VERY_ACTIVE -> "Sehr aktiv (intensiv + k\u00f6rperliche Arbeit)"
            })
        }
    }
}

@Composable
private fun StepGoal(value: DietGoal?, onChange: (DietGoal) -> Unit) {
    Text("Was ist dein Ziel?", style = MaterialTheme.typography.titleLarge)
    DietGoal.entries.forEach { opt ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = value == opt, onClick = { onChange(opt) })
            Text(when (opt) {
                DietGoal.LOSE -> "Abnehmen (\u221220 % Kalorien)"
                DietGoal.MAINTAIN -> "Gewicht halten"
                DietGoal.GAIN -> "Aufbau (+15 % Kalorien)"
            })
        }
    }
}

@Composable
private fun StepAllergies(selected: Set<AllergenType>, onToggle: (AllergenType) -> Unit) {
    Text("Allergien (EU-14)", style = MaterialTheme.typography.titleLarge)
    Text("Mehrfachauswahl m\u00f6glich.", style = MaterialTheme.typography.bodySmall)
    AllergenType.entries.forEach { a ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = a in selected, onCheckedChange = { onToggle(a) })
            Text(a.germanLabel)
        }
    }
}

@Composable
private fun StepIntolerances(
    selected: Set<FodmapType>,
    histamine: HistamineSensitivity,
    onToggle: (FodmapType) -> Unit,
    onHistamine: (HistamineSensitivity) -> Unit,
) {
    Text("FODMAP-Intoleranzen", style = MaterialTheme.typography.titleLarge)
    FodmapType.entries.forEach { f ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = f in selected, onCheckedChange = { onToggle(f) })
            Text(f.germanLabel)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Histamin-Empfindlichkeit", style = MaterialTheme.typography.titleMedium)
    HistamineSensitivity.entries.forEach { h ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = histamine == h, onClick = { onHistamine(h) })
            Text(when (h) {
                HistamineSensitivity.NONE -> "Keine"
                HistamineSensitivity.MILD -> "Leicht"
                HistamineSensitivity.MODERATE -> "Mittel"
                HistamineSensitivity.HIGH -> "Stark"
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepMealSlots(selected: Set<MealSlot>, onToggle: (MealSlot) -> Unit) {
    Text("Welche Mahlzeiten m\u00f6chtest du planen?", style = MaterialTheme.typography.titleLarge)
    MealSlot.entries.forEach { m ->
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = m in selected, onCheckedChange = { onToggle(m) })
            Text(when (m) {
                MealSlot.FRUEHSTUECK -> "Fr\u00fchst\u00fcck"
                MealSlot.ZWEITES_FRUEHSTUECK -> "Zweites Fr\u00fchst\u00fcck"
                MealSlot.MITTAG -> "Mittagessen"
                MealSlot.SNACK -> "Snack"
                MealSlot.ABENDESSEN -> "Abendessen"
                MealSlot.NACHT -> "Nachts"
            })
        }
    }
}

@Composable
private fun StepMaxPrep(value: Int?, onChange: (Int?) -> Unit) {
    NumberStep(
        title = "Maximale Zubereitungszeit",
        label = "Minuten (optional)",
        value = value?.toString().orEmpty(),
    ) { onChange(it.toIntOrNull()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepTheme(value: ThemePreference, onChange: (ThemePreference) -> Unit) {
    Text("Erscheinungsbild", style = MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreference.entries.forEach { t ->
            FilterChip(
                selected = value == t,
                onClick = { onChange(t) },
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
}

@Composable
private fun StepReview(s: OnboardingState) {
    Text("Zusammenfassung", style = MaterialTheme.typography.titleLarge)
    val kcal = s.computedKcalTarget
    if (kcal != null) {
        Text("Berechnetes Kalorienziel: $kcal kcal/Tag", style = MaterialTheme.typography.bodyLarge)
    } else {
        Text(
            "Kalorienziel konnte nicht berechnet werden \u2014 einige Werte fehlen.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Text("Allergien: ${if (s.allergies.isEmpty()) "keine" else s.allergies.joinToString { it.germanLabel }}")
    Text("Intoleranzen: ${if (s.intolerances.isEmpty()) "keine" else s.intolerances.joinToString { it.germanLabel }}")
    Text("Mahlzeiten: ${s.mealSlots.joinToString { it.name }}")
}
