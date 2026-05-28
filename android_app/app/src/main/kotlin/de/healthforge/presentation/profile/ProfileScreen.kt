package de.healthforge.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.domain.DailyTargets
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SectionPill
import de.healthforge.presentation.theme.ThemePreference

/**
 * Profil-Tab. P6.S5: Histamind-Glass-Visual (AmbientBackdrop + GlassCard sections +
 * SectionPill headers + GradientText title). P7.S4 (REQ-PROFILE-LAYOUT-001):
 * Tagesziele expandiert auf vollen NutrientCatalog (~30 Einträge, gruppiert nach Kategorie),
 * Wasser ist hier integriert (vorher eigene WASSERZIEL-Section), und das
 * P6.S6-Pin-Mgmt-Chip-Grid ist entfernt (Pin-Verwaltung erfolgt jetzt im Home-Tab).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val hm = LocalHmTokens.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            GradientText(
                text = "Profil",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )

            val p = full?.profile
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        p?.displayName?.takeIf { it.isNotBlank() } ?: "Anonymes Profil",
                        style = MaterialTheme.typography.titleLarge,
                        color = hm.fgPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (p != null) {
                        Text("Alter: ${p.ageYears ?: "\u2013"}", color = hm.fgSecondary)
                        Text("Gr\u00f6\u00dfe: ${p.heightCm ?: "\u2013"} cm", color = hm.fgSecondary)
                        Text("Gewicht: ${p.weightKg ?: "\u2013"} kg", color = hm.fgSecondary)
                        Text("Aktivit\u00e4t: ${p.activityLevel?.name ?: "\u2013"}", color = hm.fgSecondary)
                        Text("Ziel: ${p.dietGoal?.name ?: "\u2013"}", color = hm.fgSecondary)
                        Text("Mahlzeiten: ${p.mealSlotsJson}", color = hm.fgTertiary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    val allergies = full?.allergies?.joinToString { it.germanLabel } ?: ""
                    val intol = full?.intolerances?.joinToString { it.germanLabel } ?: ""
                    Text(
                        "Allergien: ${allergies.ifBlank { "keine" }}",
                        color = hm.fgTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Intoleranzen: ${intol.ifBlank { "keine" }}",
                        color = hm.fgTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SectionPill(label = "ALLERGIEN & INTOLERANZEN")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                val selectedAllergies = full?.allergies ?: emptySet()
                val selectedIntol = full?.intolerances ?: emptySet()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Allergien (EU-14)",
                        color = hm.fgSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        de.healthforge.data.db.entities.AllergenType.entries.forEach { a ->
                            val isOn = a in selectedAllergies
                            FilterChip(
                                selected = isOn,
                                onClick = {
                                    val next = selectedAllergies.toMutableSet()
                                    if (isOn) next.remove(a) else next.add(a)
                                    vm.setAllergies(next)
                                },
                                label = { Text(a.germanLabel, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                    Text(
                        "FODMAP-Intoleranzen",
                        color = hm.fgSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        de.healthforge.data.db.entities.FodmapType.entries.forEach { f ->
                            val isOn = f in selectedIntol
                            FilterChip(
                                selected = isOn,
                                onClick = {
                                    val next = selectedIntol.toMutableSet()
                                    if (isOn) next.remove(f) else next.add(f)
                                    vm.setIntolerances(next)
                                },
                                label = { Text(f.germanLabel, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }

            SectionPill(label = "ERSCHEINUNGSBILD")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
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
            }

            SectionPill(label = "TAGESZIELE")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                val defaults by vm.computedDefaults.collectAsStateWithLifecycle()
                val goalsJson = p?.dailyNutrientGoalsJson ?: "{}"
                val goals = remember(goalsJson) {
                    runCatching { org.json.JSONObject(goalsJson) }.getOrElse { org.json.JSONObject() }
                }
                val waterMl = p?.waterGoalMl ?: 2000
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    de.healthforge.domain.nutrition.NutrientCatalog.Category.entries.forEach { cat ->
                        val rows = de.healthforge.domain.nutrition.NutrientCatalog.ofCategory(cat)
                        if (rows.isEmpty()) return@forEach
                        Text(
                            categoryLabel(cat),
                            color = hm.fgSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                        rows.forEach { nut ->
                            val computedDefault = effectiveDefault(nut, defaults, nut.defaultPerDay)
                            val override: Double? = when (nut.key) {
                                "water" -> waterMl.toDouble().takeIf { it != 2000.0 }
                                else -> if (goals.has(nut.key)) goals.optDouble(nut.key) else null
                            }
                            de.healthforge.presentation.profile.components.NutrientGoalRow(
                                nutrient = nut,
                                effectiveDefault = computedDefault,
                                override = override,
                                onChange = { vm.setNutrientGoal(nut.key, it) },
                                onReset = { vm.clearNutrientGoal(nut.key) },
                            )
                        }
                    }
                }
            }

            SectionPill(label = "MEHR")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenGroups, modifier = Modifier.fillMaxWidth()) {
                        Text("Meine Gruppen")
                    }
                    OutlinedButton(onClick = onOpenSymptomManager, modifier = Modifier.fillMaxWidth()) {
                        Text("Symptome verwalten")
                    }
                    OutlinedButton(onClick = onOpenExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Daten exportieren")
                    }
                    OutlinedButton(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) {
                        Text("Erkenntnisse")
                    }
                    OutlinedButton(
                        onClick = {
                            vm.restartOnboarding()
                            onRestartOnboarding()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Onboarding wiederholen") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * P7.S4 / REQ-PROFILE-LAYOUT-001 — Effektiver Default-Wert pro Nährstoff:
 * Makros + Wasser kommen aus [ComputeNutrientTargetsUseCase] (profilabhängig),
 * Mikros aus der statischen DGE-Empfehlung im [NutrientCatalog].
 */
private fun effectiveDefault(
    nut: de.healthforge.domain.nutrition.NutrientCatalog.Nutrient,
    computed: DailyTargets,
    catalogDefault: Double,
): Double = when (nut.key) {
    "kcal" -> computed.kcal.toDouble()
    "protein" -> computed.proteinG.toDouble()
    "carbs" -> computed.carbsG.toDouble()
    "fat" -> computed.fatG.toDouble()
    "water" -> computed.waterMl.toDouble()
    else -> catalogDefault
}

private fun categoryLabel(c: de.healthforge.domain.nutrition.NutrientCatalog.Category): String = when (c) {
    de.healthforge.domain.nutrition.NutrientCatalog.Category.MACRO -> "Makros"
    de.healthforge.domain.nutrition.NutrientCatalog.Category.VITAMIN -> "Vitamine"
    de.healthforge.domain.nutrition.NutrientCatalog.Category.MINERAL -> "Mineralstoffe"
    de.healthforge.domain.nutrition.NutrientCatalog.Category.WATER -> "Wasser"
}
