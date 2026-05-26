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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SectionPill
import de.healthforge.presentation.theme.ThemePreference

/**
 * Profil-Tab. P6.S5: Histamind-Glass-Visual (AmbientBackdrop + GlassCard sections +
 * SectionPill headers + GradientText title). Inhaltliche Goals-Editor-Erweiterung
 * ist nach P6.S6 verschoben (REQ-PROFILE-GOALS-001) — gleicher Defer-Pfad wie
 * PinnedNutrientsManager in P6.S4.
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
                    Text("Allergien: ${allergies.ifBlank { "keine" }}", color = hm.fgSecondary)
                    Text("Intoleranzen: ${intol.ifBlank { "keine" }}", color = hm.fgSecondary)
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

            SectionPill(label = "WASSERZIEL")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                val waterMl = p?.waterGoalMl ?: 2000
                Column {
                    Text("$waterMl ml pro Tag", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = waterMl.toFloat(),
                        onValueChange = { vm.setWaterGoalMl(it.toInt()) },
                        valueRange = 500f..5000f,
                        steps = 8,
                    )
                }
            }

            SectionPill(label = "TAGESZIELE")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                val goalsJson = p?.dailyNutrientGoalsJson ?: "{}"
                val pinnedJson = p?.pinnedNutrientsJson ?: "[]"
                val goals = remember(goalsJson) {
                    runCatching { org.json.JSONObject(goalsJson) }.getOrElse { org.json.JSONObject() }
                }
                val pinned: List<String> = remember(pinnedJson) {
                    runCatching {
                        val a = org.json.JSONArray(pinnedJson)
                        (0 until a.length()).map { a.getString(it) }
                    }.getOrElse { emptyList() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pinned.isEmpty()) {
                        Text(
                            "Keine Nährstoffe angeheftet. Wähle unten welche aus.",
                            color = hm.fgSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        pinned.forEach { slug ->
                            val cfg = NutrientCatalog.byOrNull(slug) ?: NutrientCatalog.fallback(slug)
                            val current = goals.optDouble(slug, cfg.default).toFloat()
                            Column {
                                Text(
                                    "${cfg.label}: ${current.toInt()} ${cfg.unit}",
                                    color = hm.fgPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Slider(
                                    value = current,
                                    onValueChange = { vm.setNutrientGoal(slug, it.toDouble()) },
                                    valueRange = cfg.min..cfg.max,
                                    steps = cfg.steps,
                                )
                            }
                        }
                    }
                }
            }

            SectionPill(label = "ANGEHEFTETE NÄHRSTOFFE")
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                val pinnedJson = p?.pinnedNutrientsJson ?: "[]"
                val pinnedSet: Set<String> = remember(pinnedJson) {
                    runCatching {
                        val a = org.json.JSONArray(pinnedJson)
                        (0 until a.length()).map { a.getString(it) }.toSet()
                    }.getOrElse { emptySet() }
                }
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NutrientCatalog.all.forEach { cfg ->
                        FilterChip(
                            selected = cfg.slug in pinnedSet,
                            onClick = { vm.togglePinnedNutrient(cfg.slug) },
                            label = { Text(cfg.label) },
                        )
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
