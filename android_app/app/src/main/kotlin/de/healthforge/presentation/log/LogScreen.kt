package de.healthforge.presentation.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.SymptomDefEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onOpenCharts: () -> Unit = {},
    onOpenEntry: (Long) -> Unit = {},
    vm: LogViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            scope.launch {
                snackbar.showSnackbar(it)
                vm.clearMessage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Symptom-Tagebuch") },
                actions = {
                    IconButton(onClick = onOpenCharts) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Charts")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { QuickAddCard(state, vm) }
            item {
                HorizontalDivider()
                Text(
                    "Verlauf",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.rows.isEmpty()) {
                item {
                    Text(
                        "Noch keine Einträge. Lege oben den ersten an.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(state.rows, key = { it.entry.id }) { row ->
                    EntryRow(row = row, onClick = { onOpenEntry(row.entry.id) })
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(state: LogUiState, vm: LogViewModel) {
    val draft = state.draft
    var symptomPickerOpen by remember { mutableStateOf(false) }
    var addSymptomOpen by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                todayLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text("Mood: ${draft.mood}/10", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = draft.mood.toFloat(),
                onValueChange = { vm.setMood(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )

            Text(
                "Schlafqualität: ${draft.sleepQuality?.let { "$it/5" } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { q ->
                    FilterChip(
                        selected = draft.sleepQuality == q,
                        onClick = { vm.setSleepQuality(if (draft.sleepQuality == q) null else q) },
                        label = { Text(q.toString()) },
                    )
                }
            }

            OutlinedTextField(
                value = draft.sleepHours,
                onValueChange = vm::setSleepHours,
                label = { Text("Schlafdauer (h)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                "Symptome",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (draft.selectedSymptoms.isEmpty()) {
                Text(
                    "Keine Symptome ausgewählt.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    draft.selectedSymptoms.forEach { (id, sev) ->
                        val name = state.symptoms.firstOrNull { it.id == id }?.name ?: "?"
                        SymptomSeverityChip(
                            name = name,
                            severity = sev,
                            onSeverity = { vm.setSeverity(id, it) },
                            onRemove = { vm.toggleSymptom(id) },
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

            Text(
                "Tags",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (draft.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

            Button(
                onClick = vm::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text("Speichern")
                }
            }
        }
    }

    if (symptomPickerOpen) {
        SymptomPickerDialog(
            available = state.symptoms,
            selectedIds = state.draft.selectedSymptoms.keys,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(row: EntryRowUi, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeLabel(row.entry.occurredAtEpochMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Text("· Mood ${row.entry.mood}/10", style = MaterialTheme.typography.bodySmall)
                if (!row.editable) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text("nur lesen") })
                }
            }
            if (row.symptomNames.isNotEmpty()) {
                Text(
                    row.symptomNames.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            row.entry.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
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

@Composable
internal fun SymptomSeverityChip(
    name: String,
    severity: Int,
    onSeverity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            (1..5).forEach { lvl ->
                FilterChip(
                    selected = severity == lvl,
                    onClick = { onSeverity(lvl) },
                    label = { Text(lvl.toString()) },
                    modifier = Modifier.padding(horizontal = 1.dp),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Entfernen")
            }
        }
    }
}

private fun todayLabel(): String =
    SimpleDateFormat("EEEE, dd.MM.", Locale.GERMAN).format(Date())

private fun timeLabel(epochMs: Long): String =
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN).format(Date(epochMs))
