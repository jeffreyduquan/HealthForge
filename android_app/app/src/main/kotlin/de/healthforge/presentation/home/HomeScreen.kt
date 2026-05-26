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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.domain.IsIntakeEditableUseCase
import de.healthforge.presentation.home.components.DateNavigator
import de.healthforge.presentation.home.components.MacroRingRow
import de.healthforge.presentation.home.components.QuickAddDialog
import de.healthforge.presentation.home.components.SupplementChecklist
import de.healthforge.presentation.home.components.WaterTracker
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SectionPill
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

            // ERNÄHRUNG
            SectionPill(label = "Ern\u00e4hrung")
            GlassCard {
                MacroRingRow(
                    kcal = s.totals.kcal, kcalTarget = s.targets.kcal,
                    proteinG = s.totals.proteinG, proteinTarget = s.targets.proteinG,
                    carbsG = s.totals.carbsG, carbsTarget = s.targets.carbsG,
                    fatG = s.totals.fatG, fatTarget = s.targets.fatG,
                )
            }

            // WASSER
            SectionPill(label = "Wasser")
            GlassCard(padding = PaddingValues(0.dp)) {
                WaterTracker(
                    currentMl = s.waterMl,
                    goalMl = s.targets.waterMl,
                    onAdd = vm::addWater,
                    onCustom = vm::openWaterCustom,
                    reminderEnabled = s.waterReminderEnabled,
                    onReminderToggle = vm::setWaterReminderEnabled,
                )
            }

            if (s.supplementChecklist.isNotEmpty()) {
                SectionPill(label = "Supplemente")
                GlassCard(padding = PaddingValues(0.dp)) {
                    SupplementChecklist(
                        items = s.supplementChecklist,
                        onMarkTaken = vm::markSupplementTaken,
                    )
                }
            }

            // HEUTIGE EINTRÄGE
            SectionPill(label = "Heutige Eintr\u00e4ge")
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

    if (s.showWaterCustom) {
        AlertDialog(
            onDismissRequest = vm::closeWaterCustom,
            title = { Text("Wasser hinzuf\u00fcgen") },
            text = {
                OutlinedTextField(
                    value = s.waterCustomMl,
                    onValueChange = vm::onWaterCustomChange,
                    label = { Text("Menge (ml)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = vm::confirmWaterCustom,
                    enabled = s.waterCustomMl.toIntOrNull()?.let { it in 1..5000 } == true,
                ) { Text("Hinzuf\u00fcgen") }
            },
            dismissButton = { TextButton(onClick = vm::closeWaterCustom) { Text("Abbrechen") } },
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
