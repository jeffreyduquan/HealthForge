package de.healthforge.presentation.supplements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfSectionHeader
import de.healthforge.presentation.lebensmittel.StepDotsRow
import de.healthforge.presentation.lebensmittel.WizardNav
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GradientButton
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens

private const val SUPP_WIZARD_STEPS = 4

@Composable
fun SupplementWizardScreen(
    supplementId: Long = 0L,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: SupplementEditViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(supplementId) { if (supplementId > 0) vm.load(supplementId) }
    LaunchedEffect(s.saved) { if (s.saved) onSaved() }

    Box(
        modifier = Modifier.fillMaxSize().background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (stepIndex == 0) onBack() else stepIndex-- }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurueck", tint = hm.fgPrimary)
                }
                Spacer(Modifier.weight(1f))
                StepDotsRow(currentIndex = stepIndex, total = SUPP_WIZARD_STEPS)
            }

            // Content
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                when (stepIndex) {
                    0 -> {
                        GradientText("Supplement", style = MaterialTheme.typography.headlineMedium)
                        HfSectionHeader("Name & Marke")
                        HfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = s.name, onValueChange = vm::setName,
                                    label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = s.brand, onValueChange = vm::setBrand,
                                    label = { Text("Marke (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    1 -> {
                        GradientText("Dosierung", style = MaterialTheme.typography.headlineMedium)
                        HfSectionHeader("Dosis & Einheit")
                        HfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = s.defaultDose, onValueChange = vm::setDose,
                                        label = { Text("Dosis *") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = s.unitLabel, onValueChange = vm::setUnit,
                                        label = { Text("Einheit (z.B. Tabl.)") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                }
                                OutlinedTextField(
                                    value = s.kcal, onValueChange = vm::setKcal,
                                    label = { Text("kcal pro Dosis (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    2 -> {
                        GradientText("Naehrwerte", style = MaterialTheme.typography.headlineMedium)
                        HfSectionHeader("Pro Dosis (optional)")
                        HfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = s.protein, onValueChange = vm::setProtein,
                                        label = { Text("Eiweiss (g)") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = s.carbs, onValueChange = vm::setCarbs,
                                        label = { Text("KH (g)") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                }
                                OutlinedTextField(
                                    value = s.fat, onValueChange = vm::setFat,
                                    label = { Text("Fett (g)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = s.notes, onValueChange = vm::setNotes,
                                    label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    3 -> {
                        GradientText("Vorschau", style = MaterialTheme.typography.headlineMedium)
                        HfSectionHeader("Zusammenfassung")
                        HfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (s.name.isNotBlank()) Text("Name: ${s.name}", color = hm.fgPrimary)
                                if (s.brand.isNotBlank()) Text("Marke: ${s.brand}", color = hm.fgSecondary)
                                Text("Dosis: ${s.defaultDose.ifBlank { "-" }} ${s.unitLabel}", color = hm.fgPrimary)
                                if (s.kcal.isNotBlank()) Text("Kalorien: ${s.kcal} kcal/Dosis", color = hm.fgPrimary)
                                if (s.protein.isNotBlank()) Text("Eiweiss: ${s.protein} g", color = hm.fgPrimary)
                                if (s.carbs.isNotBlank()) Text("KH: ${s.carbs} g", color = hm.fgPrimary)
                                if (s.fat.isNotBlank()) Text("Fett: ${s.fat} g", color = hm.fgPrimary)
                                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Nav buttons
            WizardNav(
                stepIndex = stepIndex,
                total = SUPP_WIZARD_STEPS,
                nextEnabled = when (stepIndex) { 0 -> s.name.isNotBlank(); 1 -> s.defaultDose.isNotBlank(); else -> true },
                submitting = s.saving,
                submitLabel = "Speichern",
                onBack = { stepIndex-- },
                onNext = { stepIndex++ },
                onSubmit = { vm.save() },
            )
        }
    }
}
