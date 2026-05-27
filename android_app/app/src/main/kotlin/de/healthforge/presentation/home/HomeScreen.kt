package de.healthforge.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.domain.IsIntakeEditableUseCase
import de.healthforge.presentation.home.components.DateNavigator
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
import de.healthforge.presentation.home.components.QuickAddDialog
import de.healthforge.presentation.home.components.SupplementChecklist
import de.healthforge.presentation.home.components.WaterStageSlider
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val editableUseCase = IsIntakeEditableUseCase()
    val hm = LocalHmTokens.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }
    val snackbarHostState = remember { SnackbarHostState() }

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header — Greeting + Date + History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GradientText(
                        text = "Hallo!",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = s.date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = hm.fgSecondary,
                    )
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = "Verlauf",
                        tint = hm.fgSecondary,
                    )
                }
            }

            DateNavigator(date = s.date, onChange = vm::setDate)

            // P7.S3 / REQ-HOME-NUTRIENT-LIST-001 \u2014 Pinned Nutrients statt fester Macro-Bars.
            // Standard-Pins: kcal / Eiwei\u00df / Kohlenhydrate / Fett (Wasser kommt als
            // interaktive Slider-Zeile am Ende, siehe REQ-HOME-WATER-BAR-001 v2 /
            // `WaterStageSlider`). P7.S5: User darf Pins anpassen.
            NeoSectionLabel(text = "Ern\u00e4hrung")
            NeoCard {
                PinnedNutrientCard(
                    entries = s.pinnedKeys.filter { it != "water" }.map { key ->
                        when (key) {
                            "kcal" -> PinnedNutrientEntry(key, s.totals.kcal.toDouble(), s.targets.kcal.toDouble())
                            "protein" -> PinnedNutrientEntry(key, s.totals.proteinG, s.targets.proteinG.toDouble())
                            "carbs" -> PinnedNutrientEntry(key, s.totals.carbsG, s.targets.carbsG.toDouble())
                            "fat" -> PinnedNutrientEntry(key, s.totals.fatG, s.targets.fatG.toDouble())
                            else -> {
                                // P7.S3.b: Micronutrient-Totals folgen sobald Intake-Snapshots
                                // micronutrients_json mitschreiben (REQ-INGR-MICRONUTRIENTS-001).
                                val def = de.healthforge.domain.nutrition.NutrientCatalog
                                    .byKeyOrNull(key)?.defaultPerDay ?: 1.0
                                PinnedNutrientEntry(key, 0.0, def)
                            }
                        }
                    },
                    trailingSlot = if (s.pinnedKeys.contains("water")) {
                        {
                            // P7.S3a v2 / REQ-HOME-WATER-BAR-001 \u2014 Stufen-Slider als
                            // letzte Pin-Zeile. Range 0..goal (0\u2013100\u00a0% der aktuellen
                            // Stufe), 50-ml-Steps, eigene Farbe pro Stufe 0..9,
                            // Drag-Through-Zero entlocked Stufe-1.
                            WaterStageSlider(
                                currentMl = s.waterMl,
                                ghostMl = s.waterGhostMl,
                                goalMl = s.targets.waterMl,
                                reminderEnabled = s.waterReminderEnabled,
                                onCommit = vm::setWaterMl,
                                onToggleReminder = vm::setWaterReminderEnabled,
                            )
                        }
                    } else null,
                )
            }


            if (s.supplementChecklist.isNotEmpty()) {
                NeoSectionLabel(text = "Supplemente")
                NeoCard(contentPadding = PaddingValues(0.dp)) {
                    SupplementChecklist(
                        items = s.supplementChecklist,
                        onMarkTaken = vm::markSupplementTaken,
                    )
                }
            }

            // HEUTIGE EINTRÄGE
            NeoSectionLabel(text = "Heutige Eintr\u00e4ge")
            if (s.entries.isEmpty()) {
                GlassCard {
                    Text(
                        "Noch keine Eintr\u00e4ge. Tippe auf das Plus, um zu starten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = hm.fgSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    s.entries.take(5).forEach { e ->
                        IntakeRow(
                            entry = e,
                            editable = editableUseCase(e.loggedAt),
                            onDelete = { vm.deleteEntry(e.id) },
                        )
                    }
                }
                if (s.entries.size > 5) {
                    TextButton(onClick = onOpenHistory) {
                        Text("Alle ${s.entries.size} Eintr\u00e4ge anzeigen \u2192")
                    }
                }
            }

            Spacer(Modifier.height(96.dp).navigationBarsPadding())
        }

        // GradientFab overlay (REQ-HOME-003)
        GradientFab(
            onClick = vm::openQuickAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = "Hinzuf\u00fcgen", tint = hm.fgPrimary) },
        )

        // P6.S7 F-005: Snackbar-Host f\u00fcr Wasser-Undo (Long-Press-Hint).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }

    if (s.showQuickAdd) {
        QuickAddDialog(
            query = s.quickAddQuery,
            results = s.quickAddResults,
            portionGrams = s.quickAddPortion,
            selected = s.quickAddSelected,
            loading = s.quickAddLoading,
            onQueryChange = vm::onQuickAddQuery,
            onSelect = vm::onQuickAddSelect,
            onClearSelection = vm::onQuickAddClearSelection,
            onPortionChange = vm::onQuickAddPortion,
            onConfirm = vm::confirmQuickAdd,
            onDismiss = vm::closeQuickAdd,
        )
    }
}

@Composable
private fun IntakeRow(
    entry: IntakeEntryEntity,
    editable: Boolean,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.snapshotName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = hm.fgPrimary,
                )
                val portion = "${entry.portionGrams.toInt()} g"
                val kcal = entry.snapshotKcalPer100g?.let {
                    " \u00b7 ${(it * entry.portionGrams / 100.0).toInt()} kcal"
                } ?: ""
                Text(
                    "$portion$kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = hm.fgSecondary,
                )
            }
            if (editable) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "L\u00f6schen", tint = hm.fgSecondary)
                }
            } else {
                Text(
                    "\u00fcber 7 Tage",
                    style = MaterialTheme.typography.labelSmall,
                    color = hm.fgTertiary,
                )
            }
        }
    }
}
