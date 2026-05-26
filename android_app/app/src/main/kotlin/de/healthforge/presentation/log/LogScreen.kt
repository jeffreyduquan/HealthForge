package de.healthforge.presentation.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.SymptomDefEntity
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientButton
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SectionPill
import de.healthforge.presentation.theme.StatusGood
import de.healthforge.presentation.theme.StatusOverUl
import de.healthforge.presentation.theme.StatusRelax
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P6.S6 Slice B: Histamind Glass-Rewrite (AmbientBackdrop + GlassCard QuickAdd + GlassCard
 * EntryRows mit Severity-Bar). Mood/Sleep wurden in Slice A entfernt; pro Event existiert
 * jetzt nur noch eine Severity 1..5 (REQ-LOG-001..006).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    onOpenCharts: () -> Unit = {},
    onOpenEntry: (Long) -> Unit = {},
    vm: LogViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hm = LocalHmTokens.current

    LaunchedEffect(state.message) {
        state.message?.let {
            scope.launch {
                snackbar.showSnackbar(it)
                vm.clearMessage()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        GradientText(
                            text = "Symptom-Tagebuch",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            todayLabel(),
                            color = hm.fgSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(onClick = onOpenCharts) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Charts", tint = hm.fgSecondary)
                    }
                }
            }

            item { SectionPill(label = "SCHNELLEINTRAG") }
            item { QuickAddCard(state, vm) }

            item { SectionPill(label = "VERLAUF") }
            if (state.rows.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                        Text(
                            "Noch keine Einträge. Lege oben den ersten an.",
                            color = hm.fgSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                items(state.rows, key = { it.entry.id }) { row ->
                    EntryRow(row = row, onClick = { onOpenEntry(row.entry.id) })
                }
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.statusBarsPadding())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddCard(state: LogUiState, vm: LogViewModel) {
    val draft = state.draft
    val hm = LocalHmTokens.current
    var symptomPickerOpen by remember { mutableStateOf(false) }
    var addSymptomOpen by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Severity",
                    color = hm.fgPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${draft.severity}/5",
                    color = severityColor(draft.severity),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Slider(
                value = draft.severity.toFloat(),
                onValueChange = { vm.setSeverity(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
            )

            Text("Symptome", color = hm.fgSecondary, style = MaterialTheme.typography.labelMedium)
            if (draft.selectedSymptomIds.isEmpty()) {
                Text(
                    "Keine Symptome ausgewählt.",
                    color = hm.fgTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    draft.selectedSymptomIds.forEach { id ->
                        val name = state.symptoms.firstOrNull { it.id == id }?.name ?: "?"
                        AssistChip(
                            onClick = { vm.toggleSymptom(id) },
                            label = { Text(name) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { symptomPickerOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Symptom")
                }
                TextButton(onClick = { addSymptomOpen = true }) { Text("Eigenes anlegen") }
            }

            Text("Tags", color = hm.fgSecondary, style = MaterialTheme.typography.labelMedium)
            if (draft.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    draft.tags.forEach { t ->
                        AssistChip(
                            onClick = { vm.removeTag(t) },
                            label = { Text(t) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tag hinzufügen") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    vm.addTag(tagInput); tagInput = ""
                }) { Text("+") }
            }

            OutlinedTextField(
                value = draft.note,
                onValueChange = vm::setNote,
                label = { Text("Notiz") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            if (state.isSaving) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                }
            } else {
                GradientButton(text = "Speichern", onClick = vm::save)
            }
        }
    }

    if (symptomPickerOpen) {
        SymptomPickerDialog(
            available = state.symptoms,
            selectedIds = state.draft.selectedSymptomIds,
            onToggle = vm::toggleSymptom,
            onDismiss = { symptomPickerOpen = false },
        )
    }
    if (addSymptomOpen) {
        AddCustomSymptomDialog(
            onConfirm = { name ->
                vm.addCustomSymptom(name); addSymptomOpen = false
            },
            onDismiss = { addSymptomOpen = false },
        )
    }
}

@Composable
private fun EntryRow(row: EntryRowUi, onClick: () -> Unit) {
    val hm = LocalHmTokens.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        padding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor(row.entry.severity)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        timeLabel(row.entry.occurredAtEpochMs),
                        color = hm.fgPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Severity ${row.entry.severity}/5",
                        color = severityColor(row.entry.severity),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!row.editable) {
                        Spacer(Modifier.width(8.dp))
                        Text("· nur lesen", color = hm.fgTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (row.symptomNames.isNotEmpty()) {
                    Text(
                        row.symptomNames.joinToString(", "),
                        color = hm.fgSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                row.entry.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = hm.fgTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun severityColor(s: Int): Color = when (s) {
    1, 2 -> StatusGood
    3 -> StatusRelax
    else -> StatusOverUl
}

@Composable
private fun SymptomPickerDialog(
    available: List<SymptomDefEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Symptome wählen") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                items(available, key = { it.id }) { s ->
                    FilterChip(
                        selected = selectedIds.contains(s.id),
                        onClick = { onToggle(s.id) },
                        label = { Text(s.name) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
private fun AddCustomSymptomDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eigenes Symptom") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Anlegen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private fun todayLabel(): String =
    SimpleDateFormat("EEEE, dd.MM.", Locale.GERMAN).format(Date())

private fun timeLabel(epochMs: Long): String =
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN).format(Date(epochMs))
