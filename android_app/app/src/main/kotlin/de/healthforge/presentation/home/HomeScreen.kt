package de.healthforge.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val editableUseCase = IsIntakeEditableUseCase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HealthForge") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Verlauf")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::openQuickAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Hinzuf\u00fcgen") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateNavigator(date = s.date, onChange = vm::setDate)

            MacroRingRow(
                kcal = s.totals.kcal, kcalTarget = s.targets.kcal,
                proteinG = s.totals.proteinG, proteinTarget = s.targets.proteinG,
                carbsG = s.totals.carbsG, carbsTarget = s.targets.carbsG,
                fatG = s.totals.fatG, fatTarget = s.targets.fatG,
            )

            WaterTracker(
                currentMl = s.waterMl,
                goalMl = s.targets.waterMl,
                onAdd = vm::addWater,
                onCustom = vm::openWaterCustom,
                reminderEnabled = s.waterReminderEnabled,
                onReminderToggle = vm::setWaterReminderEnabled,
            )

            if (s.supplementChecklist.isNotEmpty()) {
                SupplementChecklist(
                    items = s.supplementChecklist,
                    onMarkTaken = vm::markSupplementTaken,
                )
            }

            Text("Heutige Eintr\u00e4ge", style = MaterialTheme.typography.titleMedium)
            if (s.entries.isEmpty()) {
                Text(
                    "Noch keine Eintr\u00e4ge. Tippe \u201eHinzuf\u00fcgen\u201c, um zu starten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Show max 5 latest; restliche via "Verlauf" (REQ-HOME-004).
                s.entries.take(5).forEach { e ->
                    IntakeRow(
                        entry = e,
                        editable = editableUseCase(e.loggedAt),
                        onDelete = { vm.deleteEntry(e.id) },
                    )
                }
                if (s.entries.size > 5) {
                    TextButton(onClick = onOpenHistory) {
                        Text("Alle ${s.entries.size} Eintr\u00e4ge anzeigen \u2192")
                    }
                }
            }
        }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.snapshotName, style = MaterialTheme.typography.bodyMedium)
                val portion = "${entry.portionGrams.toInt()} g"
                val kcal = entry.snapshotKcalPer100g?.let {
                    " \u00b7 ${(it * entry.portionGrams / 100.0).toInt()} kcal"
                } ?: ""
                Text("$portion$kcal", style = MaterialTheme.typography.bodySmall)
            }
            if (editable) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "L\u00f6schen")
                }
            } else {
                Text(
                    "\u00fcber 7 Tage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
