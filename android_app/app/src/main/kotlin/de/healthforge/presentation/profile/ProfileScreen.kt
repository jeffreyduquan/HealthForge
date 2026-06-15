package de.healthforge.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.BuildConfig
import de.healthforge.domain.DailyTargets
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientButton
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
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val hm = LocalHmTokens.current
    var showAllergies by remember { mutableStateOf(false) }
    var showGoals by remember { mutableStateOf(false) }

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
                }
            }

            // P7.S5: Onboarding-Button prominent direkt unter Profil-Card
            GradientButton(
                text = "Onboarding wiederholen",
                onClick = {
                    vm.restartOnboarding()
                    onRestartOnboarding()
                },
            )

            // Allergien — opens ModalBottomSheet
            SectionPill(label = "ALLERGIEN & INTOLERANZEN")
            TextButton(onClick = { showAllergies = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Allergien & Intoleranzen bearbeiten →", color = hm.fgPrimary, style = MaterialTheme.typography.bodyLarge)
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

            // Tagesziele — opens ModalBottomSheet
            SectionPill(label = "TAGESZIELE")
            TextButton(onClick = { showGoals = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tagesziele bearbeiten →", color = hm.fgPrimary, style = MaterialTheme.typography.bodyLarge)
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

                    // === In-App Update ===
                    when {
                        updateState.checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Suche nach Updates…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        updateState.isUpToDate -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text("✅ App ist aktuell (${BuildConfig.VERSION_NAME})",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        updateState.updateAvailable != null -> {
                            val rel = updateState.updateAvailable!!
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "📦 ${rel.version} verfügbar (${formatFileSize(rel.fileSize)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (!rel.changelog.isNullOrBlank()) {
                                    Text(rel.changelog, style = MaterialTheme.typography.bodySmall,
                                        color = hm.fgTertiary, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
                                }
                                Button(
                                    onClick = { vm.downloadUpdate(ctx) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !updateState.downloading,
                                ) {
                                    if (updateState.downloading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (updateState.downloading) "Lädt herunter…" else "Update herunterladen")
                                }
                            }
                        }
                        updateState.error != null -> {
                            Text("❌ ${updateState.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (updateState.updateAvailable == null && !updateState.checking && !updateState.isUpToDate && updateState.error == null) {
                        OutlinedButton(
                            onClick = { vm.checkForUpdate() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Nach Updates suchen")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Modal Sheets
    if (showAllergies) {
        val selectedAllergies = full?.allergies ?: emptySet()
        val selectedIntol = full?.intolerances ?: emptySet()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAllergies = false }, sheetState = sheetState, containerColor = hm.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientText("Allergien & Intoleranzen", style = MaterialTheme.typography.titleLarge)
                Text("Allergien (EU-14)", color = hm.fgPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    de.healthforge.data.db.entities.AllergenType.entries.forEach { a ->
                        val isOn = a in selectedAllergies
                        FilterChip(selected = isOn, onClick = {
                            val next = selectedAllergies.toMutableSet()
                            if (isOn) next.remove(a) else next.add(a)
                            vm.setAllergies(next)
                        }, label = { Text(a.germanLabel, style = MaterialTheme.typography.bodySmall) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("FODMAP-Intoleranzen", color = hm.fgPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    de.healthforge.data.db.entities.FodmapType.entries.forEach { f ->
                        val isOn = f in selectedIntol
                        FilterChip(selected = isOn, onClick = {
                            val next = selectedIntol.toMutableSet()
                            if (isOn) next.remove(f) else next.add(f)
                            vm.setIntolerances(next)
                        }, label = { Text(f.germanLabel, style = MaterialTheme.typography.bodySmall) })
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    if (showGoals) {
        val p = full?.profile
        val defaults by vm.computedDefaults.collectAsStateWithLifecycle()
        val goalsJson = p?.dailyNutrientGoalsJson ?: "{}"
        val goals = remember(goalsJson) { runCatching { org.json.JSONObject(goalsJson) }.getOrElse { org.json.JSONObject() } }
        val waterMl = p?.waterGoalMl ?: 2000
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showGoals = false }, sheetState = sheetState, containerColor = hm.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                GradientText("Tagesziele", style = MaterialTheme.typography.titleLarge)
                de.healthforge.domain.nutrition.NutrientCatalog.Category.entries.forEach { cat ->
                    val rows = de.healthforge.domain.nutrition.NutrientCatalog.ofCategory(cat)
                    if (rows.isEmpty()) return@forEach
                    Text(categoryLabel(cat), color = hm.fgSecondary, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    rows.forEach { nut ->
                        val computedDefault = effectiveDefault(nut, defaults, nut.defaultPerDay)
                        val override: Double? = when (nut.key) {
                            "water" -> waterMl.toDouble().takeIf { it != 2000.0 }
                            else -> if (goals.has(nut.key)) goals.optDouble(nut.key) else null
                        }
                        de.healthforge.presentation.profile.components.NutrientGoalRow(
                            nutrient = nut, effectiveDefault = computedDefault, override = override,
                            onChange = { vm.setNutrientGoal(nut.key, it) },
                            onReset = { vm.clearNutrientGoal(nut.key) },
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
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

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "${"%.1f".format(bytes / 1_000_000.0)} MB"
    bytes >= 1_000 -> "${"%.0f".format(bytes / 1_000.0)} KB"
    else -> "$bytes B"
}
